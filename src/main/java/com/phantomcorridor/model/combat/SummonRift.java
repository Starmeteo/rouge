package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.EnemyKind;

/**
 * 首领召唤裂隙：先出现、后出怪。
 *
 * <p>召唤最怕“凭空冒出来”。裂隙把召唤拆成两段：起手时先在预定落点撕开一道口子，
 * 玩家能从预警圈上看到哪里要出怪、还剩多久；{@link #isReady()} 之后才在那个位置放出召唤物。
 * 这样召唤既是压力，也是可以读、可以躲、可以提前占位的招式。
 */
public final class SummonRift {
    private final EnemyKind kind;
    private final WorldType world;
    private final double x;
    private final double y;
    private final double duration;
    private double age;

    public SummonRift(EnemyKind kind, WorldType world, double x, double y, double duration) {
        this.kind = kind;
        this.world = world;
        this.x = x;
        this.y = y;
        this.duration = Math.max(0.05, duration);
    }

    public void update(double dt) { age += Math.max(0.0, dt); }

    /** 裂隙是否已经成型到可以放出召唤物。 */
    public boolean isReady() { return age >= duration; }

    /** 成型进度 0～1：渲染层据此让预警圈收缩、传送门张开。 */
    public double progress() { return Math.min(1.0, age / duration); }

    /** 这只裂隙将要放出的物种。 */
    public EnemyKind kind() { return kind; }

    public WorldType world() { return world; }

    public double x() { return x; }

    public double y() { return y; }

    public double age() { return age; }

    public double duration() { return duration; }
}
