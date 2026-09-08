/**
 * 核心运行支持：游戏主循环、界面状态管理。
 *
 * <p>对应新需求 §9.2「controller 处理界面切换、游戏循环流程」中的循环基础设施：
 * <ul>
 *   <li>{@link com.phantomcorridor.core.GameLoop} —— AnimationTimer 游戏主循环（固定时间步长）；</li>
 *   <li>{@link com.phantomcorridor.core.GameState} —— 界面状态（登录/主菜单/运行/暂停/结算）。</li>
 * </ul>
 *
 * <p>之所以独立出 core 而非并入 controller：主循环作为可复用的固定步长底座，
 * 同时被 controller（驱动）与 view（渲染）使用，独立成包避免循环依赖。
 */
package com.phantomcorridor.core;
