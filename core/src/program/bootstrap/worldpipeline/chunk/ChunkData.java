package program.bootstrap.worldpipeline.chunk;

import program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;

public enum ChunkData {

        LOAD_DATA(
                        false, null,
                        new String[] {},
                        new String[] { "ESSENTIAL_DATA" }),
        ESSENTIAL_DATA(
                        false, null,
                        new String[] { "LOAD_DATA" },
                        new String[] { "GENERATION_DATA" }),
        GENERATION_DATA(
                        true, GridSlotDetailLevel.NEAR,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA" },
                        new String[] { "NEIGHBOR_DATA", "ITEM_DATA" }),
        NEIGHBOR_DATA(
                        false, null,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA" },
                        new String[] { "BUILD_DATA" }),
        BUILD_DATA(
                        true, GridSlotDetailLevel.NEAR,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA", "NEIGHBOR_DATA" },
                        new String[] { "MERGE_DATA" }),
        MERGE_DATA(
                        true, GridSlotDetailLevel.IMMEDIATE,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA", "NEIGHBOR_DATA",
                                        "BUILD_DATA" },
                        new String[] { "RENDER_DATA" }),
        RENDER_DATA(
                        true, GridSlotDetailLevel.IMMEDIATE,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA", "NEIGHBOR_DATA", "BUILD_DATA",
                                        "MERGE_DATA" },
                        new String[] { "BATCH_DATA" }),
        BATCH_DATA(
                        false, null,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA", "NEIGHBOR_DATA", "BUILD_DATA",
                                        "MERGE_DATA", "RENDER_DATA" },
                        new String[] {}),
        ITEM_DATA(
                        true, GridSlotDetailLevel.NEAR,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA" },
                        new String[] { "ITEM_RENDER_DATA" }),
        ITEM_RENDER_DATA(
                        true, GridSlotDetailLevel.IMMEDIATE,
                        new String[] { "LOAD_DATA", "ESSENTIAL_DATA", "GENERATION_DATA", "ITEM_DATA" },
                        new String[] {});

        public final int index;
        public final boolean dumpable;
        public final GridSlotDetailLevel minimumLevel;
        public ChunkData[] requires;
        public ChunkData[] leadsTo;

        private final String[] requiresNames;
        private final String[] leadsToNames;

        public static final ChunkData[] VALUES = values();
        public static final int LENGTH = VALUES.length;

        static {
                for (ChunkData stage : VALUES)
                        stage.link();
        }

        ChunkData(
                        boolean dumpable,
                        GridSlotDetailLevel minimumLevel,
                        String[] requiresNames,
                        String[] leadsToNames) {
                this.index = this.ordinal();
                this.dumpable = dumpable;
                this.minimumLevel = minimumLevel;
                this.requiresNames = requiresNames;
                this.leadsToNames = leadsToNames;
        }

        private void link() {
                this.requires = new ChunkData[requiresNames.length];
                for (int i = 0; i < requiresNames.length; i++)
                        this.requires[i] = ChunkData.valueOf(requiresNames[i]);
                this.leadsTo = new ChunkData[leadsToNames.length];
                for (int i = 0; i < leadsToNames.length; i++)
                        this.leadsTo[i] = ChunkData.valueOf(leadsToNames[i]);
        }
}