package com.internal.platform;

import org.lwjgl.glfw.GLFW;

public interface Input {
    void setInputProcessor(InputProcessor inputProcessor);
    int getDeltaX();
    int getDeltaY();
    void setCursorCatched(boolean captured);

    final class Buttons {
        public static final int LEFT = 0;
        public static final int RIGHT = 1;
    }

    final class Keys {
        public static final int W = GLFW.GLFW_KEY_W;
        public static final int A = GLFW.GLFW_KEY_A;
        public static final int S = GLFW.GLFW_KEY_S;
        public static final int D = GLFW.GLFW_KEY_D;
        public static final int I = GLFW.GLFW_KEY_I;
        public static final int SPACE = GLFW.GLFW_KEY_SPACE;
        public static final int SHIFT_LEFT = GLFW.GLFW_KEY_LEFT_SHIFT;
        public static final int CONTROL_LEFT = GLFW.GLFW_KEY_LEFT_CONTROL;
        public static final int NUM_1 = GLFW.GLFW_KEY_1;
    }
}
