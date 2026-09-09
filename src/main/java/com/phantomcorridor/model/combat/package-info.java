/**
 * 战斗逻辑层：子弹管理、伤害计算（§9.1 model.combat）。
 *
 * <p>规划类（对应《双界行者》需求 §6 敌人、§3.3 子弹）：
 * <ul>
 *   <li>{@code BulletManager} —— 统一管理所有子弹的生成、移动、回收（对象池，限制数量上限）；</li>
 *   <li>{@code DamageCalculator} —— 伤害结算；仅同世界弹/攻击可命中对应敌人（§3.2 核心矛盾）。</li>
 * </ul>
 *
 * <p>伤害与可见性规则（§6.2 敌人世界属性）在此收敛，避免四处硬编码。
 */
package com.phantomcorridor.model.combat;
