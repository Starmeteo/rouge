package com.phantomcorridor.core;

import javafx.animation.AnimationTimer;

/**
 * 游戏主循环基类：基于 {@link AnimationTimer} 的<b>固定时间步长</b>循环。
 *
 * <p>设计说明（对应新需求 §9.2「controller 处理游戏循环流程」与 §3.3 手感保障）：
 * <ul>
 *   <li><b>固定时间步长</b>：逻辑更新始终以 {@value #FIXED_DT} 秒为步长调用 {@link #update(double)}，
 *       与显示器刷新率解耦，保证不同刷新率下移动/射击手感一致；</li>
 *   <li><b>帧时间上限</b>：单帧耗时超过 {@link #MAX_FRAME_TIME} 时截断累加器，
 *       防止卡顿后追帧过多形成"死亡螺旋"；</li>
 *   <li><b>更新与渲染分离</b>：子类只需实现 {@link #update(double)} 与 {@link #render(double)}，
 *       本类负责时间统计与 FPS 采样。</li>
 * </ul>
 *
 * <p>典型帧执行流程（§9.2 / 旧需求 §5.3）：
 * 更新输入 → 更新角色 → 更新子弹 → 更新敌人 AI → 碰撞检测 → 清理死亡对象 →
 * 更新房间状态 → 渲染画面。
 */
public abstract class GameLoop extends AnimationTimer {

    /** 逻辑更新固定时间步长（秒），与 {@link com.phantomcorridor.config.AppConfig#FIXED_DT} 一致 */
    protected static final double FIXED_DT = com.phantomcorridor.config.AppConfig.FIXED_DT;

    /** 单帧耗时上限（秒）：超过则截断，避免卡顿后追帧导致性能崩溃 */
    private static final double MAX_FRAME_TIME = 0.25;

    /** 帧率计算的最小分母，避免首帧抖动除零 */
    private static final double MIN_FRAME_DELTA = 1e-9;

    /** 逻辑更新累加器（秒） */
    private double accumulator = 0.0;

    /** 上一帧时间戳（纳秒），-1 表示尚未收到首帧 */
    private long lastNanos = -1L;

    /** 累计渲染帧数（调试用） */
    private long frameCount = 0L;

    /** 指数平滑后的帧率（FPS，调试用） */
    private double smoothedFps = 0.0;

    /**
     * 固定步长逻辑更新。一帧内可能被调用 0 次（高刷新率屏幕）或多次（低刷新率屏幕），
     * 参数恒为 {@link #FIXED_DT}。
     *
     * @param dt 固定时间步长（秒），恒等于 1/60
     */
    protected abstract void update(double dt);

    /**
     * 渲染回调，每帧恰好调用一次。
     *
     * @param frameDelta 实际帧间隔（秒），仅用于渲染层（如帧间插值），勿用于逻辑计算
     */
    protected abstract void render(double frameDelta);

    @Override
    public void start() {
        // 重新开始/恢复时重置时间戳与累加器，避免上次停止前的陈旧时间造成超长 dt
        lastNanos = -1L;
        accumulator = 0.0;
        super.start();
    }

    @Override
    public final void handle(long now) {
        // 首帧仅记录时间戳（没有合法的"上一帧时间"），不执行更新与渲染
        if (lastNanos < 0L) {
            lastNanos = now;
            return;
        }

        // 1. 计算实际帧间隔，并截断上限
        double frameDelta = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (frameDelta > MAX_FRAME_TIME) {
            frameDelta = MAX_FRAME_TIME;
        }

        // 2. 按固定步长消费累加器：一帧可执行多次逻辑更新（低刷新率），
        //    也可能不执行（高刷新率）；渲染始终每帧执行一次，保证画面连贯
        accumulator += frameDelta;
        while (accumulator >= FIXED_DT) {
            update(FIXED_DT);
            accumulator -= FIXED_DT;
        }

        // 3. 渲染与帧率统计（指数平滑，避免 FPS 数字剧烈抖动）
        render(frameDelta);
        frameCount++;
        double instantFps = 1.0 / Math.max(frameDelta, MIN_FRAME_DELTA);
        smoothedFps = smoothedFps == 0.0 ? instantFps
                : smoothedFps * 0.9 + instantFps * 0.1;
    }

    /** @return 当前平滑帧率（FPS） */
    public double getSmoothedFps() {
        return smoothedFps;
    }

    /** @return 累计渲染帧数 */
    public long getFrameCount() {
        return frameCount;
    }
}
