package com.phantomcorridor.model.room;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.RoomConfig;
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
        Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, RoomConfig.ROOM_INNER_PADDING);

        navigation.move(player, 0, -1, 0.1);

        assertEquals(1, navigation.getCurrentRoom().id());
        assertTrue(player.getY() > AppConfig.VIEW_HEIGHT / 2.0);
    }

    @Test
    void activePhaseWallBlocksOnlyMatchingWorld() {
        Room room = new Room(0, RoomType.ENTRANCE, 0, 0);
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));
        Player player = new Player(215, 400);

        navigation.move(player, 1, 0, 0.2);
        assertTrue(player.getX() < 235);

        player.toggleWorld();
        navigation.move(player, 1, 0, 0.2);
        assertEquals(WorldType.SHADOW, player.getCurrentWorld());
        assertTrue(player.getX() > 235);
    }

    @Test
    void unclearedBattleRoomKeepsItsDoorsLocked() {
        Room battle = new Room(0, RoomType.BATTLE, 0, 0);
        Room reward = new Room(1, RoomType.REWARD, 0, -1);
        battle.connect(Direction.NORTH, 1);
        reward.connect(Direction.SOUTH, 0);
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(battle, reward)));
        Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, RoomConfig.ROOM_INNER_PADDING);

        navigation.move(player, 0, -1, 0.1);

        assertEquals(0, navigation.getCurrentRoom().id());
        assertEquals(RoomConfig.ROOM_INNER_PADDING, player.getY());
    }
}
