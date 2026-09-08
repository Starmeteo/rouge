/**
 * 视图层：JavaFX 场景面板（§9.1 view；§9.2 只负责 JavaFX 控件的布局与绘制，禁止写业务判断逻辑）。
 *
 * <ul>
 *   <li>{@link com.phantomcorridor.view.LoginView} —— 登录界面（§8.1）；</li>
 *   <li>{@link com.phantomcorridor.view.MainMenuView} —— 主菜单（§8.2，双界主题）；</li>
 *   <li>{@link com.phantomcorridor.view.GameView} —— Canvas 容器、输入转发和切界视觉反馈；</li>
 *   <li>{@link com.phantomcorridor.view.GameRenderer} —— 游戏状态的纯绘制；</li>
 *   <li>{@link com.phantomcorridor.view.DualWorldBackdrop} —— 登录/菜单动态回廊背景；</li>
 *   <li>{@link com.phantomcorridor.view.PauseView} —— 暂停面板（§4.1）；</li>
 *   <li>{@link com.phantomcorridor.view.SettingsOverlay} —— 设置覆盖层。</li>
 * </ul>
 *
 * <p>基础 HUD 已接入，GameOverView 将在对应里程碑加入。场景流转由
 * {@code com.phantomcorridor.App} 与 {@code controller.SceneManager} 编排；
 * 渲染逻辑与游戏模型解耦（模型更新由 GameController 驱动）。
 */
package com.phantomcorridor.view;
