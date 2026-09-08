/**
 * 核心运行支持：游戏主循环、界面状态管理与视口坐标转换。
 *
 * <p>对应需求 §5.2 工程结构：
 * <ul>
 *   <li>{@link org.example.rouge.core.GameLoop} —— AnimationTimer 游戏主循环（固定时间步长）；</li>
 *   <li>{@link org.example.rouge.core.GameState} —— 界面状态（主菜单 / 运行 / 暂停）；</li>
 *   <li>{@link org.example.rouge.core.Settings} —— 全局设置（灵敏度 / 音量 / 开发种子，随功能逐步生效）；</li>
 *   <li>Camera —— 固定视口与坐标转换，后续里程碑（房间网格滚动）时加入。</li>
 * </ul>
 */
package org.example.rouge.core;
