package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeLodUtility extends EngineUtility {

    /*
     * The LOD ladder every tree is drawn by, whichever chunk, mega or macro
     * tile draws it: its own mesh levels, from the full tree on, then one
     * last stage, its species' impostor, from the shared level on. A tree's
     * LOD is its mesh level, or TREE_LOD_IMPOSTOR at that last stage, which
     * falls back to the coarsest mesh level while its species has no impostor
     * or something altered the tree. resolveLevel() is the one place a
     * distance picks a mesh level, holding a tree on its level a margin past
     * either end so a player pacing along a line never has the trees on it
     * redrawn back and forth. The girth a level hides below grows with the
     * distance it starts at, as a limb's width on screen shrinks with
     * distance. writeImpostorInstance() is the one place a tree's impostor
     * instance is laid out, for registered trees and distant terrain's sites
     * alike.
     */

    // Code \\

    // The LOD a tree at a mesh level stands at: the impostor stage from the shared level on
    public static int resolveLod(int level, int sharedLevel) {
        return level >= sharedLevel ? EngineSetting.TREE_LOD_IMPOSTOR : level;
    }

    // The mesh level a LOD draws its tree at: its own, or the coarsest at the impostor stage
    public static int toLevel(int lod) {
        return lod == EngineSetting.TREE_LOD_IMPOSTOR ? EngineSetting.TREE_LOD_LEVEL_COUNT - 1 : lod;
    }

    public static boolean isImpostorStage(int lod) {
        return lod == EngineSetting.TREE_LOD_IMPOSTOR;
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

    // Impostor \\

    // True while a tree at a LOD is drawn as its species' impostor: at the impostor stage, nothing having altered
    // it, once its species has one baked
    public static boolean isImpostor(TreeHandle treeHandle, int lod, boolean altered) {
        return isImpostorStage(lod) && !altered && treeHandle.getImpostor() != null;
    }

    // One tree's impostor instance written at an offset: its root chunk as raw bits and its place in it, the height
    // its framing sphere's centre stands at, that sphere's radius, its turn and its species' layer — false, nothing
    // written, while its species has no impostor
    public static boolean writeImpostorInstance(
            TreeHandle treeHandle,
            long seed,
            float matureHeight,
            float age,
            long anchorX,
            long anchorZ,
            float baseY,
            float[] out,
            int offset) {

        TreeImpostorStruct impostor = treeHandle.getImpostor();

        if (impostor == null)
            return false;

        int chunkSize = EngineSetting.CHUNK_SIZE;
        int chunkX = (int) Math.floorDiv(anchorX, chunkSize);
        int chunkZ = (int) Math.floorDiv(anchorZ, chunkSize);
        float scale = matureHeight * TreeShapeUtility.resolveScale(matureHeight, age) / impostor.getHeight();

        out[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_X_FLOAT] = Float.intBitsToFloat(chunkX);
        out[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_Z_FLOAT] = Float.intBitsToFloat(chunkZ);
        out[offset + EngineSetting.TREE_IMPOSTOR_LOCAL_X_FLOAT] =
                (float) (anchorX - (long) chunkX * chunkSize) + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        out[offset + EngineSetting.TREE_IMPOSTOR_LOCAL_Z_FLOAT] =
                (float) (anchorZ - (long) chunkZ * chunkSize) + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        out[offset + EngineSetting.TREE_IMPOSTOR_CENTER_Y_FLOAT] = baseY + impostor.getCenterY() * scale;
        out[offset + EngineSetting.TREE_IMPOSTOR_RADIUS_FLOAT] = impostor.getRadius() * scale;
        out[offset + EngineSetting.TREE_IMPOSTOR_TURN_FLOAT] = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                seed ^ EngineSetting.TREE_IMPOSTOR_TURN_SALT, 0, 0)) * EngineSetting.TREE_IMPOSTOR_FULL_TURN;
        out[offset + EngineSetting.TREE_IMPOSTOR_LAYER_FLOAT] = treeHandle.getTreeID();

        return true;
    }

    // The seed a species' shared tree grows from: its LOD seed salted by its name, so species never share a tree
    public static long resolveSharedSeed(TreeHandle treeHandle) {
        return ((long) treeHandle.getNameSeed() << Integer.SIZE ^ treeHandle.getLodSeed())
                ^ EngineSetting.TREE_LOD_SHARED_SEED_SALT;
    }
}
