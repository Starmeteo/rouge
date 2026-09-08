package com.phantomcorridor.view;

import com.phantomcorridor.config.Settings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.DoubleConsumer;

/**
 * 设置覆盖层（主菜单"设置"入口弹出的覆盖层，无边框设计，风格与道具图鉴一致）。
 *
 * <p>第一版设置项（对应 {@link Settings} 的接入计划，均会随功能开发逐步生效）：
 * <ul>
 *   <li>鼠标灵敏度（0.5×~2.0×）—— 第 3 天战斗接入；</li>
 *   <li>音效 / 音乐音量 —— 第 8 天音频接入；</li>
 *   <li>开发模式随机种子 —— 第 5 天地图生成接入（复现地图）；</li>
 *   <li>玩家昵称 —— 登录界面写入（此处只读展示）。</li>
 * </ul>
 *
 * <p>面板只读写注入的 {@link Settings} 对象，不直接关心具体功能如何使用这些值。
 */
public class SettingsOverlay extends VBox {

    /** 设置行名称标签最小宽度，保证各滑块左对齐 */
    private static final double LABEL_MIN_WIDTH = 120.0;

    /** 滑块宽度 */
    private static final double SLIDER_WIDTH = 260.0;

    private final Settings settings;

    /** 鼠标灵敏度滑块（引用保存，供"恢复默认"时回写） */
    private final Slider sensitivitySlider = new Slider(
            Settings.MIN_MOUSE_SENSITIVITY, Settings.MAX_MOUSE_SENSITIVITY, Settings.DEFAULT_MOUSE_SENSITIVITY);

    /** 音效音量滑块（0~100 百分比） */
    private final Slider sfxSlider = new Slider(0.0, 100.0, Settings.DEFAULT_SFX_VOLUME * 100.0);

    /** 音乐音量滑块（0~100 百分比） */
    private final Slider musicSlider = new Slider(0.0, 100.0, Settings.DEFAULT_MUSIC_VOLUME * 100.0);

    /** 随机种子输入框 */
    private final TextField seedField = new TextField();

    /** 玩家昵称只读展示标签 */
    private final Label nicknameLabel = new Label();

    /**
     * 构建设置面板。
     *
     * @param settings 全局设置对象（面板读写该对象）
     * @param onBack   点击"返回菜单"回调（由主菜单执行覆盖层收起动画）
     */
    public SettingsOverlay(Settings settings, Runnable onBack) {
        this.settings = settings;
        getStyleClass().add("overlay-panel");
        setAlignment(Pos.CENTER);
        setFillWidth(false); // 各行保持自然宽度并整体居中，避免内容被拉伸后贴向一侧
        setMaxWidth(VBox.USE_PREF_SIZE);
        setSpacing(20.0);

        Label title = new Label("设置");
        title.getStyleClass().add("overlay-title");

        // ---- 鼠标灵敏度（×0.50 ~ ×2.00） ----
        Label sensitivityValue = new Label();
        sensitivityValue.getStyleClass().add("settings-value");
        bindSensitivity(sensitivityValue);
        HBox sensitivityRow = createSettingRow("鼠标灵敏度", sensitivitySlider, sensitivityValue);

        // ---- 音效音量（0~100%） ----
        Label sfxValue = new Label();
        sfxValue.getStyleClass().add("settings-value");
        bindVolume(sfxSlider, sfxValue, settings::setSfxVolume);
        HBox sfxRow = createSettingRow("音效音量", sfxSlider, sfxValue);

        // ---- 音乐音量（0~100%） ----
        Label musicValue = new Label();
        musicValue.getStyleClass().add("settings-value");
        bindVolume(musicSlider, musicValue, settings::setMusicVolume);
        HBox musicRow = createSettingRow("音乐音量", musicSlider, musicValue);

        // ---- 开发模式随机种子（复现地图） ----
        Label seedLabel = new Label("随机种子");
        seedLabel.getStyleClass().add("settings-label");
        seedLabel.setMinWidth(LABEL_MIN_WIDTH);
        seedField.getStyleClass().add("settings-seed-field");
        // 宽度与滑块行的"滑块+数值"区域对齐（260 + 14 + 56 = 330），保证各行等宽、左右整齐
        seedField.setPrefWidth(330.0);
        seedField.setPromptText("留空 = 每次随机（开发模式，用于复现地图）");
        seedField.setText(settings.getDevSeed());
        seedField.textProperty().addListener((obs, oldValue, newValue) ->
                settings.setDevSeed(newValue));
        HBox seedRow = new HBox(14.0, seedLabel, seedField);
        seedRow.setAlignment(Pos.CENTER);
        seedRow.setMaxWidth(HBox.USE_PREF_SIZE);

        // ---- 玩家昵称（只读展示，登录界面写入） ----
        Label nicknameLabelBox = new Label("玩家昵称");
        nicknameLabelBox.getStyleClass().add("settings-label");
        nicknameLabelBox.setMinWidth(LABEL_MIN_WIDTH);
        nicknameLabel.getStyleClass().add("settings-value");
        refreshNickname();
        HBox nicknameRow = new HBox(14.0, nicknameLabelBox, nicknameLabel);
        nicknameRow.setAlignment(Pos.CENTER);
        nicknameRow.setMaxWidth(HBox.USE_PREF_SIZE);

        // ---- 操作按钮 ----
        Button defaultButton = menuButton("恢复默认", this::resetToDefaults);
        Button backButton = menuButton("返回菜单", onBack);
        HBox buttonRow = new HBox(16.0, defaultButton, backButton);
        buttonRow.setAlignment(Pos.CENTER);

        Label hint = new Label("提示：灵敏度将在战斗系统（第 3 天）、音量将在音频（第 8 天）、" +
                "种子将在地图生成（第 5 天）接入后生效，更多设置项将持续完善。");
        hint.getStyleClass().add("hint-text");

        getChildren().addAll(title, sensitivityRow, sfxRow, musicRow, seedRow, nicknameRow, buttonRow, hint);
    }

    /** 创建一行设置项：名称标签 + 滑块 + 当前值标签（行内容整体居中，各行等宽对齐） */
    private HBox createSettingRow(String name, Slider slider, Label valueLabel) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("settings-label");
        nameLabel.setMinWidth(LABEL_MIN_WIDTH);
        slider.getStyleClass().add("settings-slider");
        slider.setPrefWidth(SLIDER_WIDTH);
        HBox row = new HBox(14.0, nameLabel, slider, valueLabel);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(HBox.USE_PREF_SIZE);
        return row;
    }

    /** 绑定鼠标灵敏度滑块：滑块值 → 设置对象 + 数值展示（×1.00 格式） */
    private void bindSensitivity(Label valueLabel) {
        sensitivitySlider.setBlockIncrement(0.1);
        sensitivitySlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            settings.setMouseSensitivity(newValue.doubleValue());
            valueLabel.setText(String.format("×%.2f", settings.getMouseSensitivity()));
        });
        valueLabel.setText(String.format("×%.2f", settings.getMouseSensitivity()));
    }

    /** 绑定音量滑块：滑块值（0~100）→ 设置对象（0.0~1.0）+ 百分比展示 */
    private void bindVolume(Slider slider, Label valueLabel, DoubleConsumer setter) {
        slider.setBlockIncrement(5.0);
        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            setter.accept(newValue.doubleValue() / 100.0);
            valueLabel.setText(Math.round(newValue.doubleValue()) + "%");
        });
        valueLabel.setText(Math.round(slider.getValue()) + "%");
    }

    /** 刷新昵称只读展示文本 */
    private void refreshNickname() {
        String nickname = settings.getPlayerNickname();
        nicknameLabel.setText(nickname == null || nickname.isEmpty() ? "（未设置）" : nickname);
    }

    /** 恢复全部默认值并同步刷新 UI 控件（setValue 会触发监听器刷新数值标签） */
    private void resetToDefaults() {
        settings.reset();
        sensitivitySlider.setValue(settings.getMouseSensitivity());
        sfxSlider.setValue(settings.getSfxVolume() * 100.0);
        musicSlider.setValue(settings.getMusicVolume() * 100.0);
        seedField.setText(settings.getDevSeed());
        refreshNickname();
    }

    /** 创建与主菜单一致的菜单按钮（样式类复用 ui.css 的 .menu-button） */
    private Button menuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setOnAction(event -> action.run());
        return button;
    }
}
