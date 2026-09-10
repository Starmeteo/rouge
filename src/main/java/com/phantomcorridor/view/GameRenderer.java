package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.GameSession;
import com.phantomcorridor.model.Pickup;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.entity.PlayerAnimationState;
import com.phantomcorridor.model.combat.Projectile;
import com.phantomcorridor.model.combat.EnemyAttack;
import com.phantomcorridor.model.combat.EnemyProjectile;
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
    /** 小地图：已清空的怪物房标记色与“还有东西可拿”的金点色。 */
    private static final Color CLEARED_GREEN = Color.web("#7fbf7a");
    private static final Color LOOT_GOLD = Color.web("#f7d56e");
    private static final Map<String, Image[]> LIGHT_FRAMES = loadCharacterFrames("white_cyan");
    private static final Map<String, Image[]> SHADOW_FRAMES = loadCharacterFrames("black_magenta");
    private static final Image[] LIGHT_BULLETS = loadSeries("white_cyan", "projectiles", "bullet_fly_right", 1);
    private static final Image[] SHADOW_BULLETS = loadSeries("black_magenta", "projectiles", "bullet_fly_right", 1);
    private static final Image[] LIGHT_SLASHES = loadSeries("white_cyan", "effects_aligned", "slash_arc_right", 4);
    private static final Image[] SHADOW_SLASHES = loadSeries("black_magenta", "effects_aligned", "slash_arc_right", 4);
    private static final Image[] LIGHT_AURA = loadSeries("white_cyan", "effects_aligned", "aura", 3);
    private static final Image[] SHADOW_AURA = loadSeries("black_magenta", "effects_aligned", "aura", 3);
    private static final Image REWARD_ICONS = loadUiImage("reward_icons_v1.png");
    private static final Image WEAPON_ICONS = loadUiImage("weapons_v1.png");
    private static final Image EQUIPMENT_ICONS = loadUiImage("equipment_v1.png");
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
        drawPickups(g, session);
        drawInteractionPrompt(g, session);
        drawPlayer(g, player, light);
        // 攻击层在角色之后绘制，避免角色把斩击和投射物遮住。
        drawAttacks(g, session, light);
        drawPhasePulse(g, session, light);
        drawAim(g, session, light);
        drawHud(g, session, fps, light);
        drawMiniMap(g, session, light);
        drawRoomAnnouncement(g, session);
        drawControls(g, light);
        if (player.getHp() <= 0) drawDeathOverlay(g, session);
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

    private void drawEnemyProjectiles(GraphicsContext g, GameSession session, boolean light) {
        for (EnemyProjectile p : session.getEnemyProjectiles().getProjectiles()) {
            if ((p.getWorld() == WorldType.LIGHT) != light) continue;
            double r = p.getRadius();
            g.setGlobalBlendMode(BlendMode.ADD);
            g.setFill(light ? Color.rgb(255, 231, 151, .75) : Color.rgb(218, 105, 255, .78));
            g.fillRect(p.getX() - r, p.getY() - r, r * 2, r * 2);
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
    }

    private void drawEnemies(GraphicsContext g, GameSession session, boolean light) {
        for (Enemy enemy : session.getEnemies().getEnemies()) {
            if ((enemy.getWorld() == WorldType.LIGHT) != light) continue;
            String action = enemy.getAlertRemaining() > 0.0 ? "attack_windup" : "idle";
            String direction = "front";
            var resource = getClass().getResource("/com/phantomcorridor/enemies/atlases/" + enemy.getKind().assetId()
                    + "/" + (light ? "light" : "shadow") + "/body/" + action + "/" + direction + ".png");
            if (resource == null) resource = getClass().getResource("/com/phantomcorridor/enemies/atlases/" + enemy.getKind().assetId()
                    + "/" + (light ? "light" : "shadow") + "/body/idle/front.png");
            if (resource == null) continue;
            Image atlas = new Image(resource.toExternalForm(), false);
            double frameW = atlas.getWidth() / 4.0;
            int frame = 0;
            double size = enemy.isBoss() ? 260 : enemy.getKind().elite() ? 180 : 132;
            double h = size * atlas.getHeight() / Math.max(1.0, atlas.getWidth() / 4.0);
            g.drawImage(atlas, frame * frameW, 0, frameW, atlas.getHeight(),
                        enemy.getX() - size / 2.0, enemy.getY() - h * .875, size, h);
            g.setFill(Color.rgb(20, 10, 18, .8)); g.fillRect(enemy.getX() - 28, enemy.getY() - h * .95, 56, 5);
            g.setFill(light ? Color.web("#f1c56e") : Color.web("#d783ff"));
            g.fillRect(enemy.getX() - 28, enemy.getY() - h * .95, 56.0 * enemy.getHp() / enemy.getMaxHp(), 5);
            if (enemy.getAlertRemaining() > 0.0) {
                g.setStroke(light ? Color.web("#ffe89a") : Color.web("#ed8cff"));
                g.setLineWidth(3); g.strokeOval(enemy.getX() - 28, enemy.getY() - 28, 56, 56);
            }
        }
    }

    private void drawPickups(GraphicsContext g, GameSession session) {
        for (var pickup : session.getPickups()) {
            Color color = switch (pickup.type()) {
                case COIN -> Color.web("#f7d56e");
                case PHASE_FRAGMENT -> Color.web("#b882ff");
                case HEALTH -> Color.web("#e86b70");
                case ITEM -> Color.web("#70d8ff");
                case EQUIPMENT -> Color.web("#f0c86e");
            };
            // 商店商品额外画出价格，并标出“已选中、等待确认”的那一件。
            int price = session.getShopPrice(pickup);
            if (price >= 0) drawPriceTag(g, session, pickup, price);

            int icon = pickup.type() == com.phantomcorridor.model.Pickup.Type.COIN ? 1
                    : pickup.type() == com.phantomcorridor.model.Pickup.Type.ITEM ? 2 + Math.floorMod(pickup.amount(), 6) : -1;
            if (pickup.type() == com.phantomcorridor.model.Pickup.Type.EQUIPMENT) {
                Image source = pickup.amount() < 3 ? WEAPON_ICONS : EQUIPMENT_ICONS;
                int local = pickup.amount() % 3;
                if (source != null) g.drawImage(source, local * source.getWidth() / 3.0, 0,
                        source.getWidth() / 3.0, source.getHeight(), pickup.x() - 24, pickup.y() - 24, 48, 48);
                else { g.setFill(color); g.fillRect(pickup.x() - 8, pickup.y() - 8, 16, 16); }
            } else if (icon >= 0 && REWARD_ICONS != null) {
                double cellW = REWARD_ICONS.getWidth() / 4.0, cellH = REWARD_ICONS.getHeight() / 2.0;
                double sx = (icon % 4) * cellW, sy = (icon / 4) * cellH;
                g.drawImage(REWARD_ICONS, sx, sy, cellW, cellH, pickup.x() - 22, pickup.y() - 22, 44, 44);
            } else {
                g.setFill(color);
                g.fillRect(pickup.x() - 7, pickup.y() - 7, 14, 14);
            }
            if (price >= 0 && session.isShopOfferSelected(pickup)) {
                g.setStroke(Color.web("#fff2b0"));
                g.setLineWidth(3.0);
                g.strokeRect(pickup.x() - 26, pickup.y() - 26, 52, 52);
            }
            g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), .45));
            g.strokeRect(pickup.x() - 11, pickup.y() - 11, 22, 22);
        }
        if (session.isChestVisible()) {
            Room room = session.getNavigation().getCurrentRoom();
            double x = room.doorCenter(Direction.NORTH), y = room.minY() + RoomConfig.CHEST_OFFSET_Y;
            if (REWARD_ICONS != null) {
                double cellW = REWARD_ICONS.getWidth() / 4.0, cellH = REWARD_ICONS.getHeight() / 2.0;
                g.drawImage(REWARD_ICONS, 0, 0, cellW, cellH, x - 32, y - 32, 64, 64);
            } else {
                g.setFill(Color.web("#8d542e")); g.fillRect(x - 22, y - 16, 44, 28);
                g.setFill(Color.web("#f3cf6b")); g.fillRect(x - 4, y - 4, 8, 10);
                g.setStroke(Color.web("#f0b858")); g.strokeRect(x - 22, y - 16, 44, 28);
            }
        }
    }

    private void drawDeathOverlay(GraphicsContext g, GameSession session) {
        g.setFill(Color.rgb(8, 4, 12, 0.78));
        g.fillRect(0, 0, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        double centerX = AppConfig.VIEW_WIDTH / 2.0;
        double centerY = AppConfig.VIEW_HEIGHT / 2.0;
        g.setFill(Color.rgb(18, 12, 28, .97));
        g.fillRoundRect(centerX - 300, centerY - 160, 600, 330, 24, 24);
        g.setStroke(Color.web("#a878c7")); g.setLineWidth(2.0);
        g.strokeRoundRect(centerX - 300, centerY - 160, 600, 330, 24, 24);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#f0d7e8"));
        g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 42));
        g.fillText("倒下了", AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0 - 24);
        g.setFont(Font.font("Microsoft YaHei UI", 18));
        g.setFill(Color.web("#c9b4ca"));
        g.fillText("本次探索结束 · 金币 " + session.getCoins(), AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0 + 18);
        drawDeathButton(g, session, centerX - 170, centerY + 44, 140, 50, "重新开始", false);
        drawDeathButton(g, session, centerX + 30, centerY + 44, 140, 50, "返回主菜单", true);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawDeathButton(GraphicsContext g, GameSession session, double x, double y,
                                 double width, double height, String label, boolean menu) {
        boolean hover = session.getAimX() >= x && session.getAimX() <= x + width
                && session.getAimY() >= y && session.getAimY() <= y + height;
        g.setFill(Color.rgb(0, 0, 0, .35)); g.fillRoundRect(x + 4, y + 5, width, height, 10, 10);
        Color base = menu ? Color.web("#6b4a92") : Color.web("#9a5d72");
        g.setFill(hover ? base.brighter() : base);
        g.fillRoundRect(x, y - (hover ? 2 : 0), width, height, 10, 10);
        g.setStroke(hover ? Color.web("#fff0bd") : Color.web("#dcb8e5")); g.setLineWidth(2.0);
        g.strokeRoundRect(x, y - (hover ? 2 : 0), width, height, 10, 10);
        g.setFill(Color.web("#fff6e8")); g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 16));
        g.fillText(label, x + width / 2.0, y + 31 - (hover ? 2 : 0));
    }

    /** 商店商品的价格牌：买得起显示金色，买不起显示灰红色并写明状态。 */
    private void drawPriceTag(GraphicsContext g, GameSession session, Pickup pickup, int price) {
        boolean affordable = session.canAfford(pickup);
        boolean selected = session.isShopOfferSelected(pickup);
        String text = price + " 金币";
        double width = 26 + text.length() * 8.0;
        double x = pickup.x() - width / 2.0;
        double y = pickup.y() + 30;
        g.setFill(Color.rgb(8, 6, 12, selected ? 0.95 : 0.82));
        g.fillRoundRect(x, y, width, 22, 7, 7);
        g.setStroke(selected ? Color.web("#fff2b0")
                : affordable ? Color.web("#f0c86e") : Color.web("#8a5a60"));
        g.setLineWidth(selected ? 2.5 : 1.5);
        g.strokeRoundRect(x, y, width, 22, 7, 7);
        g.setFill(affordable ? Color.web("#ffe6a6") : Color.web("#c98f93"));
        g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 13));
        g.fillText(text, x + 13, y + 16);
        if (selected) {
            g.setFill(Color.web("#fff2b0"));
            g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 12));
            g.fillText(affordable ? "再按 E 确认" : "金币不足", x - 2, y + 40);
        }
    }

    private void drawInteractionPrompt(GraphicsContext g, GameSession session) {
        String prompt = session.getInteractionPrompt();
        if (prompt.isEmpty()) return;
        Player p = session.getPlayer();
        boolean confirming = prompt.startsWith("E  确认") || prompt.startsWith("金币不足");
        double width = 24 + prompt.length() * 13.0;
        // 提示框贴着角色，但不能顶出画布：商店确认文案比旧提示长不少。
        double x = Math.max(8, Math.min(p.getX() + 24, AppConfig.VIEW_WIDTH - width - 8));
        double y = Math.max(8, p.getY() - 58);
        g.setFill(Color.rgb(8, 6, 12, .92));
        g.fillRoundRect(x, y, width, 32, 8, 8);
        g.setStroke(confirming ? Color.web("#ffd766") : Color.web("#f2d27a"));
        g.setLineWidth(confirming ? 2.5 : 1.0);
        g.strokeRoundRect(x, y, width, 32, 8, 8);
        g.setFill(confirming ? Color.web("#fff6cf") : Color.web("#fff0bb"));
        g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 14));
        g.fillText(prompt, x + 12, y + 21);
    }

    private void drawEquipmentBar(GraphicsContext g, GameSession session) {
        Player player = session.getPlayer();
        g.setFill(Color.rgb(4, 4, 8, .74));
        g.fillRoundRect(56, 228, 370, 84, 12, 12);
        g.setFill(Color.web("#d8c9df")); g.setFont(Font.font("Microsoft YaHei UI", 12));
        g.fillText("装备（最多 3 件）", 70, 248);
        g.setFill(Color.web("#a99bb2"));
        g.fillText("攻击 1 + " + (player.getAttackDamage() - 1) + "  ·  已装备属性实时生效", 70, 306);
        int index = 0;
        for (var item : player.getEquipment()) {
            Image source = item.iconIndex() < 3 ? WEAPON_ICONS : EQUIPMENT_ICONS;
            int local = item.iconIndex() % 3;
            double x = 110 + index * 52;
            if (source != null) g.drawImage(source, local * source.getWidth() / 3.0, 0,
                    source.getWidth() / 3.0, source.getHeight(), x, 255, 42, 42);
            if (Math.hypot(session.getAimX() - (x + 21), session.getAimY() - 276) < 26) {
                g.setFill(Color.rgb(10, 8, 16, .94)); g.fillRoundRect(x, 300, 220, 38, 7, 7);
                g.setFill(Color.WHITE); g.fillText(item.displayName() + "：" + item.description(), x + 6, 324);
            }
            index++;
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
        if (session.getLightEnemyCount() + session.getShadowEnemyCount() > 0) return;
        Room current = session.getNavigation().getCurrentRoom();
        double panelX = AppConfig.VIEW_WIDTH - 226.0;
        double panelY = 188.0;
        double panelSize = 178.0;
        double originX = panelX + panelSize / 2.0;
        double originY = panelY + panelSize / 2.0;
        double scale = 19.0;
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
            // 状态提示：已清空的怪物房套一圈绿框（不用再回去），还有东西可拿的房间点一颗金点。
            if (room.isDefeatedBattleRoom()) {
                g.setStroke(CLEARED_GREEN);
                g.setLineWidth(2.0);
                g.strokeRect(x - 8.5, y - 8.5, 17, 17);
            }
            if (room.hasRemainingLoot()) {
                g.setFill(LOOT_GOLD);
                g.fillOval(x + 2.5, y - 9.5, 7, 7);
                g.setStroke(Color.rgb(30, 20, 8, 0.85));
                g.setLineWidth(1.0);
                g.strokeOval(x + 2.5, y - 9.5, 7, 7);
            }
        }
        g.restore();
        g.setFill(Color.rgb(235, 226, 242, 0.76));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        g.fillText("探索地图", panelX + 12, panelY + panelSize + 18);
        g.setFill(LOOT_GOLD);
        g.fillOval(panelX + 12, panelY + panelSize + 25, 7, 7);
        g.setFill(Color.rgb(235, 226, 242, 0.62));
        g.fillText("有未拿取", panelX + 24, panelY + panelSize + 32);
        g.setStroke(CLEARED_GREEN);
        g.setLineWidth(2.0);
        g.strokeRect(panelX + 96, panelY + panelSize + 23, 11, 11);
        g.setFill(Color.rgb(235, 226, 242, 0.62));
        g.fillText("已清空", panelX + 113, panelY + panelSize + 32);
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

    private static Image loadUiImage(String name) {
        var resource = GameRenderer.class.getResource("/com/phantomcorridor/ui/" + name);
        return resource == null ? null : new Image(resource.toExternalForm(), false);
    }

    private void drawHud(GraphicsContext g, GameSession session, double fps, boolean light) {
        Color domain = light ? LIGHT_GOLD : SHADOW_VIOLET;
        Player player = session.getPlayer();

        g.setFill(Color.rgb(4, 4, 8, 0.76));
        g.fillRoundRect(56, 58, 370, 164, 16, 16);
        g.setStroke(Color.color(domain.getRed(), domain.getGreen(), domain.getBlue(), 0.55));
        g.setLineWidth(1.0);
        g.strokeRoundRect(56, 58, 370, 164, 16, 16);

        g.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 17));
        g.setFill(Color.web("#efe7d8"));
        g.fillText("生命", 78, 91);
        for (int i = 0; i < GameConfig.PLAYER_MAX_HP; i++) {
            g.setFill(i < player.getHp() ? Color.web("#d75b54") : Color.web("#3b2528"));
            g.fillOval(137 + i * 25.0, 76, 14, 14);
        }

        g.setFill(Color.rgb(111, 177, 255, 0.9));
        g.fillText("攻击", 78, 130);
        int chargeSlots = player.getMaxAttackCharges();
        double cellWidth = Math.min(40.0, 240.0 / Math.max(1, chargeSlots) - 6.0);
        for (int i = 0; i < chargeSlots; i++) {
            boolean filled = i < player.getAttackCharges();
            double x = 137 + i * (cellWidth + 6);
            g.setFill(filled ? Color.web("#61a9ff") : Color.rgb(255, 255, 255, 0.10));
            g.fillRoundRect(x, 118, cellWidth, 12, 3, 3);
            g.setStroke(Color.color(0.38, 0.66, 1.0, filled ? 0.9 : 0.28));
            g.setLineWidth(1.0); g.strokeRoundRect(x, 118, cellWidth, 12, 3, 3);
        }

        g.setFill(Color.web("#efe7d8"));
        g.fillText("相位", 78, 177);
        g.setFill(Color.rgb(255, 255, 255, 0.09));
        g.fillRoundRect(137, 160, 252, 18, 9, 9);
        double ratio = player.getPhaseEnergy() / GameConfig.PHASE_ENERGY_MAX;
        g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, LIGHT_GOLD), new Stop(1, SHADOW_VIOLET)));
        g.fillRoundRect(137, 160, 252 * ratio, 18, 9, 9);
        if (ratio >= 0.999) {
            g.setStroke(Color.web("#fff4bc")); g.setLineWidth(2.0);
            g.strokeRoundRect(135, 158, 256, 22, 11, 11);
        }

        g.setFill(Color.rgb(235, 226, 242, 0.64));
        g.fillText("光界残敌  " + session.getLightEnemyCount() + "     影界残敌  "
                + session.getShadowEnemyCount() + "     金币  " + session.getCoins(), 78, 207);

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
        drawEquipmentBar(g, session);
    }

    private void drawControls(GraphicsContext g, boolean light) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(light ? Color.rgb(237, 210, 156, 0.56) : Color.rgb(198, 169, 230, 0.58));
        g.setFont(Font.font("Microsoft YaHei UI", 13));
        g.fillText("WASD / 方向键移动    ·    鼠标瞄准 / 左键攻击    ·    E 交互    ·    TAB 穿梭双界    ·    ESC 暂停",
                AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT - 24.0);
        g.setTextAlign(TextAlignment.LEFT);
    }
}
