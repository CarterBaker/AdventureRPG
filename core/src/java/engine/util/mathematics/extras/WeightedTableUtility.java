package engine.util.mathematics.extras;

import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class WeightedTableUtility extends EngineUtility {

    /*
     * Stateless weighted draws. A table is its entries' running weight totals
     * scaled to end at one, so a single roll from 0 up to 1 picks an entry with
     * a chance in proportion to its weight, the same entry for the same roll
     * on every thread. Every weighted list the engine draws from, furniture,
     * structures and settlements alike, is built and drawn through here.
     */

    // Build \\

    // Running totals of the weights, scaled so the last is exactly one
    public static float[] buildCumulative(FloatArrayList weights, String ownerName) {

        if (weights.isEmpty())
            throwException("Weighted table \"" + ownerName + "\" lists no entries.");

        float total = 0f;

        for (int i = 0; i < weights.size(); i++) {

            float weight = weights.getFloat(i);

            if (weight <= 0f)
                throwException("Weighted table \"" + ownerName + "\" entry " + i + " has weight " + weight
                        + " — every weight must be greater than 0.");

            total += weight;
        }

        float[] cumulative = new float[weights.size()];
        float running = 0f;

        for (int i = 0; i < weights.size(); i++) {
            running += weights.getFloat(i);
            cumulative[i] = running / total;
        }

        cumulative[cumulative.length - 1] = 1f;

        return cumulative;
    }

    // Draw \\

    // The entry a roll from 0 up to 1 lands on
    public static int pick(float[] cumulative, float roll) {

        int low = 0;
        int high = cumulative.length - 1;

        while (low < high) {

            int middle = (low + high) >>> 1;

            if (roll < cumulative[middle])
                high = middle;
            else
                low = middle + 1;
        }

        return low;
    }
}
