package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.EnemyKind;

/** 独立于本体姿态的攻击、蓄力、命中和消散视觉层。 */
public final class EnemyVisualEffect {
    private final EnemyKind source;
    private final WorldType world;
    private final String effectId;
    private final boolean bodyAnimation;
    private final String facing;
    private final double x, y, angleRadians, size;
    private final double duration;
    private double age;

    public EnemyVisualEffect(EnemyKind source, WorldType world, String effectId,
                             double x, double y, double angleRadians, double size, double duration) {
        this(source, world, effectId, x, y, angleRadians, size, duration, false, "right");
    }
    private EnemyVisualEffect(EnemyKind source, WorldType world, String effectId,
                              double x, double y, double angleRadians, double size, double duration,
                              boolean bodyAnimation, String facing) {
        this.source = source; this.world = world; this.effectId = effectId;
        this.x = x; this.y = y; this.angleRadians = angleRadians; this.size = size;
        this.duration = duration; this.bodyAnimation = bodyAnimation; this.facing = facing; }
    public static EnemyVisualEffect body(EnemyKind source, WorldType world, String action, String facing,
                                         double x, double y, double size, double duration) {
        return new EnemyVisualEffect(source, world, action, x, y, 0.0, size, duration, true, facing);
    }
    public void update(double dt) { age += Math.max(0, dt); }
    public boolean expired() { return age >= duration; }
    public EnemyKind source() { return source; }
    public WorldType world() { return world; }
    public String effectId() { return effectId; }
    public double x() { return x; }
    public double y() { return y; }
    public double angleRadians() { return angleRadians; }
    public double size() { return size; }
    public double age() { return age; }
    public double duration() { return duration; }
    public boolean isBodyAnimation() { return bodyAnimation; }
    public String facing() { return facing; }
}
