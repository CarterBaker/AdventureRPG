package engine.input;

public final class Binding {

    /*
     * Represents a single bindable action. Holds one or more codes that must
     * all be held simultaneously for the binding to register as active.
     * Single-key bindings are just a one-element array.
     */

    private int[] codes;

    public Binding(int... codes) {
        this.codes = codes;
    }

    public boolean isDown(Input input) {
        for (int code : codes)
            if (!input.isKeyDown(code))
                return false;
        return true;
    }

    public boolean isMouseDown(Input input) {
        for (int code : codes)
            if (!input.isMouseDown(code))
                return false;
        return true;
    }

    public void set(int... codes) {
        this.codes = codes;
    }

    public int[] getCodes() {
        return codes;
    }
}