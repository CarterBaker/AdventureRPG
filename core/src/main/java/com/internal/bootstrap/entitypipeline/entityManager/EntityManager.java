package com.internal.bootstrap.entitypipeline.entitymanager;

import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class EntityManager extends ManagerPackage {

    // Internal
    private WorldStreamManager worldStreamManager;

    // Template Retrieval Mapping
    private Object2IntOpenHashMap<String> entityDataName2EntityDataID;
    private Int2ObjectOpenHashMap<EntityData> entityDataID2EntityData;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.entityDataName2EntityDataID = new Object2IntOpenHashMap<>();
        this.entityDataID2EntityData = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // On-Demand Loading \\

    public void request(String templateName) {
        ((InternalLoadManager) internalLoader).request(templateName);
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
        int templateID = getTemplateIDFromTemplateName(templateName);
        return getTemplateDataFromTemplateID(templateID);
    }

    public EntityHandle createEntity(EntityData entityData) {
        WorldHandle activeWorldHandle = worldStreamManager.getActiveWorld();
        long randomChunk = WorldPositionUtility.getRandomChunk(activeWorldHandle);
        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(
                entityData,
                worldStreamManager.getActiveWorld(),
                new Vector3(),
                randomChunk,
                entityData.getRandomSize(),
                entityData.getRandomWeight());
        return entityHandle;
    }
}