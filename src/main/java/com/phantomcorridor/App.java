package com.phantomcorridor;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.Settings;
import com.phantomcorridor.controller.SceneManager;
import com.phantomcorridor.core.GameState;
import com.phantomcorridor.view.GameView;
import com.phantomcorridor.view.LoginView;
import com.phantomcorridor.view.MainMenuView;
import com.phantomcorridor.view.PauseView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX 应用主类：负责窗口创建与场景流转编排（对应《双界行者》需求 §9.1 的顶层编排层）。
 *
 * <p>职责（§4.1 界面流程）：
 * <ul>
 *   <li>创建 1280×960 固定逻辑分辨率主窗口，支持 <b>F11 / Alt+Enter</b> 切换全屏；</li>
 *   <li>加载全局样式表 {@code ui/ui.css}（双界旅人主题）；</li>
 *   <li>初始化 {@link SceneManager}，并把各场景面板装入根容器；</li>
 *   <li>场景流转编排：登录 → 主菜单 → 游戏 → 暂停 → 主菜单/结算，对应需求 §4.1。</li>
 * </ul>
 *
 * <p>本类承担「顶层控制器」角色，负责把 view 层回调转发为场景切换（调 {@link SceneManager}）；
 * 具体的游戏业务逻辑不放在这里（§9.2）。
 */
public class App extends Application {

    /** 全屏切换快捷键 1：F11 */
    private static final KeyCombination FULLSCREEN_F11 = new KeyCodeCombination(KeyCode.F11);

    /** 全屏切换快捷键 2：Alt+Enter */
    private static final KeyCombination FULLSCREEN_ALT_ENTER =
            new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

    /** 主窗口（"退出"操作与全屏切换需要） */
    private Stage stage;

    /** 所有界面面板的根节点（由 SceneManager 持有并切换可见性） */
    private final StackPane root = new StackPane();

    private LoginView loginView;
    private MainMenuView mainMenuView;
    private GameView gameView;
    private PauseView pauseView;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // 全局设置对象：由登录界面写昵称、设置面板读写其它项，后续天数接入消费
        Settings settings = new Settings();

        // 面板之间不直接相互引用，全部通过回调交给本类编排，降低耦合
        loginView = new LoginView(settings, this::showMainMenu);
        mainMenuView = new MainMenuView(this::showGame, () -> stage.close(), settings);
        gameView = new GameView(this::showPause);
        pauseView = new PauseView(this::resumeGame, this::showMainMenu);
        root.getChildren().addAll(loginView, mainMenuView, gameView, pauseView);

        SceneManager.init(root);

        Scene scene = new Scene(root, AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("ui/ui.css").toExternalForm());
        scene.setOnKeyPressed(this::handleGlobalKeys);

        stage.setTitle(AppConfig.APP_TITLE);
        stage.setResizable(false); // 固定窗口尺寸；全屏时逻辑分辨率保持 1280×960 不变
        stage.setFullScreenExitHint("按 F11 或 Alt+Enter 退出全屏");
        // 屏蔽 JavaFX 默认的 Esc 退出全屏，避免与游戏暂停快捷键（Esc/P）冲突
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setScene(scene);
        stage.show();

        // 应用启动后进入第一个场景：登录界面（§8.1）
        showLogin();
    }

    /** 全局快捷键处理：F11 / Alt+Enter 切换全屏 */
    private void handleGlobalKeys(KeyEvent event) {
        if (FULLSCREEN_F11.match(event) || FULLSCREEN_ALT_ENTER.match(event)) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    /** 进入登录界面（应用启动默认） */
    private void showLogin() {
        SceneManager.setState(GameState.LOGIN);
        SceneManager.switchTo(loginView);
        loginView.requestFocus();
    }

    /** 切换到主菜单（登录成功，或从暂停界面/结算返回） */
    public void showMainMenu() {
        // 无论当前是否处于游戏中，停止主循环都是安全的（GameView 内部保证幂等）
        gameView.onLeave();
        SceneManager.setState(GameState.MAIN_MENU);
        SceneManager.switchTo(mainMenuView);
        mainMenuView.requestFocus();
    }

    /** 进入游戏界面（主菜单点击"开始游戏"） */
    public void showGame() {
        SceneManager.setState(GameState.PLAYING);
        SceneManager.switchTo(gameView);
        gameView.onEnter(); // 启动主循环并请求键盘焦点
    }

    /** 打开暂停界面（游戏中按 Esc/P 或点击暂停） */
    public void showPause() {
        SceneManager.setState(GameState.PAUSED);
        SceneManager.switchTo(pauseView);
        gameView.onLeave(); // 暂停即停止主循环，节省 CPU
        pauseView.requestFocus();
    }

    /** 恢复游戏（暂停界面点击"继续游戏"或按 Esc/P） */
    public void resumeGame() {
        SceneManager.setState(GameState.PLAYING);
        SceneManager.switchTo(gameView);
        gameView.onEnter();
    }
}
