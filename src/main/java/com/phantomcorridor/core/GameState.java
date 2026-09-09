package com.phantomcorridor.core;

/**
 * 应用级界面状态枚举（对应《双界行者》需求 §10 核心数据模型约定 GameState）。
 *
 * <p>用于标识当前应用所处的界面，由 {@link com.phantomcorridor.controller.SceneManager}
 * 驱动切换。状态流转（§4.1 界面流程）：
 * <pre>
 * LOGIN ──登录──▶ MAIN_MENU ──开始游戏──▶ PLAYING ──Esc/P──▶ PAUSED
 *                    ▲                        │   ▲                │
 *                    │                        └───┴──继续游戏──────┘
 *                    └────────── 回到主菜单（含 GAME_OVER 后返回） ──────┘
 * </pre>
 *
 * <p>注意：{@link #GAME_OVER} 结算状态与对应结算界面将在第 9 天界面完善阶段加入；
 * 枚举先保留占位，避免后续改动破坏 switch 语义。
 */
public enum GameState {

    /** 登录界面：应用启动后的首个场景（§8.1） */
    LOGIN,

    /** 主菜单：登录后可进入（§8.2） */
    MAIN_MENU,

    /** 游戏中：{@link GameLoop} 正常驱动逻辑更新与渲染 */
    PLAYING,

    /** 暂停中：游戏主循环已停止，显示暂停面板 */
    PAUSED,

    /** 结算（死亡/通关）：游戏中生命归零或击败最终 Boss 后进入（§8.5，第 9 天实现） */
    GAME_OVER
}
