package com.phantomcorridor.model;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.effect.WorldShiftSystem;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.combat.PlayerAttackSystem;
import com.phantomcorridor.model.combat.EnemyProjectileSystem;
import com.phantomcorridor.model.combat.EnemySystem;
import com.phantomcorridor.model.dungeon.MapGenerator;
import com.phantomcorridor.model.room.RoomContentSystem;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.model.room.Room;
import java.util.List;

/** 一局游戏的聚合状态。后续房间、敌人、掉落都从这里接入。 */
public final class GameSession {

    private final Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
    private final WorldShiftSystem worldShift = new WorldShiftSystem();
    private final PlayerAttackSystem attackSystem = new PlayerAttackSystem();
    private final EnemyProjectileSystem enemyProjectiles = new EnemyProjectileSystem();
    private final EnemySystem enemies = new EnemySystem();
    private final RoomNavigationSystem navigation = new RoomNavigationSystem();
    private final RoomContentSystem roomContent = new RoomContentSystem();
    private long dungeonSeed;
    private double phasePulseVisibleRemaining;
    private double aimX;
    private double aimY;
    private String roomAnnouncement = "";
    private double roomAnnouncementRemaining;
    private boolean combatActive;
    private boolean interactRequested;

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
        dungeonSeed = MapGenerator.parseSeed(configuredSeed);
        roomContent.reset(dungeonSeed);
        navigation.reset(new MapGenerator().generate(dungeonSeed));
        navigation.placeAtEntrance(player);
        roomContent.enterRoom(navigation.getCurrentRoom(), player);
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
            Room entered = navigation.getCurrentRoom();
            roomContent.enterRoom(entered, player);
            enemies.enterRoom(entered, dungeonSeed, player, navigation);
            roomAnnouncement = roomTypeLabel(entered.type());
            roomAnnouncementRemaining = 2.2;
        }
        // 房间内容只在第一次进入时生成，进出不会重刷；这里只维护“待确认商品”的有效性。
        roomContent.update(navigation.getCurrentRoom(), player);
        attackSystem.update(dt, navigation);
        enemies.update(dt, player, attackSystem, navigation);
        int kills = enemies.consumeKills();
        Room current = navigation.getCurrentRoom();
        if (current.type() == RoomType.BATTLE || current.type() == RoomType.BOSS
                || current.type() == RoomType.EVENT) {
            current.setCleared(enemies.isRoomCleared());
        }
        if (kills > 0) player.restorePhaseEnergy(kills * GameConfig.PHASE_ENERGY_PER_FRAGMENT);
        if (kills > 0) player.addCoins(kills + Math.floorMod((int) (dungeonSeed + kills * 13L), kills * 3 + 1));
        combatActive = !enemies.isRoomCleared();
        if (interactRequested) {
            interactRequested = false;
            interact(current);
        }
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
    public int getCoins() { return player.getCoins(); }
    public RoomNavigationSystem getNavigation() { return navigation; }
    public long getDungeonSeed() { return dungeonSeed; }
    public boolean isRoomAnnouncementVisible() { return roomAnnouncementRemaining > 0.0; }
    public String getRoomAnnouncement() { return roomAnnouncement; }
    public double getRoomAnnouncementRemaining() { return roomAnnouncementRemaining; }
    public void requestInteract() { interactRequested = true; }

    /** 当前房间地面上的拾取物：挂在房间上，所以离开再回来东西还在。 */
    public List<Pickup> getPickups() { return navigation.getCurrentRoom().loot().pickupsView(); }

    /** 当前房间是否有没打开的宝箱。 */
    public boolean isChestVisible() { return navigation.getCurrentRoom().hasUnopenedChest(); }

    public String getInteractionPrompt() {
        return roomContent.prompt(navigation.getCurrentRoom(), player);
    }

    /** 商店商品的售价（金币）；当前房间不是商店或物品不可售时返回 -1。 */
    public int getShopPrice(Pickup pickup) {
        return navigation.getCurrentRoom().type() == RoomType.SHOP
                ? RoomContentSystem.priceOf(pickup) : -1;
    }

    /** 该商品是否已被选中、正等待二次确认（渲染层用它高亮）。 */
    public boolean isShopOfferSelected(Pickup pickup) { return roomContent.isOfferSelected(pickup); }

    /** 金币是否够买这件商品。 */
    public boolean canAfford(Pickup pickup) { return RoomContentSystem.canAfford(player, pickup); }

    private void interact(Room current) {
        RoomContentSystem.Outcome outcome = roomContent.interact(current, player);
        if (outcome == RoomContentSystem.Outcome.EVENT_AMBUSH) {
            // 事件房抽到伏击：房间里重新刷怪并重新关门，清空后宝箱照常出现。
            current.setCleared(false);
            enemies.spawnEventEnemies(current, dungeonSeed, player, navigation);
        }
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
