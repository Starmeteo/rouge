package com.phantomcorridor.config;

/**
 * 应用级配置与常量（对应新需求 §9.1 config 层 · AppConfig，窗口尺寸、标题、帧率）。
 *
 * <p>所有「魔法数值」集中在本层，禁止在代码中直接写死（需求 §9.2 架构原则）。
 * 本文件只放应用/窗口层面与帧率相关的常量；玩家属性与房间参数分别见
 * {@link GameConfig} 与 {@link RoomConfig}。
 */
public final class AppConfig {

    /** 窗口标题（双界主题） */
    public static final String APP_TITLE = "双界行者";

    /** 逻辑视口宽度（画布尺寸，固定逻辑分辨率，双界同一视口） */
    public static final int VIEW_WIDTH = 1280;

    /** 逻辑视口高度（画布尺寸，固定逻辑分辨率） */
    public static final int VIEW_HEIGHT = 960;

    /** 逻辑目标帧率（Hz，用于说明与调试展示） */
    public static final double TARGET_FPS = 60.0;

    /** 逻辑更新固定时间步长（秒）：1/60 即 60Hz 逻辑帧率 */
    public static final double FIXED_DT = 1.0 / TARGET_FPS;

    /** 工具类：不允许实例化 */
    private AppConfig() {
    }
}
