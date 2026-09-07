/**
 * 《幻境回廊》模块信息。
 *
 * <p>游戏画面使用 Canvas 代码绘制（见 {@code core.GameLoop} 与 {@code ui.GamePane}），
 * 模块仅依赖 javafx.controls（其透传 javafx.graphics / javafx.base）。
 * 未导出包（model / generator / ai / collision）为模块内部实现，
 * 仅导出对外可见的启动入口与核心运行支持、界面层。
 */
module org.example.rouge {
    requires javafx.controls;

    exports org.example.rouge;
    exports org.example.rouge.core;
    exports org.example.rouge.ui;
}
