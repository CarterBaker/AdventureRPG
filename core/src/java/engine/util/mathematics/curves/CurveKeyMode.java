package engine.util.mathematics.curves;

public enum CurveKeyMode {

    /*
     * How a curve key's handles stand. Auto aims both along the curve's own
     * slope through the key, flat at a peak or a trough, so the curve passes
     * through smoothly and never overshoots its neighbours; aligned keeps
     * both on one line as either is dragged, so the curve bends but stays
     * smooth through the key; free lets each go its own way for a sharp
     * turn; linear aims each straight at its neighbour; constant holds the
     * key's value until the next key. Dragging the handle of an auto, linear
     * or constant key makes it aligned. Files name a mode by its lower-case
     * constant name.
     */

    AUTO,
    ALIGNED,
    FREE,
    LINEAR,
    CONSTANT;

    // Values
    public static final CurveKeyMode[] VALUES = values();

    // Accessible \\

    // True for a key whose handles the curve sets itself
    public boolean isResolved() {
        return this == AUTO || this == LINEAR || this == CONSTANT;
    }

    // True for a key whose handles a file must carry, since nothing else sets them
    public boolean holdsHandles() {
        return !isResolved();
    }
}
