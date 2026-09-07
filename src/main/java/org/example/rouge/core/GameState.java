package org.example.rouge.core;

/**
 * 应用级界面状态枚举。
 *
 * <p>用于标识当前应用所处的界面，由界面层（{@link org.example.rouge.GameApplication}）驱动切换。
 * 状态流转（需求 §4.1 界面流程，第一天实现主菜单/运行/暂停三个状态）：
 * <pre>
 * MAIN_MENU ──开始游戏──▶ PLAYING ──Esc/P──▶ PAUSED
 *     ▲                      │    ▲             │
 *     │                      └────┴──继续游戏───┘
 *     └──────────────回到主菜单──────────────────┘
 * </pre>
 *
 * <p>GAME_OVER / VICTORY 等结算状态将在第 8 天界面完善时加入。
 */
public enum GameState {

    /** 主菜单：应用启动后的默认界面 */
    MAIN_MENU,

    /** 游戏中：{@link GameLoop} 正常驱动逻辑更新与渲染 */
    PLAYING,

    /** 暂停中：游戏主循环已停止，显示暂停面板 */
    PAUSED
}
