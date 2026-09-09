/**
 * 实体层：纯数据实体（§9.1 model.entity）。
 *
 * <p>实体（对应《双界行者》需求 §3.2 玩家形态、§6 敌人、§3.3 子弹、§7.3 道具拾取）：
 * <ul>
 *   <li>{@link com.phantomcorridor.model.entity.Player} —— 已实现玩家位置、生命、能量、世界和道具契约；</li>
 *   <li>{@code Enemy} —— 敌人；至少含 hp / speed / damage / world / state（§10）；</li>
 *   <li>{@code Bullet} —— 子弹（光弹/暗影斩击/敌方弹，含所属世界）；</li>
 *   <li>{@code ItemPickup} —— 道具与相位碎片掉落拾取物。</li>
 * </ul>
 *
 * <p>所有实体为纯 POJO，不 import 任何 JavaFX 控件（§9.2 禁止事项）。
 */
package com.phantomcorridor.model.entity;
