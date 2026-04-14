package application.bootstrap.entitypipeline.entitymanager;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class EntityManager extends ManagerPackage {

    /*
     * Owns the entity template palette and drives the entity bootstrap pipeline.
     * Handles on-demand template loading and provides the spawnEntity() factory
     * for creating runtime EntityInstances from template handles.
     */

    // Internal
    private WorldManager worldManager;
    private BehaviorManager behaviorManager;

    // Palette
    private Object2IntOpenHashMap<String> templateName2TemplateID;
    private Int2ObjectOpenHashMap<EntityHandle> templateID2EntityHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.templateName2TemplateID = new Object2IntOpenHashMap<>();
        this.templateID2EntityHandle = new Int2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.worldManager = get(WorldManager.class);
        this.behaviorManager = get(BehaviorManager.class);
    }

    // Management \\

    void addEntityTemplate(String templateName, EntityHandle entityHandle) {

        int id = RegistryUtility.toIntID(templateName);

        templateName2TemplateID.put(templateName, id);
        templateID2EntityHandle.put(id, entityHandle);
    }

    // Accessible \\

    public boolean hasTemplate(String templateName) {
        return templateName2TemplateID.containsKey(templateName);
    }

    public int getTemplateIDFromTemplateName(String templateName) {

        if (!templateName2TemplateID.containsKey(templateName))
            ((InternalLoader) internalLoader).request(templateName);

        return templateName2TemplateID.getInt(templateName);
    }

    public EntityHandle getEntityHandleFromTemplateID(int templateID) {

        EntityHandle handle = templateID2EntityHandle.get(templateID);

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

        return entityInstance;
    }

    public EntityInstance spawnEntity(String templateName) {
        return spawnEntity(getEntityHandleFromTemplateName(templateName));
    }
}