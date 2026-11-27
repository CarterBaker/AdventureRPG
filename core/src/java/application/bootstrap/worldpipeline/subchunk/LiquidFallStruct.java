package application.bootstrap.worldpipeline.subchunk;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class LiquidFallStruct extends StructPackage {

    /*
     * The feet of the falls one subchunk's last geometry build found, each one word LiquidFlowUtility packs. The
     * builder clears and appends under the chunk's lock while the particle systems read on the main thread, so a foot
     * is written before the count that publishes it and a reader takes only what the count covers; a foot read while
     * a rebuild runs is at worst one this build or the last found. Storage grows only past the most this subchunk has
     * held and is released with the subchunk.
     */

    private static final int[] EMPTY = new int[0];

    // Feet
    private int[] feet;
    private volatile int count;

    // Constructor \\

    public LiquidFallStruct() {
        this.feet = EMPTY;
    }

    // Management \\

    public void clear() {
        count = 0;
    }

    public void add(int foot) {

        int index = count;

        if (index == feet.length)
            feet = Arrays.copyOf(feet, Math.max(EngineSetting.LIQUID_FALL_FEET_INITIAL, index * 2));

        feet[index] = foot;
        count = index + 1;
    }

    public void release() {
        count = 0;
        feet = EMPTY;
    }

    // Accessible \\

    public int getCount() {
        return count;
    }

    // A foot the count covers, or LIQUID_FALL_FOOT_NONE where a release has let its storage go since the count was read
    public int getFoot(int index) {

        int[] current = feet;

        return index < current.length ? current[index] : EngineSetting.LIQUID_FALL_FOOT_NONE;
    }
}
