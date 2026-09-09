package com.phantomcorridor.controller;

/** 与 JavaFX 按键事件解耦后的连续移动输入状态。 */
public final class InputState {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    public double horizontal() {
        return (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
    }

    public double vertical() {
        return (down ? 1.0 : 0.0) - (up ? 1.0 : 0.0);
    }

    public void clear() {
        up = down = left = right = false;
    }

    public void setUp(boolean up) { this.up = up; }
    public void setDown(boolean down) { this.down = down; }
    public void setLeft(boolean left) { this.left = left; }
    public void setRight(boolean right) { this.right = right; }
}
