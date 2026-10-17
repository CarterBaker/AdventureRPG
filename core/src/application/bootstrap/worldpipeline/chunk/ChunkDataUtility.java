package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import engine.root.EngineUtility;

public final class ChunkDataUtility extends EngineUtility {

    /*
     * Stateless walker over the ChunkData dependency graph. Decides the next
     * stage to load or dump from the slot's detail level, individual render
     * need, whether the chunk's own trees are drawn and mega membership. A
     * stage is needed when its own condition holds or a stage that currently
     * needs it requires it, so loading and dumping never drift apart. A
     * chunk's own trees are drawn while the grid draws the chunk on its own,
     * and kept on past that only until its mega's stand-ins are on the GPU.
     * Every stage's need is resolved once per combination of conditions into
     * a table, so the streaming sweep reads a decision instead of walking the
     * graph for every chunk it visits.
     */

    // Need Table — condition index → stage index → needed
    private static final boolean[][] NEEDED = buildNeedTable();

    // Load \\

    public static ChunkData nextToLoad(
            boolean[] flags,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        boolean[] needed = NEEDED[toConditionIndex(
                slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock)];

        for (ChunkData stage : ChunkData.VALUES) {

            if (flags[stage.index])
                continue;

            if (!needed[stage.index])
                continue;

            if (!requiresMet(stage, flags))
                continue;

            return stage;
        }

        return null;
    }

    private static boolean isNeeded(
            ChunkData stage,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        if (isDirectlyRequired(stage, slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock))
            return true;

        for (ChunkData other : ChunkData.VALUES) {

            if (!isDirectlyRequired(other, slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock))
                continue;

            for (ChunkData req : other.requires)
                if (req == stage)
                    return true;
        }

        return false;
    }

    private static boolean isDirectlyRequired(
            ChunkData stage,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        if (stage == ChunkData.BATCH_DATA)
            return partOfMegaBlock;

        if (stage == ChunkData.RENDER_DATA)
            return needsIndividualRender;

        if (stage == ChunkData.TREE_RENDER_DATA)
            return treesDrawnIndividually;

        if (stage.minimumLevel == null)
            return false;

        return slotLevel.level <= stage.minimumLevel.level;
    }

    private static boolean requiresMet(ChunkData stage, boolean[] flags) {

        for (ChunkData req : stage.requires)
            if (!flags[req.index])
                return false;

        return true;
    }

    // Dump \\

    public static ChunkData nextToDump(
            boolean[] flags,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        boolean[] needed = NEEDED[toConditionIndex(
                slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock)];

        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {

            ChunkData stage = ChunkData.VALUES[i];

            if (!flags[stage.index])
                continue;

            if (!stage.dumpable)
                continue;

            if (needed[stage.index])
                continue;

            return stage;
        }

        return null;
    }

    // Need Table \\

    private static boolean[][] buildNeedTable() {

        GridSlotDetailLevel[] levels = GridSlotDetailLevel.values();
        boolean[] states = { false, true };
        boolean[][] table = new boolean[toConditionIndex(levels[levels.length - 1], true, true, true) + 1][];

        for (GridSlotDetailLevel slotLevel : levels)
            for (boolean needsIndividualRender : states)
                for (boolean treesDrawnIndividually : states)
                    for (boolean partOfMegaBlock : states) {

                        int conditionIndex = toConditionIndex(
                                slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock);

                        table[conditionIndex] = resolveNeeds(
                                slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock);
                    }

        return table;
    }

    private static boolean[] resolveNeeds(
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        boolean[] needs = new boolean[ChunkData.LENGTH];

        for (ChunkData stage : ChunkData.VALUES)
            needs[stage.index] = isNeeded(
                    stage, slotLevel, needsIndividualRender, treesDrawnIndividually, partOfMegaBlock);

        return needs;
    }

    private static int toConditionIndex(
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean treesDrawnIndividually,
            boolean partOfMegaBlock) {

        int index = slotLevel.ordinal();
        index = (index << 1) | (needsIndividualRender ? 1 : 0);
        index = (index << 1) | (treesDrawnIndividually ? 1 : 0);
        index = (index << 1) | (partOfMegaBlock ? 1 : 0);

        return index;
    }

    // Forced Invalidation \\

    public static void cascadeClear(ChunkData stage, boolean[] flags) {

        flags[stage.index] = false;

        for (ChunkData next : stage.leadsTo) {

            if (!flags[next.index])
                continue;

            if (!next.dumpable)
                continue;

            cascadeClear(next, flags);
        }
    }
}