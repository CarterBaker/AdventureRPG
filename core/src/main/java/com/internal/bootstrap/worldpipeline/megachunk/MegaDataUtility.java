package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.core.engine.UtilityPackage;

/*
 * All mega data graph traversal lives here.
 *
 * Megas dump in the opposite direction to chunks:
 *   Chunks  — dump when slot becomes MORE distant (level goes up)
 *   Megas   — dump when slot becomes MORE detailed (level goes down)
 *
 * maximumLevel on MegaData is the most detailed level at which that
 * stage must remain present. Below that level (more detail), it dumps.
 * No renderMode checks anywhere — purely level-number driven.
 */
public final class MegaDataUtility extends UtilityPackage {

    // Load \\

    public static MegaData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel) {
        for (MegaData stage : MegaData.VALUES) {
            if (flags[stage.index])
                continue;
            if (!requiresMet(stage, flags))
                continue;
            if (!isNeeded(stage, slotLevel))
                continue;
            return stage;
        }
        return null;
    }

    /*
     * A stage is needed when the slot is at or above its maximumLevel
     * (less detailed than or equal to the maximum).
     * Non-dumpable stages are always needed.
     */
    private static boolean isNeeded(MegaData stage, GridSlotDetailLevel slotLevel) {
        if (!stage.dumpable)
            return true;
        if (stage.maximumLevel == null)
            return false;
        return slotLevel.level >= stage.maximumLevel.level;
    }

    private static boolean requiresMet(MegaData stage, boolean[] flags) {
        for (MegaData req : stage.requires)
            if (!flags[req.index])
                return false;
        return true;
    }

    // Dump \\

    public static MegaData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel) {
        for (int i = MegaData.LENGTH - 1; i >= 0; i--) {
            MegaData stage = MegaData.VALUES[i];
            if (!flags[stage.index])
                continue;
            if (!stage.dumpable)
                continue;
            if (stage.maximumLevel == null)
                continue;
            if (slotLevel.level >= stage.maximumLevel.level)
                continue;
            if (leadsToSafe(stage, flags))
                return stage;
        }
        return null;
    }

    private static boolean leadsToSafe(MegaData stage, boolean[] flags) {
        for (MegaData next : stage.leadsTo) {
            if (!flags[next.index])
                return false;
            if (!leadsToSafe(next, flags))
                return false;
        }
        return true;
    }

    // Cascade Clear \\

    public static void cascadeClear(MegaData stage, boolean[] flags) {
        flags[stage.index] = false;
        for (MegaData next : stage.leadsTo) {
            if (!flags[next.index])
                continue;
            if (!next.dumpable)
                continue;
            cascadeClear(next, flags);
        }
    }
}