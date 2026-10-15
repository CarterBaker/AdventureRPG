package application.bootstrap.entitypipeline.npc;

import engine.root.DataPackage;

public class NpcData extends DataPackage {

    /*
     * Immutable NPC definition loaded from ARPG: the entity template it walks
     * the world as, the loot tables it is dressed and armed from as it comes
     * into the world, the loot table it drops when it falls, and how far from
     * its spot it wanders. Owned by NpcHandle for the engine lifetime.
     */

    // Identity
    private final String npcName;
    private final short npcID;

    // Entity
    private final int entityTemplateID;

    // Loot
    private final short[] gearTableIDs;
    private final short lootTableID;

    // Behavior
    private final float wanderBlocks;

    // Constructor \\

    public NpcData(
            String npcName,
            short npcID,
            int entityTemplateID,
            short[] gearTableIDs,
            short lootTableID,
            float wanderBlocks) {

        // Identity
        this.npcName = npcName;
        this.npcID = npcID;

        // Entity
        this.entityTemplateID = entityTemplateID;

        // Loot
        this.gearTableIDs = gearTableIDs;
        this.lootTableID = lootTableID;

        // Behavior
        this.wanderBlocks = wanderBlocks;
    }

    // Accessible \\

    public String getNpcName() {
        return npcName;
    }

    public short getNpcID() {
        return npcID;
    }

    public int getEntityTemplateID() {
        return entityTemplateID;
    }

    public short[] getGearTableIDs() {
        return gearTableIDs;
    }

    public short getLootTableID() {
        return lootTableID;
    }

    public float getWanderBlocks() {
        return wanderBlocks;
    }
}
