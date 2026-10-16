package application.bootstrap.entitypipeline.npcmanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.npc.NpcHandle;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.util.IdentityUtility;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.worldspawn.WorldSpawnPaletteHandle;
import application.bootstrap.worldpipeline.worldspawn.WorldSpawnStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class NpcManager extends ManagerPackage {

    /*
     * Owns the NPC palette and every NPC living in the world; spawnResidents()
     * and despawnResidents() are the one way residents enter and leave it, as
     * the items of the chunk they live in come into and go out of the world. A
     * resident comes in standing in the block its spot names, as an entity of
     * its template, known by an identity drawn from its spot, its level drawn
     * from its definition's range and its gender from its race's, both by that
     * identity so it is the same each time it returns; it is raised to that
     * level in its profession through ProgressionManager, its skin and hair
     * drawn from its race's palettes, and dressed and armed from its gear
     * tables at its own level and luck, anything drawn that no free slot is
     * made for left behind. Each frame every resident whose chunk has
     * generated takes stock of the beings about it through
     * NpcPerceptionBranch, which settles its mind, and that mind writes what
     * it wants into its input exactly as the player's keys and mouse would: a
     * calm one wanders through NpcWanderBranch, a fighting or fleeing one
     * fights or runs through NpcCombatBranch, and one whose fight is over
     * walks home, every way walked through NpcSteerBranch along the paths
     * PathfindingManager finds. It then moves, animates and fights exactly as
     * the player does, through MovementManager and CombatManager.control(),
     * its primary action only ever swinging what it holds. alarm() hears
     * every blow that lands from a source, provoking the struck resident and
     * any near enough that defends the struck. A resident brought to no
     * health falls through defeat(): at the end of the frame it scatters
     * everything it carried and one draw of its loot table at its level and
     * by the luck of whatever felled it, and leaves the world, its spot left
     * empty for as long as the chunk keeps its items. Every NPC is resolved
     * in awake(), before any chunk generates. NPC IDs are assigned in
     * registration order.
     */

    // Internal
    private EntityManager entityManager;
    private LootTableManager lootTableManager;
    private MovementManager movementManager;
    private CombatManager combatManager;
    private ProjectileManager projectileManager;
    private WorldStreamManager worldStreamManager;
    private ProgressionManager progressionManager;
    private StatManager statManager;

    // Branches
    private NpcSteerBranch npcSteerBranch;
    private NpcPerceptionBranch npcPerceptionBranch;
    private NpcWanderBranch npcWanderBranch;
    private NpcCombatBranch npcCombatBranch;

    // Palette
    private Object2IntOpenHashMap<String> npcName2NpcID;
    private ObjectArrayList<NpcHandle> npcID2NpcHandle;

    // Residents
    private ObjectArrayList<NpcInstance> residents;

    // Scratch
    private ObjectArrayList<ItemInstance> itemScratch;
    private Vector3 positionScratch;
    private Vector3 velocityScratch;
    private Vector3 spinScratch;
    private Quaternion orientationScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.npcName2NpcID = RegistryUtility.createNameIndex();
        this.npcID2NpcHandle = RegistryUtility.createPalette();
        create(NpcLoader.class);

        // Branches
        this.npcSteerBranch = create(NpcSteerBranch.class);
        this.npcPerceptionBranch = create(NpcPerceptionBranch.class);
        this.npcWanderBranch = create(NpcWanderBranch.class);
        this.npcCombatBranch = create(NpcCombatBranch.class);

        // Systems
        create(NpcRenderSystem.class);

        // Residents
        this.residents = new ObjectArrayList<>();

        // Scratch
        this.itemScratch = new ObjectArrayList<>();
        this.positionScratch = new Vector3();
        this.velocityScratch = new Vector3();
        this.spinScratch = new Vector3();
        this.orientationScratch = new Quaternion();
    }

    @Override
    protected void get() {

        // Internal
        this.entityManager = get(EntityManager.class);
        this.lootTableManager = get(LootTableManager.class);
        this.movementManager = get(MovementManager.class);
        this.combatManager = get(CombatManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.statManager = get(StatManager.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Update \\

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();

        for (int i = residents.size() - 1; i >= 0; i--) {

            NpcInstance resident = residents.get(i);

            if (resident.isFallen()) {
                fall(resident);
                residents.remove(i);
                continue;
            }

            if (isGenerated(resident.getEntityInstance()))
                step(resident, deltaTime);
        }
    }

    // The resident's mind writes its input, and its body moves, animates and fights by it exactly as the player's does
    private void step(NpcInstance resident, float deltaTime) {

        EntityInstance entity = resident.getEntityInstance();
        EntityInputHandle input = entity.getEntityInputHandle();

        npcPerceptionBranch.perceive(resident, deltaTime);
        input.clear();
        think(resident, deltaTime);

        movementManager.writeMovementState(entity, combatManager.isSlowed(entity), combatManager.isAttacking(entity));
        movementManager.move(entity);
        entity.updateAnimation(deltaTime);
        combatManager.control(entity);

        if (input.isPrimaryAction())
            combatManager.swing(entity);
    }

    private void think(NpcInstance resident, float deltaTime) {
        switch (resident.getMind()) {
            case CALM -> npcWanderBranch.wander(resident, deltaTime);
            case FIGHT -> npcCombatBranch.fight(resident, deltaTime);
            case FLEE -> npcCombatBranch.flee(resident, deltaTime);
            case RETURN -> npcWanderBranch.returnHome(resident, deltaTime);
        }
    }

    // True while the chunk the entity stands in has generated, so it has ground to walk on
    private boolean isGenerated(EntityInstance entity) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(
                entity.getWorldPositionStruct().getChunkCoordinate());

        return chunk != null && chunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA);
    }

    // Management \\

    short registerNpcName(String npcName) {
        return (short) RegistryUtility.registerID(
                npcName2NpcID, npcID2NpcHandle, npcName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addNpcHandle(NpcHandle npcHandle) {

        short npcID = npcHandle.getNpcID();

        if (npcID2NpcHandle.get(npcID) != null)
            throwException("Duplicate NPC name: '" + npcHandle.getNpcName() + "' was registered more than once");

        npcID2NpcHandle.set(npcID, npcHandle);
    }

    // On-Demand \\

    public void request(String npcName) {
        ((NpcLoader) internalLoader).request(npcName);
    }

    // Residents \\

    // Main thread — every spot recorded in a chunk whose items just came into the world gets its NPC
    public void spawnResidents(ChunkInstance chunkInstance) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {

            if (!subChunks[i].hasWorldSpawns())
                continue;

            WorldSpawnPaletteHandle palette = subChunks[i].getWorldSpawnPaletteHandle();
            ObjectArrayList<WorldSpawnStruct> spawns = palette.getSpawns();

            for (int j = 0; j < spawns.size(); j++)
                spawnResident(chunkInstance, palette, spawns.get(j));
        }
    }

    // Main thread — every NPC living in a chunk whose items just left the world leaves with them; one that fell
    // first leaves its spot empty as it goes
    public void despawnResidents(long chunkCoordinate) {

        for (int i = residents.size() - 1; i >= 0; i--) {

            NpcInstance resident = residents.get(i);

            if (resident.getHomeChunkCoordinate() != chunkCoordinate)
                continue;

            if (resident.isFallen())
                resident.getWorldSpawnPaletteHandle().removeSpawn(resident.getWorldSpawnStruct());

            entityManager.despawnEntity(resident.getEntityInstance());
            residents.remove(i);
        }
    }

    // The NPC stands in the middle of its spot's block, varied in look and dressed from its gear
    private void spawnResident(ChunkInstance chunkInstance, WorldSpawnPaletteHandle palette, WorldSpawnStruct spawn) {

        NpcHandle npcHandle = getNpcHandleFromNpcID(spawn.getNpcID());

        positionScratch.set(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());

        EntityInstance entity = entityManager.spawnEntity(
                entityManager.getEntityHandleFromTemplateID(npcHandle.getEntityTemplateID()),
                chunkInstance.getWorldHandle(),
                chunkInstance.getCoordinate(),
                positionScratch);
        Vector3 size = entity.getSize();

        entity.getWorldPositionStruct().getPosition().add(0.5f - size.x * 0.5f, 0f, 0.5f - size.z * 0.5f);
        WorldPositionUtility.settleChunk(entity.getWorldHandle(), entity.getWorldPositionStruct());

        raise(entity, npcHandle, IdentityUtility.fromSpawn(
                chunkInstance.getCoordinate(), spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()));
        varyAppearance(entity);
        dress(entity, npcHandle);

        NpcInstance resident = create(NpcInstance.class);
        resident.constructor(npcHandle, entity, chunkInstance.getCoordinate(), palette, spawn);

        residents.add(resident);
    }

    // Known by its spawn's identity, its gender and level drawn by that identity, and raised in its profession
    private void raise(EntityInstance entity, NpcHandle npcHandle, long identity) {

        EntityData entityData = entity.getEntityData();
        int levelSpan = npcHandle.getLevelMax() - npcHandle.getLevelMin() + 1;
        int level = npcHandle.getLevelMin()
                + Math.min(levelSpan - 1, (int) (BiomeFieldUtility.hash01(identity ^ EngineSetting.NPC_LEVEL_SALT)
                        * levelSpan));
        int genderIndex = Math.min(
                entityData.getGenderCount() - 1,
                (int) (BiomeFieldUtility.hash01(identity ^ EngineSetting.NPC_GENDER_SALT)
                        * entityData.getGenderCount()));

        progressionManager.identify(entity, identity);
        entity.setGenderID(entityData.getGenderID(genderIndex));
        progressionManager.raiseBeing(entity, level, npcHandle.getProfessionID());
    }

    // Skin and hair drawn from the race's palettes, so no two residents look quite alike
    private void varyAppearance(EntityInstance entity) {

        if (!entity.hasAppearance())
            return;

        AppearanceData appearanceData = entity.getEntityData().getAppearanceData();
        AppearanceHandle appearanceHandle = entity.getAppearanceHandle();
        Color skin = drawColor(appearanceData.getSkinPalette());
        Color hair = drawColor(appearanceData.getHairPalette());

        appearanceHandle.setSkinColor(skin.r, skin.g, skin.b);
        appearanceHandle.setHairColor(hair.r, hair.g, hair.b);
    }

    private Color drawColor(Color[] palette) {
        return palette[ThreadLocalRandom.current().nextInt(palette.length)];
    }

    // Every gear table drawn once at the resident's level and luck, and every item drawn put on where it is worn
    private void dress(EntityInstance entity, NpcHandle npcHandle) {

        short[] gearTableIDs = npcHandle.getGearTableIDs();
        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        float luck = entity.getStat(statManager.getLuckStatID());

        for (int i = 0; i < gearTableIDs.length; i++) {

            itemScratch.clear();
            lootTableManager.drawLoot(
                    gearTableIDs[i], ThreadLocalRandom.current().nextLong(), entity.getLevel(), luck, itemScratch);

            for (int j = 0; j < itemScratch.size(); j++)
                inventoryHandle.wear(itemScratch.get(j));
        }

        itemScratch.clear();
    }

    // Defeat \\

    // True when the entity is a resident, which falls at the end of the frame remembering what felled it, if
    // anything — anything else is left to its own context
    public boolean defeat(EntityInstance entityInstance, EntityInstance killer) {

        NpcInstance resident = findResident(entityInstance);

        if (resident == null)
            return false;

        resident.setFallen(true);
        resident.setKiller(killer);

        return true;
    }

    // Everything the resident carried and one draw of its loot table are thrown clear of it, its spot is left empty
    // and it leaves the world
    private void fall(NpcInstance resident) {

        EntityInstance entity = resident.getEntityInstance();
        NpcHandle npcHandle = resident.getNpcHandle();

        itemScratch.clear();
        collectCarried(entity.getInventoryHandle());

        if (npcHandle.hasLoot())
            lootTableManager.drawLoot(
                    npcHandle.getLootTableID(),
                    ThreadLocalRandom.current().nextLong(),
                    entity.getLevel(),
                    resolveKillerLuck(resident),
                    itemScratch);

        for (int i = 0; i < itemScratch.size(); i++)
            throwDrop(entity, itemScratch.get(i), i, itemScratch.size());

        itemScratch.clear();

        resident.getWorldSpawnPaletteHandle().removeSpawn(resident.getWorldSpawnStruct());
        entityManager.despawnEntity(entity);
    }

    // The luck of whatever felled the resident, none when nothing did or it has since left the world
    private float resolveKillerLuck(NpcInstance resident) {

        EntityInstance killer = resident.getKiller();

        return killer != null ? killer.getStat(statManager.getLuckStatID()) : 0f;
    }

    private void collectCarried(InventoryHandle inventoryHandle) {

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            if (inventoryHandle.hasItem(equipmentSlot))
                itemScratch.add(inventoryHandle.unequip(equipmentSlot));
    }

    // One item thrown up and out from the resident's middle, each drop at its own share of a full turn
    private void throwDrop(EntityInstance entity, ItemInstance itemInstance, int index, int count) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();
        float angle = (float) (index * Math.PI * 2.0 / count);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        positionScratch.set(
                position.x + size.x * 0.5f,
                position.y + EngineSetting.NPC_DROP_LIFT_BLOCKS,
                position.z + size.z * 0.5f);
        velocityScratch.set(
                cos * EngineSetting.NPC_DROP_SPREAD_SPEED,
                EngineSetting.NPC_DROP_LIFT_SPEED,
                sin * EngineSetting.NPC_DROP_SPREAD_SPEED);
        spinScratch.set(sin, 0f, cos);
        orientationScratch.set(1f, 0f, 0f, 0f);

        projectileManager.drop(
                entity,
                itemInstance,
                entity.getWorldPositionStruct().getChunkCoordinate(),
                positionScratch,
                velocityScratch,
                spinScratch,
                EngineSetting.NPC_DROP_SPIN_RATE,
                orientationScratch);
    }

    // Alarm \\

    // A blow landed by a source on a target, the share of the target's health it took — the struck resident is
    // provoked by it, and every other resident that defends may take the struck one's side
    public void alarm(EntityInstance source, EntityInstance target, float share) {

        if (source == target)
            return;

        NpcInstance struck = findResident(target);

        if (struck != null && !struck.isFallen())
            npcPerceptionBranch.provoke(struck, source, findResident(source));

        for (int i = 0; i < residents.size(); i++) {

            NpcInstance resident = residents.get(i);

            if (resident != struck && !resident.isFallen())
                npcPerceptionBranch.defend(resident, source, target, share);
        }
    }

    private NpcInstance findResident(EntityInstance entityInstance) {

        for (int i = 0; i < residents.size(); i++)
            if (residents.get(i).getEntityInstance() == entityInstance)
                return residents.get(i);

        return null;
    }

    // Accessible \\

    public ObjectArrayList<NpcInstance> getResidents() {
        return residents;
    }

    public boolean hasNpc(String npcName) {
        return RegistryUtility.getHandle(npcName2NpcID, npcID2NpcHandle, npcName) != null;
    }

    public NpcHandle getNpcHandleFromNpcName(String npcName) {

        NpcHandle handle = RegistryUtility.getHandle(npcName2NpcID, npcID2NpcHandle, npcName);

        if (handle == null) {
            request(npcName);
            handle = RegistryUtility.getHandle(npcName2NpcID, npcID2NpcHandle, npcName);
        }

        if (handle == null)
            throwException("NPC \"" + npcName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the NPC directory.");

        return handle;
    }

    public short getNpcIDFromNpcName(String npcName) {
        return getNpcHandleFromNpcName(npcName).getNpcID();
    }

    public NpcHandle getNpcHandleFromNpcID(short npcID) {

        NpcHandle handle = RegistryUtility.getHandle(npcID2NpcHandle, npcID);

        if (handle == null)
            throwException("No handle registered for NPC ID: " + npcID);

        return handle;
    }
}
