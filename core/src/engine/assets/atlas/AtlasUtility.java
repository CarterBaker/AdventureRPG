package engine.assets.atlas;

import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AtlasUtility extends EngineUtility {

    /*
     * Stateless atlas packing. Runs MaxRects best short side fit over
     * AtlasTileData to find the smallest square power-of-two canvas, writes
     * each tile's position back, and returns the atlas size. Compositing stays
     * with each pipeline's builder.
     */

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

    private static boolean tryPack(
            ObjectArrayList<? extends AtlasTileData> tiles, int canvasSize) {

        ObjectArrayList<int[]> free = new ObjectArrayList<>();
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

    private static int[] bestShortSideFit(ObjectArrayList<int[]> free, int w, int h) {
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

    private static void splitFreeRects(ObjectArrayList<int[]> free, int px, int py, int pw, int ph) {

        ObjectArrayList<int[]> toRemove = new ObjectArrayList<>();
        ObjectArrayList<int[]> toAdd = new ObjectArrayList<>();

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

    private static void pruneContained(ObjectArrayList<int[]> rects) {

        ObjectArrayList<int[]> toRemove = new ObjectArrayList<>();

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