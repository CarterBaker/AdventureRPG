package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructureSpawnStruct extends StructPackage {

    /*
     * One spot a structure is lived in at: the NPC that may live there, by
     * name, resolved ID and the name seed its roll is salted with, the block
     * it stands in measured from the structure's origin, and the chance the
     * spot is lived in at all.
     */

    // NPC
    private final String npcName;
    private final short npcID;
    private final int nameSeed;

    // Placement
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    // Chance
    private final float chance;

    // Constructor \\

    public StructureSpawnStruct(
            String npcName,
            short npcID,
            int nameSeed,
            int offsetX,
            int offsetY,
            int offsetZ,
            float chance) {

        // NPC
        this.npcName = npcName;
        this.npcID = npcID;
        this.nameSeed = nameSeed;

        // Placement
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;

        // Chance
        this.chance = chance;
    }

    // Accessible \\

    public String getNpcName() {
        return npcName;
    }

    public short getNpcID() {
        return npcID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public float getChance() {
        return chance;
    }
}
