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
    private boolean chestRewardGranted;
    private boolean interactRequested;
    private boolean eventPending;

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
        pickups.clear(); chestVisible = false; chestOpened = false; chestRewardGranted = false; eventPending = false;
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
        if (player.getHp() <= 0) {
            player.updateAnimation(dt, 0.0, 0.0, false, false);
            return;
        }
        aimX = targetX;
        aimY = targetY;
        navigation.move(player, movementX, movementY, dt);
        if (navigation.consumeRoomChanged()) {
            attackSystem.clearTransientAttacks();
            enemies.enterRoom(navigation.getCurrentRoom(), dungeonSeed, player, navigation);
            roomAnnouncement = roomTypeLabel(navigation.getCurrentRoom().type());
            roomAnnouncementRemaining = 2.2;
            pickups.clear(); chestVisible = false; chestOpened = false; chestRewardGranted = false; eventPending = false;
            Room entered = navigation.getCurrentRoom();
            if (!entered.isRewardClaimed()) {
                switch (entered.type()) {
                    case REWARD -> {
                        pickups.add(new Pickup(Pickup.Type.COIN, player.getX() + 36, player.getY(), 5));
                        pickups.add(new Pickup(Pickup.Type.EQUIPMENT, player.getX() - 36, player.getY(),
                                Math.floorMod((int) (dungeonSeed + entered.id()), EquipmentType.values().length)));
                    }
                    case EVENT -> eventPending = true;
                    case SHOP -> pickups.add(new Pickup(Pickup.Type.EQUIPMENT, currentRoomCenterX(), currentRoomCenterY(),
                            Math.floorMod((int) (dungeonSeed + entered.id()), EquipmentType.values().length)));
                    default -> { }
                }
                entered.claimReward();
            }
        }
        attackSystem.update(dt, navigation);
        enemies.update(dt, player, attackSystem, navigation);
        int kills = enemies.consumeKills();
        if (navigation.getCurrentRoom().type() == RoomType.BATTLE
                || navigation.getCurrentRoom().type() == RoomType.BOSS
                || navigation.getCurrentRoom().type() == RoomType.EVENT) {
            navigation.getCurrentRoom().setCleared(enemies.isRoomCleared());
            if (navigation.getCurrentRoom().isCleared() && kills > 0) {
                chestVisible = true;
                // 宝箱本体由北侧绘制，奖励在按键打开后生成。
            }
        }
        if (kills > 0) player.restorePhaseEnergy(kills * GameConfig.PHASE_ENERGY_PER_FRAGMENT);
        if (kills > 0) coins += kills + Math.floorMod((int) (dungeonSeed + kills * 13L), kills * 3 + 1);
        combatActive = !enemies.isRoomCleared();
        Room current = navigation.getCurrentRoom();
        if (interactRequested) { interactRequested = false; interactNearby(current); }
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
    public void requestInteract() { interactRequested = true; }
    public String getInteractionPrompt() {
        Room room = navigation.getCurrentRoom();
        if (chestVisible && !chestOpened && Math.hypot(player.getX() - room.doorCenter(com.phantomcorridor.model.room.Direction.NORTH),
                player.getY() - room.minY() - 70) < 150) return "E  打开";
        if (eventPending && room.type() == RoomType.EVENT) return "E  触发事件";
        for (Pickup p : pickups) if (Math.hypot(p.x() - player.getX(), p.y() - player.getY()) < 120) {
            return p.type() == Pickup.Type.EQUIPMENT && room.type() == RoomType.SHOP ? "E  购买"
                    : p.type() == Pickup.Type.EQUIPMENT ? "E  装备" : "E  拾取";
        }
        return "";
    }
    private void interactNearby(Room current) {
        if (eventPending && current.type() == RoomType.EVENT) {
            eventPending = false;
            int roll = Math.floorMod((int) (dungeonSeed + current.id() * 17L), 4);
            if (roll == 0) pickups.add(new Pickup(Pickup.Type.HEALTH, currentRoomCenterX(), currentRoomCenterY(), 2));
            else if (roll == 1) pickups.add(new Pickup(Pickup.Type.PHASE_FRAGMENT, currentRoomCenterX(), currentRoomCenterY(), 3));
            else if (roll == 2) pickups.add(new Pickup(Pickup.Type.EQUIPMENT, currentRoomCenterX(), currentRoomCenterY(),
                    Math.floorMod((int) (dungeonSeed + current.id()), EquipmentType.values().length)));
            else { enemies.spawnEventEnemies(current, dungeonSeed, player, navigation); current.setCleared(false); }
            return;
        }
        if (chestVisible && !chestOpened && Math.hypot(player.getX() - current.doorCenter(com.phantomcorridor.model.room.Direction.NORTH),
                player.getY() - current.minY() - 70) < 100) {
            chestOpened = true;
            if (!chestRewardGranted) {
                chestRewardGranted = true;
                double x = current.doorCenter(com.phantomcorridor.model.room.Direction.NORTH), y = current.minY() + 76;
                int reward = Math.floorMod((int) (dungeonSeed + current.id() * 31L), 4);
                if (reward == 0) pickups.add(new Pickup(Pickup.Type.COIN, x, y, 8 + Math.floorMod(current.id(), 13)));
                else if (reward == 1) pickups.add(new Pickup(Pickup.Type.HEALTH, x, y, 2));
                else if (reward == 2) pickups.add(new Pickup(Pickup.Type.PHASE_FRAGMENT, x, y, 2));
                else pickups.add(new Pickup(Pickup.Type.EQUIPMENT, x, y,
                        Math.floorMod((int) (dungeonSeed + current.id()), EquipmentType.values().length)));
            }
            return;
        }
        for (int i = 0; i < pickups.size(); i++) {
            Pickup p = pickups.get(i);
            if (Math.hypot(p.x() - player.getX(), p.y() - player.getY()) > 80) continue;
            if (p.type() == Pickup.Type.COIN) coins += p.amount();
            else if (p.type() == Pickup.Type.PHASE_FRAGMENT) player.restorePhaseEnergy(GameConfig.PHASE_ENERGY_PER_FRAGMENT * Math.max(1, p.amount()));
            else if (p.type() == Pickup.Type.HEALTH) player.restoreHealth(Math.max(1, p.amount()));
            else if (p.type() == Pickup.Type.ITEM) player.addItem(ItemType.values()[Math.floorMod(p.amount(), ItemType.values().length)]);
            else if (p.type() == Pickup.Type.EQUIPMENT) {
                if (current.type() == RoomType.SHOP && coins < 3) return;
                if (current.type() == RoomType.SHOP) coins -= 3;
                player.equip(EquipmentType.values()[Math.floorMod(p.amount(), EquipmentType.values().length)]);
            }
            pickups.remove(i);
            return;
        }
    }
    private double currentRoomCenterX() {
        Room room = navigation.getCurrentRoom();
        return (room.minX() + room.maxX()) / 2.0;
    }
    private double currentRoomCenterY() {
        Room room = navigation.getCurrentRoom();
        return (room.minY() + room.maxY()) / 2.0;
    }

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
