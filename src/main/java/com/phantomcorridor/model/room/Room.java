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
    private final RoomShape shape;
    private final List<RoomArea> areas;
    private final List<Wall> walls;
    private boolean cleared;
    private boolean discovered;
    private boolean visited;

    public Room(int id, RoomType type, int mapX, int mapY) {
        this(id, type, mapX, mapY, Objects.hash(id, type, mapX, mapY));
    }

    public Room(int id, RoomType type, int mapX, int mapY, long layoutSeed) {
        this.id = id;
        this.type = type;
        this.mapX = mapX;
        this.mapY = mapY;
        this.cleared = type != RoomType.BATTLE && type != RoomType.BOSS;
        RoomLayout layout = RoomLayout.generate(type, layoutSeed);
        this.shape = layout.shape();
        this.areas = layout.areas();
        this.walls = layout.walls();
    }

    /** 供固定模板和几何测试直接提供轮廓与障碍。 */
    public Room(int id, RoomType type, int mapX, int mapY, RoomShape shape,
                List<RoomArea> areas, List<Wall> walls) {
        this.id = id;
        this.type = type;
        this.mapX = mapX;
        this.mapY = mapY;
        this.cleared = type != RoomType.BATTLE && type != RoomType.BOSS;
        this.shape = Objects.requireNonNull(shape);
        this.areas = List.copyOf(areas);
        this.walls = List.copyOf(walls);
        if (this.areas.isEmpty()) throw new IllegalArgumentException("房间至少需要一个可行走区域");
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
    public RoomShape shape() { return shape; }
    public List<RoomArea> areas() { return areas; }
    public boolean isDiscovered() { return discovered; }
    public boolean isVisited() { return visited; }
    public void discover() { discovered = true; }
    public void visit() { discovered = true; visited = true; }

    public double minX() { return areas.stream().mapToDouble(RoomArea::x).min().orElseThrow(); }
    public double minY() { return areas.stream().mapToDouble(RoomArea::y).min().orElseThrow(); }
    public double maxX() { return areas.stream().mapToDouble(a -> a.x() + a.width()).max().orElseThrow(); }
    public double maxY() { return areas.stream().mapToDouble(a -> a.y() + a.height()).max().orElseThrow(); }

    /** 返回指定边缘上最长区段的中心，确保门一定落在真实墙面上。 */
    public double doorCenter(Direction direction) {
        final double tolerance = 0.01;
        return switch (direction) {
            case NORTH -> areas.stream().filter(a -> Math.abs(a.y() - minY()) < tolerance)
                    .max(Comparator.comparingDouble(RoomArea::width))
                    .map(a -> a.x() + a.width() / 2.0).orElse((minX() + maxX()) / 2.0);
            case SOUTH -> areas.stream().filter(a -> Math.abs(a.y() + a.height() - maxY()) < tolerance)
                    .max(Comparator.comparingDouble(RoomArea::width))
                    .map(a -> a.x() + a.width() / 2.0).orElse((minX() + maxX()) / 2.0);
            case WEST -> areas.stream().filter(a -> Math.abs(a.x() - minX()) < tolerance)
                    .max(Comparator.comparingDouble(RoomArea::height))
                    .map(a -> a.y() + a.height() / 2.0).orElse((minY() + maxY()) / 2.0);
            case EAST -> areas.stream().filter(a -> Math.abs(a.x() + a.width() - maxX()) < tolerance)
                    .max(Comparator.comparingDouble(RoomArea::height))
                    .map(a -> a.y() + a.height() / 2.0).orElse((minY() + maxY()) / 2.0);
        };
    }
}
