package org.example.rouge;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.rouge.core.GameState;
import org.example.rouge.core.Settings;
import org.example.rouge.ui.GamePane;
import org.example.rouge.ui.MainMenuPane;
import org.example.rouge.ui.PausePane;

/**
 * JavaFX 应用主类（对应需求书 §5.2 工程结构中位于根目录的 GameApplication.java）。
 *
 * <p>职责：
 * <ul>
 *   <li>创建 1280×960 固定逻辑分辨率主窗口，支持 <b>F11 / Alt+Enter</b> 切换全屏；</li>
 *   <li>加载全局样式表 {@code ui/ui.css}（血月旅人主题）；</li>
 *   <li>界面流转编排（主菜单 → 游戏 → 暂停 → 主菜单），对应需求 §4.1 界面流程。</li>
 * </ul>
 */
public class GameApplication extends Application {

    /** 窗口标题 */
    public static final String APP_TITLE = "幻境回廊";

    /** 逻辑视口宽度（画布尺寸，固定逻辑分辨率） */
    public static final int VIEW_WIDTH = 1280;

    /** 逻辑视口高度（画布尺寸，固定逻辑分辨率） */
    public static final int VIEW_HEIGHT = 960;

    /** 全屏切换快捷键 1：F11 */
    private static final KeyCombination FULLSCREEN_F11 = new KeyCodeCombination(KeyCode.F11);

    /** 全屏切换快捷键 2：Alt+Enter */
    private static final KeyCombination FULLSCREEN_ALT_ENTER =
            new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

    /** 主窗口（"退出"操作与全屏切换需要） */
    private Stage stage;

    /** 所有界面面板的根节点 */
    private final StackPane root = new StackPane();

    private MainMenuPane mainMenuPane;
    private GamePane gamePane;
    private PausePane pausePane;

    /** 当前界面状态（需求 §4.1） */
    private GameState currentState = GameState.MAIN_MENU;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // 全局设置对象：由主菜单的设置面板读写，后续天数的灵敏度/音量/种子接入消费
        Settings settings = new Settings();

        // 面板之间不直接相互引用，全部通过回调交给本类编排，降低耦合
        mainMenuPane = new MainMenuPane(this::showGame, () -> stage.close(), settings);
        pausePane = new PausePane(this::resumeGame, this::showMainMenu);
        gamePane = new GamePane(this::showPause);
        root.getChildren().addAll(mainMenuPane, gamePane, pausePane);
        setActive(mainMenuPane);

        Scene scene = new Scene(root, VIEW_WIDTH, VIEW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("ui/ui.css").toExternalForm());
        scene.setOnKeyPressed(this::handleGlobalKeys);

        stage.setTitle(APP_TITLE);
        stage.setResizable(false); // 固定窗口尺寸；全屏时逻辑分辨率保持 1280×960 不变
        stage.setFullScreenExitHint("按 F11 或 Alt+Enter 退出全屏");
        // 屏蔽 JavaFX 默认的 Esc 退出全屏，避免与游戏暂停快捷键（Esc/P）冲突
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setScene(scene);
        stage.show();
    }

    /** 全局快捷键处理：F11 / Alt+Enter 切换全屏 */
    private void handleGlobalKeys(KeyEvent event) {
        if (FULLSCREEN_F11.match(event) || FULLSCREEN_ALT_ENTER.match(event)) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    /** 切换到主菜单（应用启动默认，或从暂停界面返回） */
    public void showMainMenu() {
        // 无论当前是否处于游戏中，停止主循环都是安全的（GamePane 内部保证幂等）
        gamePane.onLeave();
        currentState = GameState.MAIN_MENU;
        setActive(mainMenuPane);
    }

    /** 进入游戏界面（主菜单点击"开始游戏"） */
    public void showGame() {
        currentState = GameState.PLAYING;
        setActive(gamePane);
        gamePane.onEnter(); // 启动主循环并请求键盘焦点
    }

    /** 打开暂停界面（游戏中按 Esc/P 或点击暂停） */
    public void showPause() {
        currentState = GameState.PAUSED;
        setActive(pausePane);
        gamePane.onLeave(); // 暂停即停止主循环，节省 CPU
        pausePane.requestFocus();
    }

    /** 恢复游戏（暂停界面点击"继续游戏"或按 Esc/P） */
    public void resumeGame() {
        currentState = GameState.PLAYING;
        setActive(gamePane);
        gamePane.onEnter();
    }

    /** 将目标面板设为唯一可见、可参与布局的子节点 */
    private void setActive(Pane target) {
        for (javafx.scene.Node node : root.getChildren()) {
            boolean active = node == target;
            node.setVisible(active);
            node.setManaged(active);
        }
    }
}
