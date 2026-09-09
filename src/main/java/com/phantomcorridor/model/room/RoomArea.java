package com.phantomcorridor.model.room;

/** 构成房间轮廓的一个轴对齐矩形区域。 */
public record RoomArea(double x, double y, double width, double height) {
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
