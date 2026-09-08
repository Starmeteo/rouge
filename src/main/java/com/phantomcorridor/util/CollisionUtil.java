package com.phantomcorridor.util;

/**
 * 统一的几何碰撞工具（§9.1 util.CollisionUtil；无状态通用工具，§9.2 禁止持有游戏运行状态）。
 *
 * <p>对应《双界行者》需求 §5.5 / §9.2 碰撞检测：
 * 玩家与墙体/障碍物采用 AABB 或圆形碰撞、子弹与敌人圆形碰撞、玩家与道具矩形/圆形相交。
 * 所有判定集中在本类，不写入渲染代码。
 *
 * <p>统一坐标系为像素，圆心用 (cx, cy) 与半径 r，矩形用左上角 (rx, ry) 与宽高 (rw, rh)。
 * 双界通用：世界对齐/可见性不在此判定（由 {@code model.combat.DamageCalculator} 负责）。
 */
public final class CollisionUtil {

    /** 工具类：不允许实例化 */
    private CollisionUtil() {
    }

    /**
     * 圆与圆是否相交（两圆心距小于等于半径和）。
     *
     * @param x1 圆心1 X
     * @param y1 圆心1 Y
     * @param r1 圆1 半径
     * @param x2 圆心2 X
     * @param y2 圆心2 Y
     * @param r2 圆2 半径
     * @return 相交（含相切）返回 true
     */
    public static boolean circleIntersectsCircle(double x1, double y1, double r1,
                                                 double x2, double y2, double r2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double rr = r1 + r2;
        return dx * dx + dy * dy <= rr * rr;
    }

    /**
     * 圆与矩形（轴对齐 AABB）是否相交。
     *
     * <p>做法：找到矩形上离圆心最近的点，再判断该点是否在圆内。
     *
     * @param cx 圆心 X
     * @param cy 圆心 Y
     * @param r  半径
     * @param rx 矩形左上角 X
     * @param ry 矩形左上角 Y
     * @param rw 矩形宽
     * @param rh 矩形高
     * @return 相交（含相切）返回 true
     */
    public static boolean circleIntersectsRect(double cx, double cy, double r,
                                               double rx, double ry, double rw, double rh) {
        double nearestX = clamp(cx, rx, rx + rw);
        double nearestY = clamp(cy, ry, ry + rh);
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return dx * dx + dy * dy <= r * r;
    }

    /**
     * 点是否落在矩形（轴对齐 AABB）内。
     *
     * @param px 点 X
     * @param py 点 Y
     * @param rx 矩形左上角 X
     * @param ry 矩形左上角 Y
     * @param rw 矩形宽
     * @param rh 矩形高
     * @return 在矩形内（含边界）返回 true
     */
    public static boolean pointInRect(double px, double py,
                                      double rx, double ry, double rw, double rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    /** 将数值钳制到 [min, max] 闭区间 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
