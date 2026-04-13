package engine.util.input;

public final class Binding {

    /*
     * Represents a single bindable action. Holds one or more codes that must
     * all be held simultaneously for the binding to be active. For combos the
     * last code is the trigger — all preceding codes are treated as modifiers.
     */

    private int[] codes;

    public Binding(int... codes) {
        this.codes = codes;
    }

    // Accessible \\

    public boolean isClicked(Input input) {

        for (int i = 0; i < codes.length - 1; i++)
            if (!input.isKeyDown(codes[i]))
                return false;

        return input.isKeyClicked(codes[codes.length - 1]);
    }

    public boolean isDown(Input input) {

        for (int code : codes)
            if (!input.isKeyDown(code))
                return false;

        return true;
    }

    public boolean isReleased(Input input) {

        for (int i = 0; i < codes.length - 1; i++)
            if (!input.isKeyDown(codes[i]))
                return false;

        return input.isKeyReleased(codes[codes.length - 1]);
    }

    public boolean isMouseClicked(Input input) {

        for (int code : codes)
            if (!input.isMouseClicked(code))
                return false;

        return true;
    }

    public boolean isMouseDown(Input input) {

        for (int code : codes)
            if (!input.isMouseDown(code))
                return false;

        return true;
    }

    public boolean isMouseReleased(Input input) {

        for (int code : codes)
            if (!input.isMouseReleased(code))
                return false;

        return true;
    }

    public void set(int... codes) {

        if (codes == null)
            return;

        this.codes = codes;
    }

    public int[] getCodes() {
        return codes;
    }
}