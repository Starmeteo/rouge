/**
 * 控制器层：处理界面切换、场景流转（§9.1 controller；§9.2 负责 UI 事件、切换场景、调用 model）。
 *
 * <p>当前实现：
 * <ul>
 *   <li>{@link com.phantomcorridor.controller.SceneManager} —— 统一场景切换（§9.3）；</li>
 *   <li>{@link com.phantomcorridor.controller.LoginController} —— 登录校验与档案写入；</li>
 *   <li>{@link com.phantomcorridor.controller.MainMenuController} —— 菜单事件编排；</li>
 *   <li>{@link com.phantomcorridor.controller.GameController} —— 输入、模型更新与渲染调度。</li>
 * </ul>
 *
 * <p>控制器把 view 层事件转发为对 model 的调用；App 仅保留顶层场景流转。
 */
package com.phantomcorridor.controller;
