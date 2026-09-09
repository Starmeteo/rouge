package com.phantomcorridor.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputStateTest {

    @Test
    void oppositeDirectionsCancelEachOther() {
        InputState input = new InputState();
        input.setLeft(true);
        input.setRight(true);
        input.setUp(true);

        assertEquals(0.0, input.horizontal());
        assertEquals(-1.0, input.vertical());
        input.clear();
        assertEquals(0.0, input.vertical());
    }
}
