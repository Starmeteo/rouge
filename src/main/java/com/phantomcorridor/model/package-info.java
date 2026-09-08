/**
 * 数据模型层：纯 POJO 实体与游戏逻辑，不依赖任何 JavaFX 节点（§9.2 架构原则）。
 *
 * <p>按《双界行者》需求 §9.1 拆分为若干子包，各子包职责与实现天规划如下：
 * <ul>
 *   <li>{@link com.phantomcorridor.model.entity} —— 实体：Player / Enemy / Bullet / ItemPickup（§3.2、§6）；</li>
 *   <li>{@link com.phantomcorridor.model.room} —— 房间：Room / Door（§5.3）；{@link com.phantomcorridor.model.RoomType} 亦属房间概念；</li>
 *   <li>{@link com.phantomcorridor.model.dungeon} —— 地图生成：MapGenerator / NodeGraph（§5.1）；</li>
 *   <li>{@link com.phantomcorridor.model.combat} —— 战斗逻辑：BulletManager / DamageCalculator（§6.2）；</li>
 *   <li>{@link com.phantomcorridor.model.ai} —— 敌人 AI：IdleAI / ChaseAI / AttackAI（§6.3）；</li>
 *   <li>{@link com.phantomcorridor.model.effect} —— 道具效果：ItemEffect / WorldShiftEffect（§7）。</li>
 * </ul>
 *
 * <p>跨包共用数据契约（§10）：{@link com.phantomcorridor.model.WorldType}、
 * {@link com.phantomcorridor.model.RoomType}、{@link com.phantomcorridor.model.ItemType}。
 */
package com.phantomcorridor.model;
