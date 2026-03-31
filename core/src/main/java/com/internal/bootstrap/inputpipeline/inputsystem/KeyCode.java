package com.internal.bootstrap.inputpipeline.inputsystem;

/*
 * Engine-owned keyboard key codes.
 *
 * Values intentionally match GLFW so the backend can map platform input
 * without pulling LibGDX key enums into core systems/settings.
 */
public final class KeyCode {

    private KeyCode() {
    }

    // Letters
    public static final int A = 65;
    public static final int D = 68;
    public static final int I = 73;
    public static final int S = 83;
    public static final int W = 87;

    // Modifiers / controls
    public static final int SPACE = 32;
    public static final int LEFT_SHIFT = 340;
    public static final int LEFT_CONTROL = 341;

    // Number row
    public static final int NUM_1 = 49;
}
