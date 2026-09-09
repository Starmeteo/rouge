package com.phantomcorridor.model;

/**
 * 道具类型枚举（对应新需求 §10 核心数据模型约定 ItemType，§7.1 道具分类）。
 *
 * <p>道具按世界流派区分，而非单纯加攻击/加血量：
 * <ul>
 *   <li>{@link #LIGHT} —— 光属性：强化光形态攻击、远程手感；</li>
 *   <li>{@link #SHADOW} —— 影属性：强化影形态近战、速度、吸血；</li>
 *   <li>{@link #DUAL} —— 双属性：两种形态都强化，但数值较低；</li>
 *   <li>{@link #UNIVERSAL} —— 通用：提高血量、移速、拾取范围等。</li>
 * </ul>
 */
public enum ItemType {

    /** 光属性道具（§7.1） */
    LIGHT,

    /** 影属性道具（§7.1） */
    SHADOW,

    /** 双属性道具（§7.1） */
    DUAL,

    /** 通用道具（§7.1） */
    UNIVERSAL
}
