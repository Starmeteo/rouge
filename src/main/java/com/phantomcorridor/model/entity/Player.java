package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.ItemType;
import com.phantomcorridor.model.EquipmentType;
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
    private int attackCharges;
    private int maxAttackCharges;
    private double attackChargeRecoveryTimer;
    private WorldType currentWorld;
    private PlayerAnimationState animationState;
    private double animationTime;
    private double facingX;
    private double facingY;
    private double hitInvulnerability;
    private final List<ItemType> items = new ArrayList<>();
    private final List<EquipmentType> equipment = new ArrayList<>();

    public Player(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        this.hp = GameConfig.PLAYER_MAX_HP;
        this.x = x;
        this.y = y;
        this.phaseEnergy = GameConfig.PHASE_ENERGY_INITIAL;
        this.maxAttackCharges = GameConfig.ATTACK_CHARGE_MAX;
        this.attackCharges = maxAttackCharges;
        this.attackChargeRecoveryTimer = 0.0;
        this.currentWorld = WorldType.LIGHT;
        this.animationState = PlayerAnimationState.IDLE;
        this.animationTime = 0.0;
        this.facingX = 1.0;
        this.facingY = 0.0;
        this.hitInvulnerability = 0.0;
        this.items.clear();
        this.equipment.clear();
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

    public void restoreAttackCharges(int amount) { attackCharges = Math.min(maxAttackCharges, attackCharges + Math.max(0, amount)); }
    public void updateAttackCharges(double dt) {
        if (attackCharges >= maxAttackCharges) { attackChargeRecoveryTimer = 0.0; return; }
        attackChargeRecoveryTimer += Math.max(0.0, dt);
        while (attackChargeRecoveryTimer >= GameConfig.ATTACK_CHARGE_RECOVERY_TIME
                && attackCharges < maxAttackCharges) {
            attackCharges++;
            attackChargeRecoveryTimer -= GameConfig.ATTACK_CHARGE_RECOVERY_TIME;
        }
    }
    public void increaseAttackChargeCapacity(int amount) {
        maxAttackCharges = Math.max(GameConfig.ATTACK_CHARGE_MAX, maxAttackCharges + Math.max(0, amount));
        attackCharges = maxAttackCharges;
    }
    public void addItem(ItemType item) {
        if (item == null || items.contains(item)) return;
        items.add(item);
        if (item == ItemType.UNIVERSAL || item == ItemType.DUAL) increaseAttackChargeCapacity(1);
    }
    public void equip(EquipmentType item) {
        if (item == null || equipment.contains(item)) return;
        equipment.removeIf(existing -> existing.affinity().equals(item.affinity())
                && !existing.affinity().equals("双界") && !existing.affinity().equals("通用"));
        equipment.add(item);
        while (equipment.size() > 3) equipment.remove(0);
    }
    public List<EquipmentType> getEquipment() { return Collections.unmodifiableList(equipment); }
    public boolean consumeAttackCharge() {
        if (attackCharges <= 0) return false;
        attackCharges--; return true;
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
    public void restoreHealth(int amount) { hp = Math.min(GameConfig.PLAYER_MAX_HP, hp + Math.max(0, amount)); }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getPhaseEnergy() { return phaseEnergy; }
    public int getAttackCharges() { return attackCharges; }
    public int getMaxAttackCharges() { return maxAttackCharges; }
    public int getAttackDamage() {
        int bonus = items.stream().mapToInt(item -> item == ItemType.DUAL || item == ItemType.UNIVERSAL
                || (currentWorld == WorldType.LIGHT && item == ItemType.LIGHT)
                || (currentWorld == WorldType.SHADOW && item == ItemType.SHADOW) ? 1 : 0).sum();
        bonus += equipment.stream().mapToInt(item -> item == EquipmentType.DAWN_WAND && currentWorld == WorldType.LIGHT ? 1
                : item == EquipmentType.SHADOW_FANG && currentWorld == WorldType.SHADOW ? 1
                : item == EquipmentType.RIFT_TWINBLADE || item == EquipmentType.DAWN_SEAL && currentWorld == WorldType.LIGHT ? 1
                : 0).sum();
        return 1 + bonus;
    }
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
