package application.bootstrap.worldpipeline.layout;

import engine.root.StructPackage;

public class LayoutRangeStruct extends StructPackage {

    /*
     * An inclusive range of whole numbers a layout draws one value from, such
     * as how many streets leave a settlement or how long each runs.
     */

    // Range
    private final int min;
    private final int max;

    // Constructor \\

    public LayoutRangeStruct(int min, int max) {

        // Range
        this.min = min;
        this.max = max;
    }

    // Accessible \\

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
