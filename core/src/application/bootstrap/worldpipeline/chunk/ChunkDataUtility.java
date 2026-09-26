package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import engine.root.EngineUtility;

public final class ChunkDataUtility extends EngineUtility {

    /*
     * Stateless walker over the ChunkData dependency graph. Decides the next
     * stage to load or dump from the slot's detail level, individual render
     * need and mega membership. A stage is needed when its own condition holds
     * or a stage that currently needs it requires it, so loading and dumping
     * never drift apart.
     */

    // Load \\

    public static ChunkData nextToLoad(
            boolean[] flags,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean partOfMegaBlock) {

        for (ChunkData stage : ChunkData.VALUES) {

            if (flags[stage.index])
                continue;

            if (!requiresMet(stage, flags))
                continue;

            if (!isNeeded(stage, slotLevel, needsIndividualRender, partOfMegaBlock))
                continue;

            return stage;
        }

        return null;
    }

    private static boolean isNeeded(
            ChunkData stage,
            GridSlotDetailLevel slotLevel,
            boolean needsIndividualRender,
            boolean partOfMegaBlock) {

        if (isDirectlyRequired(stage, slotLevel, needsIndividualRender, partOfMegaBlock))
            return true;

        for (ChunkData other : ChunkData.VALUES) {

            if (!isDirectlyRequired(other, slotLevel, needsIndividualRender, partOfMegaBlock))
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
            boolean partOfMegaBlock) {

        if (stage == ChunkData.BATCH_DATA)
            return partOfMegaBlock;

        if (stage == ChunkData.RENDER_DATA)
            return needsIndividualRender;

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
            boolean partOfMegaBlock) {

        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {

            ChunkData stage = ChunkData.VALUES[i];

            if (!flags[stage.index])
                continue;

            if (!stage.dumpable)
                continue;

            if (isNeeded(stage, slotLevel, needsIndividualRender, partOfMegaBlock))
                continue;

            return stage;
        }

        return null;
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