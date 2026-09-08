package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

import java.util.Random;

/**
 * 双界交融背景组件（「光域 · 影域」主题，对应《双界行者》需求 §3.1 / §8.4 双界配色）。
 *
 * <p>画面构成（上半为光、下半为影，两界在<b>动态波浪交界线</b>交融）：
 * <ul>
 *   <li><b>光之界（上）</b>：暖金渐变天空，白金星点与<b>金色光尘</b>上浮、大团<b>暖光光斑</b>，
 *      一条<b>白金色弧光</b>呼吸流动（§3.1 光界金色/白色为主）；</li>
 *   <li><b>影之界（下）</b>：冷紫渐变暗域，<b>紫色影尘</b>下沉、大团<b>紫影迷雾</b>漂浮、
 *      数条<b>暗紫影弧</b>幽幽脉动（§3.1 影界冷色紫黑为主）；</li>
 *   <li><b>交界交融</b>：两界边界为<b>三频正弦叠加的流动波浪线</b>（非水平线），
 *      沿线绘金→白→紫的<b>交融光带</b>，并有<b>渗透微粒</b>多次穿越交界游走（§2 两界并存映像）。</li>
 * </ul>
 *
 * <p>供 {@link MainMenuView}（主菜单）与 {@link LoginView}（登录界面）复用；
 * 每个实例持有独立的随机粒子状态与自带动画定时器。
 */
public class NightSkyBackdrop extends Canvas {

    // ---- 元素数量 ----
    /** 光界星点数量 */
    private static final int STAR_COUNT = 40;
    /** 金色光尘数量（上半区） */
    private static final int LIGHT_DUST_COUNT = 46;
    /** 紫色影尘数量（下半区） */
    private static final int SHADOW_MOTE_COUNT = 40;
    /** 交界渗透微粒数量（穿越两界） */
    private static final int SEEP_COUNT = 24;
    /** 暖光光斑数量（上半区） */
    private static final int LIGHT_ORB_COUNT = 3;
    /** 紫影迷雾数量（下半区） */
    private static final int SHADOW_FOG_COUNT = 4;
    /** 光界极光带数量 */
    private static final int AURORA_BAND_COUNT = 2;
    /** 影界涡旋数量 */
    private static final int SHADOW_VORTEX_COUNT = 2;
    /** 交界流动火花数量（沿波浪线滑动） */
    private static final int WAVE_SPARK_COUNT = 14;
    /** 影界暗处微光闪烁数量 */
    private static final int SHADOW_GLINT_COUNT = 16;

    /** 交界基准线（视口高比例，两界大体分界） */
    private static final double HORIZON_RATIO = 0.55;

    // ---- 交界波浪参数（三频叠加随时间流动；长波长低频率，任何位置都不出现陡坡/深谷） ----
    /** 波浪振幅 1（像素） */
    private static final double WAVE_AMP1 = 14.0;
    /** 波浪振幅 2（像素） */
    private static final double WAVE_AMP2 = 7.0;
    /** 波浪振幅 3（像素） */
    private static final double WAVE_AMP3 = 4.0;
    /** 波浪频率 1（弧度/像素，波长约 3500px，宽缓起伏） */
    private static final double WAVE_FREQ1 = 0.0018;
    /** 波浪频率 2（弧度/像素，波长约 1400px，次级起伏） */
    private static final double WAVE_FREQ2 = 0.0045;
    /** 波浪频率 3（弧度/像素，波长约 7000px，最宽缓的漂移） */
    private static final double WAVE_FREQ3 = 0.0009;

    // ---- 配色池（颜色尽量丰富：实测深金/琥珀/亮金/白金 | 暗紫/品紫/蓝紫/浅紫） ----
    /** 光尘颜色池 */
    private static final Color[] LIGHT_DUST_COLORS = {
            Color.web("#ffd76e"), Color.web("#fff3d0"),
            Color.web("#f5a04a"), Color.web("#e8c258")};
    /** 影尘颜色池 */
    private static final Color[] SHADOW_DUST_COLORS = {
            Color.web("#8a5ad8"), Color.web("#c9a0f0"),
            Color.web("#5a2a9a"), Color.web("#a878e8")};

    // ---- 光尘状态 ----
    private final double[] dustX = new double[LIGHT_DUST_COUNT];
    private final double[] dustY = new double[LIGHT_DUST_COUNT];
    private final double[] dustSpeed = new double[LIGHT_DUST_COUNT];
    private final double[] dustPhase = new double[LIGHT_DUST_COUNT];
    private final double[] dustSize = new double[LIGHT_DUST_COUNT];
    private final int[] dustColorIdx = new int[LIGHT_DUST_COUNT];

    // ---- 影尘状态 ----
    private final double[] moteX = new double[SHADOW_MOTE_COUNT];
    private final double[] moteY = new double[SHADOW_MOTE_COUNT];
    private final double[] moteSpeed = new double[SHADOW_MOTE_COUNT];
    private final double[] motePhase = new double[SHADOW_MOTE_COUNT];
    private final double[] moteSize = new double[SHADOW_MOTE_COUNT];
    private final int[] moteColorIdx = new int[SHADOW_MOTE_COUNT];

    // ---- 渗透微粒状态（dir > 0 自下而上（金），< 0 自上而下（紫）） ----
    private final double[] seepX = new double[SEEP_COUNT];
    private final double[] seepY = new double[SEEP_COUNT];
    private final double[] seepSpeed = new double[SEEP_COUNT];
    private final double[] seepDir = new double[SEEP_COUNT];
    private final double[] seepSize = new double[SEEP_COUNT];

    // ---- 星点（光界上方） ----
    private final double[] starX = new double[STAR_COUNT];
    private final double[] starY = new double[STAR_COUNT];
    private final double[] starPhase = new double[STAR_COUNT];

    // ---- 光斑 / 影雾 ----
    private final double[] orbX = new double[LIGHT_ORB_COUNT];
    private final double[] orbY = new double[LIGHT_ORB_COUNT];
    private final double[] orbR = new double[LIGHT_ORB_COUNT];
    private final double[] orbPhase = new double[LIGHT_ORB_COUNT];
    private final double[] fogX = new double[SHADOW_FOG_COUNT];
    private final double[] fogY = new double[SHADOW_FOG_COUNT];
    private final double[] fogR = new double[SHADOW_FOG_COUNT];
    private final double[] fogPhase = new double[SHADOW_FOG_COUNT];

    /** 背景动效定时器（面板不可见时通过 {@link #stop()} 停止） */
    private final AnimationTimer timer;

    /** 随机数源 */
    private final Random random = new Random();

    /** 动画累计时间（秒） */
    private double time = 0.0;

    /** 上一帧时间戳（纳秒），-1 表示尚未收到首帧 */
    private long lastNanos = -1L;

    /** 定时器是否运行（保证 start/stop 幂等） */
    private boolean running = false;

    /**
     * @param width  逻辑宽度（与 {@link AppConfig#VIEW_WIDTH} 一致）
     * @param height 逻辑高度（与 {@link AppConfig#VIEW_HEIGHT} 一致）
     */
    public NightSkyBackdrop(double width, double height) {
        super(width, height);
        randomizeScene();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos < 0L) {
                    lastNanos = now;
                    return;
                }
                double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
                lastNanos = now;
                time += dt;
                render(time, dt);
            }
        };
    }

    /** 启动背景动效（幂等：已运行时不重复启动） */
    public void start() {
        if (!running) {
            running = true;
            lastNanos = -1L;
            timer.start();
        }
    }

    /** 停止背景动效（幂等：未运行时直接返回） */
    public void stop() {
        if (running) {
            running = false;
            timer.stop();
        }
    }

    /** 初始化全部粒子的随机参数（只执行一次）；重力/方向在渲染中按固定规则演化 */
    private void randomizeScene() {
        // 光尘：初始散布在光界区域（上方 60%）
        for (int i = 0; i < LIGHT_DUST_COUNT; i++) {
            dustX[i] = random.nextDouble();
            dustY[i] = random.nextDouble() * 0.9;
            dustSpeed[i] = 9.0 + random.nextDouble() * 18.0;   // 上浮速度
            dustPhase[i] = random.nextDouble() * Math.PI * 2.0;
            dustSize[i] = 1.2 + random.nextDouble() * 2.2;
            dustColorIdx[i] = random.nextInt(LIGHT_DUST_COLORS.length);
        }
        // 影尘：散布全屏，呈下沉趋势
        for (int i = 0; i < SHADOW_MOTE_COUNT; i++) {
            moteX[i] = random.nextDouble();
            moteY[i] = 0.55 + random.nextDouble() * 0.45; // 初始即位于影界区域（交界带以下）
            moteSpeed[i] = 6.0 + random.nextDouble() * 12.0;
            motePhase[i] = random.nextDouble() * Math.PI * 2.0;
            moteSize[i] = 1.4 + random.nextDouble() * 2.6;
            moteColorIdx[i] = random.nextInt(SHADOW_DUST_COLORS.length);
        }
        // 渗透微粒：生成在交界线附近，一半向上（金色穿入光界）一半向下（紫粒沉入影界）
        for (int i = 0; i < SEEP_COUNT; i++) {
            seepX[i] = random.nextDouble();
            seepY[i] = 0.35 + random.nextDouble() * 0.35;
            seepSpeed[i] = 10.0 + random.nextDouble() * 18.0;
            seepDir[i] = (i % 2 == 0) ? 1.0 : -1.0;
            seepSize[i] = 1.6 + random.nextDouble() * 2.2;
        }
        // 星点：光界上方
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble() * 0.42;
            starPhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
        // 光斑（暖光漂浮）
        for (int i = 0; i < LIGHT_ORB_COUNT; i++) {
            orbX[i] = random.nextDouble();
            orbY[i] = 0.10 + random.nextDouble() * 0.32;
            orbR[i] = 60.0 + random.nextDouble() * 60.0;
            orbPhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
        // 影雾（紫雾漂浮）
        for (int i = 0; i < SHADOW_FOG_COUNT; i++) {
            fogX[i] = random.nextDouble();
            fogY[i] = 0.62 + random.nextDouble() * 0.30;
            fogR[i] = 110.0 + random.nextDouble() * 90.0;
            fogPhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
    }

    /** 每帧绘制：光界基底（全屏）→ 影界暗域（波浪边界覆盖）→ 交融光带 → 弧线 → 粒子层（星点/光尘/影尘/光斑/影雾/渗透粒） */
    private void render(double t, double dt) {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        double baseY = h * HORIZON_RATIO;

        // ---- 1. 光之界基底（全屏，波浪线以下会被影界暗域覆盖） ----
        g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#fff3d0")),
                new Stop(0.22, Color.web("#ffd98a")),
                new Stop(0.42, Color.web("#e8a952")),
                new Stop(0.62, Color.web("#b07830")),
                new Stop(0.82, Color.web("#6a3a1a")),
                new Stop(1.00, Color.web("#3a1e10"))));
        g.fillRect(0, 0, w, h);

        // ---- 2. 影之界暗域（以流动波浪线为上边界填充下半部分） ----
        g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#5a2a9a")),
                new Stop(0.25, Color.web("#3a1a68")),
                new Stop(0.50, Color.web("#1a0a30")),
                new Stop(0.80, Color.web("#0d0518")),
                new Stop(1.00, Color.web("#07020e"))));
        g.beginPath();
        g.moveTo(0, h);
        g.lineTo(0, waveY(0, baseY, t));
        for (double x = 8; x <= w; x += 16) {
            g.lineTo(x, waveY(x, baseY, t));
        }
        g.lineTo(w, h);
        g.closePath();
        g.fill();

        // ---- 3. 交界交融光带（沿波浪线：金 → 白 → 紫 三层辉光） ----
        drawBoundaryBand(g, w, baseY, t);

        // ---- 3.5 光影光效层：光界极光带 / 影界涡旋（背景氛围，位于粒子层之下） ----
        drawAuroraBands(g, w, baseY, t);
        drawShadowVortexes(g, w, h, baseY, t);

        // ---- 4. 弧线：光区白金光缕（上）+ 影区紫影缕（下），均两端渐隐、缓慢漂移 ----
        drawLightStrands(g, w, baseY, t);
        drawShadowStrands(g, w, h, baseY, t);

        // ---- 5. 粒子层：星点 → 光斑 → 影雾 → 光尘 → 影尘 → 渗透微粒 ----
        drawStars(g, w, baseY, t);
        drawLightOrbs(g, w, baseY, t);
        drawShadowFog(g, w, h, baseY, t);

        updateLightDust(t, dt);
        drawLightDust(g, w, baseY, t);
        updateShadowMotes(dt);
        drawShadowMotes(g, w, t);
        updateSeepParticles(dt);
        drawSeepParticles(g, w, baseY, t);
        drawWaveSparks(g, w, baseY, t);
        drawShadowGlints(g, w, h, baseY, t);
    }

    /** 交界波浪线：三频正弦叠加，随时间流动（宽缓起伏，非水平线，两界动态交融） */
    private double waveY(double x, double baseY, double t) {
        return baseY
                + WAVE_AMP1 * Math.sin(x * WAVE_FREQ1 + t * 1.1)
                + WAVE_AMP2 * Math.sin(x * WAVE_FREQ2 - t * 0.7)
                + WAVE_AMP3 * Math.sin(x * WAVE_FREQ3 + t * 0.35);
    }

    /** 绘制交界交融光带：垂直金→白→紫渐变曲线描边（亮度大幅弱化：仅一条微光细线，
     *  避免在金色天空上叠加出"亮黄边"） */
    private void drawBoundaryBand(GraphicsContext g, double w, double baseY, double t) {
        LinearGradient glow = new LinearGradient(0, baseY - 22, 0, baseY + 22, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 220, 150, 0.00)),
                new Stop(0.35, Color.rgb(255, 230, 185, 0.05)),
                new Stop(0.50, Color.rgb(255, 250, 235, 0.14)),
                new Stop(0.65, Color.rgb(190, 150, 245, 0.06)),
                new Stop(1.00, Color.rgb(190, 150, 245, 0.00)));
        strokeWave(g, w, baseY, t, glow, 11.0);
        strokeWave(g, w, baseY, t, glow, 2.8);
        strokeWave(g, w, baseY, t, glow, 1.1);
    }

    /** 以波浪线为路径描边（补上右端终点，避免边口截断） */
    private void strokeWave(GraphicsContext g, double w, double baseY, double t,
                            LinearGradient paint, double lineWidth) {
        g.setStroke(paint);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(lineWidth);
        g.beginPath();
        g.moveTo(0, waveY(0, baseY, t));
        for (double x = 8; x <= w; x += 16) {
            g.lineTo(x, waveY(x, baseY, t));
        }
        g.lineTo(w, waveY(w, baseY, t));
        g.stroke();
    }

    /** 光界「极光带」：数条水平流动的金白波浪光带，缓慢漂移与呼吸（光界动态光影） */
    private void drawAuroraBands(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < AURORA_BAND_COUNT; i++) {
            double cy = baseY * (0.10 + i * 0.15);
            double amp = 16.0 + i * 7.0;
            double phase = t * 0.55 + i * 2.2;
            double alpha = 0.05 + 0.035 * (0.5 + 0.5 * Math.sin(t * 0.5 + i * 1.6));
            // 光带主体：竖向渐变（透明 → 金白 → 透明）
            g.setFill(new LinearGradient(0, cy - 30 - i * 6, 0, cy + 30 + i * 6, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 235, 190, 0.00)),
                    new Stop(0.50, Color.rgb(255, 240, 205, alpha)),
                    new Stop(1.00, Color.rgb(255, 235, 190, 0.00))));
            g.beginPath();
            for (double x = 0; x <= w; x += 24) {
                double y = cy + amp * Math.sin(x * 0.0032 + phase) + 6 * Math.sin(x * 0.0011 - phase * 0.7);
                if (x == 0) {
                    g.moveTo(x, y);
                } else {
                    g.lineTo(x, y);
                }
            }
            g.lineTo(w, cy + 34 + i * 6);
            g.lineTo(0, cy + 34 + i * 6);
            g.closePath();
            g.fill();
            // 亮芯细线（细腻柔和，避免在金色天空上形成黄色条纹）
            g.setStroke(Color.rgb(255, 248, 225, alpha * 1.2));
            g.setLineWidth(1.0);
            g.beginPath();
            for (double x = 0; x <= w; x += 24) {
                double y = cy + amp * Math.sin(x * 0.0032 + phase) + 6 * Math.sin(x * 0.0011 - phase * 0.7);
                if (x == 0) {
                    g.moveTo(x, y);
                } else {
                    g.lineTo(x, y);
                }
            }
            g.stroke();
        }
    }

    /** 影界「涡旋」：缓慢旋转的暗紫漩涡（多层椭圆弧错开相位，中心暗核），暗示影的流动 */
    private void drawShadowVortexes(GraphicsContext g, double w, double h, double baseY, double t) {
        double span = h - baseY;
        for (int v = 0; v < SHADOW_VORTEX_COUNT; v++) {
            double cx = w * (0.24 + v * 0.52);
            double cy = baseY + span * (0.28 + v * 0.18);
            double rot = t * 16.0 + v * 2.6;
            g.setStroke(Color.rgb(150, 105, 235, 0.10));
            g.setLineWidth(1.6);
            for (int k = 0; k < 4; k++) {
                double r = 40.0 + k * 22.0;
                g.strokeArc(cx - r, cy - r * 0.55, r * 2, r * 1.1, 20.0 * rot + k * 82.0, 86.0, ArcType.OPEN);
            }
            // 中心暗核
            g.setFill(Color.rgb(18, 7, 36, 0.22));
            g.fillOval(cx - 25, cy - 15, 50, 30);
        }
    }

    /** 交界「流动火花」：沿波浪线滑行的金/紫交替光点（体现两界交融的活性） */
    private void drawWaveSparks(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < WAVE_SPARK_COUNT; i++) {
            double s = ((i / (double) WAVE_SPARK_COUNT) + t * 0.010) % 1.0;
            double x = s * w;
            double y = waveY(x, baseY, t) + Math.sin(t * 3.0 + i * 1.3) * 5.0;
            Color c = (i % 2 == 0) ? Color.web("#ffe9b0") : Color.web("#b48ae8");
            double alpha = 0.22 + 0.20 * (0.5 + 0.5 * Math.sin(t * 2.2 + i));
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            double r = 1.6 + (i % 3) * 0.5;
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    /** 影界「暗处微光」：紫域内随机分布的微弱闪烁点（黄金比散布，无需状态数组） */
    private void drawShadowGlints(GraphicsContext g, double w, double h, double baseY, double t) {
        for (int i = 0; i < SHADOW_GLINT_COUNT; i++) {
            double gx = ((i * 0.61803) % 1.0) * w;
            double gy = baseY + ((i * 0.38197) % 1.0) * (h - baseY);
            double tw = 0.5 + 0.5 * Math.sin(t * 1.1 + i * 2.7);
            g.setFill(Color.rgb(190, 150, 245, 0.07 + 0.13 * tw));
            double r = 1.0 + (i % 3) * 0.6;
            g.fillOval(gx - r, gy - r, r * 2, r * 2);
        }
    }

    /** 光区白金「光缕」：数条两端渐隐的细弧，缓慢漂移（柔光融入画面，避免突兀的单线感） */
    private void drawLightStrands(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < 3; i++) {
            double drift = Math.sin(t * 0.30 + i * 2.1) * 14.0;
            double x0 = w * 0.06;
            double x2 = w * 0.94;
            double y0 = baseY * (0.78 - i * 0.09);
            double y1 = baseY * (0.22 - i * 0.05) - 8.0 * Math.sin(t * 0.5 + i * 1.3);
            double y2 = baseY * (0.42 + i * 0.06);
            // 两端 -> 中段渐变的柔光缕：起点与终点均淡出
            double alpha = 0.16 + 0.08 * (0.5 + 0.5 * Math.sin(t * 0.8 + i * 2.0));
            g.setStroke(new LinearGradient(x0, 0, x2, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 235, 190, 0.0)),
                    new Stop(0.42, Color.rgb(255, 235, 190, alpha)),
                    new Stop(1.00, Color.rgb(255, 235, 190, 0.0))));
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineWidth(3.0 - i * 0.5);
            strokeQuad(g, x0, y0, w * 0.5 + drift, y1, x2, y2);
        }
    }

    /** 影区紫影「影缕」：数条两端渐隐的细弧，幽幽浮现（与光缕呼应但更暗沉） */
    private void drawShadowStrands(GraphicsContext g, double w, double h, double baseY, double t) {
        double span = h - baseY;
        for (int i = 0; i < 3; i++) {
            double drift = Math.sin(t * 0.24 + i * 1.7) * 18.0;
            double y0 = baseY + span * (0.30 + i * 0.16);
            double y1 = baseY + span * (0.12 + i * 0.10) + 8.0 * Math.cos(t * 0.4 + i);
            double y2 = baseY + span * (0.38 + i * 0.14);
            double alpha = 0.12 + 0.07 * (0.5 + 0.5 * Math.sin(t * 0.7 + i * 1.9));
            g.setStroke(new LinearGradient(0, 0, w, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(160, 110, 235, 0.0)),
                    new Stop(0.50, Color.rgb(160, 110, 235, alpha)),
                    new Stop(1.00, Color.rgb(160, 110, 235, 0.0))));
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineWidth(2.2);
            strokeQuad(g, w * 0.05, y0, w * 0.5 + drift, y1, w * 0.95, y2);
        }
    }

    /** 光界星点（白金色，微微闪烁） */
    private void drawStars(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < STAR_COUNT; i++) {
            double twinkle = 0.35 + 0.55 * (0.5 + 0.5 * Math.sin(t * 1.4 + starPhase[i]));
            g.setFill(Color.rgb(255, 248, 220, Math.max(0.0, twinkle)));
            double sx = starX[i] * w;
            double sy = starY[i] * baseY;
            double sr = 0.8 + (i % 3) * 0.5;
            g.fillOval(sx - sr, sy - sr, sr * 2, sr * 2);
        }
    }

    /** 光界暖光光斑（大团柔和光晕，缓慢漂移呼吸） */
    private void drawLightOrbs(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < LIGHT_ORB_COUNT; i++) {
            double cx = orbX[i] * w + Math.sin(t * 0.18 + orbPhase[i]) * 40.0;
            double cy = orbY[i] * baseY + Math.sin(t * 0.24 + orbPhase[i] * 2.0) * 16.0;
            double alpha = 0.08 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.9 + orbPhase[i]));
            g.setFill(new RadialGradient(0, 0, 0, 0, orbR[i], false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(255, 225, 160, alpha)),
                    new Stop(0.6, Color.rgb(255, 200, 120, alpha * 0.5)),
                    new Stop(1.0, Color.TRANSPARENT)));
            g.fillOval(cx - orbR[i], cy - orbR[i], orbR[i] * 2, orbR[i] * 2);
        }
    }

    /** 影界紫影迷雾（大团冷雾，缓慢漂移） */
    private void drawShadowFog(GraphicsContext g, double w, double h, double baseY, double t) {
        for (int i = 0; i < SHADOW_FOG_COUNT; i++) {
            double cx = fogX[i] * w + Math.sin(t * 0.14 + fogPhase[i]) * 50.0;
            double cy = fogY[i] * h + Math.sin(t * 0.18 + fogPhase[i] * 1.7) * 20.0;
            double alpha = 0.07 + 0.04 * (0.5 + 0.5 * Math.sin(t * 0.7 + fogPhase[i]));
            g.setFill(new RadialGradient(0, 0, 0, 0, fogR[i], false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(140, 100, 220, alpha)),
                    new Stop(0.6, Color.rgb(90, 55, 160, alpha * 0.6)),
                    new Stop(1.0, Color.TRANSPARENT)));
            g.fillOval(cx - fogR[i], cy - fogR[i], fogR[i] * 2, fogR[i] * 2);
        }
    }

    /** 更新光尘：缓慢上浮 + 正弦横向摆动（从光界中下部升入，出顶后回到交界带上方重生） */
    private void updateLightDust(double t, double dt) {
        for (int i = 0; i < LIGHT_DUST_COUNT; i++) {
            double y = dustY[i] - dustSpeed[i] * dt / getHeight();
            if (y < -0.02) {
                y = 0.30 + random.nextDouble() * 0.16; // 光界区域（交界带上方的中下段）
                dustX[i] = random.nextDouble();
            }
            dustY[i] = y;
            dustX[i] += Math.sin(t * 0.9 + dustPhase[i]) * 0.00025;
            dustX[i] = wrap01(dustX[i]);
        }
    }

    /** 绘制光尘（光界特点：金色小微粒上浮，部分带渐隐拖尾） */
    private void drawLightDust(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < LIGHT_DUST_COUNT; i++) {
            Color c = LIGHT_DUST_COLORS[dustColorIdx[i]];
            double alpha = 0.35 + 0.45 * (0.5 + 0.5 * Math.sin(t * 1.6 + dustPhase[i]));
            double sx = dustX[i] * w;
            double sy = dustY[i] * baseY;
            double s = dustSize[i];
            // 拖尾：与上浮方向相反（向下），渐隐短线
            if (i % 2 == 0) {
                g.setStroke(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha * 0.35));
                g.setLineWidth(s * 0.9);
                g.strokeLine(sx, sy, sx, sy + s * 2.4 + dustSpeed[i] * 0.35);
            }
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            g.fillOval(sx - s, sy - s, s * 2, s * 2);
        }
    }

    /** 更新影尘：缓慢下沉 + 横向摆动（出底后从影界区域重生，避免紫尘误入光界） */
    private void updateShadowMotes(double dt) {
        for (int i = 0; i < SHADOW_MOTE_COUNT; i++) {
            double y = moteY[i] + moteSpeed[i] * dt / getHeight();
            if (y > 1.02) {
                y = 0.55 + random.nextDouble() * 0.45; // 影界区域（交界带以下）
                moteX[i] = random.nextDouble();
            }
            moteY[i] = y;
        }
    }

    /** 绘制影尘（影界特点：紫色小微粒下沉，部分带渐隐拖尾） */
    private void drawShadowMotes(GraphicsContext g, double w, double t) {
        for (int i = 0; i < SHADOW_MOTE_COUNT; i++) {
            Color c = SHADOW_DUST_COLORS[moteColorIdx[i]];
            double alpha = 0.30 + 0.40 * (0.5 + 0.5 * Math.sin(t * 1.3 + motePhase[i]));
            double sx = moteX[i] * w + Math.sin(t * 0.8 + motePhase[i]) * 9.0;
            double sy = moteY[i] * getHeight();
            double s = moteSize[i];
            // 拖尾：与下沉方向相反（向上），渐隐短线
            if (i % 2 == 0) {
                g.setStroke(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha * 0.35));
                g.setLineWidth(s * 0.9);
                g.strokeLine(sx, sy, sx, sy - s * 2.4 - moteSpeed[i] * 0.35);
            }
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            g.fillOval(sx - s, sy - s, s * 2, s * 2);
        }
    }

    /** 更新渗透微粒：沿交界线上下穿越两界（出界后回到交界重新开始） */
    private void updateSeepParticles(double dt) {
        double h = getHeight();
        for (int i = 0; i < SEEP_COUNT; i++) {
            double y = seepY[i] + seepDir[i] * seepSpeed[i] * dt / h;
            // 越过交界 ±0.08 视口范围后重生在交界附近，并随机换向
            if (y > HORIZON_RATIO + 0.08 || y < HORIZON_RATIO - 0.08) {
                y = HORIZON_RATIO + (random.nextDouble() - 0.5) * 0.06;
                seepX[i] = random.nextDouble();
                seepDir[i] = random.nextBoolean() ? 1.0 : -1.0;
            }
            seepY[i] = y;
        }
    }

    /** 绘制渗透微粒（金色上穿光界 / 紫色下沉影界，体现两界互相渗透） */
    private void drawSeepParticles(GraphicsContext g, double w, double baseY, double t) {
        for (int i = 0; i < SEEP_COUNT; i++) {
            boolean rising = seepDir[i] > 0;
            Color c = rising ? Color.web("#ffe9b0") : Color.web("#b48ae8");
            double alpha = 0.45 + 0.3 * (0.5 + 0.5 * Math.sin(t * 2.0 + i));
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            double sx = seepX[i] * w + Math.sin(t * 1.1 + i) * 6.0;
            double sy = seepY[i] * getHeight();
            double s = seepSize[i];
            g.fillOval(sx - s, sy - s, s * 2, s * 2);
        }
    }

    /** 将比例值回绕到 [0,1)（横向摆动不越界） */
    private static double wrap01(double v) {
        double r = v % 1.0;
        return r < 0 ? r + 1.0 : r;
    }

    /** 绘制一条二次贝塞尔曲线 */
    private void strokeQuad(GraphicsContext g, double x0, double y0, double x1, double y1,
                            double x2, double y2) {
        g.beginPath();
        g.moveTo(x0, y0);
        g.quadraticCurveTo(x1, y1, x2, y2);
        g.stroke();
    }
}
