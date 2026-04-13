package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.core.engine.EngineUtility;

public final class MegaDataUtility extends EngineUtility {

    /*
     * Stateless graph walker for MegaData stage transitions. Megas dump in the
     * opposite direction to chunks — when a slot becomes MORE detailed (level
     * goes down) rather than more distant. maximumLevel on MegaData is the most
     * detailed level at which that stage must remain present. Below that level
     * it dumps. No render mode checks — purely level-number driven.
     */

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
     * (less detailed than or equal to the maximum). Non-dumpable stages
     * are always needed.
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