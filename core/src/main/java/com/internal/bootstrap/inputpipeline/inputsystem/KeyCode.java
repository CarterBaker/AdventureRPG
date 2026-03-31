package com.internal.bootstrap.inputpipeline.inputsystem;

import com.badlogic.gdx.Input;

/*
 * Engine-owned keyboard key codes.
 *
 * During the LibGDX backend phase these values intentionally mirror
 * Input.Keys so existing saved settings and runtime input behavior stay
 * stable while callers stop importing LibGDX enums directly.
 */
public final class KeyCode {

    private KeyCode() {
    }

    // Letters
    public static final int A = Input.Keys.A;
    public static final int D = Input.Keys.D;
    public static final int I = Input.Keys.I;
    public static final int S = Input.Keys.S;
    public static final int W = Input.Keys.W;

    // Modifiers / controls
    public static final int SPACE = Input.Keys.SPACE;
    public static final int LEFT_SHIFT = Input.Keys.SHIFT_LEFT;
    public static final int LEFT_CONTROL = Input.Keys.CONTROL_LEFT;

    // Number row
    public static final int NUM_1 = Input.Keys.NUM_1;
}
