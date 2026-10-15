package application.bootstrap.combatpipeline;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.combatpipeline.movesetmanager.MovesetManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import engine.root.PipelinePackage;

public class CombatPipeline extends PipelinePackage {

    /*
     * Registers the combat pipeline managers in dependency order.
     * MovesetManager holds how each kind of weapon fights, and loads a
     * moveset on demand when an item definition names one first.
     * CombatManager drives every entity's actions and owns damage, and
     * ProjectileManager flies the items those actions throw. It is created
     * after the world pipeline, so a swing or a landing can edit blocks and
     * place world items, and after the entity pipeline, so the actions a
     * player starts each frame advance in that same frame.
     */

    @Override
    protected void create() {
        create(MovesetManager.class);
        create(CombatManager.class);
        create(ProjectileManager.class);
    }
}
