package com.phantomcorridor.controller;

import com.phantomcorridor.core.GameState;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * 场景管理器（对应《双界行者》需求 §9.3 场景切换，统一使用
 * {@code SceneManager.switchTo(view.getRoot())}）。
 *
 * <p>职责：持有应用根容器与当前界面状态，负责把某一场景面板设为唯一可见、可参与布局的节点。
 * 界面流转（§4.1）：登录 → 主菜单 → 游戏 → 暂停 →（回到主菜单 / 结算）。状态由
 * {@link GameState} 描述。
 *
 * <p>实现说明：以「单例 + 静态入口」形式提供，贴合需求书中 {@code SceneManager.switchTo(...)}
 * 的调用写法；应用启动时先 {@link #init(StackPane)} 注册根容器，之后即可静态调用。
 * 控制器/视图只负责发起切换请求，不直接操作根节点。
 */
public final class SceneManager {

    /** 当前单例 */
    private static SceneManager instance;

    /** 应用根容器（所有场景面板的父节点） */
    private final StackPane root;

    /** 当前界面状态 */
    private GameState state = GameState.LOGIN;

    /** 私有构造：通过 {@link #init(StackPane)} 创建单例 */
    private SceneManager(StackPane root) {
        this.root = root;
    }

    /**
     * 初始化并注册根容器（应用启动时调用一次）。
     *
     * @param root 应用根容器（StackPane，容纳所有场景面板）
     * @return 创建的单例
     */
    public static SceneManager init(StackPane root) {
        instance = new SceneManager(root);
        return instance;
    }

    /**
     * 切换到指定场景面板：仅该面板可见且参与布局，其余隐藏。
     * 无需再显式设置状态；场景面板所属阶段应先用 {@link #setState(GameState)} 记录。
     *
     * @param target 目标场景面板
     */
    public static void switchTo(Pane target) {
        if (instance == null) {
            throw new IllegalStateException("SceneManager 尚未初始化：请先调用 SceneManager.init(root)");
        }
        for (Node node : instance.root.getChildren()) {
            boolean active = node == target;
            node.setVisible(active);
            node.setManaged(active);
        }
    }

    /** @param state 记录当前界面状态（切换场景时调用） */
    public static void setState(GameState state) {
        requireInit();
        instance.state = state;
    }

    /** @return 当前界面状态 */
    public static GameState state() {
        requireInit();
        return instance.state;
    }

    /** 校验单例已初始化，否则抛出异常 */
    private static void requireInit() {
        if (instance == null) {
            throw new IllegalStateException("SceneManager 尚未初始化：请先调用 SceneManager.init(root)");
        }
    }
}
