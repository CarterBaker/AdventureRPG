package application.kernel.inputpipeline.input;

import engine.input.Binding;
import engine.input.BindingType;
import engine.input.InputCode;
import engine.root.HandlePackage;

public class RawInputHandle extends HandlePackage {

    /*
     * Per-context snapshot of raw hardware state written each frame by InputSystem.
     * Everything that needs to know what the user is physically doing reads from
     * here.
     * InputSystem only writes if the mouse is hovering over this context's window —
     * otherwise all state stays false/zero. No concept of active or focused window.
     */

    // Keys
    private final boolean[] keysClicked = new boolean[512];
    private final boolean[] keysHeld = new boolean[512];
    private final boolean[] keysReleased = new boolean[512];

    // Buttons
    private final boolean[] buttonsClicked = new boolean[8];
    private final boolean[] buttonsHeld = new boolean[8];
    private final boolean[] buttonsReleased = new boolean[8];

    // Mouse
    private float mouseX;
    private float mouseY;
    private float deltaX;
    private float deltaY;

    // Internal \\

    public void write(
            boolean[] keysClicked, boolean[] keysHeld, boolean[] keysReleased,
            boolean[] buttonsClicked, boolean[] buttonsHeld, boolean[] buttonsReleased,
            float mouseX, float mouseY, float deltaX, float deltaY) {

        System.arraycopy(keysClicked, 0, this.keysClicked, 0, this.keysClicked.length);
        System.arraycopy(keysHeld, 0, this.keysHeld, 0, this.keysHeld.length);
        System.arraycopy(keysReleased, 0, this.keysReleased, 0, this.keysReleased.length);
        System.arraycopy(buttonsClicked, 0, this.buttonsClicked, 0, this.buttonsClicked.length);
        System.arraycopy(buttonsHeld, 0, this.buttonsHeld, 0, this.buttonsHeld.length);
        System.arraycopy(buttonsReleased, 0, this.buttonsReleased, 0, this.buttonsReleased.length);

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public void clear() {
        java.util.Arrays.fill(keysClicked, false);
        java.util.Arrays.fill(keysHeld, false);
        java.util.Arrays.fill(keysReleased, false);
        java.util.Arrays.fill(buttonsClicked, false);
        java.util.Arrays.fill(buttonsHeld, false);
        java.util.Arrays.fill(buttonsReleased, false);
        mouseX = mouseY = deltaX = deltaY = 0f;
    }

    // Keys \\

    public boolean isKeyClicked(int key) {
        return key >= 0 && key < keysClicked.length && keysClicked[key];
    }

    public boolean isKeyHeld(int key) {
        return key >= 0 && key < keysHeld.length && keysHeld[key];
    }

    public boolean isKeyReleased(int key) {
        return key >= 0 && key < keysReleased.length && keysReleased[key];
    }

    // Buttons \\

    public boolean isButtonClicked(int btn) {
        return btn >= 0 && btn < buttonsClicked.length && buttonsClicked[btn];
    }

    public boolean isButtonHeld(int btn) {
        return btn >= 0 && btn < buttonsHeld.length && buttonsHeld[btn];
    }

    public boolean isButtonReleased(int btn) {
        return btn >= 0 && btn < buttonsReleased.length && buttonsReleased[btn];
    }

    // Bindings \\

    public boolean isBindingHeld(Binding binding) {
        for (InputCode code : binding.getCodes()) {
            if (code.type == BindingType.KEY && !isKeyHeld(code.code))
                return false;
            if (code.type == BindingType.BUTTON && !isButtonHeld(code.code))
                return false;
        }
        return true;
    }

    public boolean isBindingClicked(Binding binding) {
        InputCode[] codes = binding.getCodes();
        for (int i = 0; i < codes.length - 1; i++) {
            if (codes[i].type == BindingType.KEY && !isKeyHeld(codes[i].code))
                return false;
            if (codes[i].type == BindingType.BUTTON && !isButtonHeld(codes[i].code))
                return false;
        }
        InputCode trigger = codes[codes.length - 1];
        return trigger.type == BindingType.KEY
                ? isKeyClicked(trigger.code)
                : isButtonClicked(trigger.code);
    }

    public boolean isBindingReleased(Binding binding) {
        InputCode[] codes = binding.getCodes();
        for (int i = 0; i < codes.length - 1; i++) {
            if (codes[i].type == BindingType.KEY && !isKeyHeld(codes[i].code))
                return false;
            if (codes[i].type == BindingType.BUTTON && !isButtonHeld(codes[i].code))
                return false;
        }
        InputCode trigger = codes[codes.length - 1];
        return trigger.type == BindingType.KEY
                ? isKeyReleased(trigger.code)
                : isButtonReleased(trigger.code);
    }

    // Mouse \\

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }
}