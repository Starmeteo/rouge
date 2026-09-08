package com.phantomcorridor;

import javafx.application.Application;

/**
 * 程序启动入口（对应《双界行者》需求 §9.1 Launcher.java，程序入口）。
 *
 * <p>JavaFX 平台要求 {@link Application} 子类由 JavaFX 运行时加载，不能直接作为
 * JVM 主类，因此通过本类的 {@link #main(String[])} 调用 {@link Application#launch} 完成启动。
 *
 * <p>等价启动方式：{@code mvn javafx:run}（pom.xml 已配置主类为本类）。
 */
public final class Launcher {

    /** 工具类：不允许实例化 */
    private Launcher() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（透传给 JavaFX 运行时）
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
