package application.runtime.input;

import engine.settings.Settings;
import engine.util.input.Binding;
import engine.util.input.Buttons;
import engine.util.input.Keys;

public final class Bindings {

    /*
     * One Binding per game action. Loaded from Settings at startup.
     * Non-final so rebinding can replace the key array at runtime.
     */

    // Movement
    public static Binding MOVE_FORWARD = new Binding(Keys.W);
    public static Binding MOVE_BACK = new Binding(Keys.S);
    public static Binding MOVE_LEFT = new Binding(Keys.A);
    public static Binding MOVE_RIGHT = new Binding(Keys.D);
    public static Binding JUMP = new Binding(Keys.SPACE);
    public static Binding WALK = new Binding(Keys.CONTROL_LEFT);
    public static Binding SPRINT = new Binding(Keys.SHIFT_LEFT);

    // Actions
    public static Binding INVENTORY = new Binding(Keys.I);

    // Camera
    public static Binding LOOK = new Binding(Buttons.RIGHT);

    // Accessible \\

    public static void load(Settings settings) {
        MOVE_FORWARD.set(settings.bindMoveForward);
        MOVE_BACK.set(settings.bindMoveBack);
        MOVE_LEFT.set(settings.bindMoveLeft);
        MOVE_RIGHT.set(settings.bindMoveRight);
        JUMP.set(settings.bindJump);
        WALK.set(settings.bindWalk);
        SPRINT.set(settings.bindSprint);
        INVENTORY.set(settings.bindInventory);
    }
}