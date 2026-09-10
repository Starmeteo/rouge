package com.phantomcorridor.model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.phantomcorridor.model.room.RoomNavigationSystem;

/** 管理敌方弹幕；第 4 天先提供脉冲清弹，第 6 天敌人攻击直接接入此容器。 */
public final class EnemyProjectileSystem {
    private final List<EnemyProjectile> projectiles = new ArrayList<>();

    public void reset() {
        projectiles.clear();
    }

    public void add(EnemyProjectile projectile) {
        if (projectile != null) projectiles.add(projectile);
    }

    public int clearWithin(double centerX, double centerY, double radius) {
        int before = projectiles.size();
        double radiusSquared = radius * radius;
        projectiles.removeIf(projectile -> {
            double dx = projectile.getX() - centerX;
            double dy = projectile.getY() - centerY;
            return projectile.isPulseClearable() && dx * dx + dy * dy <= radiusSquared;
        });
        return before - projectiles.size();
    }

    public void update(double dt) { projectiles.forEach(p -> p.update(dt)); projectiles.removeIf(EnemyProjectile::isExpired); }
    public void update(double dt, RoomNavigationSystem navigation) {
        projectiles.forEach(p -> { p.update(dt); if (navigation != null && !navigation.canProjectileOccupy(
                p.getX(), p.getY(), p.getRadius(), p.getWorld())) p.expire(); });
        projectiles.removeIf(EnemyProjectile::isExpired);
    }

    public List<EnemyProjectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }
}
