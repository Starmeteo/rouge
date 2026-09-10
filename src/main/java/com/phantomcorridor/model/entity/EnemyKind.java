package com.phantomcorridor.model.entity;

/** 素材包中的七个可生成物种；id 与 monster_pack_v1 目录保持一致。 */
public enum EnemyKind {
    LANTERN("n01_lantern", false, 2, 0.45, "seeker_orb"),
    WOLF("n02_wolf", false, 3, 0.75, "bite_arc"),
    GOLEM("n03_golem", false, 5, 0.30, "ground_crack"),
    MAGE("n04_mage", false, 3, 0.40, "fan_pellet"),
    EXECUTIONER("e01_executioner", true, 10, 0.55, "spear_projectile"),
    BELL("e02_bell", true, 9, 0.25, "bell_pellet"),
    WATCHER("b01_watcher", true, 50, 0.32, "rift_spear");

    private final String assetId;
    private final boolean elite;
    private final int hitPoints;
    private final double speedMultiplier;
    private final String lightEffect;

    EnemyKind(String assetId, boolean elite, int hitPoints, double speedMultiplier, String lightEffect) {
        this.assetId = assetId;
        this.elite = elite;
        this.hitPoints = hitPoints;
        this.speedMultiplier = speedMultiplier;
        this.lightEffect = lightEffect;
    }

    public String assetId() { return assetId; }
    public boolean elite() { return elite; }
    public int hitPoints() { return hitPoints; }
    public double speedMultiplier() { return speedMultiplier; }
    public String lightEffect() { return lightEffect; }
}
