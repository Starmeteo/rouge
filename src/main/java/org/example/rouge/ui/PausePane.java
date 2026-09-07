package org.example.rouge.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 暂停面板（需求 §4.1 界面流程、FR-14"游戏界面支持暂停并返回主菜单"）。
 *
 * <p>游戏进行中按 Esc/P 时由 {@link org.example.rouge.GameApplication} 切换到本面板，
 * 提供"继续游戏"与"回到主菜单"两个操作。
 */
public class PausePane extends StackPane {

    /**
     * 构建暂停面板。
     *
     * @param onResume     "继续游戏"回调 —— 恢复游戏主循环
     * @param onQuitToMenu "回到主菜单"回调
     */
    public PausePane(Runnable onResume, Runnable onQuitToMenu) {
        getStyleClass().add("pause-pane");

        Label title = new Label("已暂停");
        title.getStyleClass().add("pause-title");

        Button resumeButton = createPauseButton("继续游戏", onResume);
        resumeButton.setDefaultButton(true); // Enter 快捷继续（需求 §4.3）

        Button quitButton = createPauseButton("回到主菜单", onQuitToMenu);

        VBox box = new VBox(26.0, title, resumeButton, quitButton);
        box.setAlignment(Pos.CENTER);
        getChildren().add(box);

        // Esc/P 继续游戏（需求 §4.3）
        setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE || code == KeyCode.P) {
                onResume.run();
            }
        });
    }

    /** 统一创建暂停界面按钮并绑定动作 */
    private Button createPauseButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setOnAction(event -> action.run());
        return button;
    }
}
