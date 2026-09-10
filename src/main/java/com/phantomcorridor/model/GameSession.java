package com.phantomcorridor.model;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.effect.WorldShiftSystem;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.combat.PlayerAttackSystem;
import com.phantomcorridor.model.combat.EnemyProjectileSystem;
import com.phantomcorridor.model.combat.EnemySystem;
import com.phantomcorridor.model.dungeon.MapGenerator;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.model.room.Room;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一局游戏的聚合状态。后续房间、敌人、掉落都从这里接入。 */
public final class GameSession {

    private final Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
    private final WorldShiftSystem worldShift = new WorldShiftSystem();
    private final PlayerAttackSystem attackSystem = new PlayerAttackSystem();
    private final EnemyProjectileSystem enemyProjectiles = new EnemyProjectileSystem();
    private final EnemySystem enemies = new EnemySystem();
    private final RoomNavigationSystem navigation = new RoomNavigationSystem();
    private long dungeonSeed;
    private double phasePulseVisibleRemaining;
    private double aimX;
    private double aimY;
    private String roomAnnouncement = "";
    private double roomAnnouncementRemaining;
    private boolean combatActive;
    private int coins;
    private final List<Pickup> pickups = new ArrayList<>();
    private boolean chestVisible;
    private boolean chestOpened;

    public void newRun() { newRun(""); }

    public void newRun(String configuredSeed) {
        player.reset(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
        worldShift.reset();
        attackSystem.reset();
        enemyProjectiles.reset();
        enemies.reset();
        phasePulseVisibleRemaining = 0.0;
        roomAnnouncement = "入口房";
        roomAnnouncementRemaining = 2.2;
        aimX = player.getX() + 1.0;
        aimY = player.getY();
        combatActive = false;
        coins = 0;
        pickups.clear(); chestVisible = false; chestOpened = false;
        dungeonSeed = MapGenerator.parseSeed(configuredSeed);
        navigation.reset(new MapGenerator().generate(dungeonSeed));
        navigation.placeAtEntrance(player);
        enemies.enterRoom(navigation.getCurrentRoom(), dungeonSeed, player, navigation);
    }

    public void update(double dt, double movementX, double movementY,
                       double targetX, double targetY, boolean attacking) {
        worldShift.update(dt);
        phasePulseVisibleRemaining = Math.max(0.0, phasePulseVisibleRemaining - dt);
        roomAnnouncementRemaining = Math.max(0.0, roomAnnouncementRemaining - Math.max(0.0, dt));
        aimX = targetX;
        aimY = targetY;
        navigation.move(player, movementX, movementY, dt);
        if (navigation.consumeRoomChanged()) {
            attackSystem.clearTransientAttacks();
            enemies.enterRoom(navigation.getCurrentRoom(), dungeonSeed, player, navigation);
            roomAnnouncement = roomTypeLabel(navigation.getCurrentRoom().type());
            roomAnnouncementRemaining = 2.2;
            enemies.enterRoom(navigation.getCurrentRoom());
            pickups.clear(); chestVisible = false; chestOpened = false;
            if (navigation.getCurrentRoom().type() == RoomType.REWARD) {
                pickups.add(new Pickup(Pickup.Type.COIN, player.getX() + 36, player.getY(), 5));
                pickups.add(new Pickup(Pickup.Type.PHASE_FRAGMENT, player.getX() - 36, player.getY(), 1));
            }
        }
        attackSystem.update(dt, navigation);
        enemies.update(dt, player, attackSystem, navigation);
        if (navigation.getCurrentRoom().type() == RoomType.BATTLE || navigation.getCurrentRoom().type() == RoomType.BOSS) {
            navigation.getCurrentRoom().setCleared(enemies.isRoomCleared());
        }
        int kills = enemies.consumeKills();
        if (kills > 0) player.restorePhaseEnergy(kills * GameConfig.PHASE_ENERGY_PER_FRAGMENT);
        combatActive = !enemies.isRoomCleared();
        if (!combatActive) {
            player.restorePhaseEnergy(GameConfig.PHASE_ENERGY_REGEN_PER_SEC * dt);
        }
        pickups.removeIf(p -> {
            if (Math.hypot(p.x() - player.getX(), p.y() - player.getY()) > 42) return false;
            if (p.type() == Pickup.Type.COIN) coins += p.amount();
            else if (p.type() == Pickup.Type.PHASE_FRAGMENT) player.restorePhaseEnergy(GameConfig.PHASE_ENERGY_PER_FRAGMENT);
            return true;
        });
        if (chestVisible && !chestOpened && Math.hypot(player.getX() - current.doorCenter(com.phantomcorridor.model.room.Direction.NORTH),
                player.getY() - current.minY() - 70) < 80) chestOpened = true;
        player.restorePhaseEnergy(GameConfig.PHASE_ENERGY_REGEN_PER_SEC * dt);
        player.updateAttackCharges(dt);
        if (attacking) {
            attackSystem.tryAttack(player, aimX, aimY);
        }
        player.updateAnimation(dt, movementX, movementY, attacking,
                phasePulseVisibleRemaining > 0.0);
    }

    public boolean tryShiftWorld() {
        WorldType targetWorld = player.getCurrentWorld() == WorldType.LIGHT
                ? WorldType.SHADOW : WorldType.LIGHT;
        double[] safePosition = navigation.findNearestSafePosition(
                player.getX(), player.getY(), targetWorld);
        if (safePosition == null) return false;
        if (!worldShift.tryShift(player)) return false;
        player.setPosition(safePosition[0], safePosition[1]);
        if (worldShift.consumePulse()) {
            enemyProjectiles.clearWithin(player.getX(), player.getY(), GameConfig.PHASE_PULSE_RADIUS);
            phasePulseVisibleRemaining = GameConfig.PHASE_PULSE_VISIBLE_TIME;
        }
        enemies.onWorldChanged(player.getCurrentWorld());
        return true;
    }

    public Player getPlayer() { return player; }
    public WorldShiftSystem getWorldShift() { return worldShift; }
    public PlayerAttackSystem getAttackSystem() { return attackSystem; }
    public EnemyProjectileSystem getEnemyProjectiles() { return enemyProjectiles; }
    public EnemySystem getEnemies() { return enemies; }
    public boolean isPhasePulseVisible() { return phasePulseVisibleRemaining > 0.0; }
    public double getAimX() { return aimX; }
    public double getAimY() { return aimY; }
    public int getLightEnemyCount() { return enemies.getCount(WorldType.LIGHT); }
    public int getShadowEnemyCount() { return enemies.getCount(WorldType.SHADOW); }
    public int getCoins() { return coins; }
    public List<Pickup> getPickups() { return Collections.unmodifiableList(pickups); }
    public boolean isChestVisible() { return chestVisible && !chestOpened; }
    public RoomNavigationSystem getNavigation() { return navigation; }
    public long getDungeonSeed() { return dungeonSeed; }
    public boolean isRoomAnnouncementVisible() { return roomAnnouncementRemaining > 0.0; }
    public String getRoomAnnouncement() { return roomAnnouncement; }
    public double getRoomAnnouncementRemaining() { return roomAnnouncementRemaining; }

    private static String roomTypeLabel(RoomType type) {
        return switch (type) {
            case ENTRANCE -> "入口房";
            case BATTLE -> "战斗房";
            case REWARD -> "奖励房";
            case SHOP -> "商店房";
            case EVENT -> "事件房";
            case BOSS -> "Boss 房";
        };
    }
}
