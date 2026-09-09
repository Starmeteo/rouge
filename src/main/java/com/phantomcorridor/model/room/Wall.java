package com.phantomcorridor.model.room;

import com.phantomcorridor.model.WorldType;

/** 房间内轴对齐墙体；world 为 null 时代表两界共有实体墙。 */
public record Wall(double x, double y, double width, double height, WorldType world) {
    public boolean activeIn(WorldType currentWorld) { return world == null || world == currentWorld; }
}
