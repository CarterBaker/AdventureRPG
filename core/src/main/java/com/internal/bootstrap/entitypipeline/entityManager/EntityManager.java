package com.internal.bootstrap.entitypipeline.entitymanager;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import com.internal.bootstrap.entitypipeline.entity.EntityData;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
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

    // Palette
    private Object2IntOpenHashMap<String> name2TemplateID;
    private Int2ObjectOpenHashMap<EntityHandle> id2EntityHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.name2TemplateID = new Object2IntOpenHashMap<>();
        this.id2EntityHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.worldManager = get(WorldManager.class);
        this.behaviorManager = get(BehaviorManager.class);
    }

    // Template Management \\

    void addEntityTemplate(String templateName, int templateID, EntityHandle entityHandle) {
        name2TemplateID.put(templateName, templateID);
        id2EntityHandle.put(templateID, entityHandle);
    }

    // Accessible \\

    public int getTemplateID(String templateName) {

        if (!name2TemplateID.containsKey(templateName))
            ((InternalLoader) internalLoader).request(templateName);

        return name2TemplateID.getInt(templateName);
    }

    public EntityHandle getEntityHandle(int templateID) {

        EntityHandle handle = id2EntityHandle.get(templateID);

        if (handle == null)
            throwException("Entity template ID not found: " + templateID);

        return handle;
    }

    public EntityHandle getEntityHandle(String templateName) {
        return getEntityHandle(getTemplateID(templateName));
    }

    public EntityInstance spawnEntity(EntityHandle entityHandle) {

        EntityData entityData = entityHandle.getEntityData();
        WorldHandle activeWorld = worldManager.getActiveWorld();
        BehaviorHandle behavior = behaviorManager.getBehavior(entityData.behaviorName);
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
        return spawnEntity(getEntityHandle(templateName));
    }
}