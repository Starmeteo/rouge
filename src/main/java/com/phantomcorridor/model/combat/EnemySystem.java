package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Enemy;
import com.phantomcorridor.model.entity.EnemyKind;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.util.CollisionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 敌人生成、同界 AI、伤害以及清房门禁的纯逻辑系统。 */
public final class EnemySystem {
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<EnemyAttack> attacks = new ArrayList<>();
    private int activeRoomId = -1;
    private int killsSinceLastRead;

    public void reset() {
        enemies.clear();
        attacks.clear();
        activeRoomId = -1;
        killsSinceLastRead = 0;
    }

    /** 仅在未清理的战斗/Boss 房生成；Boss 房严格只生成一名首领。 */
    public void enterRoom(Room room, long dungeonSeed, Player player, RoomNavigationSystem navigation) {
        if (activeRoomId == room.id()) return;
        enemies.clear();
        attacks.clear();
        activeRoomId = room.id();
        if (room.isCleared() || (room.type() != RoomType.BATTLE && room.type() != RoomType.BOSS)) return;

        Random random = new Random(dungeonSeed ^ ((long) room.id() * 0x9E3779B97F4A7C15L));
        if (room.type() == RoomType.BOSS) {
            spawn(EnemyKind.WATCHER, WorldType.LIGHT, room, player, navigation, random);
            return;
        }
        int count = GameConfig.BATTLE_ENEMY_MIN
                + random.nextInt(GameConfig.BATTLE_ENEMY_MAX - GameConfig.BATTLE_ENEMY_MIN + 1);
        EnemyKind[] normals = {EnemyKind.LANTERN, EnemyKind.WOLF, EnemyKind.GOLEM, EnemyKind.MAGE};
        for (int i = 0; i < count; i++) {
            // 后段战斗房偶尔用一只精英替换普通怪，不改变 5~7 的总量。
            EnemyKind kind = i == count - 1 && room.id() >= 5 && random.nextDouble() < 0.35
                    ? (random.nextBoolean() ? EnemyKind.EXECUTIONER : EnemyKind.BELL)
                    : normals[random.nextInt(normals.length)];
            WorldType world = i % 2 == 0 ? WorldType.LIGHT : WorldType.SHADOW;
            spawn(kind, world, room, player, navigation, random);
        }
    }

    private void spawn(EnemyKind kind, WorldType world, Room room, Player player,
                       RoomNavigationSystem navigation, Random random) {
        for (int attempt = 0; attempt < 128; attempt++) {
            RoomArea area = room.areas().get(random.nextInt(room.areas().size()));
            double x = area.x() + 58 + random.nextDouble() * Math.max(1, area.width() - 116);
            double y = area.y() + 58 + random.nextDouble() * Math.max(1, area.height() - 116);
            if (Math.hypot(x - player.getX(), y - player.getY()) < 170) continue;
            if (navigation.canOccupy(x, y, 24, world)) {
                enemies.add(new Enemy(kind, world, x, y));
                return;
            }
        }
        // 找不到合法点时不生成，避免敌人出生在墙/障碍物内部。
    }

    public void update(double dt, Player player, PlayerAttackSystem playerAttacks,
                       RoomNavigationSystem navigation) {
        resolvePlayerHits(player, playerAttacks);
        enemies.removeIf(enemy -> {
            if (!enemy.isDead()) return false;
            killsSinceLastRead++;
            return true;
        });

        for (Enemy enemy : enemies) {
            // 首领在半血时从光界进入暗界，保留同一实体与血量。
            if (enemy.isBoss() && enemy.getHp() * 2 <= enemy.getMaxHp() && enemy.getWorld() == WorldType.LIGHT) {
                enemy.setWorld(WorldType.SHADOW);
                attacks.removeIf(attack -> attack.getSource() == EnemyKind.WATCHER);
            }
            if (enemy.getWorld() != player.getCurrentWorld()) continue;
            enemy.updateTimers(dt);
            double distance = Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());
            double detection = detectionRange(enemy.getKind());
            if (distance > detection || !navigation.isSegmentClear(enemy.getX(), enemy.getY(),
                    player.getX(), player.getY(), enemyRadius(enemy), enemy.getWorld())) continue;
            moveTowardPlayer(enemy, player, navigation, dt);
            if (enemy.canAttack() && distance <= attackRange(enemy.getKind())) fire(enemy, player);
        }
        updateEnemyAttacks(dt, player, navigation);
    }

    private void resolvePlayerHits(Player player, PlayerAttackSystem playerAttacks) {
        for (Projectile projectile : playerAttacks.getProjectiles()) {
            for (Enemy enemy : enemies) {
                if (!enemy.isDead() && projectile.getWorld() == enemy.getWorld()
                        && CollisionUtil.circleIntersectsCircle(projectile.getX(), projectile.getY(), projectile.getRadius(),
                        enemy.getX(), enemy.getY(), enemyRadius(enemy))) {
                    enemy.damage(player.getAttackDamage());
                    projectile.expire();
                    break;
                }
            }
        }
        if (player.getCurrentWorld() == WorldType.SHADOW && playerAttacks.isMeleeVisible()) {
            int attackId = playerAttacks.getMeleeAttackId();
            for (Enemy enemy : enemies) {
                if (enemy.getWorld() != WorldType.SHADOW || enemy.getLastMeleeHitId() == attackId) continue;
                if (Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY())
                        <= GameConfig.SHADOW_MELEE_RANGE + enemyRadius(enemy)) {
                    enemy.damage(player.getAttackDamage());
                    enemy.setLastMeleeHitId(attackId);
                }
            }
        }
    }

    private void moveTowardPlayer(Enemy enemy, Player player, RoomNavigationSystem navigation, double dt) {
        double dx = player.getX() - enemy.getX();
        double dy = player.getY() - enemy.getY();
        double distance = Math.hypot(dx, dy);
        double desiredDistance = switch (enemy.getKind()) {
            case LANTERN, MAGE, BELL, WATCHER -> 190.0;
            default -> 105.0;
        };
        if (distance <= desiredDistance || distance < 0.001) return;
        double step = GameConfig.PLAYER_BASE_SPEED * enemy.getKind().speedMultiplier() * dt;
        double radius = enemyRadius(enemy);
        double nx = enemy.getX() + dx / distance * step;
        double ny = enemy.getY() + dy / distance * step;
        if (navigation.canOccupy(nx, ny, radius, enemy.getWorld())) {
            enemy.setPosition(nx, ny);
            return;
        }
        // 简易局部寻路：沿障碍边缘尝试切向方向，避免直线撞墙后完全僵住。
        double tx = -dy / distance, ty = dx / distance;
        for (int sign : new int[]{1, -1}) {
            double sx = enemy.getX() + tx * sign * step;
            double sy = enemy.getY() + ty * sign * step;
            if (navigation.canOccupy(sx, sy, radius, enemy.getWorld())
                    && navigation.isSegmentClear(enemy.getX(), enemy.getY(), sx, sy, radius, enemy.getWorld())) {
                enemy.setPosition(sx, sy);
                return;
            }
        }
    }

    private void fire(Enemy enemy, Player player) {
        double dx = player.getX() - enemy.getX();
        double dy = player.getY() - enemy.getY();
        double length = Math.max(0.001, Math.hypot(dx, dy));
        double speed = GameConfig.ENEMY_PROJECTILE_SPEED * (enemy.isBoss() ? 1.32 : 1.0);
        attacks.add(new EnemyAttack(enemy.getX(), enemy.getY(), dx / length * speed, dy / length * speed,
                GameConfig.ENEMY_PROJECTILE_RADIUS + (enemy.isBoss() ? 5 : 0), enemy.getWorld(), enemy.getKind(),
                GameConfig.ENEMY_PROJECTILE_LIFETIME));
        enemy.setAttackCooldown(enemy.isBoss() ? 1.15 : 1.75 + enemy.getKind().ordinal() * 0.08);
    }

    private void updateEnemyAttacks(double dt, Player player, RoomNavigationSystem navigation) {
        for (EnemyAttack attack : attacks) {
            double oldX = attack.getX();
            double oldY = attack.getY();
            attack.update(dt);
            if (!navigation.canProjectileOccupy(attack.getX(), attack.getY(), attack.getRadius(), attack.getWorld())
                    || !navigation.isSegmentClear(oldX, oldY, attack.getX(), attack.getY(), attack.getRadius(), attack.getWorld())) attack.expire();
            if (!attack.isExpired() && attack.getWorld() == player.getCurrentWorld()
                    && CollisionUtil.circleIntersectsCircle(attack.getX(), attack.getY(), attack.getRadius(),
                    player.getX(), player.getY(), GameConfig.PLAYER_RADIUS)) {
                player.takeDamage(1);
                attack.expire();
            }
        }
        attacks.removeIf(EnemyAttack::isExpired);
    }

    /** 切界时不能留下看不见的旧世界伤害。 */
    public void onWorldChanged(WorldType currentWorld) { attacks.removeIf(attack -> attack.getWorld() != currentWorld); }
    public void spawnEventEnemies(Room room, long seed, Player player, RoomNavigationSystem navigation) {
        Random random = new Random(seed ^ room.id() * 0x51ED270BL);
        enemies.clear(); attacks.clear(); activeRoomId = room.id();
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            EnemyKind kind = i == 0 ? EnemyKind.WOLF : EnemyKind.LANTERN;
            spawn(kind, i % 2 == 0 ? WorldType.LIGHT : WorldType.SHADOW, room, player, navigation, random);
        }
    }
    public boolean isRoomCleared() { return enemies.isEmpty(); }
    public int getCount(WorldType world) { return (int) enemies.stream().filter(enemy -> enemy.getWorld() == world).count(); }
    public int consumeKills() { int result = killsSinceLastRead; killsSinceLastRead = 0; return result; }
    public List<Enemy> getEnemies() { return Collections.unmodifiableList(enemies); }
    public List<EnemyAttack> getAttacks() { return Collections.unmodifiableList(attacks); }
    private static double detectionRange(EnemyKind kind) {
        return switch (kind) {
            case LANTERN, MAGE -> 360.0;
            case WOLF -> 300.0;
            case GOLEM -> 240.0;
            case EXECUTIONER, BELL -> 430.0;
            case WATCHER -> 560.0;
        };
    }
    private static double attackRange(EnemyKind kind) {
        return switch (kind) {
            case LANTERN, MAGE, BELL, WATCHER -> 520.0;
            default -> 150.0;
        };
    }
    private static double enemyRadius(Enemy enemy) { return enemy.isBoss() ? 46.0 : enemy.getKind().elite() ? 34.0 : 27.0; }
}
