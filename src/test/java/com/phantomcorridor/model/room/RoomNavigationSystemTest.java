package com.phantomcorridor.model.room;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomNavigationSystemTest {
    @Test
    void openDoorMovesPlayerToConnectedRoom() {
        Room entrance = new Room(0, RoomType.ENTRANCE, 0, 0);
        Room reward = new Room(1, RoomType.REWARD, 0, -1);
        entrance.connect(Direction.NORTH, 1);
        reward.connect(Direction.SOUTH, 0);
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(entrance, reward)));
        Player player = new Player(entrance.doorCenter(Direction.NORTH),
                entrance.minY() + GameConfig.PLAYER_RADIUS);

        navigation.move(player, 0, -1, 0.1);

        assertEquals(1, navigation.getCurrentRoom().id());
        assertTrue(player.getY() > reward.maxY() / 2.0);
    }

    @Test
    void activePhaseWallBlocksOnlyMatchingWorld() {
        Room room = testRoom(RoomType.ENTRANCE,
                List.of(new Wall(300, 300, 40, 180, WorldType.LIGHT)));
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));
        Player player = new Player(280, 390);

        navigation.move(player, 1, 0, 0.2);
        assertTrue(player.getX() < 300);

        player.toggleWorld();
        navigation.move(player, 1, 0, 0.2);
        assertEquals(WorldType.SHADOW, player.getCurrentWorld());
        assertTrue(player.getX() > 300);
    }

    @Test
    void unclearedBattleRoomKeepsItsDoorsLocked() {
        Room battle = new Room(0, RoomType.BATTLE, 0, 0);
        Room reward = new Room(1, RoomType.REWARD, 0, -1);
        battle.connect(Direction.NORTH, 1);
        reward.connect(Direction.SOUTH, 0);
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(battle, reward)));
        Player player = new Player(battle.doorCenter(Direction.NORTH),
                battle.minY() + GameConfig.PLAYER_RADIUS);
        double startY = player.getY();

        navigation.move(player, 0, -1, 0.1);

        assertEquals(0, navigation.getCurrentRoom().id());
        assertEquals(startY, player.getY());
    }

    @Test
    void worldShiftFindsNearbySafePositionOutsideTargetPhaseWall() {
        Room room = testRoom(RoomType.ENTRANCE,
                List.of(new Wall(300, 300, 80, 180, WorldType.SHADOW)));
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));

        double[] safe = navigation.findNearestSafePosition(340, 390, WorldType.SHADOW);

        assertNotNull(safe);
        assertTrue(navigation.canOccupy(safe[0], safe[1], GameConfig.PLAYER_RADIUS, WorldType.SHADOW));
        assertTrue(Math.hypot(safe[0] - 340, safe[1] - 390) > 0);
    }

    private static Room testRoom(RoomType type, List<Wall> walls) {
        return new Room(0, type, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(120, 120, 1040, 720)), walls);
    }
}
