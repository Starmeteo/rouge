package com.phantomcorridor.model.combat;

public enum EnemyType {
    LANTERN("n01_lantern", "灯魇", 5, 0.45), WOLF("n02_wolf", "棱晶狼", 5, 0.75),
    GOLEM("n03_golem", "界石傀儡", 10, 0.30), MAGE("n04_mage", "盾面法师", 10, 0.40),
    EXECUTIONER("e01_executioner", "双相执刑者", 20, 0.55), BELL("e02_bell", "回廊鸣钟", 25, 0.25),
    WATCHER("b01_watcher", "裂隙守望者", 35, 0.0);
    private final String id; private final String displayName; private final int maxHp; private final double speedFactor;
    EnemyType(String id, String displayName, int maxHp, double speedFactor) { this.id=id; this.displayName=displayName; this.maxHp=maxHp; this.speedFactor=speedFactor; }
    public String id() { return id; } public String displayName() { return displayName; }
    public int maxHp() { return maxHp; } public double speedFactor() { return speedFactor; }
}
