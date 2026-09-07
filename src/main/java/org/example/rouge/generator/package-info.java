/**
 * 地图生成层：随机游走生成 5×5 网格房间、房间类型分配与连通性保证（BFS）。
 *
 * <p>对应需求 §3.2 地图与房间生成规则：
 * <ul>
 *   <li>{@code LevelGenerator} —— 随机游走生成 8~12 个相互连通房间；</li>
 *   <li>{@code RoomTemplateLibrary} —— 房间内部布局模板库（石块/柱子/箱子障碍物）。</li>
 * </ul>
 *
 * <p>按开发计划在第 4 天实现（房间生成网格、门与房间切换，里程碑 M1）。
 */
package org.example.rouge.generator;
