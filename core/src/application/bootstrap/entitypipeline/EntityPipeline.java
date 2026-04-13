package application.bootstrap.entitypipeline;

import application.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.core.engine.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    /*
     * Registers all entity pipeline managers in dependency order. BehaviorManager
     * and EntityManager are registered before PlayerManager since player spawn
     * depends on both being available.
     */

    @Override
    protected void create() {
        create(BehaviorManager.class);
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}