package com.phantomcorridor.model.room;

import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;

import java.util.*;

/** 一个可进入房间的运行时数据。 */
public final class Room {
    private final int id;
    private final RoomType type;
    private final int mapX;
    private final int mapY;
    private final EnumMap<Direction, Integer> neighbors = new EnumMap<>(Direction.class);
    private final List<Wall> walls;
    private boolean cleared;

    public Room(int id, RoomType type, int mapX, int mapY) {
        this.id = id;
        this.type = type;
        this.mapX = mapX;
        this.mapY = mapY;
        this.cleared = type != RoomType.BATTLE && type != RoomType.BOSS;
        this.walls = List.of(
                new Wall(235, 245, 26, 310, WorldType.LIGHT),
                new Wall(1019, 405, 26, 310, WorldType.SHADOW));
    }

    public void connect(Direction direction, int roomId) { neighbors.put(direction, roomId); }
    public Integer neighbor(Direction direction) { return neighbors.get(direction); }
    public boolean hasDoor(Direction direction) { return neighbors.containsKey(direction); }
    public boolean isDoorOpen(Direction direction) { return cleared && hasDoor(direction); }
    public int id() { return id; }
    public RoomType type() { return type; }
    public int mapX() { return mapX; }
    public int mapY() { return mapY; }
    public Map<Direction, Integer> neighbors() { return Collections.unmodifiableMap(neighbors); }
    public List<Wall> walls() { return walls; }
    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }
}
