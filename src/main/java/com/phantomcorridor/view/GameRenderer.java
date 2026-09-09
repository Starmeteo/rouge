package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.GameSession;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.entity.PlayerAnimationState;
import com.phantomcorridor.model.combat.Projectile;
import com.phantomcorridor.model.combat.EnemyAttack;
import com.phantomcorridor.model.entity.Enemy;
import com.phantomcorridor.model.entity.EnemyKind;
import com.phantomcorridor.model.room.Direction;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.Wall;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    private static final Map<String, Image[]> LIGHT_FRAMES = loadCharacterFrames("white_cyan");
    private static final Map<String, Image[]> SHADOW_FRAMES = loadCharacterFrames("black_magenta");
    private static final Image[] LIGHT_BULLETS = loadSeries("white_cyan", "projectiles", "bullet_fly_right", 1);
    private static final Image[] SHADOW_BULLETS = loadSeries("black_magenta", "projectiles", "bullet_fly_right", 1);
    private static final Image[] LIGHT_SLASHES = loadSeries("white_cyan", "effects_aligned", "slash_arc_right", 4);
    private static final Image[] SHADOW_SLASHES = loadSeries("black_magenta", "effects_aligned", "slash_arc_right", 4);
    private static final Image[] LIGHT_AURA = loadSeries("white_cyan", "effects_aligned", "aura", 3);
    private static final Image[] SHADOW_AURA = loadSeries("black_magenta", "effects_aligned", "aura", 3);
    private static final Map<String, Image> MONSTER_IMAGES = new HashMap<>();

    public void render(GraphicsContext g, GameSession session, double fps) {
        g.setImageSmoothing(false);
        Player player = session.getPlayer();
        boolean light = player.getCurrentWorld() == WorldType.LIGHT;
        drawFloor(g, light);
        drawRoom(g, session, light);
        drawPhaseWalls(g, session, light);
        drawEnemies(g, session, player.getCurrentWorld());
        drawEnemyAttacks(g, session, player.getCurrentWorld());
        drawPlayer(g, player, light);
        // 攻击层在角色之后绘制，避免角色把斩击和投射物遮住。
        drawAttacks(g, session, light);
        drawPhasePulse(g, session, light);
        drawAim(g, session, light);
        drawHud(g, session, fps, light);
        drawMiniMap(g, session, light);
        drawRoomAnnouncement(g, session);
        drawControls(g, light);
    }

    private void drawPhasePulse(GraphicsContext g, GameSession session, boolean light) {
        if (!session.isPhasePulseVisible()) return;
        Player player = session.getPlayer();
        double radius = GameConfig.PHASE_PULSE_RADIUS;
        Color color = light ? LIGHT_GOLD : SHADOW_VIOLET;
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.74));
        g.setLineWidth(4.0);
        g.strokeRect(player.getX() - radius, player.getY() - radius, radius * 2.0, radius * 2.0);
    }

    /** 非当前世界的敌人及攻击完全不绘制，与模型的同界碰撞规则保持一致。 */
    private void drawEnemies(GraphicsContext g, GameSession session, WorldType currentWorld) {
        List<Enemy> visible = session.getEnemies().getEnemies().stream()
                .filter(enemy -> enemy.getWorld() == currentWorld).sorted(Comparator.comparingDouble(Enemy::getY)).toList();
        for (Enemy enemy : visible) {
            String world = enemy.getWorld() == WorldType.LIGHT ? "light" : "shadow";
            Image body = monsterImage("enemies/" + enemy.getKind().assetId() + "/" + world + "/idle/front_01.png");
            double size = enemy.isBoss() ? 230 : enemy.getKind().elite() ? 154 : 118;
            double x = Math.rint(enemy.getX() - size / 2.0);
            if (body != null) {
                double height = size * body.getHeight() / Math.max(1.0, body.getWidth());
                g.drawImage(body, x, Math.rint(enemy.getY() - height * 0.70), size, height);
            } else {
                g.setFill(enemy.getWorld() == WorldType.LIGHT ? LIGHT_GOLD : SHADOW_VIOLET);
                g.fillRect(x, enemy.getY() - size / 2.0, size, size);
            }
            drawEnemyHealth(g, enemy, currentWorld == WorldType.LIGHT ? LIGHT_GOLD : SHADOW_VIOLET);
        }
    }

    private void drawEnemyHealth(GraphicsContext g, Enemy enemy, Color domain) {
        double width = enemy.isBoss() ? 126 : 58;
        double x = enemy.getX() - width / 2.0;
        double y = enemy.getY() - (enemy.isBoss() ? 120 : 66);
        g.setFill(Color.rgb(0, 0, 0, 0.68));
        g.fillRect(x, y, width, 6);
        g.setFill(Color.color(domain.getRed(), domain.getGreen(), domain.getBlue(), 0.92));
        g.fillRect(x + 1, y + 1, (width - 2) * enemy.getHp() / enemy.getMaxHp(), 4);
    }

    private void drawEnemyAttacks(GraphicsContext g, GameSession session, WorldType currentWorld) {
        for (EnemyAttack attack : session.getEnemies().getAttacks()) {
            if (attack.getWorld() != currentWorld) continue;
            String world = currentWorld == WorldType.LIGHT ? "light" : "shadow";
            String effect = enemyEffect(attack.getSource(), currentWorld);
            Image sprite = monsterImage("effects/" + attack.getSource().assetId() + "/" + world + "/" + effect + "/right_01.png");
            double size = attack.getSource() == EnemyKind.WATCHER ? 84 : 54;
            if (sprite != null) {
                double height = size * sprite.getHeight() / Math.max(1.0, sprite.getWidth());
                g.setGlobalBlendMode(BlendMode.ADD);
                g.drawImage(sprite, attack.getX() - size / 2.0, attack.getY() - height / 2.0, size, height);
                g.setGlobalBlendMode(BlendMode.SRC_OVER);
            } else {
                g.setFill(currentWorld == WorldType.LIGHT ? Color.web("#fff0a2") : Color.web("#d59aff"));
                g.fillOval(attack.getX() - attack.getRadius(), attack.getY() - attack.getRadius(),
                        attack.getRadius() * 2, attack.getRadius() * 2);
            }
        }
    }

    private static String enemyEffect(EnemyKind kind, WorldType world) {
        boolean light = world == WorldType.LIGHT;
        return switch (kind) {
            case LANTERN -> light ? "seeker_orb" : "dusk_needle";
            case WOLF -> light ? "bite_flash" : "bite_arc";
            case GOLEM -> light ? "ground_crack" : "slam_sector";
            case MAGE -> light ? "fan_pellet" : "mirror_arc";
            case EXECUTIONER -> light ? "spear_projectile" : "cleave_arc";
            case BELL -> light ? "bell_pellet" : "annular_burst";
            case WATCHER -> light ? "rift_spear" : "slash_arc";
        };
    }

    private static Image monsterImage(String relativePath) {
        if (MONSTER_IMAGES.containsKey(relativePath)) return MONSTER_IMAGES.get(relativePath);
        var resource = GameRenderer.class.getResource("/com/phantomcorridor/sprites/monsters/" + relativePath);
        Image image = resource == null ? null : new Image(resource.toExternalForm(), false);
        MONSTER_IMAGES.put(relativePath, image);
        return image;
    }

    private void drawAttacks(GraphicsContext g, GameSession session, boolean light) {
        Player player = session.getPlayer();
        for (Projectile projectile : session.getAttackSystem().getProjectiles()) {
            double radius = projectile.getRadius();
            Image[] bullets = light ? LIGHT_BULLETS : SHADOW_BULLETS;
            if (bullets.length > 0) {
                Image bullet = bullets[0];
                double size = Math.max(28.0, radius * 7.0);
                double height = size * bullet.getHeight() / Math.max(1.0, bullet.getWidth());
                boolean reverse = projectile.getVelocityX() < 0;
                g.drawImage(bullet, reverse ? projectile.getX() + size / 2.0 : projectile.getX() - size / 2.0,
                        projectile.getY() - height / 2.0, reverse ? -size : size, height);
                continue;
            }
            g.setGlobalBlendMode(BlendMode.ADD);
            g.setFill(Color.rgb(255, 220, 138, 0.30));
            g.fillRect(projectile.getX() - radius * 2.5, projectile.getY() - radius * 2.5,
                    radius * 5.0, radius * 5.0);
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
            g.setFill(Color.web("#fff2bd"));
            g.fillRect(projectile.getX() - radius, projectile.getY() - radius,
                    radius * 2.0, radius * 2.0);
        }
        if (!light && session.getAttackSystem().isMeleeVisible()) {
            Image[] slashes = SHADOW_SLASHES;
            if (slashes.length > 0) {
                Image slash = slashes[Math.min(slashes.length - 1,
                        (int) (player.getAnimationTime() * 12.0) % slashes.length)];
                double size = 160.0;
                double angle = Math.toDegrees(Math.atan2(session.getAimY() - player.getY(), session.getAimX() - player.getX()));
                g.save();
                g.translate(player.getX(), player.getY());
                g.rotate(angle);
                g.drawImage(slash, -size / 2.0, -size * 0.5,
                        size, size * slash.getHeight() / Math.max(1.0, slash.getWidth()));
                g.restore();
                return;
            }
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
        if (player.getAnimationState() == PlayerAnimationState.SHIFTING) {
            Image[] aura = light ? LIGHT_AURA : SHADOW_AURA;
            if (aura.length > 0) {
                Image frame = aura[Math.min(aura.length - 1,
                        (int) (player.getAnimationTime() * 10.0) % aura.length)];
                double size = 180.0;
                g.setGlobalBlendMode(BlendMode.ADD);
                g.drawImage(frame, player.getX() - size / 2.0, player.getY() - size * 0.5,
                        size, size * frame.getHeight() / Math.max(1.0, frame.getWidth()));
                g.setGlobalBlendMode(BlendMode.SRC_OVER);
            }
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
        g.strokeRect(cursorX - 5.0, cursorY - 5.0, 10.0, 10.0);
    }

    private void drawFloor(GraphicsContext g, boolean light) {
        g.setFill(light ? Color.web("#100f12") : Color.web("#08070d"));
        g.fillRect(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        g.setFill(light ? Color.web("#17151a") : Color.web("#0e0b16"));
        for (int y = 0; y < AppConfig.VIEW_HEIGHT; y += 16) {
            for (int x = (y / 16 % 2) * 16; x < AppConfig.VIEW_WIDTH; x += 32) {
                g.fillRect(x, y, 16, 16);
            }
        }
    }

    private void drawRoom(GraphicsContext g, GameSession session, boolean light) {
        Room room = session.getNavigation().getCurrentRoom();
        Color floor = light ? Color.web("#332d25") : Color.web("#231a31");
        Color tile = light ? Color.web("#3e372c") : Color.web("#2d213e");
        Color border = light ? Color.web("#c09145") : Color.web("#7a4eb0");
        for (RoomArea area : room.areas()) {
            g.setFill(floor);
            g.fillRect(area.x(), area.y(), area.width(), area.height());
            g.setFill(tile);
            for (double y = area.y(); y < area.y() + area.height(); y += 32) {
                for (double x = area.x() + (((int) ((y - area.y()) / 32)) % 2) * 16;
                     x < area.x() + area.width(); x += 32) g.fillRect(x, y, 16, 16);
            }
            g.setStroke(Color.rgb(0, 0, 0, 0.72));
            g.setLineWidth(14);
            g.strokeRect(area.x(), area.y(), area.width(), area.height());
            g.setStroke(border);
            g.setLineWidth(4);
            g.strokeRect(area.x(), area.y(), area.width(), area.height());
        }

        for (Direction direction : Direction.values()) {
            if (room.hasDoor(direction)) {
                drawDoor(g, room, direction, room.isDoorOpen(direction), light);
            }
        }
    }

    private void drawDoor(GraphicsContext g, Room room, Direction direction, boolean open, boolean light) {
        double half = com.phantomcorridor.config.RoomConfig.DOOR_HALF_WIDTH;
        double center = room.doorCenter(direction);
        Color portal = open ? (light ? Color.web("#ffe08a") : Color.web("#bd78ff")) : Color.web("#d84b55");
        g.setFill(Color.web("#05050a"));
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            double y = direction == Direction.NORTH ? room.minY() - 9 : room.maxY() - 9;
            g.fillRect(center - half, y, half * 2, 18);
            g.setFill(portal);
            for (double x = center - half + 6; x < center + half - 6; x += 16) g.fillRect(x, y + 5, 10, 8);
            g.fillRect(center - 5, y + (direction == Direction.NORTH ? 18 : -10), 10, 10);
        } else {
            double x = direction == Direction.WEST ? room.minX() - 9 : room.maxX() - 9;
            g.fillRect(x, center - half, 18, half * 2);
            g.setFill(portal);
            for (double y = center - half + 6; y < center + half - 6; y += 16) g.fillRect(x + 5, y, 8, 10);
            g.fillRect(x + (direction == Direction.WEST ? 18 : -10), center - 5, 10, 10);
        }
    }

    private void drawPhaseWalls(GraphicsContext g, GameSession session, boolean light) {
        for (Wall wall : session.getNavigation().getCurrentRoom().walls()) {
            boolean active = wall.activeIn(session.getPlayer().getCurrentWorld());
            Color color = wall.world() == null ? Color.web("#85808a")
                    : wall.world() == WorldType.LIGHT ? LIGHT_GOLD : SHADOW_VIOLET;
            drawPhaseWall(g, wall.x(), wall.y(), wall.width(), wall.height(), color, active ? 0.86 : 0.18);
        }
    }

    private void drawMiniMap(GraphicsContext g, GameSession session, boolean light) {
        Room current = session.getNavigation().getCurrentRoom();
        double panelX = AppConfig.VIEW_WIDTH - 274.0;
        double panelY = 205.0;
        double panelSize = 238.0;
        double originX = panelX + panelSize / 2.0;
        double originY = panelY + panelSize / 2.0;
        double scale = 25.0;
        g.save();
        g.beginPath(); g.rect(panelX, panelY, panelSize, panelSize); g.closePath(); g.clip();
        g.setFill(Color.rgb(3, 3, 7, 0.88));
        g.fillRect(panelX, panelY, panelSize, panelSize);
        g.setStroke(light ? Color.web("#76572d") : Color.web("#533478"));
        g.setLineWidth(3); g.strokeRect(panelX, panelY, panelSize, panelSize);
        g.setStroke(Color.rgb(220, 210, 225, 0.28));
        g.setLineWidth(2.0);
        for (Room room : session.getNavigation().getMap().rooms()) {
            if (!room.isDiscovered()) continue;
            for (var edge : room.neighbors().entrySet()) {
                Room neighbor = session.getNavigation().getMap().room(edge.getValue());
                if (!neighbor.isDiscovered() || room.id() > neighbor.id()) continue;
                g.strokeLine(originX + (room.mapX() - current.mapX()) * scale,
                        originY + (room.mapY() - current.mapY()) * scale,
                        originX + (neighbor.mapX() - current.mapX()) * scale,
                        originY + (neighbor.mapY() - current.mapY()) * scale);
            }
        }
        for (Room room : session.getNavigation().getMap().rooms()) {
            if (!room.isDiscovered()) continue;
            double x = originX + (room.mapX() - current.mapX()) * scale;
            double y = originY + (room.mapY() - current.mapY()) * scale;
            if (room == current) {
                g.setFill(light ? Color.web("#fff0a8") : Color.web("#d5a0ff"));
                g.fillRect(x - 7, y - 7, 14, 14);
                g.setFill(Color.web("#ffffff"));
                g.fillRect(x - 2, y - 2, 4, 4);
            } else if (room.isVisited()) {
                g.setFill(room.type() == com.phantomcorridor.model.RoomType.BOSS
                        ? Color.web("#c54e58") : Color.web("#8e8995"));
                g.fillRect(x - 5, y - 5, 10, 10);
            } else {
                g.setStroke(room.type() == com.phantomcorridor.model.RoomType.BOSS
                        ? Color.web("#d94b5b") : Color.web("#77717f"));
                g.setLineWidth(2);
                g.strokeRect(x - 5, y - 5, 10, 10);
            }
        }
        g.restore();
        g.setFill(Color.rgb(235, 226, 242, 0.76));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        g.fillText("探索地图", panelX + 12, panelY + panelSize + 18);
    }

    private void drawPhaseWall(GraphicsContext g, double x, double y, double width, double height,
                               Color color, double alpha) {
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha * 0.66));
        g.fillRect(x, y, width, height);
        g.setFill(Color.color(0.05, 0.04, 0.08, alpha));
        for (double py = y + 4; py < y + height; py += 16) {
            for (double px = x + 4; px < x + width; px += 16) g.fillRect(px, py, 7, 7);
        }
    }

    private void drawPlayer(GraphicsContext g, Player player, boolean light) {
        if (!(light ? LIGHT_FRAMES : SHADOW_FRAMES).isEmpty()) {
            drawSpritePlayer(g, player, light);
            return;
        }
        Color domain = light ? LIGHT_GOLD : SHADOW_VIOLET;
        double x = Math.rint(player.getX());
        double y = Math.rint(player.getY());
        PlayerAnimationState state = player.getAnimationState();
        int frame = (int) (player.getAnimationTime() * 8) & 1;
        if (state == PlayerAnimationState.DOWN) {
            g.setFill(Color.web("#17131d")); g.fillRect(x - 19, y + 4, 38, 10);
            g.setFill(domain); g.fillRect(x - 15, y, 24, 8);
            g.setFill(light ? Color.web("#f4e5bd") : Color.web("#cbb0e8")); g.fillRect(x + 9, y + 2, 9, 9);
            return;
        }
        double bob = state == PlayerAnimationState.MOVING && frame == 1 ? -3 : 0;
        if (state == PlayerAnimationState.SHIFTING && frame == 1) {
            g.setFill(Color.rgb(255, 255, 255, 0.34)); g.fillRect(x - 24, y - 28, 48, 52);
        }
        g.setFill(Color.rgb(0, 0, 0, 0.55)); g.fillRect(x - 14, y + 14, 28, 7);
        g.setFill(Color.web("#15121a")); g.fillRect(x - 13, y - 15 + bob, 26, 28);
        g.setFill(domain); g.fillRect(x - 10, y - 18 + bob, 20, 7);
        g.fillRect(x - 14, y - 8 + bob, 5, 17); g.fillRect(x + 9, y - 8 + bob, 5, 17);
        g.setFill(light ? Color.web("#f4e5bd") : Color.web("#cbb0e8"));
        g.fillRect(x - 7, y - 10 + bob, 14, 12);
        g.setFill(Color.web("#18121d")); g.fillRect(x - 4, y - 6 + bob, 3, 3); g.fillRect(x + 3, y - 6 + bob, 3, 3);
        g.setFill(domain);
        double legOffset = state == PlayerAnimationState.MOVING ? (frame == 0 ? 4 : -4) : 0;
        g.fillRect(x - 10 + legOffset, y + 11 + bob, 7, 10);
        g.fillRect(x + 3 - legOffset, y + 11 + bob, 7, 10);
        if (state == PlayerAnimationState.ATTACKING) {
            int fx = player.getFacingX() >= 0 ? 1 : -1;
            g.setFill(light ? Color.web("#fff2a3") : Color.web("#d398ff"));
            g.fillRect(x + fx * 14 - (fx < 0 ? 12 : 0), y - 3, 12, 7);
        }
    }

    private void drawSpritePlayer(GraphicsContext g, Player player, boolean light) {
        Map<String, Image[]> all = light ? LIGHT_FRAMES : SHADOW_FRAMES;
        String direction = player.getFacingY() < -0.35 ? "back" : player.getFacingY() > 0.35 ? "front"
                : player.getFacingX() < 0 ? "left" : "right";
        boolean mirror = direction.equals("left");
        String assetDirection = mirror ? "right" : direction;
        String action = switch (player.getAnimationState()) {
            case ATTACKING -> "slash_" + (assetDirection.equals("right") ? "right" : "recover_front");
            case SHIFTING -> "cast_right";
            case DOWN -> "down_" + assetDirection;
            case MOVING -> "move_" + assetDirection;
            default -> "idle_" + assetDirection;
        };
        Image[] frames = all.getOrDefault(action, all.getOrDefault("idle_front", new Image[0]));
        if (frames.length == 0) return;
        int frame = (player.getAnimationState() == PlayerAnimationState.IDLE
                || player.getAnimationState() == PlayerAnimationState.DOWN) ? 0
                : Math.min(frames.length - 1, (int) (player.getAnimationTime() * 10.0) % frames.length);
        Image sprite = frames[frame];
        double drawW = 160.0;
        double drawH = drawW * sprite.getHeight() / Math.max(1.0, sprite.getWidth());
        double left = Math.rint(player.getX() - drawW / 2);
        // v2 资源的视觉锚点落在胸口附近，而不是画布顶部或脚底。
        double top = Math.rint(player.getY() - drawH * 0.58);
        g.drawImage(sprite, mirror ? left + drawW : left, top, mirror ? -drawW : drawW, drawH);
    }

    private void drawRoomAnnouncement(GraphicsContext g, GameSession session) {
        if (!session.isRoomAnnouncementVisible()) return;
        double remaining = session.getRoomAnnouncementRemaining();
        double alpha = remaining < 0.35 ? Math.max(0.0, remaining / 0.35) : 1.0;
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        g.setFill(Color.rgb(255, 239, 178, alpha));
        g.fillText("◆  " + session.getRoomAnnouncement() + "  ◆", AppConfig.VIEW_WIDTH / 2.0, 84);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private static Map<String, Image[]> loadCharacterFrames(String palette) {
        Map<String, Image[]> result = new HashMap<>();
        String[][] specs = {
                {"idle_front", "2"}, {"idle_back", "1"}, {"idle_right", "1"},
                {"move_front", "2"}, {"move_back", "2"}, {"move_right", "4"},
                {"cast_right", "6"}, {"slash_right", "5"}, {"slash_recover_front", "6"},
                {"down_front", "1"}, {"down_back", "1"}, {"down_right", "2"}
        };
        for (String[] spec : specs) result.put(spec[0], loadSeries(palette, "characters", spec[0], Integer.parseInt(spec[1])));
        return result;
    }

    private static Image[] loadSeries(String palette, String folder, String prefix, int count) {
        List<Image> frames = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            var resource = GameRenderer.class.getResource("/com/phantomcorridor/sprites/v2/" + palette + "/" + folder + "/" + prefix + "_" + String.format("%02d", i) + ".png");
            if (resource != null) frames.add(new Image(resource.toExternalForm(), false));
        }
        return frames.toArray(Image[]::new);
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
