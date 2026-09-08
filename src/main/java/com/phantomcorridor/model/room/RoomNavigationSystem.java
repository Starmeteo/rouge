package com.phantomcorridor.model.room;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.util.CollisionUtil;

/** 处理房间墙体碰撞、门禁与节点切换。 */
public final class RoomNavigationSystem {
    private DungeonMap map;
    private Room currentRoom;

    public void reset(DungeonMap map) {
        this.map = map;
        this.currentRoom = map.entrance();
    }

    public void move(Player player, double directionX, double directionY, double dt) {
        double length = Math.hypot(directionX, directionY);
        if (length == 0.0) return;
        double speed = GameConfig.PLAYER_BASE_SPEED
                * (player.getCurrentWorld() == com.phantomcorridor.model.WorldType.SHADOW
                ? GameConfig.SHADOW_SPEED_MULTIPLIER : 1.0);
        double dx = directionX / length * speed * dt;
        double dy = directionY / length * speed * dt;
        double nextX = player.getX() + dx;
        if (!hitsWall(nextX, player.getY(), player)) player.setPosition(nextX, player.getY());
        double nextY = player.getY() + dy;
        if (!hitsWall(player.getX(), nextY, player)) player.setPosition(player.getX(), nextY);
        attemptTransition(player, directionX, directionY);
        clampToRoom(player);
    }

    private boolean hitsWall(double x, double y, Player player) {
        for (Wall wall : currentRoom.walls()) {
            if (wall.activeIn(player.getCurrentWorld()) && CollisionUtil.circleIntersectsRect(
                    x, y, GameConfig.PLAYER_RADIUS, wall.x(), wall.y(), wall.width(), wall.height())) return true;
        }
        return false;
    }

    private void attemptTransition(Player player, double dx, double dy) {
        double edge = RoomConfig.ROOM_INNER_PADDING;
        boolean horizontalDoor = Math.abs(player.getX() - AppConfig.VIEW_WIDTH / 2.0)
                <= RoomConfig.DOOR_HALF_WIDTH;
        boolean verticalDoor = Math.abs(player.getY() - AppConfig.VIEW_HEIGHT / 2.0)
                <= RoomConfig.DOOR_HALF_WIDTH;
        if (dy < 0 && player.getY() <= edge && horizontalDoor) transition(player, Direction.NORTH);
        else if (dy > 0 && player.getY() >= AppConfig.VIEW_HEIGHT - edge && horizontalDoor)
            transition(player, Direction.SOUTH);
        else if (dx < 0 && player.getX() <= edge && verticalDoor) transition(player, Direction.WEST);
        else if (dx > 0 && player.getX() >= AppConfig.VIEW_WIDTH - edge && verticalDoor)
            transition(player, Direction.EAST);
    }

    private void transition(Player player, Direction direction) {
        if (!currentRoom.isDoorOpen(direction)) return;
        currentRoom = map.room(currentRoom.neighbor(direction));
        double edge = RoomConfig.ROOM_INNER_PADDING + GameConfig.PLAYER_RADIUS + 2.0;
        switch (direction) {
            case NORTH -> player.setPosition(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT - edge);
            case SOUTH -> player.setPosition(AppConfig.VIEW_WIDTH / 2.0, edge);
            case WEST -> player.setPosition(AppConfig.VIEW_WIDTH - edge, AppConfig.VIEW_HEIGHT / 2.0);
            case EAST -> player.setPosition(edge, AppConfig.VIEW_HEIGHT / 2.0);
        }
        // 第 6 天接入敌人后由存活数量控制；当前保持地图可完整探索。
        if (currentRoom.type() == com.phantomcorridor.model.RoomType.BATTLE
                || currentRoom.type() == com.phantomcorridor.model.RoomType.BOSS) currentRoom.setCleared(true);
    }

    private void clampToRoom(Player player) {
        double edge = RoomConfig.ROOM_INNER_PADDING;
        player.setPosition(Math.max(edge, Math.min(AppConfig.VIEW_WIDTH - edge, player.getX())),
                Math.max(edge, Math.min(AppConfig.VIEW_HEIGHT - edge, player.getY())));
    }

    public DungeonMap getMap() { return map; }
    public Room getCurrentRoom() { return currentRoom; }
}
