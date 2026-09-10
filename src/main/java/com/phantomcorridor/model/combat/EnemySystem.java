package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import java.util.*;

public final class EnemySystem {
    private final List<Enemy> enemies = new ArrayList<>();
    public void reset() { enemies.clear(); }
    public void enterRoom(Room room) {
        enemies.clear(); if (room.type()==RoomType.ENTRANCE || room.type()==RoomType.REWARD || room.type()==RoomType.SHOP) return;
        java.util.Random random = new java.util.Random(room.id() * 7919L + room.mapX() * 97L);
        int count = room.type()==RoomType.BOSS ? 1 : 3 + random.nextInt(3);
        EnemyType[] normal = {EnemyType.LANTERN, EnemyType.WOLF, EnemyType.GOLEM, EnemyType.MAGE};
        for (int i = 0; i < count; i++) {
            EnemyType type = room.type()==RoomType.BOSS ? EnemyType.WATCHER
                    : normal[Math.floorMod(room.id() + i + random.nextInt(2), normal.length)];
            WorldType world = i % 2 == 0 ? WorldType.LIGHT : WorldType.SHADOW;
            double angle = i * Math.PI * 2.0 / count;
            double distance = 170 + random.nextInt(100);
            enemies.add(new Enemy(type, world, 640 + Math.cos(angle) * distance, 480 + Math.sin(angle) * distance));
        }
    }
    public void update(double dt, Player player, RoomNavigationSystem navigation, EnemyProjectileSystem projectiles) {
        enemies.forEach(e -> e.update(dt, player, navigation, projectiles)); enemies.removeIf(e -> e.state()==Enemy.State.DEAD);
    }
    public List<Enemy> getEnemies(){ return Collections.unmodifiableList(enemies); }
    public int count(WorldType world){ return (int)enemies.stream().filter(e->e.world()==world).count(); }

    /** 结算玩家攻击，严格限制在当前世界和当前房间内。 */
    public void resolvePlayerAttacks(Player player, PlayerAttackSystem attacks) {
        for (Projectile projectile : attacks.getProjectiles()) {
            if (projectile.isExpired() || projectile.getWorld() != player.getCurrentWorld()) continue;
            for (Enemy enemy : enemies) {
                if (enemy.world() == projectile.getWorld()
                        && Math.hypot(enemy.getX() - projectile.getX(), enemy.getY() - projectile.getY())
                        < 26 + projectile.getRadius()) {
                    enemy.damage(1);
                    projectile.expire();
                    break;
                }
            }
        }
        if (player.getCurrentWorld() == WorldType.SHADOW && attacks.isMeleeVisible()) {
            double angle = attacks.getMeleeAngleRadians();
            for (Enemy enemy : enemies) {
                double dx = enemy.getX() - player.getX(), dy = enemy.getY() - player.getY();
                double distance = Math.hypot(dx, dy);
                double delta = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (distance < 132 && Math.abs(delta) < Math.toRadians(150)) enemy.damage(1);
            }
        }
    }
}
