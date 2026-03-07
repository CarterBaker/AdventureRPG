package com.internal.bootstrap.entitypipeline.entitymanager;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import com.internal.bootstrap.entitypipeline.entity.EntityData;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldmanager.WorldManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class EntityManager extends ManagerPackage {

    // Internal
    private WorldManager worldManager;
    private BehaviorManager behaviorManager;

    // Template Retrieval
    private Object2IntOpenHashMap<String> entityDataName2EntityDataID;
    private Int2ObjectOpenHashMap<EntityData> entityDataID2EntityData;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.entityDataName2EntityDataID = new Object2IntOpenHashMap<>();
        this.entityDataID2EntityData = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.behaviorManager = get(BehaviorManager.class);
    }

    // On-Demand Loading \\

    public void request(String templateName) {
        ((InternalLoader) internalLoader).request(templateName);
    }

    // Template Management \\

    void addEntityTemplate(String templateName, int templateID, EntityData templateData) {
        entityDataName2EntityDataID.put(templateName, templateID);
        entityDataID2EntityData.put(templateID, templateData);
    }

    // Accessible \\

    public int getTemplateIDFromTemplateName(String templateName) {
        if (!entityDataName2EntityDataID.containsKey(templateName))
            request(templateName);
        return entityDataName2EntityDataID.getInt(templateName);
    }

    public EntityData getTemplateDataFromTemplateID(int templateID) {
        EntityData data = entityDataID2EntityData.get(templateID);
        if (data == null)
            throwException("Entity template ID not found: " + templateID);
        return data;
    }

    public EntityData getTemplateDataFromTemplateName(String templateName) {
        return getTemplateDataFromTemplateID(getTemplateIDFromTemplateName(templateName));
    }

    public EntityHandle createEntity(EntityData entityData) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        BehaviorHandle behavior = behaviorManager.getBehavior(entityData.getBehaviorName());
        long randomChunk = WorldPositionUtility.getRandomChunk(activeWorld);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(
                entityData,
                activeWorld,
                behavior,
                new Vector3(),
                randomChunk,
                entityData.getRandomSize(),
                entityData.getRandomWeight());

        return entityHandle;
    }
}