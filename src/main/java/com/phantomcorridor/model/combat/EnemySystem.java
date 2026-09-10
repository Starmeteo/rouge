package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Enemy;
import com.phantomcorridor.model.entity.EnemyKind;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.RoomFlowField;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.util.CollisionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 敌人生成、同界 AI、伤害以及清房门禁的纯逻辑系统。
 *
 * <p>索敌规则：同界敌人只要与玩家同处一房就会锁定玩家并主动接近（见 {@link #detectionRange()}），
 * 被墙挡住视线时继续绕行接近、只有确实看得见玩家时才开火；玩家换界后敌人丢失目标，重新索敌。
 */
public final class EnemySystem {
    /**
     * 贴墙绕行的候选方向偏移（角度制），第 0 组对应 +1 侧、第 1 组对应 -1 侧。
     *
     * <p>先走切线，再朝同一侧逐步加大转角，最后允许直接后退；两侧互为镜像。
     */
    private static final double[][] SLIDE_OFFSETS_DEGREES = {
            {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180},
            {0, -22.5, -45, -67.5, -90, -112.5, -135, -157.5, 180}
    };

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<EnemyAttack> attacks = new ArrayList<>();
    private final EnumMap<WorldType, Map<Integer, RoomFlowField>> flowFields = new EnumMap<>(WorldType.class);
    private int activeRoomId = -1;
    private int killsSinceLastRead;

    public void reset() {
        enemies.clear();
        attacks.clear();
        flowFields.clear();
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
        Enemy enemy = new Enemy(kind, world, 0.0, 0.0);
        // 必须按物种真实身位校验：首领 46、精英 34 都比普通怪大，
        // 用统一的小半径放行会让它们出生就压在墙上，之后一步都走不动。
        double radius = enemyRadius(enemy);
        for (int attempt = 0; attempt < 128; attempt++) {
            RoomArea area = room.areas().get(random.nextInt(room.areas().size()));
            double x = area.x() + 58 + random.nextDouble() * Math.max(1, area.width() - 116);
            double y = area.y() + 58 + random.nextDouble() * Math.max(1, area.height() - 116);
            if (Math.hypot(x - player.getX(), y - player.getY()) < 170) continue;
            if (navigation.canOccupy(x, y, radius, world)) {
                enemy.setPosition(x, y);
                enemies.add(enemy);
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
            if (!acquireTarget(enemy, distance)) continue;
            // 索敌成功后即使隔着墙/障碍也会持续接近；只有真正看得见玩家时才开火。
            // 视线用弹体半径探测：判定的其实是“这条线上弹体能不能飞过去”。
            // 若用敌人自身半径（首领 46 像素），玩家只要站在只有更小身位放得下的位置，
            // 敌人就会判定“看不见”而一路贴到脸上也不开火。
            double fireRange = attackRange(enemy.getKind());
            boolean lineOfSight = distance <= fireRange
                    && navigation.isSegmentClear(enemy.getX(), enemy.getY(),
                    player.getX(), player.getY(), GameConfig.ENEMY_PROJECTILE_RADIUS, enemy.getWorld());
            moveTowardPlayer(enemy, player, navigation, dt, lineOfSight);
            if (lineOfSight && enemy.canAttack()) fire(enemy, player);
        }
        updateEnemyAttacks(dt, player, navigation);
    }

    /**
     * 索敌判定：描述敌人这一帧是否“发现”玩家。
     *
     * <p>整房索敌开启时（{@link GameConfig#ENEMY_AGGRO_WHOLE_ROOM}），同界敌人只要与玩家同处一房
     * 就会锁定并主动接近；锁定后不再受距离限制，会一直追到玩家换界或离开房间为止。
     */
    private static boolean acquireTarget(Enemy enemy, double distance) {
        if (enemy.isAware()) return true;
        if (distance > detectionRange()) return false;
        enemy.markAware();
        return true;
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

    private void moveTowardPlayer(Enemy enemy, Player player, RoomNavigationSystem navigation,
                                  double dt, boolean lineOfSight) {
        double dx = player.getX() - enemy.getX();
        double dy = player.getY() - enemy.getY();
        double distance = Math.hypot(dx, dy);
        // 看得见玩家时按物种保持开火站位；视线被遮挡时不再保持距离，一路贴近到重新获得视线。
        double desiredDistance = lineOfSight ? standoffDistance(enemy.getKind()) : 0.0;
        if (distance <= desiredDistance || distance < 0.001) return;
        double step = GameConfig.PLAYER_BASE_SPEED * enemy.getKind().speedMultiplier() * dt;
        double radius = enemyRadius(enemy);
        // 只有整段直线都走得通才走直线；被墙挡住时一律改走房间距离场，
        // 否则“这一帧直线能挪一点、下一帧被挡回原位”会精确抵消，敌人贴着墙永远走不出去。
        if (navigation.isPathClearTo(enemy.getX(), enemy.getY(), player.getX(), player.getY(),
                radius, enemy.getWorld())) {
            double nx = enemy.getX() + dx / distance * step;
            double ny = enemy.getY() + dy / distance * step;
            if (navigation.canOccupy(nx, ny, radius, enemy.getWorld())) {
                enemy.setPosition(nx, ny);
                enemy.setAvoidanceHeading(Math.atan2(dy, dx));
                enemy.clearAvoidance();
                return;
            }
        }
        if (stepAlongFlowField(enemy, player, navigation, step, radius)) return;
        slideAlongObstacle(enemy, navigation, dt, dx, dy, distance);
    }

    /**
     * 取该敌人对应的房间距离场，必要时重建。
     *
     * <p>按世界 + 身位半径缓存：首领（半径 46）过得去的缝和普通怪（27）不一样，
     * 用同一个半径建场会把大体型敌人引到它挤不过去的窄缝里，然后卡死在缝口。
     * 距离场只在玩家换格、换房间或换界时重建，每个身位档最多一张。
     */
    private RoomFlowField flowFieldFor(Enemy enemy, Player player, RoomNavigationSystem navigation,
                                       double radius) {
        Room room = navigation.getCurrentRoom();
        int radiusKey = (int) Math.round(radius);
        Map<Integer, RoomFlowField> byRadius =
                flowFields.computeIfAbsent(enemy.getWorld(), world -> new HashMap<>());
        RoomFlowField field = byRadius.get(radiusKey);
        if (field == null || field.roomId() != room.id()) {
            field = new RoomFlowField(room);
            byRadius.put(radiusKey, field);
        }
        if (!field.isTargeting(player.getX(), player.getY())) {
            field.rebuild(navigation, player.getX(), player.getY(), enemy.getWorld(), radius);
        }
        return field;
    }

    /** 沿房间距离场朝玩家推进一格方向；距离场给不出方向（或该方向仍被挡）时返回 false。 */
    private boolean stepAlongFlowField(Enemy enemy, Player player, RoomNavigationSystem navigation,
                                       double step, double radius) {
        RoomFlowField field = flowFieldFor(enemy, player, navigation, radius);
        // 先按前瞻方向走；被挡时退回只看相邻格的方向，再不行才交给贴墙绕行。
        if (stepAlong(enemy, navigation, field.directionFrom(enemy.getX(), enemy.getY()), step, radius)) return true;
        return stepAlong(enemy, navigation, field.neighbourDirectionFrom(enemy.getX(), enemy.getY()), step, radius);
    }

    private boolean stepAlong(Enemy enemy, RoomNavigationSystem navigation, double[] direction,
                              double step, double radius) {
        if (direction == null) return false;
        double nx = enemy.getX() + direction[0] * step;
        double ny = enemy.getY() + direction[1] * step;
        if (!navigation.canOccupy(nx, ny, radius, enemy.getWorld())) return false;
        if (!navigation.isSegmentClear(enemy.getX(), enemy.getY(), nx, ny, radius, enemy.getWorld())) return false;
        enemy.setPosition(nx, ny);
        enemy.setAvoidanceHeading(Math.atan2(direction[1], direction[0]));
        enemy.clearAvoidance();
        return true;
    }

    /**
     * 贴住障碍时的局部绕行。
     *
     * <p>绕行只沿选定的那一侧进行：先试与追击方向垂直的切线，再朝同一侧逐步加大旋转角度。
     * 这里有两件事必须记住，否则敌人会永久卡死：
     * <ul>
     *   <li>绕行方向不能每帧重挑，否则会在两个相邻位置之间来回横跳；</li>
     *   <li>不允许掉头，否则「直线朝玩家走一步 + 绕行退回原处」会精确抵消，位置纹丝不动。</li>
     * </ul>
     * 选定的一侧彻底走不动超过 {@link GameConfig#ENEMY_AVOIDANCE_FLIP_TIME} 秒时才改走另一侧。
     */
    private boolean slideAlongObstacle(Enemy enemy, RoomNavigationSystem navigation, double dt,
                                       double dx, double dy, double distance) {
        double radius = enemyRadius(enemy);
        double step = GameConfig.PLAYER_BASE_SPEED * enemy.getKind().speedMultiplier() * dt;
        double tangentAngle = Math.atan2(dx / distance, -dy / distance);
        int side = enemy.getAvoidanceSide();
        if (side == 0) {
            side = chooseAvoidanceSide(enemy, navigation, radius, tangentAngle);
            enemy.setAvoidanceSide(side);
        }
        double heading = enemy.getAvoidanceHeading();
        for (double offsetDegrees : SLIDE_OFFSETS_DEGREES[side > 0 ? 0 : 1]) {
            double angle = tangentAngle + Math.toRadians(offsetDegrees);
            if (!Double.isNaN(heading)
                    && Math.cos(angle - heading) < GameConfig.ENEMY_AVOIDANCE_MIN_TURN_COSINE) continue;
            double sx = enemy.getX() + Math.cos(angle) * step;
            double sy = enemy.getY() + Math.sin(angle) * step;
            if (!navigation.canOccupy(sx, sy, radius, enemy.getWorld())) continue;
            if (!navigation.isSegmentClear(enemy.getX(), enemy.getY(), sx, sy, radius, enemy.getWorld())) continue;
            enemy.setPosition(sx, sy);
            enemy.setAvoidanceHeading(angle);
            enemy.resetAvoidanceStuckTime();
            return true;
        }
        enemy.addAvoidanceStuckTime(dt);
        if (enemy.getAvoidanceStuckTime() >= GameConfig.ENEMY_AVOIDANCE_FLIP_TIME) {
            enemy.setAvoidanceSide(-side);
        }
        return false;
    }

    /**
     * 选择绕行方向：比较两侧「绕过障碍」方向上前方能走多远，取更开阔的一侧；一样开阔时固定取 +1。
     *
     * <p>只看切线本身分不出两侧——两侧的切线是同一条，区别在于接下来往哪边转。
     */
    private static int chooseAvoidanceSide(Enemy enemy, RoomNavigationSystem navigation,
                                           double radius, double tangentAngle) {
        int bestSide = 1;
        double bestClear = -1.0;
        for (int side : new int[]{1, -1}) {
            double angle = tangentAngle + side * Math.PI / 2.0;
            double clear = 0.0;
            for (double travelled = GameConfig.ENEMY_AVOIDANCE_PROBE_STEP;
                 travelled <= GameConfig.ENEMY_AVOIDANCE_PROBE_DISTANCE;
                 travelled += GameConfig.ENEMY_AVOIDANCE_PROBE_STEP) {
                double px = enemy.getX() + Math.cos(angle) * travelled;
                double py = enemy.getY() + Math.sin(angle) * travelled;
                if (!navigation.canOccupy(px, py, radius, enemy.getWorld())) break;
                clear = travelled;
            }
            if (clear > bestClear) {
                bestClear = clear;
                bestSide = side;
            }
        }
        return bestSide;
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

    /** 切界时不能留下看不见的旧世界伤害；离开当前世界的敌人清空索敌状态，回到该世界时重新索敌。 */
    public void onWorldChanged(WorldType currentWorld) {
        attacks.removeIf(attack -> attack.getWorld() != currentWorld);
        for (Enemy enemy : enemies) if (enemy.getWorld() != currentWorld) enemy.loseAwareness();
    }
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
    /**
     * 当前生效的索敌半径（像素）。
     *
     * <p>整房索敌开启时取「房间外接矩形对角线」，因此同界敌人只要与玩家同处一房就一定会参战，
     * 不再出现站得远就完全不动的情况；关闭时使用保守的固定半径。
     */
    public static double detectionRange() {
        return GameConfig.ENEMY_AGGRO_WHOLE_ROOM
                ? GameConfig.ENEMY_ROOM_AGGRO_RADIUS
                : GameConfig.ENEMY_DETECTION_RANGE;
    }

    private static double attackRange(EnemyKind kind) {
        return kind.ranged() ? GameConfig.ENEMY_RANGED_ATTACK_RANGE : GameConfig.ENEMY_MELEE_ATTACK_RANGE;
    }

    private static double standoffDistance(EnemyKind kind) {
        return kind.ranged() ? GameConfig.ENEMY_RANGED_STANDOFF_DISTANCE : GameConfig.ENEMY_MELEE_STANDOFF_DISTANCE;
    }

    private static double enemyRadius(Enemy enemy) { return enemy.isBoss() ? 46.0 : enemy.getKind().elite() ? 34.0 : 27.0; }
}
