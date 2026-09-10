package com.phantomcorridor.model;

/** 房间奖励或敌人掉落的轻量拾取物。 */
public record Pickup(Type type, double x, double y, int amount) {
    public enum Type { COIN, PHASE_FRAGMENT, HEALTH, ITEM, EQUIPMENT }
}
