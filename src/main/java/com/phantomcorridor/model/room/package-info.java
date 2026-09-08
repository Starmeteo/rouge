/**
 * 房间层：房间数据与门禁逻辑（§9.1 model.room）。
 *
 * <p>规划类（对应《双界行者》需求 §5 地图与房间设计）：
 * <ul>
 *   <li>{@code Room} —— 房间；持有类型、世界墙体、相位墙、敌人生成点（§5.3）；房间类型见
 *       {@link com.phantomcorridor.model.RoomType}；</li>
 *   <li>{@code Door} —— 门；战斗房清空敌人才开启（§5.4 房间清理规则）。</li>
 * </ul>
 *
 * <p>房间为纯数据逻辑，不含 JavaFX 节点；渲染由 view 层完成。
 */
package com.phantomcorridor.model.room;
