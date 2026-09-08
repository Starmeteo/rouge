/**
 * 视图层：JavaFX 场景面板（§9.1 view；§9.2 只负责 JavaFX 控件的布局与绘制，禁止写业务判断逻辑）。
 *
 * <ul>
 *   <li>{@link com.phantomcorridor.view.LoginView} —— 登录界面（§8.1）；</li>
 *   <li>{@link com.phantomcorridor.view.MainMenuView} —— 主菜单（§8.2，双界主题）；</li>
 *   <li>{@link com.phantomcorridor.view.GameView} —— Canvas 游戏画面与游戏主循环驱动（§5.4）；</li>
 *   <li>{@link com.phantomcorridor.view.PauseView} —— 暂停面板（§4.1）；</li>
 *   <li>{@link com.phantomcorridor.view.SettingsOverlay} —— 设置覆盖层。</li>
 * </ul>
 *
 * <p>HUD、GameOverView 将在对应里程碑（第 6 / 9 天）加入。场景流转由
 * {@code com.phantomcorridor.App} 与 {@code controller.SceneManager} 编排；
 * 渲染逻辑与游戏模型解耦（模型更新由 {@code core.GameLoop} 驱动）。
 */
package com.phantomcorridor.view;
