package application.bootstrap.menupipeline.util;

import engine.root.EngineUtility;

public class StripLayoutUtility extends EngineUtility {

    /*
     * Lays out a strip of tabs the way every tab bar in the engine does: each
     * tab keeps its preferred width while the row has room, all of them
     * shrink together as the row narrows, and once none can shrink past its
     * narrowest the strip wraps onto as many rows as it needs, so every tab
     * stays visible and its text readable. The editor's dock tab strips and
     * the Hierarchy's tab bar both lay out through here.
     */

    // Layout \\

    // How many tabs one row holds: as many as fit at their narrowest, never more than there are, never fewer than one
    public static int resolvePerRow(float availableWidth, int tabCount, float minWidth, float spacing) {

        int fitting = (int) ((availableWidth + spacing) / (minWidth + spacing));

        return Math.max(1, Math.min(tabCount, fitting));
    }

    // Each tab's width on rows holding perRow tabs: its preferred width while they fit, shrunk to share the row
    // otherwise, never below zero
    public static float resolveWidth(float availableWidth, int perRow, float preferredWidth, float spacing) {

        float shared = (availableWidth - (perRow - 1) * spacing) / perRow;

        return Math.max(0f, Math.min(preferredWidth, shared));
    }

    public static int resolveRowCount(int tabCount, int perRow) {
        return Math.max(1, (tabCount + perRow - 1) / perRow);
    }

    // The height of a strip of rows, the spacing between them included
    public static float resolveStripHeight(int rowCount, float rowHeight, float spacing) {
        return rowCount * rowHeight + (rowCount - 1) * spacing;
    }
}
