package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.controller.SceneLifecycle;
import com.phantomcorridor.model.GameSession;
import com.phantomcorridor.model.WorldType;
import javafx.animation.FadeTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

/** 游戏视图：负责输入事件转发、Canvas 展示和切界闪屏，不包含业务规则。 */
public final class GameView extends StackPane implements SceneLifecycle {

    private static final Duration SHIFT_FLASH_DURATION = Duration.millis(360);

    private final Canvas canvas = new Canvas(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
    private final GameRenderer renderer = new GameRenderer();
    private final Region shiftFlash = new Region();
    private Consumer<KeyCode> keyPressed = key -> { };
    private Consumer<KeyCode> keyReleased = key -> { };
    private BiConsumer<Double, Double> pointerMoved = (x, y) -> { };
    private Consumer<Boolean> attackChanged = attacking -> { };
    private Runnable enterAction = () -> { };
    private Runnable exitAction = () -> { };

    public GameView() {
        setFocusTraversable(true);
        shiftFlash.getStyleClass().add("world-shift-flash");
        shiftFlash.setManaged(false);
        shiftFlash.setMouseTransparent(true);
        shiftFlash.setMinSize(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        shiftFlash.setPrefSize(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        shiftFlash.setVisible(false);
        getChildren().addAll(canvas, shiftFlash);

        setOnKeyPressed(event -> keyPressed.accept(event.getCode()));
        setOnKeyReleased(event -> keyReleased.accept(event.getCode()));
        setOnMouseMoved(event -> pointerMoved.accept(event.getX(), event.getY()));
        setOnMouseDragged(event -> pointerMoved.accept(event.getX(), event.getY()));
        setOnMousePressed(event -> {
            requestFocus();
            pointerMoved.accept(event.getX(), event.getY());
            if (event.getButton() == MouseButton.PRIMARY) {
                attackChanged.accept(true);
            }
        });
        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                attackChanged.accept(false);
            }
        });
        setOnMouseExited(event -> attackChanged.accept(false));
    }

    public void bindInput(Consumer<KeyCode> onPressed, Consumer<KeyCode> onReleased) {
        keyPressed = onPressed;
        keyReleased = onReleased;
    }

    public void bindPointer(BiConsumer<Double, Double> onMoved, Consumer<Boolean> onAttackChanged) {
        pointerMoved = onMoved;
        attackChanged = onAttackChanged;
    }

    public void bindLifecycle(Runnable onEnter, Runnable onExit) {
        enterAction = onEnter;
        exitAction = onExit;
    }

    public void render(GameSession session, double fps) {
        renderer.render(canvas.getGraphicsContext2D(), session, fps);
    }

    public void playWorldShift(WorldType world) {
        shiftFlash.getStyleClass().removeAll("shift-to-light", "shift-to-shadow");
        shiftFlash.getStyleClass().add(world == WorldType.LIGHT ? "shift-to-light" : "shift-to-shadow");
        shiftFlash.setVisible(true);
        shiftFlash.setOpacity(0.72);
        FadeTransition fade = new FadeTransition(SHIFT_FLASH_DURATION, shiftFlash);
        fade.setFromValue(0.72);
        fade.setToValue(0.0);
        fade.setOnFinished(event -> shiftFlash.setVisible(false));
        fade.playFromStart();
    }

    @Override
    public void onEnter() {
        requestFocus();
        enterAction.run();
    }

    @Override
    public void onExit() {
        exitAction.run();
    }
}
