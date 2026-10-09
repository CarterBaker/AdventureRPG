package application.bootstrap.entitypipeline.entitymanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityManager extends ManagerPackage {

    /*
     * Owns the entity template palette and drives the entity bootstrap pipeline.
     * Handles on-demand template loading and provides the spawnEntity() factory
     * for creating runtime EntityInstances from template handles, each one
     * entering combat as it spawns, and despawnEntity(), its mirror, which
     * takes the entity out of combat and off every vehicle. rerollEntity()
     * rolls an existing instance again in place — a new random chunk, size,
     * weight, base statistics, an empty inventory, and the template's default
     * appearance — for anything that holds the instance by reference and
     * cannot swap in a new one.
     */

    // Internal
    private WorldManager worldManager;
    private BehaviorManager behaviorManager;
    private CombatManager combatManager;
    private VehicleManager vehicleManager;

    // Palette
    private Object2IntOpenHashMap<String> templateName2TemplateID;
    private ObjectArrayList<EntityHandle> templateID2EntityHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.templateName2TemplateID = RegistryUtility.createNameIndex();
        this.templateID2EntityHandle = RegistryUtility.createPalette();
        create(EntityLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.worldManager = get(WorldManager.class);
        this.behaviorManager = get(BehaviorManager.class);
        this.combatManager = get(CombatManager.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Management \\

    void addEntityTemplate(String templateName, EntityHandle entityHandle) {
        RegistryUtility.registerHandle(
                templateName2TemplateID, templateID2EntityHandle, templateName, entityHandle,
                EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasTemplate(String templateName) {
        return RegistryUtility.getHandle(templateName2TemplateID, templateID2EntityHandle, templateName) != null;
    }

    public int getTemplateIDFromTemplateName(String templateName) {

        if (!hasTemplate(templateName))
            ((EntityLoader) internalLoader).request(templateName);

        return templateName2TemplateID.getInt(templateName);
    }

    public EntityHandle getEntityHandleFromTemplateID(int templateID) {

        EntityHandle handle = RegistryUtility.getHandle(templateID2EntityHandle, templateID);

        if (handle == null)
            throwException("Entity template ID not found: " + templateID);

        return handle;
    }

    public EntityHandle getEntityHandleFromTemplateName(String templateName) {
        return getEntityHandleFromTemplateID(getTemplateIDFromTemplateName(templateName));
    }

    public EntityInstance spawnEntity(EntityHandle entityHandle) {

        EntityData entityData = entityHandle.getEntityData();
        WorldHandle activeWorld = worldManager.getActiveWorld();
        BehaviorHandle behavior = behaviorManager.getBehaviorHandleFromBehaviorName(
                entityData.getBehaviorName());
        long randomChunk = WorldPositionUtility.getRandomChunk(activeWorld);

        EntityInstance entityInstance = create(EntityInstance.class);
        entityInstance.constructor(
                entityData,
                activeWorld,
                behavior,
                new Vector3(),
                randomChunk,
                entityData.getRandomSize(),
                entityData.getRandomWeight());

        combatManager.addCombatant(entityInstance);

        return entityInstance;
    }

    public EntityInstance spawnEntity(String templateName) {
        return spawnEntity(getEntityHandleFromTemplateName(templateName));
    }

    public void despawnEntity(EntityInstance entityInstance) {
        combatManager.removeCombatant(entityInstance);
        vehicleManager.releaseEntity(entityInstance);
    }

    public void rerollEntity(EntityInstance entityInstance) {

        EntityData entityData = entityInstance.getEntityData();
        long randomChunk = WorldPositionUtility.getRandomChunk(entityInstance.getWorldHandle());

        entityInstance.setLocation(new Vector3(), randomChunk);
        entityInstance.setSize(entityData.getRandomSize());
        entityInstance.setWeight(entityData.getRandomWeight());

        entityInstance.getStatisticsHandle().resetBaseStats();
        entityInstance.getInventoryHandle().clear();
        entityInstance.getEntityActionHandle().release();

        if (entityInstance.hasAppearance())
            entityInstance.getAppearanceHandle().resetToDefaults();
    }
}