package com.phantomcorridor.model.dungeon;

import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.RoomType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapGeneratorTest {
    @Test
    void generatesConnectedReproducibleMapWithEntranceAndBoss() {
        DungeonMap first = new MapGenerator().generate(4281L);
        DungeonMap second = new MapGenerator().generate(4281L);

        assertEquals(RoomConfig.DEFAULT_ROOM_COUNT, first.rooms().size());
        assertEquals(RoomType.ENTRANCE, first.rooms().getFirst().type());
        assertEquals(RoomType.BOSS, first.rooms().getLast().type());
        for (int i = 0; i < first.rooms().size(); i++) {
            assertEquals(first.rooms().get(i).type(), second.rooms().get(i).type());
            assertEquals(first.rooms().get(i).mapX(), second.rooms().get(i).mapX());
            assertEquals(first.rooms().get(i).mapY(), second.rooms().get(i).mapY());
            if (i > 0) assertFalse(first.rooms().get(i).neighbors().isEmpty());
        }
    }

    @Test
    void textSeedsAreStable() {
        assertEquals(MapGenerator.parseSeed("回廊-测试"), MapGenerator.parseSeed("回廊-测试"));
        assertEquals(42L, MapGenerator.parseSeed("42"));
    }

    @Test
    void manySeedsAlwaysProduceAValidTreeWithoutOverlappingNodes() {
        MapGenerator generator = new MapGenerator();
        for (long seed = 0; seed < 500; seed++) {
            long currentSeed = seed;
            DungeonMap map = generator.generate(seed);
            Set<String> coordinates = new HashSet<>();
            int directedEdgeCount = 0;

            for (var room : map.rooms()) {
                assertTrue(coordinates.add(room.mapX() + ":" + room.mapY()),
                        () -> "seed " + currentSeed + " generated overlapping nodes");
                directedEdgeCount += room.neighbors().size();
            }

            assertEquals(RoomConfig.DEFAULT_ROOM_COUNT, coordinates.size());
            assertEquals((RoomConfig.DEFAULT_ROOM_COUNT - 1) * 2, directedEdgeCount,
                    () -> "seed " + currentSeed + " did not generate a tree");
        }
    }
}
