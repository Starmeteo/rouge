package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.dungeon.MapGenerator;
import com.phantomcorridor.model.entity.Enemy;
import com.phantomcorridor.model.entity.EnemyKind;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.model.room.RoomShape;
import com.phantomcorridor.model.room.Wall;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EnemySystemTest {
    private static final double DT = AppConfig.FIXED_DT;

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

    @Test
    void detectionRangeCoversTheWholeRoom() {
        double roomDiagonal = Math.hypot(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);

        assertTrue(EnemySystem.detectionRange() >= roomDiagonal,
                "索敌范围必须覆盖房间对角线，否则对角位置的敌人永远不会参战");
    }

    @Test
    void enemyAcrossTheRoomLocksOnAndClosesIn() {
        Room room = openRoom(3, RoomType.BATTLE);
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(180, 800);
        EnemySystem system = new EnemySystem();
        system.enterRoom(room, 42L, player, navigation);
        Enemy enemy = soleEnemy(system, WorldType.LIGHT, player, navigation);
        enemy.setPosition(1120, 120);

        double before = Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());
        assertTrue(before > 900, "测试前提：敌人开局远在房间另一头");

        runFor(system, player, navigation, 1.0);

        double after = Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());
        assertTrue(enemy.isAware(), "远处的同界敌人应当锁定玩家");
        assertTrue(after < before - 20, "锁定后应当主动接近，而不是原地不动");
    }

    @Test
    void distantRangedEnemyEventuallyAttacks() {
        Room room = openRoom(8, RoomType.BOSS);
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(240, 840);
        EnemySystem system = new EnemySystem();
        system.enterRoom(room, 7L, player, navigation);
        Enemy watcher = system.getEnemies().getFirst();
        watcher.setPosition(1160, 200);

        double startDistance = Math.hypot(watcher.getX() - player.getX(), watcher.getY() - player.getY());
        assertTrue(startDistance > GameConfig.ENEMY_RANGED_ATTACK_RANGE,
                "测试前提：开局距离超过开火距离");

        boolean fired = false;
        for (int frame = 0; frame < 25 * 60 && !fired; frame++) {
            system.update(DT, player, new PlayerAttackSystem(), navigation);
            fired = !system.getAttacks().isEmpty();
        }

        assertTrue(fired, "远处敌人走近后应当主动开火，而不是一直不攻击玩家");
    }

    @Test
    void projectileLifetimeReachesTheMaximumFiringRange() {
        assertTrue(GameConfig.ENEMY_PROJECTILE_LIFETIME * GameConfig.ENEMY_PROJECTILE_SPEED
                        >= GameConfig.ENEMY_RANGED_ATTACK_RANGE,
                "弹体寿命必须够飞到最大开火距离，否则远距离射击会在半路自行消失");
    }

    @Test
    void enemyBehindWallClosesInWithoutShootingThroughIt() {
        Room room = new Room(3, RoomType.BATTLE, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT)),
                List.of(new Wall(600, 0, 60, 500, null)));
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(200, 250);
        EnemySystem system = new EnemySystem();
        system.enterRoom(room, 11L, player, navigation);
        Enemy enemy = soleEnemy(system, WorldType.LIGHT, player, navigation);
        enemy.setPosition(1150, 250);

        double beforeX = enemy.getX();
        runFor(system, player, navigation, 1.0);

        assertTrue(enemy.getX() < beforeX - 20, "视线被墙挡住时也要继续接近，而不是僵在原地");
        assertTrue(system.getAttacks().isEmpty(), "没有视线时不能隔墙开火");
    }

    @Test
    void enemyInTheOtherWorldStaysIdle() {
        Room room = openRoom(3, RoomType.BATTLE);
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(240, 240);
        EnemySystem system = new EnemySystem();
        system.enterRoom(room, 42L, player, navigation);
        Enemy shadowEnemy = soleEnemy(system, WorldType.SHADOW, player, navigation);
        shadowEnemy.setPosition(player.getX() + 300, player.getY());
        double beforeX = shadowEnemy.getX();
        double beforeY = shadowEnemy.getY();

        runFor(system, player, navigation, 1.0);

        assertEquals(WorldType.LIGHT, player.getCurrentWorld());
        assertEquals(beforeX, shadowEnemy.getX(), 1e-9);
        assertEquals(beforeY, shadowEnemy.getY(), 1e-9);
        assertFalse(shadowEnemy.isAware());
    }

    @Test
    void shiftingWorldMakesEnemiesInTheLeftWorldLoseTheirTarget() {
        Room room = openRoom(3, RoomType.BATTLE);
        RoomNavigationSystem navigation = navigationFor(room);
        Player player = new Player(240, 240);
        EnemySystem system = new EnemySystem();
        system.enterRoom(room, 42L, player, navigation);
        List<Enemy> lightEnemies = system.getEnemies().stream()
                .filter(enemy -> enemy.getWorld() == WorldType.LIGHT).toList();
        assertFalse(lightEnemies.isEmpty());

        runFor(system, player, navigation, 0.2);
        assertTrue(lightEnemies.stream().allMatch(Enemy::isAware));

        player.toggleWorld();
        system.onWorldChanged(player.getCurrentWorld());

        assertTrue(lightEnemies.stream().noneMatch(Enemy::isAware),
                "玩家切界后，被留在旧世界的敌人应当丢失目标");
        assertTrue(lightEnemies.stream()
                        .allMatch(enemy -> enemy.getAlertRemaining() == GameConfig.ENEMY_ALERT_TIME),
                "重新索敌时需要重新起手，避免玩家刚切界回来就被贴脸开火");
    }

    @Test
    void everySameWorldEnemyInRealRoomsJoinsTheFight() {
        int centralRooms = 0;
        for (long seed = 1L; seed <= 40L; seed++) {
            DungeonMap map = new MapGenerator().generate(seed);
            for (Room room : map.rooms()) {
                if (room.type() != RoomType.BATTLE && room.type() != RoomType.BOSS) continue;
                RoomNavigationSystem navigation = navigationFor(room);
                double centerX = (room.minX() + room.maxX()) / 2.0;
                double centerY = (room.minY() + room.maxY()) / 2.0;
                // 只把玩家放在房间正中的开阔位置；若中心点本身站不下玩家，
                // 退而求其次的落点可能是只有玩家身位挤得进的窄缝，敌人根本到不了，不适合做严格断言。
                boolean centralSpot = navigation.canOccupy(centerX, centerY, GameConfig.PLAYER_RADIUS, WorldType.LIGHT);
                double[] safe = centralSpot ? new double[]{centerX, centerY}
                        : navigation.findNearestSafePosition(centerX, centerY, WorldType.LIGHT);
                assertNotNull(safe, "战斗房必须能给玩家找到安全落点");
                Player player = new Player(safe[0], safe[1]);
                EnemySystem system = new EnemySystem();
                system.enterRoom(room, seed, player, navigation);

                List<Enemy> sameWorldEnemies = system.getEnemies().stream()
                        .filter(enemy -> enemy.getWorld() == player.getCurrentWorld()).toList();
                assertFalse(sameWorldEnemies.isEmpty(), "房间 " + room.id() + " 应当有玩家当前世界的敌人");
                Set<EnemyKind> kindsInPlayerWorld = sameWorldEnemies.stream()
                        .map(Enemy::getKind).collect(Collectors.toSet());
                Map<Enemy, Double> startDistances = sameWorldEnemies.stream()
                        .collect(Collectors.toMap(enemy -> enemy, enemy -> distance(player, enemy)));
                Map<Enemy, Double> closestDistances = new HashMap<>(startDistances);
                Set<EnemyKind> kindsThatFired = EnumSet.noneOf(EnemyKind.class);
                PlayerAttackSystem playerAttacks = new PlayerAttackSystem();
                for (int frame = 0; frame < 30 * 60; frame++) {
                    system.update(DT, player, playerAttacks, navigation);
                    system.getAttacks().forEach(attack -> kindsThatFired.add(attack.getSource()));
                    for (Enemy enemy : sameWorldEnemies) {
                        closestDistances.merge(enemy, distance(player, enemy), Math::min);
                    }
                }

                String context = "种子 " + seed + " 的房间 " + room.id() + "（" + room.shape() + "）";
                for (Enemy enemy : sameWorldEnemies) {
                    assertTrue(enemy.isAware(), context + "：" + enemy.getKind() + " 应当锁定玩家");
                    assertTrue(closestDistances.get(enemy) < startDistances.get(enemy)
                                    || startDistances.get(enemy) <= GameConfig.ENEMY_RANGED_STANDOFF_DISTANCE,
                            context + "：" + enemy.getKind() + " 不能对远处的玩家无动于衷");
                }
                if (centralSpot) {
                    assertEquals(kindsInPlayerWorld, kindsThatFired, context
                            + "：玩家站在房间中央时，每种同界敌人都应当走到能开火的位置并攻击玩家");
                    centralRooms++;
                }
            }
        }
        assertTrue(centralRooms >= 20, "至少应当严格校验若干个中央开阔房间，实际 " + centralRooms);
    }

    private static double distance(Player player, Enemy enemy) {
        return Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());
    }

    /** 保留房内唯一的指定世界敌人，其余全部清掉，让断言不受其他随机敌人干扰。 */
    private static Enemy soleEnemy(EnemySystem system, WorldType world, Player player, RoomNavigationSystem navigation) {
        Enemy keep = system.getEnemies().stream()
                .filter(enemy -> enemy.getWorld() == world).findFirst().orElseThrow();
        for (Enemy enemy : system.getEnemies()) {
            if (enemy != keep) enemy.damage(enemy.getMaxHp());
        }
        system.update(DT, player, new PlayerAttackSystem(), navigation);
        assertEquals(1, system.getEnemies().size());
        return keep;
    }

    private static void runFor(EnemySystem system, Player player, RoomNavigationSystem navigation, double seconds) {
        int frames = (int) Math.round(seconds / DT);
        for (int frame = 0; frame < frames; frame++) {
            system.update(DT, player, new PlayerAttackSystem(), navigation);
        }
    }

    /** 无障碍的整幅房间，便于按指定坐标摆放玩家与敌人。 */
    private static Room openRoom(int id, RoomType type) {
        return new Room(id, type, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT)), List.of());
    }

    private static RoomNavigationSystem navigationFor(Room room) {
        // RoomNavigationSystem 进入房间时会查找相邻节点，这里为邻居补上占位房间，保证单房也能导航。
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
