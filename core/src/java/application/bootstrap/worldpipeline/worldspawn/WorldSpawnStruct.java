package application.bootstrap.worldpipeline.worldspawn;

import engine.root.StructPackage;

public class WorldSpawnStruct extends StructPackage {

    /*
     * Persistent record of one spot in a subchunk an NPC lives at: the
     * chunk-local block it stands in, its height in the world, and the NPC
     * that comes into the world there whenever the chunk's items do.
     */

    // Position
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    // NPC
    private final short npcID;

    // Constructor \\

    public WorldSpawnStruct(int blockX, int blockY, int blockZ, short npcID) {

        // Position
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;

        // NPC
        this.npcID = npcID;
    }

    // Accessible \\

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public short getNpcID() {
        return npcID;
    }
}
