package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import engine.root.EngineUtility;

public final class ChunkDataUtility extends EngineUtility {

    /*
     * Stateless graph walker for ChunkData stage transitions. Determines which
     * stage to load or dump next based on the requires/leadsTo dependency graph
     * and two live signals: the slot's current detail level, and — for
     * RENDER_DATA specifically — whether this exact chunk is currently expected
     * to render individually at all, per needsIndividualRender (see
     * ChunkQueueManager.determineQueueOperation). BATCH_DATA is gated on the
     * slot's renderMode rather than a numeric level threshold, since NEAR and
     * DISTANT slots render exclusively through mega batching while IMMEDIATE
     * slots never do.
     */

    // Load \\

    public static ChunkData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel, boolean needsIndividualRender) {

        for (ChunkData stage : ChunkData.VALUES) {

            if (flags[stage.index])
                continue;

            if (!requiresMet(stage, flags))
                continue;

            if (!isNeeded(stage, flags, slotLevel, needsIndividualRender))
                continue;

            return stage;
        }

        return null;
    }

    private static boolean isNeeded(
            ChunkData stage, boolean[] flags, GridSlotDetailLevel slotLevel, boolean needsIndividualRender) {

        if (isDirectlyRequired(stage, slotLevel, needsIndividualRender))
            return true;

        for (ChunkData other : ChunkData.VALUES) {

            if (flags[other.index])
                continue;

            if (!isDirectlyRequired(other, slotLevel, needsIndividualRender))
                continue;

            for (ChunkData req : other.requires)
                if (req == stage)
                    return true;
        }

        return false;
    }

    /*
     * A stage with a numeric minimumLevel is required from IMMEDIATE out
     * through that level, inclusive. BATCH_DATA and RENDER_DATA have no
     * numeric threshold — BATCH_DATA matches GridSlotDetailLevel's own
     * renderMode exactly, and whether THIS chunk needs an individual GPU
     * upload is a live property of the grid's own render queue rather than a
     * function of slot level, so both are checked directly. A stage with
     * neither is never directly required — it can still be pulled in
     * transitively by whatever downstream stage actually depends on it, via
     * the scan in isNeeded().
     */
    private static boolean isDirectlyRequired(
            ChunkData stage, GridSlotDetailLevel slotLevel, boolean needsIndividualRender) {

        if (stage == ChunkData.BATCH_DATA)
            return slotLevel.renderMode == RenderType.BATCHED;

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

    public static ChunkData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel, boolean needsIndividualRender) {

        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {

            ChunkData stage = ChunkData.VALUES[i];

            if (!flags[stage.index])
                continue;

            if (!stage.dumpable)
                continue;

            if (stage == ChunkData.RENDER_DATA) {
                if (needsIndividualRender)
                    continue;
            } else {
                if (stage.minimumLevel == null)
                    continue;
                if (slotLevel.level <= stage.minimumLevel.level)
                    continue;
            }

            if (leadsToSafe(stage, flags))
                return stage;
        }

        return null;
    }

    /*
     * A stage is only safe to dump when every stage in its entire leadsTo
     * chain is present and complete. If anything downstream is missing,
     * this stage must stay — it is still needed to produce that outcome.
     */
    private static boolean leadsToSafe(ChunkData stage, boolean[] flags) {

        for (ChunkData next : stage.leadsTo) {

            if (!flags[next.index])
                return false;

            if (!leadsToSafe(next, flags))
                return false;
        }

        return true;
    }

    // Cascade Clear \\

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