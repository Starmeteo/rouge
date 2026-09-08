package com.phantomcorridor.config;

/**
 * 全局游戏设置（第一版为内存版，随功能开发逐步生效与完善）。
 *
 * <p>各项设置接入计划（对应《双界行者》需求与开发计划）：
 * <ul>
 *   <li><b>鼠标灵敏度</b> —— 第 3 天战斗系统接入瞄准计算（需求 §3.3 攻击手感）；</li>
 *   <li><b>音效 / 音乐音量</b> —— 第 8 天音频系统接入；</li>
 *   <li><b>开发随机种子</b> —— 第 5 天地图生成接入（需求 §3.3 复现地图，便于调试）；</li>
 *   <li><b>玩家昵称</b> —— 登录界面写入，用于本地玩家档案识别（需求 §4.3 存档设计）；</li>
 *   <li>持久化（保存到用户目录配置文件）计划在第 8 天后的界面完善阶段加入。</li>
 * </ul>
 */
public class Settings {

    /** 默认鼠标灵敏度（1.0 = 标准，无额外增益） */
    public static final double DEFAULT_MOUSE_SENSITIVITY = 1.0;

    /** 鼠标灵敏度可调下限 */
    public static final double MIN_MOUSE_SENSITIVITY = 0.5;

    /** 鼠标灵敏度可调上限 */
    public static final double MAX_MOUSE_SENSITIVITY = 2.0;

    /** 默认音效音量（0.0~1.0） */
    public static final double DEFAULT_SFX_VOLUME = 0.8;

    /** 默认音乐音量（0.0~1.0） */
    public static final double DEFAULT_MUSIC_VOLUME = 0.6;

    /** 鼠标灵敏度倍率（作用于玩家移动/瞄准相关输入计算） */
    private double mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;

    /** 音效音量（0.0~1.0），第 8 天接入 AudioClip 播放 */
    private double sfxVolume = DEFAULT_SFX_VOLUME;

    /** 音乐音量（0.0~1.0），第 8 天接入背景音乐 */
    private double musicVolume = DEFAULT_MUSIC_VOLUME;

    /** 开发模式随机种子（空字符串表示每次随机；第 5 天地图生成接入） */
    private String devSeed = "";

    /** 玩家昵称（登录界面录入，用于本地档案识别，可留空但不建议） */
    private String playerNickname = "";

    /** @return 鼠标灵敏度倍率（0.5~2.0） */
    public double getMouseSensitivity() {
        return mouseSensitivity;
    }

    /** @param mouseSensitivity 鼠标灵敏度倍率，自动钳制到 {@link #MIN_MOUSE_SENSITIVITY}~{@link #MAX_MOUSE_SENSITIVITY} */
    public void setMouseSensitivity(double mouseSensitivity) {
        this.mouseSensitivity = clamp(mouseSensitivity, MIN_MOUSE_SENSITIVITY, MAX_MOUSE_SENSITIVITY);
    }

    /** @return 音效音量（0.0~1.0） */
    public double getSfxVolume() {
        return sfxVolume;
    }

    /** @param sfxVolume 音效音量，自动钳制到 0.0~1.0 */
    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = clamp(sfxVolume, 0.0, 1.0);
    }

    /** @return 音乐音量（0.0~1.0） */
    public double getMusicVolume() {
        return musicVolume;
    }

    /** @param musicVolume 音乐音量，自动钳制到 0.0~1.0 */
    public void setMusicVolume(double musicVolume) {
        this.musicVolume = clamp(musicVolume, 0.0, 1.0);
    }

    /** @return 开发模式随机种子（空串表示随机生成） */
    public String getDevSeed() {
        return devSeed;
    }

    /** @param devSeed 开发模式随机种子（传入空串表示随机） */
    public void setDevSeed(String devSeed) {
        this.devSeed = devSeed == null ? "" : devSeed.trim();
    }

    /** @return 玩家昵称（本地档案标识） */
    public String getPlayerNickname() {
        return playerNickname;
    }

    /** @param playerNickname 玩家昵称（为空表示未填写） */
    public void setPlayerNickname(String playerNickname) {
        this.playerNickname = playerNickname == null ? "" : playerNickname.trim();
    }

    /** 恢复全部设置到默认值（供"恢复默认"按钮调用） */
    public void reset() {
        mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        sfxVolume = DEFAULT_SFX_VOLUME;
        musicVolume = DEFAULT_MUSIC_VOLUME;
        devSeed = "";
        playerNickname = "";
    }

    /** 将数值钳制到 [min, max] 闭区间 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
