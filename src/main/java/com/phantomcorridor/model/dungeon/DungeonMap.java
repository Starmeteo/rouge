package com.phantomcorridor.model.dungeon;

import com.phantomcorridor.model.room.Room;
import java.util.*;

/** 一层回廊的节点图。 */
public final class DungeonMap {
    private final List<Room> rooms;
    public DungeonMap(List<Room> rooms) { this.rooms = List.copyOf(rooms); }
    public List<Room> rooms() { return rooms; }
    public Room entrance() { return rooms.getFirst(); }
    public Room room(int id) { return rooms.stream().filter(r -> r.id() == id).findFirst().orElseThrow(); }
}
