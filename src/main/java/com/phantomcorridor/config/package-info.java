/**
 * 配置与常量层（对应新需求 §9.1 config），遵循 §9.2 架构原则：
 * 「所有魔法数值集中管理，禁止代码中直接写死数值」。
 *
 * <ul>
 *   <li>{@link com.phantomcorridor.config.AppConfig} —— 窗口尺寸、标题、帧率；</li>
 *   <li>{@link com.phantomcorridor.config.GameConfig} —— 玩家属性、相位能量回复参数；</li>
 *   <li>{@link com.phantomcorridor.config.RoomConfig} —— 房间大小、房间类型权重；</li>
 *   <li>{@link com.phantomcorridor.config.Settings} —— 全局运行设置（灵敏度/音量/种子/昵称）。</li>
 * </ul>
 */
package com.phantomcorridor.config;
