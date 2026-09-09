package com.phantomcorridor.util;

import java.util.Random;

/**
 * 随机工具（§9.1 util.RandomUtil；无状态通用工具）。
 *
 * <p>为地图生成（§5.1 节点图）、敌人/道具随机（§6、§7）提供稳定的区间随机。
 * 所有方法都接收显式 {@link Random} 实例，便于注入固定种子复现地图（§3.3 开发模式）。
 */
public final class RandomUtil {

    /** 工具类：不允许实例化 */
    private RandomUtil() {
    }

    /**
     * 返回 [min, max) 区间内的随机浮点数。
     *
     * @param random 随机源（可注入固定 seed 的实例）
     * @param min    下界（含）
     * @param max    上界（不含）
     * @return [min, max) 的随机 double；max==min 时返回 min
     */
    public static double nextDouble(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /**
     * 返回 [min, max] 闭区间内的随机整数（包含两端）。
     *
     * @param random 随机源
     * @param min    下界（含）
     * @param max    上界（含）
     * @return [min, max] 的随机 int
     */
    public static int nextInt(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    /**
     * 返回 [0, bound) 的随机浮点数。
     *
     * @param random 随机源
     * @param bound  上界（不含，须 &gt; 0）
     * @return [0, bound) 的随机 double
     */
    public static double nextDouble(Random random, double bound) {
        return random.nextDouble() * bound;
    }

    /**
     * 按权重随机选择索引。
     *
     * <p>传入每个候选的权重数组，返回被选中的下标。权重不要求归一化。
     *
     * @param random  随机源
     * @param weights 各候选权重（须非负，且至少一个 &gt; 0）
     * @return 被选中的下标（0 起）
     */
    public static int weightedIndex(Random random, double[] weights) {
        double total = 0.0;
        for (double w : weights) {
            total += Math.max(0.0, w);
        }
        if (total <= 0.0) {
            return 0;
        }
        double r = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            r -= Math.max(0.0, weights[i]);
            if (r < 0.0) {
                return i;
            }
        }
        return weights.length - 1;
    }
}
