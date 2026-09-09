package com.phantomcorridor.config;

/**
 * 游戏数值配置（对应新需求 §9.1 config 层 · GameConfig，玩家属性、能量回复参数）。
 *
 * <p>数值来源（《双界行者》项目需求说明书）：
 * <ul>
 *   <li>§3.2 玩家形态 —— 移动速度、攻击方式；</li>
 *   <li>§3.4 相位能量 —— 能量范围（0~100）、初始满值、切换清空、回复方式。</li>
 * </ul>
 *
 * <p>部分数值当前为设计占位值（如相位碎片回收量、脱战回复速率），
 * 将在对应系统（第 4 天能量、第 7 天掉落）落地时校准。
 */
public final class GameConfig {

    // ---- 玩家半径与移动（§3.2 光/影形态，§8.4 形态差异待世界切换系统接入） ----
    /** 玩家碰撞半径（像素） */
    /** 角色碰撞半径，覆盖披风/武器的主要身体范围。 */
    public static final double PLAYER_RADIUS = 28.0;

    /** 玩家基础移动速度（像素/秒，占位值，后续形体差异在此扩展） */
    public static final double PLAYER_BASE_SPEED = 200.0;

    /** 影形态额外移动速度加成倍率（§3.2 影形态"移动略快"，占位） */
    public static final double SHADOW_SPEED_MULTIPLIER = 1.15;

    // ---- 第 3 天：双形态攻击 ----
    public static final double LIGHT_PROJECTILE_SPEED = 560.0;
    public static final double LIGHT_PROJECTILE_RADIUS = 6.0;
    public static final double LIGHT_PROJECTILE_LIFETIME = 1.8;
    public static final double LIGHT_ATTACK_COOLDOWN = 0.22;
    public static final double SHADOW_MELEE_RANGE = 120.0;
    public static final double SHADOW_MELEE_ARC_DEGREES = 300.0;
    public static final double SHADOW_MELEE_VISIBLE_TIME = 0.13;
    public static final double SHADOW_ATTACK_COOLDOWN = 0.15;

    // ---- 玩家生命 ----
    /** 玩家最大生命值（需求未对双界版给出明确数值，沿用旧版 3 点作为占位，待 §7.2 道具加成时校调） */
    public static final int PLAYER_MAX_HP = 3;

    // ---- 相位能量（§3.4） ----
    /** 相位能量上限 */
    public static final double PHASE_ENERGY_MAX = 100.0;

    /** 初始相位能量（满格） */
    public static final double PHASE_ENERGY_INITIAL = 100.0;

    /** 每次切换世界所需能量（必须达到上限，切换后清空） */
    public static final double PHASE_ENERGY_PER_SWITCH = 100.0;

    /** 脱战后自动回复速率（点/秒，占位；§3.4"脱战一段时间后缓慢自动回复"） */
    public static final double PHASE_ENERGY_REGEN_PER_SEC = 8.0;

    /** 攻击命中获得充能（点/次，占位；§3.4"攻击/被击也能少量充能"） */
    public static final double PHASE_ENERGY_ON_ATTACK = 2.0;

    /** 被击获得充能（点/次，占位） */
    public static final double PHASE_ENERGY_ON_HIT = 4.0;

    /** 相位碎片拾取回复能量（点/个，占位；§3.4 与 §8.3 掉落） */
    public static final double PHASE_ENERGY_PER_FRAGMENT = 25.0;

    /** 切换世界后的冷却时间（秒，占位；§3.3"切换后进入冷却恢复期"） */
    public static final double WORLD_SWITCH_COOLDOWN = 0.6;

    /** 切界脉冲清除玩家周围敌方弹幕的半径。 */
    public static final double PHASE_PULSE_RADIUS = 118.0;

    /** 切界脉冲圆环的显示时间。 */
    public static final double PHASE_PULSE_VISIBLE_TIME = 0.32;

    /** 工具类：不允许实例化 */
    private GameConfig() {
    }
}
