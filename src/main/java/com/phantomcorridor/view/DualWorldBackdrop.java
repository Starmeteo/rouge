package com.phantomcorridor.view;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.controller.SceneLifecycle;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
// 未使用（IDE 的 Unused import 会报）：双界背景只用线性渐变，径向光晕由几何绘制实现。
// import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * 登录与菜单共用的“双界回廊”背景：左侧圣辉、右侧幽影，中间裂隙缓慢呼吸。
 */
public final class DualWorldBackdrop extends Canvas implements SceneLifecycle {

    private static final int MOTE_COUNT = 42;
    private static final int ARCH_COUNT = 7;
    private static final double RIFT_CENTER = 0.52;

    private final double[] moteX = new double[MOTE_COUNT];
    private final double[] moteY = new double[MOTE_COUNT];
    private final double[] moteSpeed = new double[MOTE_COUNT];
    private final double[] motePhase = new double[MOTE_COUNT];
    private final AnimationTimer timer;
    private long startNanos = -1L;
    private long lastNanos = -1L;

    public DualWorldBackdrop() {
        super(AppConfig.VIEW_WIDTH, AppConfig.VIEW_HEIGHT);
        Random random = new Random(0xD0A1BEEFL);
        for (int i = 0; i < MOTE_COUNT; i++) {
            moteX[i] = random.nextDouble();
            moteY[i] = random.nextDouble();
            moteSpeed[i] = 0.018 + random.nextDouble() * 0.032;
            motePhase[i] = random.nextDouble() * Math.PI * 2.0;
        }
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startNanos < 0L) {
                    startNanos = lastNanos = now;
                }
                double time = (now - startNanos) / 1_000_000_000.0;
                double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.05);
                lastNanos = now;
                update(dt);
                draw(time);
            }
        };
        draw(0.0);
    }

    @Override
    public void onEnter() {
        lastNanos = -1L;
        timer.start();
    }

    @Override
    public void onExit() {
        timer.stop();
    }

    private void update(double dt) {
        for (int i = 0; i < MOTE_COUNT; i++) {
            boolean light = moteX[i] < RIFT_CENTER;
            moteY[i] += (light ? -1.0 : 1.0) * moteSpeed[i] * dt;
            if (moteY[i] < -0.03) moteY[i] = 1.03;
            if (moteY[i] > 1.03) moteY[i] = -0.03;
        }
    }

    private void draw(double time) {
        GraphicsContext g = getGraphicsContext2D();
        g.setImageSmoothing(false);
        double w = getWidth();
        double h = getHeight();

        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#241d16")),
                new Stop(0.42, Color.web("#111019")),
                new Stop(1.0, Color.web("#06040d"))));
        g.fillRect(0, 0, w, h);

        drawDomainGlow(g, w, h, time);
        drawStoneFloor(g, w, h);
        drawCorridorArches(g, w, h);
        drawRunes(g, w, h, time);
        drawMotes(g, w, h, time);
        drawRift(g, w, h, time);
        drawVignette(g, w, h);
    }

    private void drawDomainGlow(GraphicsContext g, double w, double h, double time) {
        double pulse = 0.88 + Math.sin(time * 0.7) * 0.06;
        for (int band = 0; band < 6; band++) {
            double inset = band * 48.0;
            double alpha = (0.20 - band * 0.026) * pulse;
            g.setFill(Color.rgb(214, 166, 74, alpha));
            g.fillRect(inset, inset, w * 0.50 - inset, h - inset * 2);
            g.setFill(Color.rgb(105, 58, 166, alpha));
            g.fillRect(w * 0.52, inset, w * 0.48 - inset, h - inset * 2);
        }
    }

    private void drawStoneFloor(GraphicsContext g, double w, double h) {
        double horizon = h * 0.50;
        g.setFill(Color.rgb(4, 4, 8, 0.44));
        g.fillRect(0, horizon, w, h - horizon);
        g.setLineWidth(1.0);
        for (int i = -7; i <= 7; i++) {
            boolean light = i < 0;
            g.setStroke(light ? Color.rgb(211, 170, 91, 0.10) : Color.rgb(139, 93, 209, 0.11));
            g.strokeLine(w * 0.5, horizon, w * 0.5 + i * 145.0, h);
        }
        for (int i = 1; i <= 9; i++) {
            double y = horizon + Math.pow(i / 9.0, 1.7) * (h - horizon);
            g.setStroke(Color.rgb(170, 150, 145, 0.075));
            g.strokeLine(0, y, w, y);
        }
    }

    private void drawCorridorArches(GraphicsContext g, double w, double h) {
        for (int i = 0; i < ARCH_COUNT; i++) {
            double inset = 34.0 + i * 42.0;
            double top = 36.0 + i * 31.0;
            double alpha = 0.25 - i * 0.025;
            g.setLineWidth(Math.max(1.2, 5.0 - i * 0.5));
            g.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(225, 183, 96, alpha)),
                    new Stop(0.49, Color.rgb(190, 157, 112, alpha * 0.42)),
                    new Stop(0.53, Color.rgb(119, 72, 176, alpha * 0.50)),
                    new Stop(1.0, Color.rgb(125, 77, 197, alpha))));
            g.strokeRect(snap(inset), snap(top), snap(w - inset * 2.0), snap(h - top + 110.0));
        }
    }

    private void drawRunes(GraphicsContext g, double w, double h, double time) {
        for (int side = 0; side < 2; side++) {
            double cx = side == 0 ? w * 0.17 : w * 0.84;
            double cy = h * 0.25;
            Color color = side == 0 ? Color.web("#e9bd62") : Color.web("#9b65e5");
            double alpha = 0.14 + 0.05 * Math.sin(time * 0.8 + side * 2.0);
            g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.setLineWidth(1.4);
            for (int ring = 0; ring < 3; ring++) {
                double radius = 45.0 + ring * 18.0;
                g.strokeRect(snap(cx - radius), snap(cy - radius), snap(radius * 2), snap(radius * 2));
            }
            for (int spoke = 0; spoke < 8; spoke++) {
                double angle = spoke * Math.PI / 4.0 + time * (side == 0 ? 0.025 : -0.025);
                g.strokeLine(cx + Math.cos(angle) * 48.0, cy + Math.sin(angle) * 48.0,
                        cx + Math.cos(angle) * 78.0, cy + Math.sin(angle) * 78.0);
            }
        }
    }

    private void drawMotes(GraphicsContext g, double w, double h, double time) {
        for (int i = 0; i < MOTE_COUNT; i++) {
            boolean light = moteX[i] < RIFT_CENTER;
            Color color = light ? Color.web("#f0c66f") : Color.web("#9e6ae2");
            double alpha = 0.18 + 0.30 * (0.5 + 0.5 * Math.sin(time * 1.4 + motePhase[i]));
            double x = moteX[i] * w + Math.sin(time * 0.45 + motePhase[i]) * 8.0;
            double y = moteY[i] * h;
            double radius = 2.0 + (i % 3) * 2.0;
            g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillRect(snap(x - radius), snap(y - radius), radius * 2, radius * 2);
        }
    }

    private void drawRift(GraphicsContext g, double w, double h, double time) {
        double baseX = w * RIFT_CENTER;
        for (int layer = 5; layer >= 0; layer--) {
            double alpha = layer == 0 ? 0.82 : 0.035 + (5 - layer) * 0.018;
            g.setStroke(layer == 0 ? Color.rgb(238, 224, 255, alpha)
                    : Color.rgb(151, 99, 220, alpha));
            g.setLineWidth(layer == 0 ? 1.5 : 5.0 + layer * 5.0);
            g.beginPath();
            for (int y = -20; y <= h + 20; y += 12) {
                double x = snap(baseX
                        + Math.sin(y * 0.020 + time * 0.65) * 10.0
                        + Math.sin(y * 0.053 - time * 0.44) * 4.0);
                if (y == -20) g.moveTo(x, y); else g.lineTo(x, y);
            }
            g.stroke();
        }
    }

    private void drawVignette(GraphicsContext g, double w, double h) {
        for (int i = 0; i < 5; i++) {
            g.setStroke(Color.rgb(0, 0, 0, 0.13 + i * 0.055));
            g.setLineWidth(32);
            g.strokeRect(i * 16, i * 16, w - i * 32, h - i * 32);
        }
    }

    private static double snap(double value) { return Math.round(value / 4.0) * 4.0; }
}
