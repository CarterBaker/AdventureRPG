package engine.util.mathematics.curves;

public enum CurveEnds {

    /*
     * How an auto key at either end of a curve aims its handles. An open
     * curve runs on along its first and last spans, as a recorded take does;
     * a clamped one eases flat into and out of its ends, as a one-shot clip
     * does; a cyclic one runs round its seam, its last key repeating its
     * first, so a looping clip turns over without a kink.
     */

    OPEN,
    CLAMPED,
    CYCLIC
}
