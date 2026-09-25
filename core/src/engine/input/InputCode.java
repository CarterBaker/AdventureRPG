package engine.input;

import engine.root.EngineSetting;

public final class InputCode {

    /*
     * One key or mouse button a Binding listens to. Settings store a code as a
     * single int — keys as their own code, mouse buttons offset past every key
     * code — so a binding keeps its key or button type across a save and load.
     */

    public final int code;
    public final BindingType type;

    private InputCode(int code, BindingType type) {
        this.code = code;
        this.type = type;
    }

    public static InputCode key(int code) {
        return new InputCode(code, BindingType.KEY);
    }

    public static InputCode mouse(int code) {
        return new InputCode(code, BindingType.BUTTON);
    }

    // Dispatch \\

    public boolean isDown(Input input) {
        return type == BindingType.KEY ? input.isKeyDown(code) : input.isMouseDown(code);
    }

    public boolean isClicked(Input input) {
        return type == BindingType.KEY ? input.isKeyClicked(code) : input.isMouseClicked(code);
    }

    public boolean isReleased(Input input) {
        return type == BindingType.KEY ? input.isKeyReleased(code) : input.isMouseReleased(code);
    }

    // Storage \\

    public int toStoredCode() {
        return type == BindingType.KEY ? code : code + EngineSetting.BINDING_MOUSE_CODE_OFFSET;
    }

    public static InputCode fromStoredCode(int storedCode) {
        return storedCode >= EngineSetting.BINDING_MOUSE_CODE_OFFSET
                ? mouse(storedCode - EngineSetting.BINDING_MOUSE_CODE_OFFSET)
                : key(storedCode);
    }

    public static int storedMouseCode(int button) {
        return button + EngineSetting.BINDING_MOUSE_CODE_OFFSET;
    }

    // Accessible \\

    public boolean matches(InputCode other) {
        return other != null && code == other.code && type == other.type;
    }
}
