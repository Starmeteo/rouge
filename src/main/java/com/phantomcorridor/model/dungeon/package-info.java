/**
 * 地牢生成层：节点图地图生成（§9.1 model.dungeon）。
 *
 * <p>规划类（对应《双界行者》需求 §5.1 地图结构：节点图，树状/分支回廊分布）：
 * <ul>
 *   <li>{@code MapGenerator} —— 生成每层房间节点与连线（非纯网格）；</li>
 *   <li>{@code NodeGraph} —— 节点图结构，保证连通并可随机选路。</li>
 * </ul>
 *
 * <p>生成结果应保证：入口可达任意房间、最终房间生成 Boss 出口；
 * 生成算法仅产出数据，不触碰 JavaFX 节点（§9.2）。
 */
package com.phantomcorridor.model.dungeon;
