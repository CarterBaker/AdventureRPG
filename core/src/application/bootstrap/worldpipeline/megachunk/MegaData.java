package application.bootstrap.worldpipeline.megachunk;

import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;

public enum MegaData {

    /*
     * The stages a mega chunk moves through, batching then rendering, with the
     * same requires, leads-to and detail level rules as ChunkData.
     */

    BATCH_DATA(
            false, null,
            new String[] {},
            new String[] { "RENDER_DATA" }),
    RENDER_DATA(
            true, GridSlotDetailLevel.NEAR,
            new String[] { "BATCH_DATA" },
            new String[] {});

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