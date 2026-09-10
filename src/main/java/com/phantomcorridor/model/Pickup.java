package com.phantomcorridor.model;

/** 房间奖励或敌人掉落的轻量拾取物。 */
public record Pickup(Type type, double x, double y, int amount) {
    public enum Type { COIN, PHASE_FRAGMENT, HEALTH, ITEM, EQUIPMENT }

    /**
     * 拾取物显示名：地面名称标签与交互提示共用。
     *
     * <p>回血道具不能只画成一个红包就了事，统一叫「生命恢复药剂」，玩家一眼能认出是什么。
     */
    public String displayName() {
        return switch (type) {
            case COIN -> "金币";
            case PHASE_FRAGMENT -> "相位碎片";
            case HEALTH -> "生命恢复药剂";
            case ITEM -> "道具";
            case EQUIPMENT -> EquipmentType.values()[
                    Math.floorMod(amount, EquipmentType.values().length)].displayName();
        };
    }
}
