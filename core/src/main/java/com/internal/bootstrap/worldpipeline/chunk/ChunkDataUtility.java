package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.core.engine.UtilityPackage;

public final class ChunkDataUtility extends UtilityPackage {

    // Load \\

    public static ChunkData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel) {
        for (ChunkData stage : ChunkData.VALUES) {
            if (flags[stage.index])
                continue;
            if (!requiresMet(stage, flags))
                continue;
            if (!isNeeded(stage, flags, slotLevel))
                continue;
            return stage;
        }
        return null;
    }

    private static boolean isNeeded(ChunkData stage, boolean[] flags, GridSlotDetailLevel slotLevel) {
        if (isDirectlyRequired(stage, slotLevel))
            return true;
        for (ChunkData other : ChunkData.VALUES) {
            if (flags[other.index])
                continue;
            if (!isDirectlyRequired(other, slotLevel))
                continue;
            for (ChunkData req : other.requires)
                if (req == stage)
                    return true;
        }
        return false;
    }

    private static boolean isDirectlyRequired(ChunkData stage, GridSlotDetailLevel slotLevel) {
        if (!stage.dumpable)
            return true;
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

    public static ChunkData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel) {
        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {
            ChunkData stage = ChunkData.VALUES[i];
            if (!flags[stage.index])
                continue;
            if (!stage.dumpable)
                continue;
            if (stage.minimumLevel == null)
                continue;
            if (slotLevel.level <= stage.minimumLevel.level)
                continue;
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