package com.phantomcorridor.config;

/**
 * 房间与地图配置（对应新需求 §9.1 config 层 · RoomConfig，房间大小、房间类型权重）。
 *
 * <p>数值来源（《双界行者》项目需求说明书）：
 * <ul>
 *   <li>§5.1 地图结构 —— 节点图（非网格），树状/分支回廊分布；</li>
 *   <li>§5.2 房间类型 —— 入口/战斗/奖励/商店/事件/Boss。</li>
 * </ul>
 *
 * <p>房间类型权重用于第 5 天地图生成算法（{@code model.dungeon.MapGenerator}）随机产生房间。
 * 此处提供默认权重，生成时按需归一化取用；「前 3 个房间必出道具」等规则由生成器实现（§7.3）。
 */
public final class RoomConfig {

    /** 玩家中心距离房间边缘的最小距离（像素）。 */
    public static final double ROOM_INNER_PADDING = 54.0;

    // ---- 房间可视区域大小（双界共用同一房间布局；全画布尺寸，内部走廊/障碍物由模板决定） ----
    /** 房间宽度（像素），与逻辑视口一致 */
    public static final double ROOM_WIDTH = AppConfig.VIEW_WIDTH;

    /** 房间高度（像素），与逻辑视口一致 */
    public static final double ROOM_HEIGHT = AppConfig.VIEW_HEIGHT;
    public static final int DEFAULT_ROOM_COUNT = 9;
    public static final double DOOR_HALF_WIDTH = 74.0;
    public static final double SHIFT_ESCAPE_SEARCH_RADIUS = 180.0;

    /** 宝箱相对房间北侧墙面的距离（像素）：渲染与交互判定共用，避免画在一处、判定在另一处。 */
    public static final double CHEST_OFFSET_Y = 76.0;

    /** 房间距离场（{@code RoomFlowField}）的格子边长（像素）：敌人用它绕开障碍接近玩家。 */
    public static final double NAV_CELL_SIZE = 40.0;

    /**
     * 商店房距离入口节点至少要隔几个房间。
     *
     * <p>开局第一间就是商店时玩家既没金币也没得选，所以入口附近只生成战斗/奖励/事件房。
     */
    public static final int SHOP_MIN_DEPTH = 3;

    // ---- 房间类型默认生成权重（第 5 天接入；权重越大越易生成） ----
    /** 入口房间权重（每层固定 1 个，权重仅供占位） */
    public static final double WEIGHT_ENTRANCE = 1.0;

    /** 战斗房权重（主力房间） */
    public static final double WEIGHT_BATTLE = 5.0;

    /** 奖励房权重 */
    public static final double WEIGHT_REWARD = 1.5;

    /** 商店权重 */
    public static final double WEIGHT_SHOP = 1.0;

    /** 事件房权重 */
    public static final double WEIGHT_EVENT = 1.2;

    /** Boss 房权重（每层固定 1 个，权重仅供占位） */
    public static final double WEIGHT_BOSS = 1.0;

    /** 工具类：不允许实例化 */
    private RoomConfig() {
    }
}
