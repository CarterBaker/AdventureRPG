package application.bootstrap.entitypipeline.npc;

import engine.root.DataPackage;

public class NpcData extends DataPackage {

    /*
     * Immutable NPC definition loaded from ARPG: the entity template it walks
     * the world as, the loot tables it is dressed and armed from as it comes
     * into the world, the loot table it drops when it falls, how far from its
     * spot it wanders, the profession it holds and the range of levels a
     * resident of it is drawn from. Owned by NpcHandle for the engine
     * lifetime.
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

    // Progression
    private final short professionID;
    private final int levelMin;
    private final int levelMax;

    // Constructor \\

    public NpcData(
            String npcName,
            short npcID,
            int entityTemplateID,
            short[] gearTableIDs,
            short lootTableID,
            float wanderBlocks,
            short professionID,
            int levelMin,
            int levelMax) {

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

        // Progression
        this.professionID = professionID;
        this.levelMin = levelMin;
        this.levelMax = levelMax;
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

    public short getProfessionID() {
        return professionID;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }
}
