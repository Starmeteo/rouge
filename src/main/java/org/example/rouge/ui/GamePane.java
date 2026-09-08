package org.example.rouge.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.rouge.GameApplication;
import org.example.rouge.core.GameLoop;

/**
 * 游戏主界面（需求 §5.4 渲染方案）。
 *
 * <p>游戏画面使用 {@link Canvas} 绘制（适合大量动态物体，避免复杂 JavaFX 节点树的
 * 性能开销），由 {@link GameLoop} 驱动"逻辑更新 → 渲染"管线（需求 §5.3）。
 *
 * <p><b>第一天占位实现</b>：一个调试用弹跳小球用于验证固定时间步长与渲染管线，
 * 其移动速度（200 像素/秒）与玩家移动速度设计值一致（需求 §3.1.1）。
 * 后续里程碑将替换为真实玩家实体与正式 HUD。
 */
public class GamePane extends StackPane {

    /** 调试占位小球半径（像素） */
    private static final double DEBUG_BALL_RADIUS = 12.0;

    /** 调试占位小球速度（像素/秒） */
    private static final double DEBUG_BALL_SPEED = 200.0;

    /** 绘制画布（与游戏视口同尺寸） */
    private final Canvas canvas = new Canvas(GameApplication.VIEW_WIDTH, GameApplication.VIEW_HEIGHT);

    /** 游戏主循环 */
    private final GameLoop gameLoop;

    /** 主循环是否处于运行状态（保证 start/stop 幂等，避免重复调用报错） */
    private boolean loopRunning = false;

    // ---- 调试占位状态：弹跳小球（第 2 天由 model.Player 取代） ----
    /** 小球 X 坐标（像素） */
    private double ballX;
    /** 小球 Y 坐标（像素） */
    private double ballY;
    /** 小球 X 方向速度分量（归一化后乘速度值，实际速度恰为 200 像素/秒） */
    private double ballDirX;
    /** 小球 Y 方向速度分量 */
    private double ballDirY;

    /**
     * 构建游戏面板。
     *
     * @param onPauseRequested 暂停请求回调（游戏中按 Esc/P 时触发，由 GameApplication 切换到暂停界面）
     */
    public GamePane(Runnable onPauseRequested) {
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

        // 暂停快捷键（需求 §4.3：Esc / P）
        setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE || code == KeyCode.P) {
                onPauseRequested.run();
            }
        });
    }

    /** 初始化调试占位小球：置于视口中央，方向 (1, 0.5) 归一化，速度恰为 200 像素/秒 */
    private void initDebugBall() {
        ballX = GameApplication.VIEW_WIDTH / 2.0;
        ballY = GameApplication.VIEW_HEIGHT / 2.0;
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
        ballX += ballDirX * DEBUG_BALL_SPEED * dt;
        ballY += ballDirY * DEBUG_BALL_SPEED * dt;

        // 碰到边界时翻转对应方向的速度分量，并向内钳制坐标避免下一帧越界
        double minX = DEBUG_BALL_RADIUS;
        double maxX = GameApplication.VIEW_WIDTH - DEBUG_BALL_RADIUS;
        if (ballX < minX || ballX > maxX) {
            ballX = clamp(ballX, minX, maxX);
            ballDirX = -ballDirX;
        }

        double minY = DEBUG_BALL_RADIUS;
        double maxY = GameApplication.VIEW_HEIGHT - DEBUG_BALL_RADIUS;
        if (ballY < minY || ballY > maxY) {
            ballY = clamp(ballY, minY, maxY);
            ballDirY = -ballDirY;
        }
    }

    /** 将数值钳制到 [min, max] 闭区间 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 每帧渲染：地牢底色 + 房间边框 + 占位小球 + 调试信息（正式 HUD 在后续里程碑替换） */
    private void renderFrame() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 1. 地面底色（占位，后续由房间渲染器绘制真实地面；色值贴合血月旅人主题）
        g.setFill(Color.web("#1b1214"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 2. 房间内壁边框（占位：模拟房间可视区域边界，需求 §3.2.3）
        g.setStroke(Color.web("#4a2c2e"));
        g.setLineWidth(4.0);
        g.strokeRect(8.0, 8.0, canvas.getWidth() - 16.0, canvas.getHeight() - 16.0);

        // 3. 占位小球（第 2 天替换为玩家实体）
        g.setFill(Color.web("#4fd3ff"));
        g.fillOval(ballX - DEBUG_BALL_RADIUS, ballY - DEBUG_BALL_RADIUS,
                DEBUG_BALL_RADIUS * 2.0, DEBUG_BALL_RADIUS * 2.0);

        // 4. 调试信息：帧率 / 累计帧数（第 6 天替换为正式 HUD）
        g.setFill(Color.LIGHTGRAY);
        g.setFont(Font.font(13.0));
        g.fillText(String.format("FPS: %.1f    Frame: %d",
                gameLoop.getSmoothedFps(), gameLoop.getFrameCount()), 16.0, 28.0);
    }
}
