/**
 * 控制器层：处理界面切换、场景流转（§9.1 controller；§9.2 负责 UI 事件、切换场景、调用 model）。
 *
 * <p>当前实现：
 * <ul>
 *   <li>{@link com.phantomcorridor.controller.SceneManager} —— 统一场景切换（§9.3）；</li>
 *   <li>{@link com.phantomcorridor.controller.AppLauncher} 之外的运行时编排由
 *       {@code com.phantomcorridor.App}（JavaFX Application 主类）承担。</li>
 * </ul>
 *
 * <p>后续每个场景（登录/主菜单/游戏/结算）对应各自 Controller（LoginController 等）时，
 * 负责把 view 层的 UI 事件转发为对 model 的调用，并触发 {@link SceneManager} 切换。
 * 现阶段各场景面板较薄，直接通过回调交给 App/SceneManager 编排即可，避免过早拆分空壳类。
 */
package com.phantomcorridor.controller;
