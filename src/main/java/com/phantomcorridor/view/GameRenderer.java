package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.GameSession;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.combat.Projectile;
import com.phantomcorridor.model.room.Direction;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.Wall;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Canvas 游戏画面渲染器。只读取模型，不修改游戏状态。 */
public final class GameRenderer {

    private static final Color LIGHT_GOLD = Color.web("#e8bd68");
    private static final Color SHADOW_VIOLET = Color.web("#9b65dc");

    public void render(GraphicsContext g, GameSession session, double fps) {
        Player player = session.getPlayer();
        boolean light = player.getCurrentWorld() == WorldType.LIGHT;
        drawFloor(g, light);
        drawRoom(g, session, light);
        drawPhaseWalls(g, session, light);
        drawAttacks(g, session, light);
        drawPlayer(g, player, light);
        drawPhasePulse(g, session, light);
        drawAim(g, session, light);
        drawHud(g, session, fps, light);
        drawMiniMap(g, session, light);
        drawControls(g, light);
    }

    private void drawPhasePulse(GraphicsContext g, GameSession session, boolean light) {
        if (!session.isPhasePulseVisible()) return;
        Player player = session.getPlayer();
        double radius = GameConfig.PHASE_PULSE_RADIUS;
        Color color = light ? LIGHT_GOLD : SHADOW_VIOLET;
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.74));
        g.setLineWidth(4.0);
        g.strokeOval(player.getX() - radius, player.getY() - radius, radius * 2.0, radius * 2.0);
    }

    private void drawAttacks(GraphicsContext g, GameSession session, boolean light) {
        for (Projectile projectile : session.getAttackSystem().getProjectiles()) {
            double radius = projectile.getRadius();
            g.setGlobalBlendMode(BlendMode.ADD);
            g.setFill(Color.rgb(255, 220, 138, 0.30));
            g.fillOval(projectile.getX() - radius * 2.5, projectile.getY() - radius * 2.5,
                    radius * 5.0, radius * 5.0);
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
            g.setFill(Color.web("#fff2bd"));
            g.fillOval(projectile.getX() - radius, projectile.getY() - radius,
                    radius * 2.0, radius * 2.0);
        }
        if (!light && session.getAttackSystem().isMeleeVisible()) {
            Player player = session.getPlayer();
            double angle = Math.toDegrees(session.getAttackSystem().getMeleeAngleRadians());
            double arc = GameConfig.SHADOW_MELEE_ARC_DEGREES;
            double range = GameConfig.SHADOW_MELEE_RANGE;
            g.setStroke(Color.rgb(190, 132, 242, 0.82));
            g.setLineWidth(10.0);
            g.strokeArc(player.getX() - range, player.getY() - range, range * 2.0, range * 2.0,
                    -angle - arc / 2.0, arc, javafx.scene.shape.ArcType.OPEN);
            g.setStroke(Color.rgb(239, 216, 255, 0.86));
            g.setLineWidth(2.0);
            g.strokeArc(player.getX() - range, player.getY() - range, range * 2.0, range * 2.0,
                    -angle - arc / 2.0, arc, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawAim(GraphicsContext g, GameSession session, boolean light) {
        Player player = session.getPlayer();
        double dx = session.getAimX() - player.getX();
        double dy = session.getAimY() - player.getY();
        double length = Math.hypot(dx, dy);
        if (length < 0.0001) return;
        double cursorX = player.getX() + dx / length * 50.0;
        double cursorY = player.getY() + dy / length * 50.0;
        Color domain = light ? LIGHT_GOLD : SHADOW_VIOLET;
        g.setStroke(Color.color(domain.getRed(), domain.getGreen(), domain.getBlue(), 0.48));
        g.setLineWidth(1.0);
        g.strokeLine(player.getX(), player.getY(), cursorX, cursorY);
        g.strokeOval(cursorX - 5.0, cursorY - 5.0, 10.0, 10.0);
    }

    private void drawFloor(GraphicsContext g, boolean light) {
        g.setFill(light
                ? new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#211c14")), new Stop(0.55, Color.web("#312719")),
                    new Stop(1, Color.web("#14110e")))
                : new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#080610")), new Stop(0.55, Color.web("#171022")),
                    new Stop(1, Color.web("#05040a"))));
        g.fillRect(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);

        Color grid = light ? Color.rgb(224, 188, 111, 0.08) : Color.rgb(157, 105, 219, 0.10);
        g.setStroke(grid);
        g.setLineWidth(1.0);
        for (int x = 48; x < AppConfig.VIEW_WIDTH; x += 64) {
            g.strokeLine(x, 0, x, AppConfig.VIEW_HEIGHT);
        }
        for (int y = 48; y < AppConfig.VIEW_HEIGHT; y += 64) {
            g.strokeLine(0, y, AppConfig.VIEW_WIDTH, y);
        }

        g.setFill(new RadialGradient(0, 0, AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0,
                AppConfig.VIEW_WIDTH * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0.38, Color.TRANSPARENT), new Stop(1, Color.rgb(0, 0, 0, 0.68))));
        g.fillRect(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
    }

    private void drawRoom(GraphicsContext g, GameSession session, boolean light) {
        Color border = light ? Color.web("#8d6d35") : Color.web("#4f3277");
        Color glow = light ? Color.rgb(236, 195, 105, 0.28) : Color.rgb(153, 91, 218, 0.30);
        g.setStroke(glow);
        g.setLineWidth(18.0);
        g.strokeRoundRect(34, 34, AppConfig.VIEW_WIDTH - 68.0, AppConfig.VIEW_HEIGHT - 68.0, 22, 22);
        g.setStroke(border);
        g.setLineWidth(4.0);
        g.strokeRoundRect(34, 34, AppConfig.VIEW_WIDTH - 68.0, AppConfig.VIEW_HEIGHT - 68.0, 22, 22);

        for (Direction direction : Direction.values()) {
            if (session.getNavigation().getCurrentRoom().hasDoor(direction)) {
                drawDoor(g, direction, session.getNavigation().getCurrentRoom().isDoorOpen(direction), light);
            }
        }
    }

    private void drawDoor(GraphicsContext g, Direction direction, boolean open, boolean light) {
        double half = com.phantomcorridor.config.RoomConfig.DOOR_HALF_WIDTH;
        double cx = AppConfig.VIEW_WIDTH / 2.0;
        double cy = AppConfig.VIEW_HEIGHT / 2.0;
        g.setFill(open ? Color.rgb(12, 9, 18, 0.35) : Color.rgb(4, 4, 8, 0.94));
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            g.fillRect(cx - half, direction == Direction.NORTH ? 24 : AppConfig.VIEW_HEIGHT - 52,
                    half * 2.0, 28);
        } else {
            g.fillRect(direction == Direction.WEST ? 24 : AppConfig.VIEW_WIDTH - 52,
                    cy - half, 28, half * 2.0);
        }
    }

    private void drawPhaseWalls(GraphicsContext g, GameSession session, boolean light) {
        for (Wall wall : session.getNavigation().getCurrentRoom().walls()) {
            boolean active = wall.activeIn(session.getPlayer().getCurrentWorld());
            Color color = wall.world() == WorldType.LIGHT ? LIGHT_GOLD : SHADOW_VIOLET;
            drawPhaseWall(g, wall.x(), wall.y(), wall.width(), wall.height(), color, active ? 0.62 : 0.15);
        }
    }

    private void drawMiniMap(GraphicsContext g, GameSession session, boolean light) {
        Room current = session.getNavigation().getCurrentRoom();
        double originX = AppConfig.VIEW_WIDTH - 210.0;
        double originY = 205.0;
        double scale = 24.0;
        g.setStroke(Color.rgb(220, 210, 225, 0.20));
        g.setLineWidth(2.0);
        for (Room room : session.getNavigation().getMap().rooms()) {
            for (var edge : room.neighbors().entrySet()) {
                Room neighbor = session.getNavigation().getMap().room(edge.getValue());
                g.strokeLine(originX + room.mapX() * scale, originY + room.mapY() * scale,
                        originX + neighbor.mapX() * scale, originY + neighbor.mapY() * scale);
            }
        }
        for (Room room : session.getNavigation().getMap().rooms()) {
            double x = originX + room.mapX() * scale;
            double y = originY + room.mapY() * scale;
            g.setFill(room == current ? (light ? LIGHT_GOLD : SHADOW_VIOLET)
                    : room.type() == com.phantomcorridor.model.RoomType.BOSS
                    ? Color.web("#b84e55") : Color.rgb(210, 200, 220, 0.46));
            g.fillRect(x - 5, y - 5, 10, 10);
        }
        g.setFill(Color.rgb(235, 226, 242, 0.62));
        g.setFont(Font.font("Microsoft YaHei UI", 12));
        g.fillText("房间 " + (current.id() + 1) + " · " + current.type() + " · 种子 "
                + session.getDungeonSeed(), originX - 78, originY - 28);
    }

    private void drawPhaseWall(GraphicsContext g, double x, double y, double width, double height,
                               Color color, double alpha) {
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha * 0.32));
        g.fillRoundRect(x - 8, y, width + 16, height, 12, 12);
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.setLineWidth(2.0);
        for (double lineY = y + 8; lineY < y + height; lineY += 20) {
            g.strokeLine(x, lineY, x + width, lineY + 12);
        }
    }

    private void drawPlayer(GraphicsContext g, Player player, boolean light) {
        Color domain = light ? LIGHT_GOLD : SHADOW_VIOLET;
        double x = player.getX();
        double y = player.getY();
        double aura = GameConfig.PLAYER_RADIUS * 3.2;

        g.setGlobalBlendMode(BlendMode.ADD);
        g.setFill(new RadialGradient(0, 0, x, y, aura, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(domain.getRed(), domain.getGreen(), domain.getBlue(), 0.48)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(x - aura, y - aura, aura * 2, aura * 2);
        g.setGlobalBlendMode(BlendMode.SRC_OVER);

        g.setFill(light ? Color.web("#f8edd5") : Color.web("#c9b4e5"));
        g.fillOval(x - GameConfig.PLAYER_RADIUS, y - GameConfig.PLAYER_RADIUS,
                GameConfig.PLAYER_RADIUS * 2, GameConfig.PLAYER_RADIUS * 2);
        g.setStroke(domain);
        g.setLineWidth(3.0);
        g.strokeOval(x - GameConfig.PLAYER_RADIUS, y - GameConfig.PLAYER_RADIUS,
                GameConfig.PLAYER_RADIUS * 2, GameConfig.PLAYER_RADIUS * 2);

        g.setStroke(domain);
        g.setLineWidth(1.4);
        g.strokeOval(x - 25, y - 25, 50, 50);
        g.strokeLine(x - 31, y, x - 21, y);
        g.strokeLine(x + 21, y, x + 31, y);
        g.strokeLine(x, y - 31, x, y - 21);
        g.strokeLine(x, y + 21, x, y + 31);
    }

    private void drawHud(GraphicsContext g, GameSession session, double fps, boolean light) {
        Color domain = light ? LIGHT_GOLD : SHADOW_VIOLET;
        Player player = session.getPlayer();

        g.setFill(Color.rgb(4, 4, 8, 0.76));
        g.fillRoundRect(56, 58, 370, 112, 16, 16);
        g.setStroke(Color.color(domain.getRed(), domain.getGreen(), domain.getBlue(), 0.55));
        g.setLineWidth(1.0);
        g.strokeRoundRect(56, 58, 370, 112, 16, 16);

        g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 17));
        g.setFill(Color.web("#efe7d8"));
        g.fillText("生命", 78, 91);
        for (int i = 0; i < GameConfig.PLAYER_MAX_HP; i++) {
            g.setFill(i < player.getHp() ? Color.web("#d75b54") : Color.web("#3b2528"));
            g.fillOval(137 + i * 25.0, 76, 14, 14);
        }

        g.setFill(Color.web("#efe7d8"));
        g.fillText("相位", 78, 130);
        g.setFill(Color.rgb(255, 255, 255, 0.09));
        g.fillRoundRect(137, 115, 252, 18, 9, 9);
        double ratio = player.getPhaseEnergy() / GameConfig.PHASE_ENERGY_MAX;
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, LIGHT_GOLD), new Stop(1, SHADOW_VIOLET)));
        g.fillRoundRect(137, 115, 252 * ratio, 18, 9, 9);
        g.setFill(Color.rgb(235, 226, 242, 0.76));
        g.setFont(Font.font("Microsoft YaHei UI", 12));
        g.fillText(String.format("%d / %d", Math.round(player.getPhaseEnergy()),
                Math.round(GameConfig.PHASE_ENERGY_MAX)), 302, 129);

        g.setFill(Color.rgb(235, 226, 242, 0.64));
        g.fillText("光界残敌  " + session.getLightEnemyCount() + "     影界残敌  "
                + session.getShadowEnemyCount() + "     金币  " + session.getCoins(), 78, 154);

        double badgeX = AppConfig.VIEW_WIDTH - 103.0;
        double badgeY = 105.0;
        g.setFill(Color.rgb(4, 4, 8, 0.78));
        g.fillOval(badgeX - 45, badgeY - 45, 90, 90);
        g.setStroke(domain);
        g.setLineWidth(2.5);
        g.strokeOval(badgeX - 45, badgeY - 45, 90, 90);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(domain);
        g.setFont(Font.font("STKaiti", FontWeight.BOLD, 30));
        g.fillText(light ? "光" : "影", badgeX, badgeY + 10);
        g.setTextAlign(TextAlignment.LEFT);

        g.setFill(Color.rgb(230, 220, 235, 0.28));
        g.setFont(Font.font("Consolas", 11));
        g.fillText(String.format("%.0f FPS", fps), AppConfig.VIEW_WIDTH - 103, 169);
    }

    private void drawControls(GraphicsContext g, boolean light) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(light ? Color.rgb(237, 210, 156, 0.56) : Color.rgb(198, 169, 230, 0.58));
        g.setFont(Font.font("Microsoft YaHei UI", 13));
        g.fillText("WASD / 方向键移动    ·    鼠标瞄准 / 左键攻击    ·    SHIFT 穿梭双界    ·    ESC 暂停",
                AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT - 24.0);
        g.setTextAlign(TextAlignment.LEFT);
    }
}
