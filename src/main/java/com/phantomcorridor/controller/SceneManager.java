package com.phantomcorridor.controller;

import com.phantomcorridor.core.GameState;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * 场景管理器（对应《双界行者》需求 §9.3）。一次切换同时更新状态、可见节点和生命周期。
 *
 * <p>职责：持有应用根容器与当前界面状态，负责把某一场景面板设为唯一可见、可参与布局的节点。
 * 界面流转（§4.1）：登录 → 主菜单 → 游戏 → 暂停 →（回到主菜单 / 结算）。状态由
 * {@link GameState} 描述。
 *
 * <p>实例由应用顶层持有，避免静态全局状态污染测试。控制器/视图不直接操作根节点。
 */
public final class SceneManager {

    /** 应用根容器（所有场景面板的父节点） */
    private final StackPane root;

    private Pane activeView;

    /** 当前界面状态 */
    private GameState state = GameState.LOGIN;

    /** 创建绑定到指定根容器的场景管理器。 */
    public SceneManager(StackPane root) {
        this.root = root;
    }

    /**
     * 原子地更新状态和可见界面，并调用前后界面的生命周期。
     *
     * @param nextState 目标游戏状态
     * @param target    已注册在根容器中的目标界面
     */
    public void switchTo(GameState nextState, Pane target) {
        if (!root.getChildren().contains(target)) {
            throw new IllegalArgumentException("目标界面尚未注册到根容器");
        }
        if (activeView == target && state == nextState) {
            return;
        }
        if (activeView instanceof SceneLifecycle lifecycle) {
            lifecycle.onExit();
        }
        for (Node node : root.getChildren()) {
            boolean active = node == target;
            node.setVisible(active);
            node.setManaged(active);
        }
        state = nextState;
        activeView = target;
        if (target instanceof SceneLifecycle lifecycle) {
            lifecycle.onEnter();
        }
    }

    /**
     * 在当前画面上显示覆盖层。底层节点保留可见但退出生命周期，适合暂停菜单。
     */
    public void showOverlay(GameState nextState, Pane overlay) {
        if (!root.getChildren().contains(overlay)) {
            throw new IllegalArgumentException("覆盖层尚未注册到根容器");
        }
        if (activeView instanceof SceneLifecycle lifecycle) {
            lifecycle.onExit();
        }
        overlay.setManaged(true);
        overlay.setVisible(true);
        overlay.toFront();
        state = nextState;
        activeView = overlay;
        if (overlay instanceof SceneLifecycle lifecycle) {
            lifecycle.onEnter();
        }
    }

    public GameState state() {
        return state;
    }
}
