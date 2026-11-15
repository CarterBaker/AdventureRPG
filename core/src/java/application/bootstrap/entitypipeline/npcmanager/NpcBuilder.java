package application.bootstrap.entitypipeline.npcmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.npc.NpcBondStruct;
import application.bootstrap.entitypipeline.npc.NpcData;
import application.bootstrap.entitypipeline.npc.NpcHandle;
import application.bootstrap.entitypipeline.npc.NpcTemperamentStruct;
import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class NpcBuilder extends BuilderPackage {

    /*
     * Parses one NPC ARPG file into an NpcData and wraps it in an NpcHandle,
     * under the short NPC ID NpcManager assigns its name. An NPC names the
     * "entity" template it walks as, the optional "gear" loot tables it is
     * dressed and armed from, each drawn once as it comes into the world, the
     * optional "loot" table it drops when it falls, the "wander_blocks" it
     * strays from its spot, the "profession" it holds, and the "level_min" and
     * "level_max" each resident of it is drawn between. Its temper is optional
     * too: the "sight_blocks" it notices a foe within and the "leash_blocks"
     * from home it chases one, the "hostility" its opinion of a being must
     * fall to before it turns on it, the "flee_health" share it runs at,
     * whether it "defends" those it likes, and how it fights — the
     * "reaction_seconds" it answers a blow in, the "guard_chance" it reads the
     * blow's side, its "aggression", and its "combo_chance" and
     * "feint_chance". Its nature is optional as well: the "traits" every
     * resident of it has, the "trait_pool" more are drawn from and the
     * "trait_draws" each resident takes from it, and the "bonds" it holds
     * residents of other NPCs by — each the "npc" it names, the "relation" a
     * resident of that NPC is to one of this, and the "blocks" apart the two
     * may come into the world and still be bound. Every template, table,
     * profession, trait and relation is resolved here, so an NPC naming an
     * unknown one fails at boot; an NPC a bond names is registered by name
     * as it is read, so two NPCs may name each other.
     */

    // Internal
    private NpcManager npcManager;
    private EntityManager entityManager;
    private LootTableManager lootTableManager;
    private ProfessionManager professionManager;
    private TraitManager traitManager;
    private RelationManager relationManager;

    // Base \\

    @Override
    protected void get() {
        this.npcManager = get(NpcManager.class);
        this.entityManager = get(EntityManager.class);
        this.lootTableManager = get(LootTableManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.traitManager = get(TraitManager.class);
        this.relationManager = get(RelationManager.class);
    }

    // Build \\

    NpcHandle build(File file, String npcName) {

        short npcID = npcManager.registerNpcName(npcName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        int entityTemplateID = entityManager.getTemplateIDFromTemplateName(ArpgUtility.validateString(arpg, "entity"));
        short[] gearTableIDs = parseGear(arpg);
        String lootTableName = ArpgUtility.getString(arpg, "loot", EngineSetting.LOOT_TABLE_NAME_NONE);
        short lootTableID = lootTableName.isEmpty()
                ? EngineSetting.LOOT_TABLE_NONE
                : lootTableManager.getLootTableIDFromLootTableName(lootTableName);
        float wanderBlocks = ArpgUtility.getFloat(arpg, "wander_blocks", EngineSetting.DEFAULT_NPC_WANDER_BLOCKS);
        NpcTemperamentStruct temperament = parseTemperament(arpg, npcName, file);

        short professionID = professionManager.getProfessionIDFromProfessionName(
                ArpgUtility.getString(arpg, "profession", EngineSetting.NPC_DEFAULT_PROFESSION));
        int levelMin = ArpgUtility.getInt(arpg, "level_min", EngineSetting.LEVEL_START);
        int levelMax = ArpgUtility.getInt(arpg, "level_max", levelMin);
        short[] traitIDs = ProgressionArpgUtility.parseNames(arpg, "traits", traitManager::getTraitIDFromTraitName);
        short[] traitPoolIDs = ProgressionArpgUtility.parseNames(
                arpg, "trait_pool", traitManager::getTraitIDFromTraitName);
        int traitDraws = ArpgUtility.getInt(arpg, "trait_draws", EngineSetting.DEFAULT_NPC_TRAIT_DRAWS);

        if (wanderBlocks < 0f)
            throwException("NPC \"" + npcName + "\" must wander 0 blocks or more. File: " + file.getName());

        if (levelMin < EngineSetting.LEVEL_START || levelMax < levelMin)
            throwException("NPC \"" + npcName + "\" is drawn from level " + levelMin + " to " + levelMax
                    + " — levels must start at " + EngineSetting.LEVEL_START + " or more and never fall. File: "
                    + file.getName());

        if (traitDraws < 0 || traitDraws > traitPoolIDs.length
                || traitIDs.length + traitDraws > EngineSetting.DIALOGUE_TRAIT_MAX)
            throwException("NPC \"" + npcName + "\" draws " + traitDraws + " traits from a pool of "
                    + traitPoolIDs.length + " beside " + traitIDs.length + " of its own — it may draw no more than "
                    + "its pool holds and hold no more than " + EngineSetting.DIALOGUE_TRAIT_MAX + " in all. File: "
                    + file.getName());

        NpcData npcData = new NpcData(
                npcName, npcID, entityTemplateID, gearTableIDs, lootTableID, wanderBlocks, temperament,
                professionID, levelMin, levelMax, traitIDs, traitPoolIDs, traitDraws,
                parseBonds(arpg, npcName, file));

        NpcHandle npcHandle = create(NpcHandle.class);
        npcHandle.constructor(npcData);

        return npcHandle;
    }

    // Parse \\

    private NpcTemperamentStruct parseTemperament(ArpgObjectStruct arpg, String npcName, File file) {

        float sightBlocks = ArpgUtility.getFloat(arpg, "sight_blocks", EngineSetting.DEFAULT_NPC_SIGHT_BLOCKS);
        float leashBlocks = ArpgUtility.getFloat(arpg, "leash_blocks", EngineSetting.DEFAULT_NPC_LEASH_BLOCKS);
        float hostility = ArpgUtility.getFloat(arpg, "hostility", EngineSetting.DEFAULT_NPC_HOSTILITY);
        float fleeHealth = ArpgUtility.getFloat(arpg, "flee_health", EngineSetting.DEFAULT_NPC_FLEE_HEALTH);
        boolean defends = ArpgUtility.getBoolean(arpg, "defends", EngineSetting.DEFAULT_NPC_DEFENDS);
        float reactionSeconds = ArpgUtility.getFloat(
                arpg, "reaction_seconds", EngineSetting.DEFAULT_NPC_REACTION_SECONDS);
        float guardChance = ArpgUtility.getFloat(arpg, "guard_chance", EngineSetting.DEFAULT_NPC_GUARD_CHANCE);
        float aggression = ArpgUtility.getFloat(arpg, "aggression", EngineSetting.DEFAULT_NPC_AGGRESSION);
        float comboChance = ArpgUtility.getFloat(arpg, "combo_chance", EngineSetting.DEFAULT_NPC_COMBO_CHANCE);
        float feintChance = ArpgUtility.getFloat(arpg, "feint_chance", EngineSetting.DEFAULT_NPC_FEINT_CHANCE);

        if (sightBlocks < 0f || leashBlocks < 0f)
            throwException("NPC \"" + npcName + "\" must see and chase 0 blocks or more. File: " + file.getName());

        if (Math.abs(hostility) > EngineSetting.AFFINITY_MAX)
            throwException("NPC \"" + npcName + "\" has a \"hostility\" of " + hostility + " — opinions run from -"
                    + EngineSetting.AFFINITY_MAX + " to " + EngineSetting.AFFINITY_MAX + ". File: " + file.getName());

        if (!isShare(fleeHealth) || !isShare(guardChance) || !isShare(comboChance) || !isShare(feintChance))
            throwException("NPC \"" + npcName + "\" must give \"flee_health\", \"guard_chance\", \"combo_chance\" "
                    + "and \"feint_chance\" between 0 and 1. File: " + file.getName());

        if (reactionSeconds < 0f || aggression <= 0f)
            throwException("NPC \"" + npcName + "\" must react in 0 seconds or more and have a positive "
                    + "\"aggression\". File: " + file.getName());

        return new NpcTemperamentStruct(
                sightBlocks, leashBlocks, hostility, fleeHealth, defends,
                reactionSeconds, guardChance, aggression, comboChance, feintChance);
    }

    private NpcBondStruct[] parseBonds(ArpgObjectStruct arpg, String npcName, File file) {

        if (!ArpgUtility.hasArray(arpg, "bonds"))
            return new NpcBondStruct[0];

        ArpgArrayStruct bondsArpg = arpg.getAsArray("bonds");
        NpcBondStruct[] bonds = new NpcBondStruct[bondsArpg.size()];

        for (int i = 0; i < bonds.length; i++) {

            ArpgObjectStruct bondArpg = bondsArpg.get(i).getAsObject();
            float blocks = ArpgUtility.getFloat(bondArpg, "blocks", EngineSetting.DEFAULT_NPC_BOND_BLOCKS);

            if (blocks <= 0f)
                throwException("NPC \"" + npcName + "\" must bind within more than 0 blocks. File: "
                        + file.getName());

            bonds[i] = new NpcBondStruct(
                    npcManager.registerNpcName(ArpgUtility.validateString(bondArpg, "npc")),
                    relationManager.getRelationIDFromRelationName(ArpgUtility.validateString(bondArpg, "relation")),
                    blocks);
        }

        return bonds;
    }

    private boolean isShare(float value) {
        return value >= 0f && value <= 1f;
    }

    private short[] parseGear(ArpgObjectStruct arpg) {

        if (!ArpgUtility.hasArray(arpg, "gear"))
            return new short[0];

        ArpgArrayStruct gearArpg = arpg.getAsArray("gear");
        short[] gearTableIDs = new short[gearArpg.size()];

        for (int i = 0; i < gearTableIDs.length; i++)
            gearTableIDs[i] = lootTableManager.getLootTableIDFromLootTableName(gearArpg.get(i).getAsString());

        return gearTableIDs;
    }
}
