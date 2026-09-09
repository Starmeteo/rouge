package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.ItemType;
import com.phantomcorridor.model.WorldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 玩家运行时模型，不依赖任何 JavaFX 控件。 */
public final class Player {

    private int hp;
    private double x;
    private double y;
    private double phaseEnergy;
    private WorldType currentWorld;
    private PlayerAnimationState animationState;
    private double animationTime;
    private double facingX;
    private double facingY;
    private double hitInvulnerability;
    private final List<ItemType> items = new ArrayList<>();

    public Player(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        this.hp = GameConfig.PLAYER_MAX_HP;
        this.x = x;
        this.y = y;
        this.phaseEnergy = GameConfig.PHASE_ENERGY_INITIAL;
        this.currentWorld = WorldType.LIGHT;
        this.animationState = PlayerAnimationState.IDLE;
        this.animationTime = 0.0;
        this.facingX = 1.0;
        this.facingY = 0.0;
        this.hitInvulnerability = 0.0;
        this.items.clear();
    }

    public void move(double directionX, double directionY, double dt,
                     double minX, double minY, double maxX, double maxY) {
        double length = Math.hypot(directionX, directionY);
        if (length == 0.0) {
            return;
        }
        double speed = GameConfig.PLAYER_BASE_SPEED
                * (currentWorld == WorldType.SHADOW ? GameConfig.SHADOW_SPEED_MULTIPLIER : 1.0);
        x = clamp(x + directionX / length * speed * dt, minX, maxX);
        y = clamp(y + directionY / length * speed * dt, minY, maxY);
    }

    public void restorePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy + Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void consumePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy - Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void toggleWorld() {
        currentWorld = currentWorld == WorldType.LIGHT ? WorldType.SHADOW : WorldType.LIGHT;
    }

    public void updateAnimation(double dt, double movementX, double movementY, boolean attacking,
                                boolean shifting) {
        hitInvulnerability = Math.max(0.0, hitInvulnerability - Math.max(0.0, dt));
        animationTime += Math.max(0.0, dt);
        if (movementX != 0.0 || movementY != 0.0) {
            double length = Math.hypot(movementX, movementY);
            facingX = movementX / length;
            facingY = movementY / length;
        }
        PlayerAnimationState next = hp <= 0 ? PlayerAnimationState.DOWN
                : shifting ? PlayerAnimationState.SHIFTING
                : attacking ? PlayerAnimationState.ATTACKING
                : movementX != 0.0 || movementY != 0.0 ? PlayerAnimationState.MOVING
                : PlayerAnimationState.IDLE;
        if (next != animationState) animationTime = 0.0;
        animationState = next;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** 受击具有短暂无敌时间，避免一帧内被重叠弹幕重复扣血。 */
    public boolean takeDamage(int damage) {
        if (damage <= 0 || hitInvulnerability > 0.0 || hp <= 0) return false;
        hp = Math.max(0, hp - damage);
        hitInvulnerability = GameConfig.PLAYER_HIT_INVULNERABILITY;
        return true;
    }

    public int getHp() { return hp; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getPhaseEnergy() { return phaseEnergy; }
    public WorldType getCurrentWorld() { return currentWorld; }
    public PlayerAnimationState getAnimationState() { return animationState; }
    public double getAnimationTime() { return animationTime; }
    public double getFacingX() { return facingX; }
    public double getFacingY() { return facingY; }
    public List<ItemType> getItems() { return Collections.unmodifiableList(items); }
    public boolean isHitInvulnerable() { return hitInvulnerability > 0.0; }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
