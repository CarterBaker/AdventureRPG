package application.bootstrap.menupipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StripLayoutUtility extends EngineUtility {

    /*
     * Lays out a strip of tabs the way every tab bar in the engine does: each
     * tab is as wide as its whole text at full size while the row has room;
     * as the row narrows the widest shrink first, down together to one shared
     * width, each no narrower than its whole text at the smallest readable
     * size, so every letter stays visible; and once no tab can shrink further
     * the strip wraps onto as many rows as it needs. The editor's dock tab
     * strips and the Hierarchy's tab bar lay out through here; a row of equal
     * cells (the editor's menu bar, the biome palette) takes the equal-width
     * helpers.
     */

    // Strip \\

    // Where the row that starts at the given tab ends, exclusive: as many tabs as fit at their narrowest, never fewer
    // than one
    public static int resolveRowEnd(
            FloatArrayList narrowestWidths,
            int start,
            float availableWidth,
            float spacing) {

        int end = start;
        float used = 0f;

        while (end < narrowestWidths.size()) {

            float narrowest = narrowestWidths.getFloat(end);
            float next = end == start ? narrowest : used + spacing + narrowest;

            if (end > start && next > availableWidth)
                break;

            used = next;
            end++;
        }

        return end;
    }

    // The widest any tab of a row may be: every tab keeps its preferred width while the row has room; past that the
    // widest shrink first, down together to one shared cap, each held at its own narrowest (capWidth)
    public static float resolveRowCap(
            FloatArrayList preferredWidths,
            FloatArrayList narrowestWidths,
            int start,
            int end,
            float availableWidth,
            float spacing) {

        float room = availableWidth - (end - start - 1) * spacing;
        float high = 0f;

        for (int i = start; i < end; i++)
            high = Math.max(high, preferredWidths.getFloat(i));

        if (sumRow(preferredWidths, narrowestWidths, start, end, high) <= room)
            return Float.MAX_VALUE;

        float low = 0f;

        for (int step = 0; step < EngineSetting.STRIP_CAP_SEARCH_STEPS; step++) {

            float cap = (low + high) * 0.5f;

            if (sumRow(preferredWidths, narrowestWidths, start, end, cap) <= room)
                low = cap;
            else
                high = cap;
        }

        return low;
    }

    // A tab's width under its row's cap: its preferred width at most, its narrowest at least
    public static float capWidth(float preferredWidth, float narrowestWidth, float cap) {
        return Math.min(preferredWidth, Math.max(cap, narrowestWidth));
    }

    // Whether rows laid out ending where the given ends are break the same way at the width given; false for none
    public static boolean breaksAt(
            FloatArrayList narrowestWidths,
            IntArrayList rowEnds,
            float availableWidth,
            float spacing) {

        if (rowEnds.isEmpty() || rowEnds.getInt(rowEnds.size() - 1) != narrowestWidths.size())
            return false;

        int start = 0;

        for (int r = 0; r < rowEnds.size(); r++) {

            int end = resolveRowEnd(narrowestWidths, start, availableWidth, spacing);

            if (end != rowEnds.getInt(r))
                return false;

            start = end;
        }

        return true;
    }

    private static float sumRow(
            FloatArrayList preferredWidths,
            FloatArrayList narrowestWidths,
            int start,
            int end,
            float cap) {

        float sum = 0f;

        for (int i = start; i < end; i++)
            sum += capWidth(preferredWidths.getFloat(i), narrowestWidths.getFloat(i), cap);

        return sum;
    }

    // Grid \\

    // How many equal cells one row holds: as many as fit at their narrowest, never more than there are, never fewer
    // than one
    public static int resolvePerRow(float availableWidth, int cellCount, float minWidth, float spacing) {

        int fitting = (int) ((availableWidth + spacing) / (minWidth + spacing));

        return Math.max(1, Math.min(cellCount, fitting));
    }

    // Each cell's width on rows holding perRow cells: its preferred width while they fit, shrunk to share the row
    // otherwise, never below zero
    public static float resolveWidth(float availableWidth, int perRow, float preferredWidth, float spacing) {

        float shared = (availableWidth - (perRow - 1) * spacing) / perRow;

        return Math.max(0f, Math.min(preferredWidth, shared));
    }

    public static int resolveRowCount(int cellCount, int perRow) {
        return Math.max(1, (cellCount + perRow - 1) / perRow);
    }

    // The height of a strip of rows, the spacing between them included
    public static float resolveStripHeight(int rowCount, float rowHeight, float spacing) {
        return rowCount * rowHeight + (rowCount - 1) * spacing;
    }
}
