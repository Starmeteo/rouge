package com.phantomcorridor.model;

/**
 * 可装备武器/饰品及其首轮属性。每个槽位只保留一件。
 *
 * <p>{@code price} 是商店售价（金币）：定位在“打两三个房间刚好买得起一件”，
 * 让商店成为需要攒钱的目标而不是随手就买。
 */
public enum EquipmentType {
    DAWN_WAND("晨曦法杖", "光", "光弹伤害 +20%，速度 +10%", 0, 24),
    SHADOW_FANG("影牙短刃", "影", "影斩伤害 +20%，冷却 -10%", 1, 24),
    RIFT_TWINBLADE("裂界双端刃", "双界", "两种形态伤害 +10%", 2, 32),
    DAWN_SEAL("晨曦圣印", "光", "光弹伤害 +10%", 3, 16),
    DUSK_CLOAK("暮色斗篷", "影", "影界移速额外 +10%", 4, 18),
    PHASE_VESSEL("相位容器", "通用", "相位碎片恢复 +20%", 5, 20);

    private final String displayName;
    private final String affinity;
    private final String description;
    private final int iconIndex;
    private final int price;
    EquipmentType(String displayName, String affinity, String description, int iconIndex, int price) {
        this.displayName = displayName; this.affinity = affinity;
        this.description = description; this.iconIndex = iconIndex; this.price = price;
    }
    public String displayName() { return displayName; }
    public String affinity() { return affinity; }
    public String description() { return description; }
    public int iconIndex() { return iconIndex; }
    /** 商店售价（金币）。 */
    public int price() { return price; }
}
