package engine.util.mathematics.extras;

public class SeasonBlendResultStruct {

    /*
     * Output of SeasonBlendUtility.resolve() — which two keyframe slots
     * `t` currently sits between, and how far across the eased blend
     * between them. Reused across calls by its owner rather than
     * reallocated.
     */

    private int prevIndex;
    private int nextIndex;
    private double easedT;

    public void set(int prevIndex, int nextIndex, double easedT) {
        this.prevIndex = prevIndex;
        this.nextIndex = nextIndex;
        this.easedT = easedT;
    }

    public int getPrevIndex() {
        return prevIndex;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public double getEasedT() {
        return easedT;
    }
}