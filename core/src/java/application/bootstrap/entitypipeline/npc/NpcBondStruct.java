package application.bootstrap.entitypipeline.npc;

import engine.root.StructPackage;

public class NpcBondStruct extends StructPackage {

    /*
     * Immutable rule of an NPC definition binding its residents to those of
     * another: the NPC it names, what a resident of that NPC is to one of
     * this — its child, its mate, its fellow at work — and how many blocks
     * apart the two may come into the world and still be bound.
     */

    // Bond
    private final short npcID;
    private final short relationID;
    private final float blocks;

    // Constructor \\

    public NpcBondStruct(short npcID, short relationID, float blocks) {

        // Bond
        this.npcID = npcID;
        this.relationID = relationID;
        this.blocks = blocks;
    }

    // Accessible \\

    public short getNpcID() {
        return npcID;
    }

    public short getRelationID() {
        return relationID;
    }

    public float getBlocks() {
        return blocks;
    }
}
