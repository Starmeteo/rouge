/**
 * 地牢生成层：节点图地图生成（§9.1 model.dungeon）。
 *
 * <p>核心类（对应《双界行者》需求 §5.1 地图结构：节点图，树状/分支回廊分布）：
 * <ul>
 *   <li>{@code MapGenerator} —— 根据数字或文本种子生成可复现的连通树状地图；</li>
 *   <li>{@code DungeonMap} —— 保存房间节点、入口与节点查询。</li>
 * </ul>
 *
 * <p>生成结果应保证：入口可达任意房间、最终房间生成 Boss 出口；
 * 生成算法仅产出数据，不触碰 JavaFX 节点（§9.2）。
 */
package com.phantomcorridor.model.dungeon;
