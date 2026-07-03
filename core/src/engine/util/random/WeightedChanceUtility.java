package engine.util.random;

import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeightedChanceUtility extends EngineUtility {

    // Internal \\

    private WeightedChanceUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Chance Weighted Selection \\

    /*
     * Sums the positive chance weight of every entry in the list. Entries
     * with a zero or negative chance contribute nothing and are treated as
     * unreachable, but are never removed from the list itself.
     */
    public static <T extends ChanceWeighted> float totalChance(ObjectArrayList<T> entries) {

        float total = 0f;

        for (int i = 0; i < entries.size(); i++)
            total += Math.max(0f, entries.get(i).getChance());

        return total;
    }

    /*
     * Picks a single entry from the list using a flat noise value in
     * [0, 1). Entries with a larger chance occupy a proportionally larger
     * band of the noise range. Falls back to the first entry if every
     * chance is zero or negative — never returns null for a non-empty list.
     */
    public static <T extends ChanceWeighted> T pickWeighted(ObjectArrayList<T> entries, float noise01) {

        if (entries.isEmpty())
            return throwException("Cannot pick a weighted entry from an empty list");

        float total = totalChance(entries);

        if (total <= 0f)
            return entries.get(0);

        float target = clamp01(noise01) * total;
        float cumulative = 0f;

        for (int i = 0; i < entries.size(); i++) {

            cumulative += Math.max(0f, entries.get(i).getChance());

            if (target <= cumulative)
                return entries.get(i);
        }

        return entries.get(entries.size() - 1);
    }

    // Utility \\

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}