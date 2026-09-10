package com.phantomcorridor.model.dungeon;

import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.room.Direction;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.util.RandomUtil;
import java.util.*;

/** 生成无环、带分支且以 Boss 收尾的可复现房间节点图。 */
public final class MapGenerator {
    private static final RoomType[] RANDOM_TYPES = {RoomType.BATTLE, RoomType.REWARD, RoomType.SHOP, RoomType.EVENT};
    private static final double[] WEIGHTS = {RoomConfig.WEIGHT_BATTLE, RoomConfig.WEIGHT_REWARD,
            RoomConfig.WEIGHT_SHOP, RoomConfig.WEIGHT_EVENT};

    public DungeonMap generate(long seed) {
        Random random = new Random(seed);
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room(0, RoomType.ENTRANCE, 0, 0));
        Set<String> occupied = new HashSet<>();
        occupied.add("0,0");
        for (int id = 1; id < RoomConfig.DEFAULT_ROOM_COUNT; id++) {
            Room preferred = id == RoomConfig.DEFAULT_ROOM_COUNT - 1
                    ? rooms.stream().filter(room -> room.id() > 0 && hasFreeDirection(room, occupied))
                    .findFirst().orElseThrow()
                    : id <= 5 ? rooms.get(id - 1) : rooms.get(RandomUtil.nextInt(random, 1, id - 2));
            Room parent = hasFreeDirection(preferred, occupied) ? preferred : rooms.stream()
                    .filter(room -> hasFreeDirection(room, occupied)).findFirst().orElseThrow();
            Direction direction = findFreeDirection(random, parent, occupied);
            int x = parent.mapX() + direction.dx();
            int y = parent.mapY() + direction.dy();
            RoomType type = id == RoomConfig.DEFAULT_ROOM_COUNT - 1 ? RoomType.BOSS
                    : RANDOM_TYPES[RandomUtil.weightedIndex(random, WEIGHTS)];
            Room room = new Room(id, type, x, y, random.nextLong());
            parent.connect(direction, id);
            room.connect(direction.opposite(), parent.id());
            rooms.add(room);
            occupied.add(x + "," + y);
        }
        return new DungeonMap(rooms);
    }

    private boolean hasFreeDirection(Room room, Set<String> occupied) {
        return Arrays.stream(Direction.values()).anyMatch(direction ->
                !occupied.contains((room.mapX() + direction.dx()) + "," + (room.mapY() + direction.dy())));
    }

    private Direction findFreeDirection(Random random, Room parent, Set<String> occupied) {
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(directions, random);
        for (Direction direction : directions) {
            String key = (parent.mapX() + direction.dx()) + "," + (parent.mapY() + direction.dy());
            if (!occupied.contains(key) && !parent.hasDoor(direction)) return direction;
        }
        throw new IllegalStateException("地图生成无法找到空闲出口");
    }

    public static long parseSeed(String configuredSeed) {
        if (configuredSeed == null || configuredSeed.isBlank()) return new Random().nextLong();
        try { return Long.parseLong(configuredSeed.trim()); }
        catch (NumberFormatException ignored) { return configuredSeed.trim().hashCode(); }
    }
}
