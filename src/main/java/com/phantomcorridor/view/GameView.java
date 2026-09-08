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
import javafx.scene.text.Font;

/**
 * 游戏主界面（对应《双界行者》需求 §5.4 渲染方案、§9.1 view.GameView）。
 *
 * <p>游戏画面使用 {@link Canvas} 绘制（适合大量动态物体，避免复杂 JavaFX 节点树性能开销），
 * 由 {@link GameLoop} 驱动「逻辑更新 → 渲染」管线。
 *
 * <p><b>架构对齐占位实现</b>：一个调试用弹跳小球用于验证固定时间步长与渲染管线，
 * 移动速度取 {@link GameConfig#PLAYER_BASE_SPEED}（与玩家移动速度设计值一致）。
 * 双界主题：画布底部以「光(金) → 影(紫)」渐变作底色，暗示两个世界并存；
 * 右上角显示当前世界占位（{@link WorldType#LIGHT}）。
 * 后续里程碑将替换为真实玩家实体与正式 HUD（§8.3）。
 */
public class GameView extends StackPane {

    /** 调试占位小球半径（像素） */
    private static final double DEBUG_BALL_RADIUS = 12.0;

    /** 占位当前世界（双世界切换系统未实现前固定为光之界） */
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

    /** 每帧渲染：双界渐变底色 + 房间边框 + 占位小球 + 调试信息（正式 HUD 在后续里程碑替换） */
    private void renderFrame() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 1. 双界底色：近黑的影界底（游戏内默认偏向影场，后续由世界渲染器接管）
        g.setFill(Color.web("#0a0a0a"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. 房间内壁边框（占位：模拟房间可视区域边界，§5.3）
        g.setStroke(Color.web("#3a3a3a"));
        g.setLineWidth(4.0);
        g.strokeRect(8.0, 8.0, canvas.getWidth() - 16.0, canvas.getHeight() - 16.0);

        // 3. 占位小球（后续替换为玩家实体；光之界用纯白，影之界用灰）
        g.setFill(currentWorld == 0 ? Color.web("#ffffff") : Color.web("#7a7a7a"));
        g.fillOval(ballX - DEBUG_BALL_RADIUS, ballY - DEBUG_BALL_RADIUS,
                DEBUG_BALL_RADIUS * 2.0, DEBUG_BALL_RADIUS * 2.0);

        // 4. 调试信息：帧率 / 累计帧数 / 当前世界（后续替换为正式 HUD §8.3）
        g.setFill(Color.LIGHTGRAY);
        g.setFont(Font.font(13.0));
        g.fillText(String.format("FPS: %.1f    Frame: %d    World: %s",
                gameLoop.getSmoothedFps(), gameLoop.getFrameCount(),
                WorldType.values()[currentWorld]), 16.0, 28.0);
    }
}
