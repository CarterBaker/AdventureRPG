package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;

public enum MegaData {

    BATCH_DATA(
            false, null,
            new String[] {},
            new String[] { "RENDER_DATA" }),
    RENDER_DATA(
            true, GridSlotDetailLevel.NEAR,
            new String[] { "BATCH_DATA" },
            new String[] {});

    /*
     * BATCH_DATA — never dumps automatically. Marks that chunks are registered
     * in this mega. Cleared per-chunk by MegaDumpBranch when RENDER_DATA dumps
     * and by invalidation when a block changes. Forces re-contribution when the
     * mega next enters NEAR range.
     *
     * RENDER_DATA — dumps at IMMEDIATE so the mega goes dormant and chunks
     * render individually at close range. Rebuilt when the slot returns to NEAR
     * and all chunks re-contribute.
     *
     * maximumLevel — the most detailed level at which this stage must remain.
     * Dump when slotLevel.level < maximumLevel.level (slot became more detailed).
     * null = never dump automatically.
     */

    public final int index;
    public final boolean dumpable;
    public final GridSlotDetailLevel maximumLevel;
    public MegaData[] requires;
    public MegaData[] leadsTo;

    private final String[] requiresNames;
    private final String[] leadsToNames;

    public static final MegaData[] VALUES = values();
    public static final int LENGTH = VALUES.length;

    static {
        for (MegaData stage : VALUES)
            stage.link();
    }

    MegaData(
            boolean dumpable,
            GridSlotDetailLevel maximumLevel,
            String[] requiresNames,
            String[] leadsToNames) {
        this.index = this.ordinal();
        this.dumpable = dumpable;
        this.maximumLevel = maximumLevel;
        this.requiresNames = requiresNames;
        this.leadsToNames = leadsToNames;
    }

    private void link() {
        this.requires = new MegaData[requiresNames.length];
        for (int i = 0; i < requiresNames.length; i++)
            this.requires[i] = MegaData.valueOf(requiresNames[i]);
        this.leadsTo = new MegaData[leadsToNames.length];
        for (int i = 0; i < leadsToNames.length; i++)
            this.leadsTo[i] = MegaData.valueOf(leadsToNames[i]);
    }
}