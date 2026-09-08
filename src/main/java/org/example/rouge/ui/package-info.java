/**
 * 界面层：JavaFX 菜单/暂停面板与 Canvas 游戏画面、HUD。
 *
 * <p>职责划分（需求 §5.4 渲染方案，对应需求书 §5.2 ui 包结构）：
 * <ul>
 *   <li>{@link org.example.rouge.ui.MainMenuPane} —— 主菜单（血月旅人主题）；</li>
 *   <li>{@link org.example.rouge.ui.GamePane} —— Canvas 游戏画面与游戏主循环驱动；</li>
 *   <li>{@link org.example.rouge.ui.PausePane} —— 暂停面板；</li>
 *   <li>HUD（第 6 天）、GameOverPane（第 8 天）将在对应里程碑实现。</li>
 * </ul>
 *
 * <p>界面流转由根包 {@code GameApplication} 编排；渲染逻辑与游戏模型解耦
 * （模型更新由 {@code core.GameLoop} 驱动）。
 */
package org.example.rouge.ui;
