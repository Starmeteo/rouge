package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.entity.EnemyKind;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnemySystemTest {
    @Test
    void battleRoomSpawnsFiveToSevenEnemiesAcrossBothWorlds() {
        Room room = new Room(3, RoomType.BATTLE, 0, 0);
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
        EnemySystem system = new EnemySystem();

        system.enterRoom(room, 42L, player, navigation);

        int count = system.getEnemies().size();
        assertTrue(count >= GameConfig.BATTLE_ENEMY_MIN && count <= GameConfig.BATTLE_ENEMY_MAX);
        assertTrue(system.getCount(WorldType.LIGHT) > 0);
        assertTrue(system.getCount(WorldType.SHADOW) > 0);
        assertFalse(room.isCleared());
    }

    @Test
    void bossRoomSpawnsOnlyTheWatcher() {
        Room room = new Room(8, RoomType.BOSS, 0, 0);
        RoomNavigationSystem navigation = navigationFor(room);
        EnemySystem system = new EnemySystem();

        system.enterRoom(room, 7L, new Player(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0), navigation);

        assertEquals(1, system.getEnemies().size());
        assertEquals(EnemyKind.WATCHER, system.getEnemies().getFirst().getKind());
        assertEquals(WorldType.LIGHT, system.getEnemies().getFirst().getWorld());
    }

    private static RoomNavigationSystem navigationFor(Room room) {
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));
        return navigation;
    }
}
