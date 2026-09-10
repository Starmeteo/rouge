package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.RoomNavigationSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 根据当前世界在远程光弹与快速影刃之间切换。 */
public final class PlayerAttackSystem {
    private final List<Projectile> projectiles = new ArrayList<>();
    private double cooldownRemaining;
    private double meleeVisibleRemaining;
    private double meleeAngleRadians;
    private int meleeAttackId;

    public void reset() {
        projectiles.clear();
        cooldownRemaining = 0.0;
        meleeVisibleRemaining = 0.0;
        meleeAngleRadians = 0.0;
        meleeAttackId = 0;
    }

    public void update(double dt) {
        update(dt, null);
    }

    public void update(double dt, RoomNavigationSystem navigation) {
        cooldownRemaining = Math.max(0.0, cooldownRemaining - dt);
        meleeVisibleRemaining = Math.max(0.0, meleeVisibleRemaining - dt);
        projectiles.forEach(projectile -> {
            double oldX = projectile.getX();
            double oldY = projectile.getY();
            projectile.update(dt);
            if (navigation != null && (!navigation.canProjectileOccupy(
                    projectile.getX(), projectile.getY(), projectile.getRadius(), projectile.getWorld())
                    || !navigation.isSegmentClear(oldX, oldY, projectile.getX(), projectile.getY(),
                    projectile.getRadius(), projectile.getWorld()))) projectile.expire();
        });
        projectiles.removeIf(Projectile::isExpired);
    }

    public void clearTransientAttacks() {
        projectiles.clear();
        meleeVisibleRemaining = 0.0;
    }

    public boolean tryAttack(Player player, double targetX, double targetY) {
        if (cooldownRemaining > 0.0) {
            return false;
        }
        double dx = targetX - player.getX();
        double dy = targetY - player.getY();
        double length = Math.hypot(dx, dy);
        if (length < 0.0001) {
            dx = 1.0;
            dy = 0.0;
            length = 1.0;
        }
        double unitX = dx / length;
        double unitY = dy / length;
        meleeAngleRadians = Math.atan2(unitY, unitX);
        if (player.getCurrentWorld() == WorldType.LIGHT) {
            if (!player.consumeAttackCharge()) return false;
            double offset = GameConfig.PLAYER_RADIUS + GameConfig.LIGHT_PROJECTILE_RADIUS + 3.0;
            projectiles.add(new Projectile(
                    player.getX() + unitX * offset, player.getY() + unitY * offset,
                    unitX * GameConfig.LIGHT_PROJECTILE_SPEED,
                    unitY * GameConfig.LIGHT_PROJECTILE_SPEED,
                    GameConfig.LIGHT_PROJECTILE_RADIUS, WorldType.LIGHT,
                    GameConfig.LIGHT_PROJECTILE_LIFETIME));
            cooldownRemaining = GameConfig.LIGHT_ATTACK_COOLDOWN;
        } else {
            if (!player.consumeAttackCharge()) return false;
            meleeVisibleRemaining = GameConfig.SHADOW_MELEE_VISIBLE_TIME;
            meleeAttackId++;
            cooldownRemaining = GameConfig.SHADOW_ATTACK_COOLDOWN;
        }
        return true;
    }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public boolean isMeleeVisible() { return meleeVisibleRemaining > 0.0; }
    public double getMeleeAngleRadians() { return meleeAngleRadians; }
    public int getMeleeAttackId() { return meleeAttackId; }
    public double getCooldownRemaining() { return cooldownRemaining; }
}
