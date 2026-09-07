/**
 * 游戏数据模型层：纯 POJO 实体，不依赖任何 JavaFX 节点，
 * 符合需求 §5.3"模型与渲染分离"的架构原则。
 *
 * <p>规划实体（按需求 §5.2，在对应开发天逐步实现）：
 * {@code Player}（玩家，第 2 天）、{@code Enemy}（敌人，第 3 天）、
 * {@code Bullet}（子弹，第 3 天）、{@code Item}（道具，第 6 天）、
 * {@code Room}（房间，第 4 天）、{@code Level}（层，第 7 天）、
 * {@code Stats}（属性集合，第 2 天）。
 */
package org.example.rouge.model;
