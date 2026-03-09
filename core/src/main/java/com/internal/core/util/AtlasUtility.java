package com.internal.core.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.UtilityPackage;
import com.internal.core.util.atlas.AtlasTileData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Stateless atlas packing utility shared across any pipeline that needs to
 * build a GPU-ready texture atlas from a set of source images.
 *
 * Accepts a flat list of AtlasTileData — the shared base type extended by
 * both TextureTileData and FontTileData — runs MaxRects Best Short Side Fit
 * to find the tightest square power-of-2 canvas, then writes the resulting
 * pixel-space atlas position back onto each tile. Returns the final atlas
 * pixel size for use in UV calculation and GPU upload.
 *
 * Compositing is handled separately by each pipeline's builder since alias
 * layer structure differs between systems.
 */
public class AtlasUtility extends UtilityPackage {

    // Entry Point \\

    public static int pack(ObjectArrayList<? extends AtlasTileData> tiles) {

        if (tiles == null || tiles.isEmpty())
            throwException("[AtlasUtility] Cannot pack an empty tile list");

        ObjectArrayList<? extends AtlasTileData> sorted = sorted(tiles);
        int canvasSize = minCanvasSize(sorted);

        while (!tryPack(sorted, canvasSize))
            canvasSize <<= 1;

        return canvasSize;
    }

    // Sorting \\

    private static ObjectArrayList<AtlasTileData> sorted(
            ObjectArrayList<? extends AtlasTileData> tiles) {
        ObjectArrayList<AtlasTileData> sorted = new ObjectArrayList<>(tiles);
        sorted.sort((a, b) -> {
            if (a.getTileHeight() != b.getTileHeight())
                return Integer.compare(b.getTileHeight(), a.getTileHeight());
            return Integer.compare(b.getTileWidth(), a.getTileWidth());
        });
        return sorted;
    }

    private static int minCanvasSize(ObjectArrayList<? extends AtlasTileData> sorted) {
        int maxSide = 0;
        int totalArea = 0;
        for (int i = 0; i < sorted.size(); i++) {
            AtlasTileData t = sorted.get(i);
            if (t.getTileWidth() > maxSide)
                maxSide = t.getTileWidth();
            if (t.getTileHeight() > maxSide)
                maxSide = t.getTileHeight();
            totalArea += t.getTileWidth() * t.getTileHeight();
        }
        return nextPow2(Math.max(maxSide, (int) Math.ceil(Math.sqrt(totalArea))));
    }

    // Packing \\

    /*
     * Attempts to fit all tiles onto a canvas of the given pixel size using
     * MaxRects BSSF. Writes pixel-space atlas positions directly onto each tile
     * only on full success. Returns false if any tile cannot be placed — caller
     * grows the canvas and retries.
     */
    private static boolean tryPack(
            ObjectArrayList<? extends AtlasTileData> tiles, int canvasSize) {

        List<int[]> free = new ArrayList<>();
        free.add(new int[] { 0, 0, canvasSize, canvasSize });

        int[] px = new int[tiles.size()];
        int[] py = new int[tiles.size()];

        for (int i = 0; i < tiles.size(); i++) {
            AtlasTileData tile = tiles.get(i);
            int[] placement = bestShortSideFit(free, tile.getTileWidth(), tile.getTileHeight());
            if (placement == null)
                return false;
            px[i] = placement[0];
            py[i] = placement[1];
            splitFreeRects(free, placement[0], placement[1], tile.getTileWidth(), tile.getTileHeight());
            pruneContained(free);
        }

        for (int i = 0; i < tiles.size(); i++)
            tiles.get(i).setAtlasPosition(px[i], py[i]);

        return true;
    }

    /*
     * Finds the free rect that wastes the fewest pixels on its shorter leftover
     * side after placing (w, h). Ties broken by the longer side. Returns [x, y]
     * of placement or null if no rect is large enough.
     */
    private static int[] bestShortSideFit(List<int[]> free, int w, int h) {
        int[] best = null;
        int bestShort = Integer.MAX_VALUE;
        int bestLong = Integer.MAX_VALUE;

        for (int i = 0; i < free.size(); i++) {
            int[] r = free.get(i);
            if (r[2] < w || r[3] < h)
                continue;
            int shortWaste = Math.min(r[2] - w, r[3] - h);
            int longWaste = Math.max(r[2] - w, r[3] - h);
            if (shortWaste < bestShort || (shortWaste == bestShort && longWaste < bestLong)) {
                bestShort = shortWaste;
                bestLong = longWaste;
                best = new int[] { r[0], r[1] };
            }
        }

        return best;
    }

    // Free Rect Maintenance \\

    /*
     * Splits every free rect that overlaps the newly placed tile into up to four
     * non-overlapping sub-rects, then discards the original overlapping rect.
     */
    private static void splitFreeRects(List<int[]> free, int px, int py, int pw, int ph) {

        List<int[]> toRemove = new ArrayList<>();
        List<int[]> toAdd = new ArrayList<>();

        for (int i = 0; i < free.size(); i++) {
            int[] r = free.get(i);
            if (!overlaps(r, px, py, pw, ph))
                continue;
            toRemove.add(r);
            if (px > r[0])
                toAdd.add(new int[] { r[0], r[1], px - r[0], r[3] });
            if (px + pw < r[0] + r[2])
                toAdd.add(new int[] { px + pw, r[1], r[0] + r[2] - (px + pw), r[3] });
            if (py > r[1])
                toAdd.add(new int[] { r[0], r[1], r[2], py - r[1] });
            if (py + ph < r[1] + r[3])
                toAdd.add(new int[] { r[0], py + ph, r[2], r[1] + r[3] - (py + ph) });
        }

        free.removeAll(toRemove);
        free.addAll(toAdd);
    }

    /*
     * Removes any free rect fully contained within another. Redundant rects
     * degrade BSSF scoring over time if left in the list.
     */
    private static void pruneContained(List<int[]> rects) {

        List<int[]> toRemove = new ArrayList<>();

        for (int i = 0; i < rects.size(); i++) {
            if (toRemove.contains(rects.get(i)))
                continue;
            for (int j = 0; j < rects.size(); j++) {
                if (i == j || toRemove.contains(rects.get(j)))
                    continue;
                if (contains(rects.get(j), rects.get(i))) {
                    toRemove.add(rects.get(i));
                    break;
                }
            }
        }

        rects.removeAll(toRemove);
    }

    // Geometry Helpers \\

    private static boolean overlaps(int[] r, int px, int py, int pw, int ph) {
        return px < r[0] + r[2]
                && px + pw > r[0]
                && py < r[1] + r[3]
                && py + ph > r[1];
    }

    private static boolean contains(int[] outer, int[] inner) {
        return inner[0] >= outer[0]
                && inner[1] >= outer[1]
                && inner[0] + inner[2] <= outer[0] + outer[2]
                && inner[1] + inner[3] <= outer[1] + outer[3];
    }

    private static int nextPow2(int value) {
        if (value <= 1)
            return 1;
        int p = 1;
        while (p < value)
            p <<= 1;
        return p;
    }
}