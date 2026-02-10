package com.internal.bootstrap.entitypipeline.entityManager;

import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class EntityManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    private WorldStreamManager worldStreamManager;

    // Template Retrieval Mapping
    private Object2IntOpenHashMap<String> entityDataName2EntityDataID;
    private Int2ObjectOpenHashMap<EntityData> entiityDataID2EntityData;

    // Base \\

    @Override
    protected void create() {
        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Template Retrieval Mapping
        this.entityDataName2EntityDataID = new Object2IntOpenHashMap<>();
        this.entiityDataID2EntityData = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        compileTemplateData();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // Template Management \\

    void compileTemplateData() {
        internalLoadManager.loadTemplateData();
    }

    void addEntityTemplate(String templateName, int templateID, EntityData templateData) {
        entityDataName2EntityDataID.put(templateName, templateID);
        entiityDataID2EntityData.put(templateID, templateData);
    }

    // Accessible \\

    public int getTemplateIDFromTemplateName(String templateName) {
        return entityDataName2EntityDataID.getInt(templateName);
    }

    public EntityData getTemplateDataFromTemplateID(int templateID) {
        return entiityDataID2EntityData.get(templateID);
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