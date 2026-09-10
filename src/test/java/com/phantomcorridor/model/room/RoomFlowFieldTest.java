package com.phantomcorridor.model.room;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.dungeon.MapGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 房间距离场：敌人靠它绕开障碍走到玩家身边。 */
class RoomFlowFieldTest {

    @Test
    void enemyFacingAWallIsDirectedAroundIt() {
        RoomNavigationSystem navigation = navigationFor(roomWithWall(620, 360, 40, 340));
        RoomFlowField field = new RoomFlowField(navigation.getCurrentRoom());
        field.rebuild(navigation, 160, 480, WorldType.LIGHT, GameConfig.PLAYER_RADIUS);

        double[] direction = field.directionFrom(700, 480);

        assertNotNull(direction, "墙这一侧的敌人应当拿到绕行方向");
        assertTrue(Math.abs(direction[1]) > 0.3,
                "方向应当偏向上/下绕行，而不是继续顶着墙走");
        assertTrue(navigation.canOccupy(700 + direction[0] * 24, 480 + direction[1] * 24,
                GameConfig.PLAYER_RADIUS, WorldType.LIGHT), "绕行方向必须真的走得通");
    }

    @Test
    void farEnemyStillGetsADirectionThroughTheRoom() {
        RoomNavigationSystem navigation = navigationFor(roomWithWall(620, 360, 40, 340));
        RoomFlowField field = new RoomFlowField(navigation.getCurrentRoom());
        field.rebuild(navigation, 160, 480, WorldType.LIGHT, GameConfig.PLAYER_RADIUS);

        double[] direction = field.directionFrom(1100, 480);

        assertNotNull(direction, "隔着障碍但房间连通时应当始终有方向");
        assertTrue(navigation.canOccupy(1100 + direction[0] * 24, 480 + direction[1] * 24,
                GameConfig.PLAYER_RADIUS, WorldType.LIGHT));
    }

    @Test
    void enemySharingThePlayersCellGetsNoDirection() {
        RoomNavigationSystem navigation = navigationFor(roomWithWall(620, 360, 40, 340));
        RoomFlowField field = new RoomFlowField(navigation.getCurrentRoom());
        field.rebuild(navigation, 480, 480, WorldType.LIGHT, GameConfig.PLAYER_RADIUS);

        assertNull(field.directionFrom(486, 486), "已经在玩家所在格时不需要绕行方向");
    }

    @Test
    void enemyCutOffFromThePlayerGetsNoDirection() {
        // 整面墙把房间一分为二：右侧敌人与玩家不连通，不能给出假方向。
        RoomNavigationSystem navigation = navigationFor(roomWithWall(620, 0, 40, 960));
        RoomFlowField field = new RoomFlowField(navigation.getCurrentRoom());
        field.rebuild(navigation, 160, 480, WorldType.LIGHT, GameConfig.PLAYER_RADIUS);

        assertNull(field.directionFrom(1100, 480));
    }

    @Test
    void fieldOnlyNeedsRebuildingWhenThePlayerChangesCell() {
        RoomNavigationSystem navigation = navigationFor(roomWithWall(620, 360, 40, 340));
        RoomFlowField field = new RoomFlowField(navigation.getCurrentRoom());
        field.rebuild(navigation, 400, 400, WorldType.LIGHT, GameConfig.PLAYER_RADIUS);

        assertTrue(field.isTargeting(400, 400));
        assertTrue(field.isTargeting(410, 415));
        assertFalse(field.isTargeting(400, 470));
    }

    @Test
    void bossRoomsAlwaysLeaveTheBossAPathAroundTheirObstacles() {
        // 首领半径 46：房间布局必须保证它的距离场里没有走不到的死角，
        // 否则玩家把它顶到障碍物后面，它就再也追不上、也打不到人了。
        double bossRadius = 46.0;
        MapGenerator generator = new MapGenerator();
        int checked = 0;
        for (long seed = 0; seed < 60; seed++) {
            DungeonMap map = generator.generate(seed);
            Room bossRoom = map.rooms().stream()
                    .filter(room -> room.type() == RoomType.BOSS).findFirst().orElseThrow();
            RoomNavigationSystem navigation = navigationFor(bossRoom);
            double centerX = (bossRoom.minX() + bossRoom.maxX()) / 2.0;
            double centerY = (bossRoom.minY() + bossRoom.maxY()) / 2.0;
            double[] start = navigation.findNearestSafePosition(centerX, centerY, WorldType.LIGHT, bossRadius);
            assertNotNull(start, "首领房中央附近必须有首领站得下的位置");
            RoomFlowField field = new RoomFlowField(bossRoom);
            field.rebuild(navigation, start[0], start[1], WorldType.LIGHT, bossRadius);

            double cell = RoomConfig.NAV_CELL_SIZE / 2.0;
            int walkable = 0;
            for (int row = 0; row < field.rows(); row++) {
                for (int column = 0; column < field.columns(); column++) {
                    double x = field.centerOfColumn(column);
                    double y = field.centerOfRow(row);
                    if (!navigation.canOccupy(x, y, bossRadius, WorldType.LIGHT)) continue;
                    walkable++;
                    long currentSeed = seed;
                    assertTrue(field.isReachable(x, y),
                            () -> "种子 " + currentSeed + " 的首领房里有一块和玩家不连通的落脚点，首领会被卡住");
                }
            }
            assertTrue(walkable > 20, "首领房应当有足够的活动空间，实际可行走格 " + walkable);
            checked++;
        }
        assertEquals(60, checked);
    }

    private static Room roomWithWall(double x, double y, double width, double height) {
        return new Room(0, RoomType.BATTLE, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(0, 0, 1280, 960)), List.of(new Wall(x, y, width, height, null)));
    }

    private static RoomNavigationSystem navigationFor(Room room) {
        // 生成出来的房间带邻居，导航系统进入房间时会查找相邻节点，这里补上占位房间。
        List<Room> rooms = new ArrayList<>();
        rooms.add(room);
        for (int neighborId : room.neighbors().values()) {
            rooms.add(new Room(neighborId, RoomType.BATTLE, 0, 0));
        }
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(rooms));
        return navigation;
    }
}
