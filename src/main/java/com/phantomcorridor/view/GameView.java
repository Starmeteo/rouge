package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.core.GameLoop;
import com.phantomcorridor.model.WorldType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 游戏主界面（对应《双界行者》需求 §5.4 渲染方案、§9.1 view.GameView）。
 *
 * <p>游戏画面使用 {@link Canvas} 绘制（适合大量动态物体，避免复杂 JavaFX 节点树性能开销），
 * 由 {@link GameLoop} 驱动「逻辑更新 → 渲染」管线。
 *
 * <p><b>双界主题（§3.1 / §8.4）</b>：当前世界为<b>光之界</b>（默认，§3.1）时画面明亮温暖，
 * 以金色/白色为主，玩家占位球携带<b>金色光环</b>；切换到<b>影之界</b>时画面转冷暗紫黑，
 * 光环为紫色（世界切换系统第 4 天接入，此处按 {@link #currentWorld} 占位切换渲染）。
 * 右上角显示当前世界徽章（光 ☀ / 影 ☾）。
 *
 * <p><b>架构对齐占位实现</b>：一个调试用弹跳小球用于验证固定时间步长与渲染管线，
 * 移动速度取 {@link GameConfig#PLAYER_BASE_SPEED}。后续里程碑将替换为真实玩家实体与正式 HUD（§8.3）。
 */
public class GameView extends StackPane {

    /** 调试占位小球半径（像素） */
    private static final double DEBUG_BALL_RADIUS = 12.0;

    /** 玩家光环半径（§8.4 形态光环，占位绘制） */
    private static final double AURA_RADIUS = 28.0;

    /** 世界徽章半径（右上角占位指示，光☀/影☾） */
    private static final double BADGE_RADIUS = 26.0;

    // ---- 双界配色（§3.1 世界基调 + §8.4 光环） ----
    /** 光之界：地面亮暖金渐变（上→下） */
    private static final LinearGradient LIGHT_FLOOR = new LinearGradient(0, 0, 0, 1, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#ecd9a6")), new Stop(1.0, Color.web("#c9ad73")));
    /** 光之界：墙体边框暖褐金 */
    private static final Color LIGHT_BORDER = Color.web("#8a6a3a");
    /** 光之界：金色光环（§8.4） */
    private static final Color LIGHT_AURA = Color.web("#ffd76e");
    /** 光之界：调试文字深褐 */
    private static final Color LIGHT_TEXT = Color.web("#4a3418");

    /** 影之界：地面冷暗紫渐变（上→下） */
    private static final LinearGradient SHADOW_FLOOR = new LinearGradient(0, 0, 0, 1, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#1c1030")), new Stop(1.0, Color.web("#0d0716")));
    /** 影之界：墙体边框冷紫 */
    private static final Color SHADOW_BORDER = Color.web("#5a3a8a");
    /** 影之界：紫色光环（§8.4） */
    private static final Color SHADOW_AURA = Color.web("#a878e8");
    /** 影之界：调试文字淡紫 */
    private static final Color SHADOW_TEXT = Color.web("#c9b6f0");

    /** 占位当前世界（双世界切换系统未实现前固定为光之界，§3.1 默认世界） */
    private int currentWorld = 0;

    /** 绘制画布（与游戏视口同尺寸） */
    private final Canvas canvas = new Canvas(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);

    /** 游戏主循环 */
    private final GameLoop gameLoop;

    /** 主循环是否处于运行状态（保证 start/stop 幂等，避免重复调用报错） */
    private boolean loopRunning = false;

    // ---- 调试占位状态：弹跳小球（后续由 model.entity.Player 取代） ----
    /** 小球 X 坐标（像素） */
    private double ballX;
    /** 小球 Y 坐标（像素） */
    private double ballY;
    /** 小球 X 方向速度分量（归一化后乘速度值） */
    private double ballDirX;
    /** 小球 Y 方向速度分量 */
    private double ballDirY;

    /**
     * 构建游戏面板。
     *
     * @param onPauseRequested 暂停请求回调（游戏中按 Esc/P 时触发，由 App 切换到暂停界面）
     */
    public GameView(Runnable onPauseRequested) {
        getChildren().add(canvas);
        initDebugBall();

        // 游戏主循环：逻辑更新与渲染分离，update 使用固定步长 1/60 秒
        gameLoop = new GameLoop() {
            @Override
            protected void update(double dt) {
                updateDebugBall(dt);
            }

            @Override
            protected void render(double frameDelta) {
                renderFrame();
            }
        };

        // 暂停快捷键（§4.3：Esc / P）
        setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE || code == KeyCode.P) {
                onPauseRequested.run();
            }
        });
    }

    /** 初始化调试占位小球：置于视口中央，方向 (1, 0.5) 归一化，速度恰为玩家基础移速 */
    private void initDebugBall() {
        ballX = AppConfig.VIEW_WIDTH / 2.0;
        ballY = AppConfig.VIEW_HEIGHT / 2.0;
        double directionLength = Math.hypot(1.0, 0.5);
        ballDirX = 1.0 / directionLength;
        ballDirY = 0.5 / directionLength;
    }

    /** 进入游戏：启动主循环并请求键盘焦点（首次进入与暂停恢复时调用） */
    public void onEnter() {
        if (!loopRunning) {
            gameLoop.start();
            loopRunning = true;
        }
        requestFocus();
    }

    /** 离开游戏：停止主循环（暂停 / 返回主菜单时调用，避免后台空转） */
    public void onLeave() {
        if (loopRunning) {
            gameLoop.stop();
            loopRunning = false;
        }
    }

    /** 固定步长逻辑更新：小球匀速移动并在视口边界反弹（占位实现，验证 dt 驱动） */
    private void updateDebugBall(double dt) {
        ballX += ballDirX * GameConfig.PLAYER_BASE_SPEED * dt;
        ballY += ballDirY * GameConfig.PLAYER_BASE_SPEED * dt;

        // 碰到边界时翻转对应方向的速度分量，并向内钳制坐标避免下一帧越界
        double minX = DEBUG_BALL_RADIUS;
        double maxX = AppConfig.VIEW_WIDTH - DEBUG_BALL_RADIUS;
        if (ballX < minX || ballX > maxX) {
            ballX = clamp(ballX, minX, maxX);
            ballDirX = -ballDirX;
        }

        double minY = DEBUG_BALL_RADIUS;
        double maxY = AppConfig.VIEW_HEIGHT - DEBUG_BALL_RADIUS;
        if (ballY < minY || ballY > maxY) {
            ballY = clamp(ballY, minY, maxY);
            ballDirY = -ballDirY;
        }
    }

    /** 将数值钳制到 [min, max] 闭区间 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 每帧渲染：按当前世界绘制场景（§3.1 光暖金 / 影冷紫）→ 角色光环 → 世界徽章 → 调试信息 */
    private void renderFrame() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        boolean inLight = currentWorld == 0; // 0=光之界（占位：切换系统第 4 天接入）

        // 1. 世界底色（光之界：明亮暖金；影之界：冷暗紫，§3.1）
        g.setFill(inLight ? LIGHT_FLOOR : SHADOW_FLOOR);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. 房间内壁边框（占位：模拟房间可视区域边界，§5.3）
        g.setStroke(inLight ? LIGHT_BORDER : SHADOW_BORDER);
        g.setLineWidth(4.0);
        g.strokeRect(8.0, 8.0, canvas.getWidth() - 16.0, canvas.getHeight() - 16.0);

        // 3. 玩家占位：光环（§8.4 金/紫）+ 球体（后续替换为玩家实体）
        drawAura(g, inLight);
        g.setFill(inLight ? Color.web("#fffdf5") : Color.web("#d8c8f0"));
        g.fillOval(ballX - DEBUG_BALL_RADIUS, ballY - DEBUG_BALL_RADIUS,
                DEBUG_BALL_RADIUS * 2.0, DEBUG_BALL_RADIUS * 2.0);

        // 4. 世界徽章（右上角：光 ☀ / 影 ☾，§8.3 HUD 占位）
        drawWorldBadge(g, inLight);

        // 5. 调试信息：帧率 / 累计帧数 / 当前世界（后续替换为正式 HUD §8.3）
        g.setFill(inLight ? LIGHT_TEXT : SHADOW_TEXT);
        g.setFont(Font.font(13.0));
        g.fillText(String.format("FPS: %.1f    Frame: %d    World: %s",
                gameLoop.getSmoothedFps(), gameLoop.getFrameCount(),
                WorldType.values()[currentWorld]), 16.0, 28.0);
    }

    /** 绘制玩家光环：以球心为心的径向渐变圆（§8.4 光=金 / 影=紫） */
    private void drawAura(GraphicsContext g, boolean inLight) {
        Color core = inLight ? LIGHT_AURA : SHADOW_AURA;
        RadialGradient aura = new RadialGradient(0, 0, 0, 0, AURA_RADIUS, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(core.getRed(), core.getGreen(), core.getBlue(), 0.55)),
                new Stop(0.7, Color.color(core.getRed(), core.getGreen(), core.getBlue(), 0.22)),
                new Stop(1.0, Color.TRANSPARENT));
        g.setFill(aura);
        g.fillOval(ballX - AURA_RADIUS, ballY - AURA_RADIUS, AURA_RADIUS * 2, AURA_RADIUS * 2);
    }

    /** 绘制当前世界徽章（右上角圆形徽记，光 ☀ 底色金 / 影 ☾ 底色紫） */
    private void drawWorldBadge(GraphicsContext g, boolean inLight) {
        double cx = canvas.getWidth() - 48.0;
        double cy = 48.0;
        g.setFill(inLight ? Color.web("#e8b74f") : Color.web("#4a2a78"));
        g.fillOval(cx - BADGE_RADIUS, cy - BADGE_RADIUS, BADGE_RADIUS * 2, BADGE_RADIUS * 2);
        g.setStroke(inLight ? Color.web("#8a6a3a") : Color.web("#8a68c8"));
        g.setLineWidth(2.0);
        g.strokeOval(cx - BADGE_RADIUS, cy - BADGE_RADIUS, BADGE_RADIUS * 2, BADGE_RADIUS * 2);

        g.setFill(inLight ? Color.web("#4a3418") : Color.web("#e8dcff"));
        g.setFont(Font.font(24.0));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(inLight ? "☀" : "☾", cx, cy + 8.0);
    }
}
