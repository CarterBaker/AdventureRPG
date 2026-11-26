package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeLodUtility extends EngineUtility {

    /*
     * The LOD ladder every tree is drawn by, whichever chunk, mega or macro
     * tile draws it. A tree's LOD is one code: its level, plus the level
     * count when that level draws its species' shared model rather than its
     * own. resolveLevel() is the one place a distance picks a coarse level,
     * holding a tree on its level a margin past either end so a player
     * pacing along a line never has the trees on it redrawn back and forth.
     * The girth a level hides below grows with the distance it starts at, as
     * a limb's width on screen shrinks with distance.
     */

    // Code \\

    public static int encode(int level, boolean shared) {
        return shared && level != EngineSetting.TREE_LOD_FULL ? level + EngineSetting.TREE_LOD_LEVEL_COUNT : level;
    }

    public static int toLevel(int lod) {
        return lod % EngineSetting.TREE_LOD_LEVEL_COUNT;
    }

    public static boolean isShared(int lod) {
        return lod >= EngineSetting.TREE_LOD_LEVEL_COUNT;
    }

    // Level \\

    // The coarse level a tree a distance from the grid stands at, kept at its current level within the margin
    public static int resolveLevel(float distanceBlocks, int currentLevel) {

        if (currentLevel > EngineSetting.TREE_LOD_FULL) {

            float low = EngineSetting.TREE_LOD_START_BLOCKS[currentLevel] - EngineSetting.TREE_LOD_KEEP_BLOCKS;
            float high = currentLevel + 1 < EngineSetting.TREE_LOD_LEVEL_COUNT
                    ? EngineSetting.TREE_LOD_START_BLOCKS[currentLevel + 1] + EngineSetting.TREE_LOD_KEEP_BLOCKS
                    : Float.MAX_VALUE;

            if (distanceBlocks >= low && distanceBlocks < high)
                return currentLevel;
        }

        int level = EngineSetting.TREE_LOD_FULL + 1;

        while (level + 1 < EngineSetting.TREE_LOD_LEVEL_COUNT
                && distanceBlocks >= EngineSetting.TREE_LOD_START_BLOCKS[level + 1])
            level++;

        return level;
    }

    // The girth below which a level leaves a limb out, unless it carries clusters too far to float
    public static float resolveGirth(int level) {
        return Math.max(
                EngineSetting.TREE_LOD_MIN_GIRTH_BLOCKS,
                EngineSetting.TREE_LOD_START_BLOCKS[level] * EngineSetting.TREE_LOD_GIRTH_PER_BLOCK);
    }

    // Shared \\

    // The seed a species' shared model grows from: its LOD seed salted by its name, so species never share a tree
    public static long resolveSharedSeed(TreeHandle treeHandle) {
        return ((long) treeHandle.getNameSeed() << Integer.SIZE ^ treeHandle.getLodSeed())
                ^ EngineSetting.TREE_LOD_SHARED_SEED_SALT;
    }
}
