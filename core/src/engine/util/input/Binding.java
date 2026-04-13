package engine.util.input;

public final class Binding {
    /*
     * Represents a single bindable action. Holds one or more InputCodes that
     * must all be held simultaneously for the binding to be active. For combos
     * the last code is the trigger — all preceding codes are treated as
     * modifiers. Keys and mouse buttons can be freely mixed.
     */
    private InputCode[] codes;

    public Binding(InputCode... codes) {
        this.codes = codes;
    }

    // Accessible \\

    public boolean isClicked(Input input) {
        for (int i = 0; i < codes.length - 1; i++)
            if (!codes[i].isDown(input))
                return false;
        return codes[codes.length - 1].isClicked(input);
    }

    public boolean isDown(Input input) {
        for (InputCode code : codes)
            if (!code.isDown(input))
                return false;
        return true;
    }

    public boolean isReleased(Input input) {
        for (int i = 0; i < codes.length - 1; i++)
            if (!codes[i].isDown(input))
                return false;
        return codes[codes.length - 1].isReleased(input);
    }

    public void set(InputCode... codes) {
        if (codes == null)
            return;
        this.codes = codes;
    }

    public InputCode[] getCodes() {
        return codes;
    }
}