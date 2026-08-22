package application.bootstrap.physicspipeline.physicsmanager;

import application.bootstrap.physicspipeline.worlddistortionsystem.WorldDistortionSystem;
import engine.root.ManagerPackage;

public class PhysicsManager extends ManagerPackage {

    // Internal \\

    public WorldDistortionSystem worldDistortionSystem;

    // Internal \\

    @Override
    public void create() {

        // Internal \\

        this.worldDistortionSystem = create(WorldDistortionSystem.class);
    }

}
