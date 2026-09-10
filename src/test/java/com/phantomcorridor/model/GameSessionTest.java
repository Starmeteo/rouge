package com.phantomcorridor.model;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.combat.EnemyProjectile;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.entity.Enemy;
import com.phantomcorridor.model.room.Direction;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.RoomShape;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    @Test
    void worldShiftConsumesPulseAndClearsNearbyEnemyProjectiles() {
        GameSession session = new GameSession();
        session.newRun();
        double x = session.getPlayer().getX();
        double y = session.getPlayer().getY();
        session.getEnemyProjectiles().add(new EnemyProjectile(x + 20.0, y, WorldType.LIGHT));
        session.getEnemyProjectiles().add(new EnemyProjectile(
                x + GameConfig.PHASE_PULSE_RADIUS + 20.0, y, WorldType.LIGHT));

        assertTrue(session.tryShiftWorld());
        assertTrue(session.isPhasePulseVisible());
        assertEquals(1, session.getEnemyProjectiles().getProjectiles().size());
        assertFalse(session.getWorldShift().consumePulse(), "脉冲事件必须由会话层即时消费");
    }

    @Test
    void shopKeepsItsStockWhenThePlayerLeavesAndComesBack() {
        GameSession session = new GameSession();
        session.newRun("1");
        Room entrance = openRoom(0, RoomType.ENTRANCE, 0, 0);
        Room shop = openRoom(1, RoomType.SHOP, 0, -1);
        enterMap(session, entrance, shop);

        walkThroughDoor(session, Direction.NORTH);
        assertEquals(RoomType.SHOP, session.getNavigation().getCurrentRoom().type());
        int stock = session.getPickups().size();
        assertEquals(GameConfig.SHOP_OFFER_COUNT, stock, "商店第一次进入应当上架商品");
        assertTrue(session.getShopPrice(session.getPickups().getFirst()) > 0, "商品必须标价");

        // 出门再回来：货架既不能空掉，也不能重刷。
        walkThroughDoor(session, Direction.SOUTH);
        assertEquals(RoomType.ENTRANCE, session.getNavigation().getCurrentRoom().type());
        walkThroughDoor(session, Direction.NORTH);

        assertEquals(RoomType.SHOP, session.getNavigation().getCurrentRoom().type());
        assertEquals(stock, session.getPickups().size(), "离开商店再回来不能把货架清空");
    }

    @Test
    void defeatedBattleRoomNeverSpawnsEnemiesAgain() {
        GameSession session = new GameSession();
        session.newRun("1");
        Room entrance = openRoom(0, RoomType.ENTRANCE, 0, 0);
        Room battle = openRoom(1, RoomType.BATTLE, 0, -1);
        enterMap(session, entrance, battle);

        walkThroughDoor(session, Direction.NORTH);
        assertFalse(session.getEnemies().getEnemies().isEmpty(), "第一次进入战斗房应当刷怪");

        // 清空房间：门解锁、宝箱出现、房间被标记为已清空。
        for (Enemy enemy : new ArrayList<>(session.getEnemies().getEnemies())) enemy.damage(999);
        for (int frame = 0; frame < 5; frame++) update(session);
        assertTrue(session.getEnemies().getEnemies().isEmpty());
        assertTrue(session.getNavigation().getCurrentRoom().isCleared(), "清空后房间应当标记为已清空");
        assertTrue(session.isChestVisible(), "清空战斗房后应当出现宝箱");
        assertTrue(session.getNavigation().getCurrentRoom().hasRemainingLoot());

        walkThroughDoor(session, Direction.SOUTH);
        walkThroughDoor(session, Direction.NORTH);

        assertEquals(RoomType.BATTLE, session.getNavigation().getCurrentRoom().type());
        assertTrue(session.getEnemies().getEnemies().isEmpty(), "已击败的怪物房再次进入不能重新刷怪");
        assertTrue(session.isChestVisible(), "没开的宝箱不会因为进出房间消失");
        assertTrue(session.getNavigation().getCurrentRoom().isCleared());
    }

    private static void enterMap(GameSession session, Room entrance, Room second) {
        entrance.connect(Direction.NORTH, second.id());
        second.connect(Direction.SOUTH, entrance.id());
        session.getNavigation().reset(new DungeonMap(List.of(entrance, second)));
        session.getNavigation().placeAtEntrance(session.getPlayer());
        update(session);
    }

    /** 朝门口一直走，直到真的换了房间。 */
    private static void walkThroughDoor(GameSession session, Direction direction) {
        Room room = session.getNavigation().getCurrentRoom();
        double[] door = doorTarget(room, direction);
        for (int frame = 0; frame < 900; frame++) {
            if (session.getNavigation().getCurrentRoom() != room) return;
            double dx = door[0] - session.getPlayer().getX();
            double dy = door[1] - session.getPlayer().getY();
            session.update(AppConfig.FIXED_DT, dx, dy, dx, dy, false);
        }
        fail("一直朝 " + direction + " 走却没有换到下一间房");
    }

    private static double[] doorTarget(Room room, Direction direction) {
        return switch (direction) {
            case NORTH -> new double[]{room.doorCenter(Direction.NORTH), room.minY()};
            case SOUTH -> new double[]{room.doorCenter(Direction.SOUTH), room.maxY()};
            case WEST -> new double[]{room.minX(), room.doorCenter(Direction.WEST)};
            case EAST -> new double[]{room.maxX(), room.doorCenter(Direction.EAST)};
        };
    }

    private static void update(GameSession session) {
        session.update(AppConfig.FIXED_DT, 0, 0, session.getPlayer().getX(), session.getPlayer().getY(), false);
    }

    private static Room openRoom(int id, RoomType type, int mapX, int mapY) {
        return new Room(id, type, mapX, mapY, RoomShape.RECTANGLE,
                List.of(new RoomArea(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT)), List.of());
    }
}
