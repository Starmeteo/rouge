package com.phantomcorridor.controller;

/** 可切换界面的生命周期，避免动画与游戏循环在后台继续运行。 */
public interface SceneLifecycle {
    default void onEnter() { }
    default void onExit() { }
}
