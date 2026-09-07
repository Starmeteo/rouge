package org.example.rouge.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.rouge.GameApplication;
import org.example.rouge.core.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 主菜单面板（需求 §4.1 界面流程、FR-01"启动程序后进入主菜单"）。
 *
 * <p><b>血月旅人主题</b>：顶部半轮血月发出红光，暖色余烬缓缓上浮，
 * 标题采用橙金渐变并带红色辉光，按钮为深色面板 + 暗红细边（实现见 ui.css）。
 *
 * <p><b>动效设计</b>（营造"即将进入地牢"的紧张氛围）：
 * <ul>
 *   <li>菜单入场：标题 → 分隔线 → 按钮依次淡入上浮（错峰入场）；</li>
 *   <li>按钮悬停：轻微放大 + 前置符文光标 ✦ 淡入；</li>
 *   <li>操作说明 / 设置：无边框覆盖层滑入滑出；</li>
 *   <li>点击"开始游戏"：血色遮罩渐入 + 标题放大淡出 + 余烬高速上浮 0.5s 后切入游戏；</li>
 *   <li>点击"退出"：血色遮罩渐入后关闭窗口。</li>
 * </ul>
 *
 * <p>菜单按钮顺序：开始游戏 / 操作说明 / 设置 / 退出（设置位于操作说明下方，随开发完善）。
 */
public class MainMenuPane extends StackPane {

    /** 漂浮余烬数量（暖色小光点，营造阴郁气氛） */
    private static final int EMBER_COUNT = 14;

    /** 血月半径（像素），圆心位于视口上边缘附近，只露出下半轮（放大后更具压迫感） */
    private static final double MOON_RADIUS = 240.0;

    /** 血月圆心相对视口顶边的下移量（像素），控制露出的月面大小 */
    private static final double MOON_CENTER_OFFSET = 60.0;

    /** 入场动画：相邻元素错峰间隔（毫秒） */
    private static final double ENTER_STEP_MS = 70.0;

    /** 入场动画：单个元素淡入时长（毫秒） */
    private static final double ENTER_FADE_MS = 320.0;

    /** 覆盖层（操作说明/设置）滑入淡入时长（毫秒） */
    private static final double OVERLAY_MS = 260.0;

    /** 开始游戏血色过渡时长（毫秒） */
    private static final double START_TRANSITION_MS = 520.0;

    /** 过渡期间余烬上浮加速倍数（营造紧张感） */
    private static final double EMBER_BOOST = 6.0;

    /** 符文光标 ✦ 固定宽度（像素），与其右侧隐形占位等宽，保证按钮文本严格居中 */
    private static final double STAR_WIDTH = 22.0;

    /** 主菜单标题文本 */
    private static final String TITLE = "幻境回廊";

    /** 副标题文本 */
    private static final String SUBTITLE = "血月之下 · 无人生还";

    /** 底部版本信息文本 */
    private static final String FOOTER = "v0.2.0 · JavaFX Roguelike 可玩原型 · 血月旅人主题";

    /** 标题节点（开始过渡动画需要引用） */
    private final Label title = new Label(TITLE);

    /** 血月节点（开始过渡时需要引用：放大下沉营造"逼近"压迫感） */
    private final Circle bloodMoon;

    /** 主菜单内容（标题组 + 分隔线 + 按钮），与覆盖层互斥显示 */
    private final VBox menuContent;

    /** 操作说明覆盖层（无边框，内容随开发逐步完善） */
    private final VBox helpContent;

    /** 设置覆盖层（构造时赋值，回调中引用自身，故不可为 final） */
    private VBox settingsContent;

    /** 血色过渡遮罩（开始游戏/退出时渐入） */
    private final Region overlay;

    /** 漂浮余烬节点列表 */
    private final List<Circle> embers = new ArrayList<>();

    /** 各余烬的基础上浮速度（像素/秒） */
    private final double[] emberSpeeds = new double[EMBER_COUNT];

    /** 余烬上浮速度倍率（开始过渡期间临时加速） */
    private double emberBoost = 1.0;

    /** 余烬动画定时器（面板不可见时不运行，避免后台空转） */
    private final AnimationTimer emberTimer;

    /** 随机数源（余烬初始化与循环重生） */
    private final Random random = new Random();

    /**
     * 构建主菜单面板。
     *
     * @param onStart  "开始游戏"回调 —— 血色过渡结束后进入游戏界面
     * @param onQuit   "退出"回调 —— 血色过渡结束后关闭主窗口
     * @param settings 全局设置对象（设置面板读写）
     */
    public MainMenuPane(Runnable onStart, Runnable onQuit, Settings settings) {
        getStyleClass().add("main-menu-pane");

        // 背景装饰：半轮大血月（最底层），圆心位于视口上边缘附近
        bloodMoon = new Circle(MOON_RADIUS);
        bloodMoon.getStyleClass().add("blood-moon");
        bloodMoon.setManaged(false);
        bloodMoon.setLayoutX(GameApplication.VIEW_WIDTH / 2.0 - MOON_RADIUS);
        bloodMoon.setLayoutY(-MOON_RADIUS + MOON_CENTER_OFFSET);

        title.getStyleClass().add("menu-title");
        menuContent = createMenuContent(onStart, onQuit);

        // 底部版本信息（常驻）
        Label footer = new Label(FOOTER);
        footer.getStyleClass().add("menu-footer");
        footer.setPadding(new Insets(0, 0, 18, 0));
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);

        // 覆盖层：操作说明与设置（默认隐藏）
        helpContent = createHelpContent();
        settingsContent = new SettingsPane(settings, () -> hideOverlay(settingsContent, this::fadeInMenu));
        settingsContent.setVisible(false);
        settingsContent.setManaged(false);

        // 血色过渡遮罩（最顶层，默认隐藏；显式指定逻辑尺寸，避免 Region 在 StackPane 中
        // 按 0×0 的 pref 尺寸渲染导致遮罩不铺满屏幕）
        overlay = new Region();
        overlay.getStyleClass().add("menu-overlay");
        overlay.setMinSize(GameApplication.VIEW_WIDTH, GameApplication.VIEW_HEIGHT);
        overlay.setPrefSize(GameApplication.VIEW_WIDTH, GameApplication.VIEW_HEIGHT);
        overlay.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        overlay.setVisible(false);
        overlay.setManaged(false);

        // 层叠顺序（从底到顶）：血月 → 余烬 → 菜单内容 → 底部信息 → 覆盖层 → 血色遮罩
        getChildren().add(bloodMoon);
        initEmbers();
        getChildren().addAll(menuContent, footer, helpContent, settingsContent, overlay);

        // Esc 收起覆盖层（需求 §4.3：Esc = 返回）
        setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ESCAPE) {
                return;
            }
            if (settingsContent.isVisible()) {
                hideOverlay(settingsContent, this::fadeInMenu);
            } else if (helpContent.isVisible()) {
                hideOverlay(helpContent, this::fadeInMenu);
            }
        });

        // 面板重新可见时复位所有动画状态并重播入场动画；不可见时停止余烬动画
        emberTimer = createEmberTimer();
        visibleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                resetForReentry();
                emberTimer.start();
            } else {
                emberTimer.stop();
            }
        });

        // 首次显示：直接播放入场动画并启动余烬
        playEnterAnimation();
        emberTimer.start();
    }

    /** 构建菜单主体：标题 + 副标题 + 分隔线 + 四个操作按钮 */
    private VBox createMenuContent(Runnable onStart, Runnable onQuit) {
        Label subtitle = new Label(SUBTITLE);
        subtitle.getStyleClass().add("menu-subtitle");

        VBox header = new VBox(10.0, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // 金色渐变分隔线（纯样式 Region，见 ui.css .menu-divider）
        Region divider = new Region();
        divider.getStyleClass().add("menu-divider");

        Button startButton = createMenuButton("开始游戏", () -> playStartTransition(onStart));
        startButton.setDefaultButton(true); // Enter 快捷开始

        Button helpButton = createMenuButton("操作说明", () -> showOverlay(helpContent));
        Button settingsButton = createMenuButton("设置", () -> showOverlay(settingsContent));
        Button quitButton = createMenuButton("退出", () -> playExitTransition(onQuit));

        VBox box = new VBox(22.0, header, divider, startButton, helpButton, settingsButton, quitButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40.0));
        return box;
    }

    /** 构建操作说明覆盖层：键位表（GridPane 行式，后续开发仅需追加行即可完善） */
    private VBox createHelpContent() {
        Label helpTitle = new Label("操作说明");
        helpTitle.getStyleClass().add("overlay-title");

        GridPane grid = new GridPane();
        grid.setHgap(18.0);
        grid.setVgap(16.0);
        grid.setAlignment(Pos.CENTER); // 键位表整体水平居中
        int row = 0;
        row = addKeyRow(grid, row, "WASD / 方向键", "移动 · 八方向");
        row = addKeyRow(grid, row, "鼠标", "瞄准 · 准星跟随指针");
        row = addKeyRow(grid, row, "鼠标左键", "射击 · 按住左键连发");
        row = addKeyRow(grid, row, "Esc / P", "暂停 / 继续");
        row = addKeyRow(grid, row, "F11 / Alt+Enter", "切换全屏");
        row = addKeyRow(grid, row, "Enter", "确认");

        Label note = new Label("提示：操作说明将随功能开发持续完善（移动碰撞、敌人、道具等在第 2~7 天加入）。");
        note.getStyleClass().add("hint-text");

        Button backButton = createMenuButton("返回菜单", () -> hideOverlay(helpContent, this::fadeInMenu));

        VBox box = new VBox(26.0, helpTitle, grid, note, backButton);
        box.setAlignment(Pos.CENTER); // 标题/键位表/提示/按钮整体水平居中
        box.setFillWidth(false);
        box.setMaxWidth(VBox.USE_PREF_SIZE);
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    /** 向键位表追加一行（按键胶囊 + 说明；两列内容均在列内水平居中，见 ui.css） */
    private int addKeyRow(GridPane grid, int row, String key, String description) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("keyboard-key");
        GridPane.setHalignment(keyLabel, HPos.CENTER);
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("help-desc");
        GridPane.setHalignment(descLabel, HPos.CENTER);
        grid.add(keyLabel, 0, row);
        grid.add(descLabel, 1, row);
        return row + 1;
    }

    /** 统一创建菜单按钮：绑定动作 + 悬停动效（放大 + 前置符文光标淡入）。
     *  <p>文字严格居中：图形内容为「光标 + 文本 + 等宽隐形占位」的对称组合，
     *  光标淡入/淡出不改变文本位置，仅增加战栗氛围。 */
    private Button createMenuButton(String text, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("menu-button");
        button.setOnAction(event -> action.run());

        // 前置符文光标 ✦：固定宽度，默认隐藏，悬停时淡入
        Label cursor = new Label("✦");
        cursor.getStyleClass().add("menu-cursor");
        cursor.setMinWidth(STAR_WIDTH);
        cursor.setMaxWidth(STAR_WIDTH);
        cursor.setOpacity(0.0);

        // 按钮文本：置于光标与右侧隐形占位之间，保证文本居中
        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("menu-button-label");

        Region spacer = new Region();
        spacer.setMinWidth(STAR_WIDTH);
        spacer.setMaxWidth(STAR_WIDTH);

        HBox content = new HBox(6.0, cursor, textLabel, spacer);
        content.setAlignment(Pos.CENTER);
        button.setGraphic(content);
        button.setGraphicTextGap(0.0);

        // 悬停：按钮轻微放大 + 光标淡入；移出：反向恢复
        ScaleTransition grow = new ScaleTransition(Duration.millis(120), button);
        grow.setToX(1.05);
        grow.setToY(1.05);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(120), button);
        shrink.setToX(1.0);
        shrink.setToY(1.0);
        FadeTransition cursorIn = new FadeTransition(Duration.millis(120), cursor);
        cursorIn.setToValue(1.0);
        FadeTransition cursorOut = new FadeTransition(Duration.millis(120), cursor);
        cursorOut.setToValue(0.0);
        button.hoverProperty().addListener((obs, oldValue, hovered) -> {
            if (hovered) {
                cursorIn.play();
                grow.play();
            } else {
                cursorOut.play();
                shrink.play();
            }
        });
        return button;
    }

    /** 菜单入场动画：标题组/分隔线/按钮依次淡入上浮 */
    private void playEnterAnimation() {
        List<Node> items = new ArrayList<>(menuContent.getChildren());
        for (Node node : items) {
            node.setOpacity(0.0);
            node.setTranslateY(18.0);
        }
        for (int i = 0; i < items.size(); i++) {
            Duration delay = Duration.millis(i * ENTER_STEP_MS);
            FadeTransition fade = new FadeTransition(Duration.millis(ENTER_FADE_MS), items.get(i));
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setDelay(delay);
            TranslateTransition slide = new TranslateTransition(Duration.millis(ENTER_FADE_MS), items.get(i));
            slide.setFromY(18.0);
            slide.setToY(0.0);
            slide.setDelay(delay);
            new ParallelTransition(fade, slide).play();
        }
    }

    /** 打开覆盖层：菜单内容淡出 → 覆盖层滑入淡入 */
    private void showOverlay(VBox panel) {
        FadeTransition out = new FadeTransition(Duration.millis(150.0), menuContent);
        out.setToValue(0.0);
        out.setOnFinished(event -> {
            menuContent.setVisible(false);
            menuContent.setManaged(false);
            panel.setVisible(true);
            panel.setManaged(true);
            panel.setOpacity(0.0);
            panel.setTranslateY(26.0);
            FadeTransition in = new FadeTransition(Duration.millis(OVERLAY_MS), panel);
            in.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(OVERLAY_MS), panel);
            slide.setToY(0.0);
            new ParallelTransition(in, slide).play();
        });
        out.play();
    }

    /** 收起覆盖层：覆盖层淡出 → 菜单内容重新显示（可指定淡入回调） */
    private void hideOverlay(VBox panel, Runnable onFinished) {
        FadeTransition out = new FadeTransition(Duration.millis(160.0), panel);
        out.setToValue(0.0);
        out.setOnFinished(event -> {
            panel.setVisible(false);
            panel.setManaged(false);
            menuContent.setVisible(true);
            menuContent.setManaged(true);
            onFinished.run();
        });
        out.play();
    }

    /** 菜单内容淡入（覆盖层收起后调用） */
    private void fadeInMenu() {
        menuContent.setOpacity(0.0);
        FadeTransition in = new FadeTransition(Duration.millis(220.0), menuContent);
        in.setToValue(1.0);
        in.play();
    }

    /**
     * 开始游戏过渡：血月放大下沉逼近 + 血色遮罩渐入 + 标题放大淡出 + 余烬高速上浮，
     * 动画结束后调用 {@code onStart} 切入游戏界面（营造紧张氛围）。
     */
    private void playStartTransition(Runnable onStart) {
        overlay.setVisible(true);
        overlay.setManaged(true);

        FadeTransition overlayIn = new FadeTransition(Duration.millis(START_TRANSITION_MS), overlay);
        overlayIn.setFromValue(0.0);
        overlayIn.setToValue(1.0);

        // 血月"逼近"：放大 1.4 倍并下沉，如同从夜空压向玩家
        ScaleTransition moonGrow = new ScaleTransition(
                Duration.millis(START_TRANSITION_MS), bloodMoon);
        moonGrow.setFromX(1.0);
        moonGrow.setFromY(1.0);
        moonGrow.setToX(1.4);
        moonGrow.setToY(1.4);
        TranslateTransition moonSink = new TranslateTransition(
                Duration.millis(START_TRANSITION_MS), bloodMoon);
        moonSink.setToY(40.0);

        ScaleTransition titleGrow = new ScaleTransition(
                Duration.millis(START_TRANSITION_MS * 0.9), title);
        titleGrow.setFromX(1.0);
        titleGrow.setFromY(1.0);
        titleGrow.setToX(1.3);
        titleGrow.setToY(1.3);

        FadeTransition titleFade = new FadeTransition(
                Duration.millis(START_TRANSITION_MS * 0.9), title);
        titleFade.setToValue(0.0);

        FadeTransition contentFade = new FadeTransition(
                Duration.millis(START_TRANSITION_MS * 0.8), menuContent);
        contentFade.setToValue(0.0);

        emberBoost = EMBER_BOOST; // 余烬加速上浮，强化"进入地牢"的压迫感

        ParallelTransition transition = new ParallelTransition(
                overlayIn, moonGrow, moonSink, titleGrow, titleFade, contentFade);
        transition.setOnFinished(event -> {
            emberBoost = 1.0;
            onStart.run();
        });
        transition.play();
    }

    /** 退出过渡：血色遮罩渐入后关闭窗口 */
    private void playExitTransition(Runnable onQuit) {
        overlay.setVisible(true);
        overlay.setManaged(true);
        FadeTransition fade = new FadeTransition(Duration.millis(300.0), overlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setOnFinished(event -> onQuit.run());
        fade.play();
    }

    /** 从游戏/暂停返回主菜单时复位所有动画状态（遮罩、标题、血月、覆盖层、菜单内容） */
    private void resetForReentry() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setOpacity(0.0);
        title.setScaleX(1.0);
        title.setScaleY(1.0);
        title.setOpacity(1.0);
        bloodMoon.setScaleX(1.0);
        bloodMoon.setScaleY(1.0);
        bloodMoon.setTranslateY(0.0);
        menuContent.setVisible(true);
        menuContent.setManaged(true);
        menuContent.setOpacity(1.0);
        helpContent.setVisible(false);
        helpContent.setManaged(false);
        settingsContent.setVisible(false);
        settingsContent.setManaged(false);
        emberBoost = 1.0;
        playEnterAnimation();
    }

    /** 初始化漂浮余烬：随机大小/色相（橙红区间）/透明度/位置/上浮速度 */
    private void initEmbers() {
        for (int i = 0; i < EMBER_COUNT; i++) {
            Circle ember = new Circle(1.5 + random.nextDouble() * 2.6);
            // 色相 5°~45° 为橙红色区间，随机亮度模拟不同温度的火星
            ember.setFill(Color.hsb(5.0 + random.nextDouble() * 40.0, 0.75, 0.95));
            ember.setOpacity(0.35 + random.nextDouble() * 0.5);
            ember.setManaged(false);
            ember.setLayoutX(random.nextDouble() * GameApplication.VIEW_WIDTH);
            ember.setLayoutY(random.nextDouble() * GameApplication.VIEW_HEIGHT);
            emberSpeeds[i] = 22.0 + random.nextDouble() * 40.0;
            embers.add(ember);
            getChildren().add(ember);
        }
    }

    /** 创建余烬动画：缓慢上浮 + 正弦横向摆动，飘出屏幕顶后从底部重生 */
    private AnimationTimer createEmberTimer() {
        return new AnimationTimer() {
            /** 上一帧时间戳（纳秒），-1 表示尚未收到首帧 */
            private long lastNanos = -1L;

            @Override
            public void handle(long now) {
                if (lastNanos < 0L) {
                    lastNanos = now;
                    return;
                }
                double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
                lastNanos = now;

                for (int i = 0; i < embers.size(); i++) {
                    Circle ember = embers.get(i);
                    double y = ember.getLayoutY() - emberSpeeds[i] * emberBoost * dt;
                    // 横向摆动：相位错开避免整齐划一
                    double x = ember.getLayoutX()
                            + Math.sin(now * 1e-9 * 2.2 + i * 1.7) * 14.0 * dt;
                    if (y < -12.0) {
                        y = GameApplication.VIEW_HEIGHT + 12.0;
                        x = random.nextDouble() * GameApplication.VIEW_WIDTH;
                    }
                    ember.setLayoutX(x);
                    ember.setLayoutY(y);
                }
            }
        };
    }
}
