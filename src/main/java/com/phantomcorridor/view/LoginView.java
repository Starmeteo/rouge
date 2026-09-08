package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.Settings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 登录界面（对应《双界行者》需求 §8.1 登录界面、§4.3 存档设计）。
 *
 * <p>应用启动的第一个场景（§8.1）。玩家输入<b>昵称</b>（必填）用于本地玩家档案识别
 * （仅本地，无服务器联网，§4.3）。登录页<b>不设密码</b>，仅需昵称即可进入主菜单。
 *
 * <p>背景复用 {@link NightSkyBackdrop}（夜空 · 双辉主题），与主菜单视觉连续，
 * 登录时即可感知游戏「光与影」的双界基调（§3.1 世界配色）。
 *
 * <p><b>视图职责界限</b>：本类只做布局与输入收集（view），不实现校验逻辑；
 * 当昵称非空时回调 {@code onLoggedIn}，并把昵称写入注入的 {@link Settings}。
 */
public class LoginView extends StackPane {

    /** 登录界面标题 */
    private static final String TITLE = "双界行者";

    /** 登录界面副标题 */
    private static final String SUBTITLE = "光与影 · 皆通途";

    /** 昵称输入框 */
    private final TextField nicknameField = new TextField();

    /** 夜空双辉背景（与主菜单共用渲染组件，保证界面风格连续） */
    private final NightSkyBackdrop backdrop;

    private final Settings settings;

    /**
     * 构建登录面板。
     *
     * @param settings   全局设置对象（登录成功后写入玩家昵称）
     * @param onLoggedIn 登录成功回调（昵称非空时由本类触发，由 App 切换到主菜单）
     */
    public LoginView(Settings settings, Runnable onLoggedIn) {
        this.settings = settings;
        getStyleClass().add("login-pane");

        // 背景置于最底层；表单内容叠加其上
        backdrop = new NightSkyBackdrop(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        backdrop.setManaged(false);
        getChildren().add(backdrop);

        // 双界图标：☀（光之界，金色）与 ☾（影之界，紫色）分列标题两侧（§3.1 世界配色）
        Label sun = new Label("☀");
        sun.getStyleClass().add("login-icon-light");
        Label moon = new Label("☾");
        moon.getStyleClass().add("login-icon-shadow");

        Label title = new Label(TITLE);
        title.getStyleClass().add("login-title");

        HBox titleRow = new HBox(20.0, sun, title, moon);
        titleRow.setAlignment(Pos.CENTER);

        Label subtitle = new Label(SUBTITLE);
        subtitle.getStyleClass().add("login-subtitle");

        nicknameField.getStyleClass().add("login-input");
        nicknameField.setPromptText("昵称（必填）");
        nicknameField.setPrefWidth(300.0);
        nicknameField.setMaxWidth(300.0);

        Label hint = new Label("本地档案识别 · 无联网（需求 §4.3）");
        hint.getStyleClass().add("hint-text");

        Button startButton = createLoginButton("进入", onLoggedIn);
        startButton.setDefaultButton(true); // Enter 快捷登录

        // 恢复上次输入的昵称（若有），提升重复游玩体验
        nicknameField.setText(settings.getPlayerNickname());

        // 登录卡：半透深底 + 金边辉光，双界主题的游戏化表单（与主菜单按钮同风格）
        VBox box = new VBox(22.0, titleRow, subtitle, nicknameField, hint, startButton);
        box.getStyleClass().add("login-card");
        box.setAlignment(Pos.CENTER);
        box.setFillWidth(false);
        box.setMaxWidth(VBox.USE_PREF_SIZE);
        getChildren().add(box);

        // 面板不可见时停止背景动效（避免 CPU 空转）；可见时启动
        visibleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                backdrop.start();
            } else {
                backdrop.stop();
            }
        });
        backdrop.start();
    }

    /** 创建登录按钮并绑定动作（样式复用 ui.css 的 .menu-button，悬停/聚焦动效与主菜单一致） */
    private Button createLoginButton(String text, Runnable onLoggedIn) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setOnAction(event -> submit(onLoggedIn));
        return button;
    }

    /** 提交登录：昵称非空才写入档案并回调；否则聚焦昵称框提示 */
    private void submit(Runnable onLoggedIn) {
        String nickname = nicknameField.getText();
        if (nickname == null || nickname.trim().isEmpty()) {
            nicknameField.requestFocus();
            return;
        }
        settings.setPlayerNickname(nickname);
        onLoggedIn.run();
    }
}
