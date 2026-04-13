package engine.util.input;

public final class InputCode {

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
}