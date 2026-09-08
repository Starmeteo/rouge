/**
 * 房间层：房间数据与门禁逻辑（§9.1 model.room）。
 *
 * <p>核心类（对应《双界行者》需求 §5 地图与房间设计）：
 * <ul>
 *   <li>{@code Room} —— 房间；持有类型、连接关系、清理状态与相位墙（§5.3）；房间类型见
 *       {@link com.phantomcorridor.model.RoomType}；</li>
 *   <li>{@code RoomNavigationSystem} —— 处理墙体碰撞、门禁、跨房间切换与出生位置；</li>
 *   <li>{@code Direction}、{@code Wall} —— 描述房间方向和双世界墙体。</li>
 * </ul>
 *
 * <p>房间为纯数据逻辑，不含 JavaFX 节点；渲染由 view 层完成。
 */
package com.phantomcorridor.model.room;
