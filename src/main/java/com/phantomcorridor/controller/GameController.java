package com.phantomcorridor.controller;

import com.phantomcorridor.core.GameLoop;
import com.phantomcorridor.model.GameSession;
import com.phantomcorridor.view.GameView;
import javafx.scene.input.KeyCode;

/** 游戏输入、模型更新和渲染调度。 */
public final class GameController {

    private final GameSession session = new GameSession();
    private final InputState input = new InputState();
    private final GameView view;
    private final Runnable onPauseRequested;
    private final GameLoop loop;
    private boolean running;
    private boolean shiftHeld;

    public GameController(GameView view, Runnable onPauseRequested) {
        this.view = view;
        this.onPauseRequested = onPauseRequested;
        this.loop = new GameLoop() {
            @Override
            protected void update(double dt) {
                session.update(dt, input.horizontal(), input.vertical());
            }

            @Override
            protected void render(double frameDelta) {
                view.render(session, getSmoothedFps());
            }
        };
        view.bindInput(this::keyPressed, this::keyReleased);
        view.bindLifecycle(this::start, this::stop);
        session.newRun();
    }

    public void newRun() {
        session.newRun();
        input.clear();
        shiftHeld = false;
        view.render(session, 0.0);
    }

    private void start() {
        if (!running) {
            loop.start();
            running = true;
        }
    }

    private void stop() {
        if (running) {
            loop.stop();
            running = false;
        }
        input.clear();
        shiftHeld = false;
    }

    private void keyPressed(KeyCode key) {
        switch (key) {
            case W, UP -> input.setUp(true);
            case S, DOWN -> input.setDown(true);
            case A, LEFT -> input.setLeft(true);
            case D, RIGHT -> input.setRight(true);
            case SHIFT -> {
                if (!shiftHeld && session.tryShiftWorld()) {
                    view.playWorldShift(session.getPlayer().getCurrentWorld());
                }
                shiftHeld = true;
            }
            case ESCAPE, P -> onPauseRequested.run();
            default -> { }
        }
    }

    private void keyReleased(KeyCode key) {
        switch (key) {
            case W, UP -> input.setUp(false);
            case S, DOWN -> input.setDown(false);
            case A, LEFT -> input.setLeft(false);
            case D, RIGHT -> input.setRight(false);
            case SHIFT -> shiftHeld = false;
            default -> { }
        }
    }
}
