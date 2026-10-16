package application.bootstrap.entitypipeline.npc;

import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class NpcHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded NPC definition. Registered and owned by
     * NpcManager. Delegates all accessors through NpcData.
     */

    // Internal
    private NpcData npcData;

    // Constructor \\

    public void constructor(NpcData npcData) {

        // Internal
        this.npcData = npcData;
    }

    // Accessible \\

    public NpcData getNpcData() {
        return npcData;
    }

    public String getNpcName() {
        return npcData.getNpcName();
    }

    public short getNpcID() {
        return npcData.getNpcID();
    }

    public int getEntityTemplateID() {
        return npcData.getEntityTemplateID();
    }

    public short[] getGearTableIDs() {
        return npcData.getGearTableIDs();
    }

    public short getLootTableID() {
        return npcData.getLootTableID();
    }

    public boolean hasLoot() {
        return npcData.getLootTableID() != EngineSetting.LOOT_TABLE_NONE;
    }

    public float getWanderBlocks() {
        return npcData.getWanderBlocks();
    }

    public short getProfessionID() {
        return npcData.getProfessionID();
    }

    public int getLevelMin() {
        return npcData.getLevelMin();
    }

    public int getLevelMax() {
        return npcData.getLevelMax();
    }
}
