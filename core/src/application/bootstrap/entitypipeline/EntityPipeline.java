package application.bootstrap.entitypipeline;

import application.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import engine.root.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    /*
     * Registers all entity pipeline managers in dependency order. BehaviorManager
     * and EntityManager are registered before PlayerManager since player spawn
     * depends on both being available. FeatureManager is registered before
     * EntityManager since entity templates resolve their default appearance
     * features against it while they load.
     */

    @Override
    protected void create() {
        create(BehaviorManager.class);
        create(FeatureManager.class);
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}