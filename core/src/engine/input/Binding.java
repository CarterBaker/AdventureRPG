package engine.input;

public final class Binding {

    /*
     * Represents a single bindable action. Holds one or more keys that must
     * all be held simultaneously for the binding to register as active.
     * Single-key bindings are just a one-element array.
     */

    private int[] keys;

    public Binding(int... keys) {
        this.keys = keys;
    }

    public boolean isDown(Input input) {
        for (int key : keys)
            if (!input.isKeyDown(key))
                return false;
        return true;
    }

    public boolean isMouseDown(Input input) {
        for (int button : keys)
            if (!input.isMouseDown(button))
                return false;
        return true;
    }

    public void set(int... keys) {
        this.keys = keys;
    }

    public int[] getKeys() {
        return keys;
    }
}