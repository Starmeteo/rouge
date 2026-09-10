package com.phantomcorridor.model.room;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import org.junit.jupiter.api.Test;

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

    private static Room roomWithWall(double x, double y, double width, double height) {
        return new Room(0, RoomType.BATTLE, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(0, 0, 1280, 960)), List.of(new Wall(x, y, width, height, null)));
    }

    private static RoomNavigationSystem navigationFor(Room room) {
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));
        return navigation;
    }
}
