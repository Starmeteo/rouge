package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.Settings;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 主菜单面板（对应《双界行者》需求 §8.2 主菜单）。
 *
 * <p><b>双界（光/影）主题 —— 深蓝夜空 · 光弧 · 水面</b>：背景为静谧的<b>深蓝夜色</b>
 * （满天星斗），一束<b>发光的白色光弧</b>划过天际，并在下方<b>水面</b>上形成倒影。
 * 水面持续<b>涟漪闪烁、光影流动</b>（动态效果），呼应『光与影两界并存、相互映照』
 * （§2 核心卖点『光与影两界并存、需不断穿梭』）。
 *
 * <p><b>动态效果</b>（在原有按钮动效基础上新增）：
 * <ul>
 *   <li>水面：光弧倒影<b>粼粼闪动</b> + 横向涟漪随波流动；</li>
 *   <li>天际：星点微微<b>闪烁</b>；光弧辉光<b>呼吸</b>。</li>
 * </ul>
 *
 * <p><b>按钮动效（参考原项目保留）</b>：悬停轻微放大 + 前置符文光标 ✦ 淡入；
 * 菜单入场标题/分隔线/按钮依次淡入上浮；覆盖层滑入滑出。
 *
 * <p><b>菜单按钮顺序</b>（§8.2）：开始游戏 / 道具图鉴 / 设置 / 退出。
 */
public class MainMenuView extends StackPane {

    /** 星点数量（天际远景，随夜色微微闪烁） */
    private static final int STAR_COUNT = 48;

    /** 水面涟漪条带数量 */
    private static final int RIPPLE_COUNT = 26;

    /** 入场动画：相邻元素错峰间隔（毫秒） */
    private static final double ENTER_STEP_MS = 70.0;

    /** 入场动画：单个元素淡入时长（毫秒） */
    private static final double ENTER_FADE_MS = 320.0;

    /** 覆盖层（道具图鉴/设置）滑入淡入时长（毫秒） */
    private static final double OVERLAY_MS = 260.0;

    /** 开始游戏过渡时长（毫秒） */
    private static final double START_TRANSITION_MS = 520.0;

    /** 符文光标 ✦ 固定宽度（像素），与其右侧隐形占位等宽，保证按钮文本严格居中 */
    private static final double STAR_WIDTH = 22.0;

    /** 主菜单标题文本 */
    private static final String TITLE = "双界行者";

    /** 副标题文本 */
    private static final String SUBTITLE = "光与影 · 皆通途";

    /** 底部版本信息文本 */
    private static final String FOOTER = "v0.3.0 · 双界行者 · JavaFX Roguelike 可玩原型";

    /** 标题节点（开始过渡动画需要引用） */
    private final Label title = new Label(TITLE);

    /** 主菜单内容（标题组 + 分隔线 + 按钮），与覆盖层互斥显示 */
    private final VBox menuContent;

    /** 道具图鉴覆盖层（对应 §8.2，占位展示道具分类） */
    private final VBox galleryContent;

    /** 设置覆盖层（构造时赋值，回调中引用自身，故不可为 final） */
    private VBox settingsContent;

    /** 双界过渡遮罩（开始游戏/退出时渐入） */
    private final Region overlay;

    /** 夜景背景画布（绘制夜空 / 光弧 / 水面倒影 / 涟漪动效） */
    private final Canvas backdrop = new Canvas(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);

    /** 背景动效定时器（绘制夜空光弧与水面涟漪；面板不可见时停止） */
    private final AnimationTimer backdropTimer;

    /** 随机数源（星点/涟漪初始化） */
    private final Random random = new Random();

    /** 星点横坐标（相对视口宽的比例） */
    private final double[] starX = new double[STAR_COUNT];

    /** 星点纵坐标（相对视口高的比例，位于天空区） */
    private final double[] starY = new double[STAR_COUNT];

    /** 星点相位（错开闪烁） */
    private final double[] starPhase = new double[STAR_COUNT];

    /** 涟漪所在横坐标（水面，相对视口宽的比例） */
    private final double[] rippleX = new double[RIPPLE_COUNT];

    /** 涟漪纵向位置偏移（相对水面区） */
    private final double[] rippleY = new double[RIPPLE_COUNT];

    /** 涟漪相位（错开波动） */
    private final double[] ripplePhase = new double[RIPPLE_COUNT];

    /**
     * 构建主菜单面板。
     *
     * @param onStart  "开始游戏"回调 —— 过渡结束后进入游戏界面
     * @param onQuit   "退出"回调 —— 过渡结束后关闭主窗口
     * @param settings 全局设置对象（设置面板读写）
     */
    public MainMenuView(Runnable onStart, Runnable onQuit, Settings settings) {
        getStyleClass().add("main-menu-pane");

        // 背景画布置于最底层（夜空光弧 + 水面动效）；菜单内容等叠加其上
        backdrop.setManaged(false);
        getChildren().add(backdrop);
        initScene();

        title.getStyleClass().add("menu-title");
        menuContent = createMenuContent(onStart, onQuit);

        // 底部版本信息（常驻）
        Label footer = new Label(FOOTER);
        footer.getStyleClass().add("menu-footer");
        footer.setPadding(new Insets(0, 0, 18, 0));
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);

        // 覆盖层：道具图鉴与设置（默认隐藏）
        galleryContent = createGalleryContent();
        settingsContent = new SettingsOverlay(settings, () -> hideOverlay(settingsContent, this::fadeInMenu));
        settingsContent.setVisible(false);
        settingsContent.setManaged(false);

        // 双界过渡遮罩（最顶层，默认隐藏；显式指定逻辑尺寸，避免 Region 在 StackPane 中
        // 按 0×0 的 pref 尺寸渲染导致遮罩不铺满屏幕）
        overlay = new Region();
        overlay.getStyleClass().add("menu-overlay");
        overlay.setMinSize(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        overlay.setPrefSize(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        overlay.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        overlay.setVisible(false);
        overlay.setManaged(false);

        // 层叠顺序：背景画布 → 菜单内容 → 底部信息 → 覆盖层 → 双界遮罩
        getChildren().addAll(menuContent, footer, galleryContent, settingsContent, overlay);

        // Esc 收起覆盖层（§4.3：Esc = 返回）
        setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ESCAPE) {
                return;
            }
            if (settingsContent.isVisible()) {
                hideOverlay(settingsContent, this::fadeInMenu);
            } else if (galleryContent.isVisible()) {
                hideOverlay(galleryContent, this::fadeInMenu);
            }
        });

        // 面板重新可见时复位所有动画状态并重播入场动画；不可见时停止背景动效
        backdropTimer = createBackdropTimer();
        visibleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                resetForReentry();
                backdropTimer.start();
            } else {
                backdropTimer.stop();
            }
        });

        // 首次显示：直接播放入场动画并启动背景动效
        playEnterAnimation();
        backdropTimer.start();
    }

    /** 初始化星点与涟漪的随机参数（只执行一次） */
    private void initScene() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble() * 0.50;            // 天空区（视口上半部分略多）
            starPhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
        for (int i = 0; i < RIPPLE_COUNT; i++) {
            rippleX[i] = random.nextDouble();
            rippleY[i] = random.nextDouble();                 // 水面区相对位置
            ripplePhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
    }

    /** 构建菜单主体：标题 + 副标题 + 分隔线 + 操作按钮（§8.2） */
    private VBox createMenuContent(Runnable onStart, Runnable onQuit) {
        Label subtitle = new Label(SUBTITLE);
        subtitle.getStyleClass().add("menu-subtitle");

        VBox header = new VBox(10.0, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // 白→蓝渐变分隔线（纯样式 Region，见 ui.css .menu-divider）
        Region divider = new Region();
        divider.getStyleClass().add("menu-divider");

        Button startButton = createMenuButton("开始游戏", () -> playStartTransition(onStart));
        startButton.setDefaultButton(true); // Enter 快捷开始

        Button galleryButton = createMenuButton("道具图鉴", () -> showOverlay(galleryContent));
        Button settingsButton = createMenuButton("设置", () -> showOverlay(settingsContent));
        Button quitButton = createMenuButton("退出", () -> playExitTransition(onQuit));

        VBox box = new VBox(22.0, header, divider, startButton, galleryButton, settingsButton, quitButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40.0));
        return box;
    }

    /** 构建道具图鉴覆盖层：占位展示道具分类（§8.2；详细图鉴内容随第 6 天道具系统完善） */
    private VBox createGalleryContent() {
        Label galleryTitle = new Label("道具图鉴");
        galleryTitle.getStyleClass().add("overlay-title");

        GridPane grid = new GridPane();
        grid.setHgap(18.0);
        grid.setVgap(16.0);
        grid.setAlignment(Pos.CENTER); // 图鉴表整体水平居中
        int row = 0;
        row = addKeyRow(grid, row, "光属性", "强化光形态攻击、远程手感（如·晨曦之矛）");
        row = addKeyRow(grid, row, "影属性", "强化影形态近战、速度、吸血（如·暮色斗篷）");
        row = addKeyRow(grid, row, "双属性", "两种形态都强化，但数值较低（如·裂界护符）");
        row = addKeyRow(grid, row, "通用", "提高血量、移速、拾取范围（如·相位容器）");

        Label note = new Label("提示：具体道具将随第 6 天道具系统逐一加入，此处仅为分类预览。");
        note.getStyleClass().add("hint-text");

        Button backButton = createMenuButton("返回菜单", () -> hideOverlay(galleryContent, this::fadeInMenu));

        VBox box = new VBox(26.0, galleryTitle, grid, note, backButton);
        box.setAlignment(Pos.CENTER);
        box.setFillWidth(false);
        box.setMaxWidth(VBox.USE_PREF_SIZE);
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    /** 向两列表格追加一行（名称 + 说明；两列内容均在列内水平居中，见 ui.css） */
    private int addKeyRow(GridPane grid, int row, String name, String description) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("keyboard-key");
        GridPane.setHalignment(nameLabel, HPos.CENTER);
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("help-desc");
        GridPane.setHalignment(descLabel, HPos.CENTER);
        grid.add(nameLabel, 0, row);
        grid.add(descLabel, 1, row);
        return row + 1;
    }

    /** 统一创建菜单按钮：绑定动作 + 悬停动效（放大 + 前置符文光标淡入），沿用原项目动效。
     *  <p>文字严格居中：图形内容为「光标 + 文本 + 等宽隐形占位」的对称组合，
     *  光标淡入/淡出不改变文本位置，仅增加氛围。 */
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

    /** 开始游戏过渡：双界遮罩渐入 + 标题放大淡出；动画结束后调用 {@code onStart} 切入游戏界面 */
    private void playStartTransition(Runnable onStart) {
        overlay.setVisible(true);
        overlay.setManaged(true);

        FadeTransition overlayIn = new FadeTransition(Duration.millis(START_TRANSITION_MS), overlay);
        overlayIn.setFromValue(0.0);
        overlayIn.setToValue(1.0);

        ScaleTransition titleGrow = new ScaleTransition(Duration.millis(START_TRANSITION_MS * 0.9), title);
        titleGrow.setFromX(1.0);
        titleGrow.setFromY(1.0);
        titleGrow.setToX(1.3);
        titleGrow.setToY(1.3);

        FadeTransition titleFade = new FadeTransition(Duration.millis(START_TRANSITION_MS * 0.9), title);
        titleFade.setToValue(0.0);

        FadeTransition contentFade = new FadeTransition(Duration.millis(START_TRANSITION_MS * 0.8), menuContent);
        contentFade.setToValue(0.0);

        ParallelTransition transition = new ParallelTransition(
                overlayIn, titleGrow, titleFade, contentFade);
        transition.setOnFinished(event -> onStart.run());
        transition.play();
    }

    /** 退出过渡：双界遮罩渐入后关闭窗口 */
    private void playExitTransition(Runnable onQuit) {
        overlay.setVisible(true);
        overlay.setManaged(true);
        FadeTransition fade = new FadeTransition(Duration.millis(300.0), overlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setOnFinished(event -> onQuit.run());
        fade.play();
    }

    /** 从游戏/暂停返回主菜单时复位所有动画状态（遮罩、标题、覆盖层、菜单内容） */
    private void resetForReentry() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setOpacity(0.0);
        title.setScaleX(1.0);
        title.setScaleY(1.0);
        title.setOpacity(1.0);
        menuContent.setVisible(true);
        menuContent.setManaged(true);
        menuContent.setOpacity(1.0);
        galleryContent.setVisible(false);
        galleryContent.setManaged(false);
        settingsContent.setVisible(false);
        settingsContent.setManaged(false);
        playEnterAnimation();
    }

    /** 创建背景动效定时器：每帧重绘夜空/光弧/水面涟漪（time 以秒计） */
    private AnimationTimer createBackdropTimer() {
        return new AnimationTimer() {
            private long lastNanos = -1L;
            private double time = 0.0;

            @Override
            public void handle(long now) {
                if (lastNanos < 0L) {
                    lastNanos = now;
                    return;
                }
                double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
                lastNanos = now;
                time += dt;
                renderBackdrop(time);
            }
        };
    }

    /** 绘制夜景背景：深蓝夜空 + 星光 + 白色光弧 + 地平线 + 水面倒影 + 涟漪动效 */
    private void renderBackdrop(double time) {
        GraphicsContext g = backdrop.getGraphicsContext2D();
        double w = backdrop.getWidth();
        double h = backdrop.getHeight();
        double horizonY = h * 0.58;

        // ---- 深蓝夜空（上半部） ----
        LinearGradient sky = new LinearGradient(0, 0, 0, horizonY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#0a1a3e")),
                new Stop(0.45, Color.web("#16335f")),
                new Stop(0.85, Color.web("#1f427a")),
                new Stop(1.0, Color.web("#0a1834")));
        g.setFill(sky);
        g.fillRect(0, 0, w, horizonY);

        // ---- 星点（微微闪烁） ----
        for (int i = 0; i < STAR_COUNT; i++) {
            double twinkle = 0.35 + 0.55 * (0.5 + 0.5 * Math.sin(time * 1.4 + starPhase[i]));
            g.setFill(Color.rgb(255, 255, 255, Math.max(0.0, twinkle)));
            double sx = starX[i] * w;
            double sy = starY[i] * horizonY;
            double sr = 0.8 + (i % 3) * 0.5;
            g.fillOval(sx - sr, sy - sr, sr * 2, sr * 2);
        }

        // ---- 深蓝水面（下半部） ----
        LinearGradient water = new LinearGradient(0, horizonY, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#17325f")),
                new Stop(0.35, Color.web("#0e2248")),
                new Stop(1.0, Color.web("#030a1c")));
        g.setFill(water);
        g.fillRect(0, horizonY, w, h - horizonY);

        // ---- 地平线暗带 + 亮线 ----
        g.setFill(Color.web("#040b18"));
        g.fillRect(0, horizonY - 2, w, 5);
        g.setStroke(Color.rgb(160, 195, 240, 0.35));
        g.setLineWidth(1.2);
        g.strokeLine(0, horizonY, w, horizonY);

        // ---- 白色光弧（天际） ----
        double trailIn = Math.min(0.9, 0.4 + 0.12 * Math.sin(time * 0.9)); // 光弧辉光"呼吸"
        drawTrail(g, w, horizonY, false, time, trailIn);

        // ---- 水面倒影（镜像，粼粼闪动） ----
        double reflectIn = 0.30 + 0.16 * Math.sin(time * 2.6);             // 倒影明暗闪动
        drawTrail(g, w, horizonY, true, time, Math.max(0.08, reflectIn));

        // ---- 水面涟漪（横向光带随波流动） ----
        drawRipples(g, w, h, horizonY, time);

        // ---- 光弧源头在水面的竖向光柱 + 高光点（强调光与影的交汇） ----
        double sourceX = w * 0.14;
        double sourceY = horizonY;
        // 竖向光柱（水中的光路，呼吸闪动）
        LinearGradient col = new LinearGradient(sourceX, horizonY, sourceX, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(235, 244, 255, 0.30 + 0.12 * Math.sin(time * 2.0))),
                new Stop(1.0, Color.rgb(235, 244, 255, 0.0)));
        g.setFill(col);
        g.beginPath();
        g.moveTo(sourceX - 5, sourceY);
        g.lineTo(sourceX + 5, sourceY);
        g.lineTo(sourceX + 26, h);
        g.lineTo(sourceX - 26, h);
        g.closePath();
        g.fill();
    }

    /** 绘制光弧（天际或被水面镜像）。{@code reflect=true} 时绘制水中倒影（y 关于地平线对称） */
    private void drawTrail(GraphicsContext g, double w, double horizonY,
                           boolean reflect, double time, double intensity) {
        // 天际基准控制点：起点在地平线附近(左下) → 控制点高抬 → 终点右上
        double p0x = w * 0.12, p0y = horizonY * 0.95;
        double p1x = w * 0.50, p1y = horizonY * 0.06;
        double p2x = w * 0.90, p2y = horizonY * 0.40;
        if (reflect) {
            // 水中镜像：y 关于地平线对称（向下），横向轻微左右漂移
            double drift = Math.sin(time * 1.3) * 6.0;
            p0x += drift; p1x += drift * 0.5; p2x += drift * 0.3;
            p0y = 2 * horizonY - p0y;
            p1y = 2 * horizonY - p1y;
            p2y = 2 * horizonY - p2y;
        }
        double alpha = reflect ? intensity : (0.45 + 0.35 * intensity);

        g.setLineCap(StrokeLineCap.ROUND);
        // 三层发光：外晕 → 中晕 → 亮芯
        g.setStroke(Color.rgb(255, 255, 255, 0.10 * alpha));
        g.setLineWidth(18);
        strokeQuad(g, p0x, p0y, p1x, p1y, p2x, p2y);
        g.setStroke(Color.rgb(255, 255, 255, 0.38 * alpha));
        g.setLineWidth(8);
        strokeQuad(g, p0x, p0y, p1x, p1y, p2x, p2y);
        g.setStroke(Color.rgb(255, 255, 255, 0.92 * alpha));
        g.setLineWidth(2.6);
        strokeQuad(g, p0x, p0y, p1x, p1y, p2x, p2y);

        // 光弧源头高光（天际起点处最亮，像光源）
        if (!reflect) {
            double glowR = 16.0 + 4.0 * Math.sin(time * 2.0);
            RadialGradient glow = new RadialGradient(0, 0, 0, 0, glowR, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(255, 255, 255, 0.95)),
                    new Stop(0.4, Color.rgb(255, 255, 255, 0.5)),
                    new Stop(1.0, Color.TRANSPARENT));
            g.setFill(glow);
            g.fillOval(p0x - glowR, p0y - glowR, glowR * 2, glowR * 2);
        }
    }

    /** 绘制一条二次贝塞尔曲线 */
    private void strokeQuad(GraphicsContext g, double x0, double y0, double x1, double y1,
                            double x2, double y2) {
        g.beginPath();
        g.moveTo(x0, y0);
        g.quadraticCurveTo(x1, y1, x2, y2);
        g.stroke();
    }

    /** 绘制水面横向涟漪条带（随波流动、明暗闪烁） */
    private void drawRipples(GraphicsContext g, double w, double h, double horizonY, double time) {
        for (int i = 0; i < RIPPLE_COUNT; i++) {
            double ry = horizonY + rippleY[i] * (h - horizonY);
            double flicker = 0.16 + 0.14 * Math.sin(time * 1.7 + ripplePhase[i]);
            double len = 18.0 + rippleX[i] * 60.0;
            double cx = rippleX[i] * w + Math.sin(time * 0.8 + ripplePhase[i]) * 14.0;
            double alpha = Math.max(0.03, flicker);
            g.setStroke(Color.rgb(190, 215, 245, alpha));
            g.setLineWidth(1.1);
            g.strokeLine(cx - len * 0.5, ry, cx + len * 0.5, ry);
        }
    }
}
