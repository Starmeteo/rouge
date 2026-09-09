/**
 * 敌人 AI 层：各类敌人工行为与 Boss 多阶段技能（§9.1 model.ai）。
 *
 * <p>规划类（对应《双界行者》需求 §6.3 典型普通敌人、§6.4 Boss 裂隙守望者）：
 * <ul>
 *   <li>{@code IdleAI} —— 待机/巡逻；</li>
 *   <li>{@code ChaseAI} —— 追踪（影狼冲刺、光魇缓慢追踪）；</li>
 *   <li>{@code AttackAI} —— 攻击（光魇追踪弹、光盾法师弹幕、Boss 扇形弹幕）。</li>
 * </ul>
 *
 * <p>AI 只修改 model 层状态（位置、朝向、攻击指令），不直接操作 JavaFX 节点（§9.2）。
 */
package com.phantomcorridor.model.ai;
