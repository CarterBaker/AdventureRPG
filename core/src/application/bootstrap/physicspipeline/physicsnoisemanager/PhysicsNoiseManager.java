package application.bootstrap.physicspipeline.physicsnoisemanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class PhysicsNoiseManager extends ManagerPackage {

    /*
     * Owns nothing physics-specific of its own — it exists purely to give
     * NaturalNoiseSystem a home inside the physics pipeline's own dependency
     * graph, since collision must be able to reach it every frame, and to
     * expose that system's CPU-side sampling API to the rest of physics.
     */

    // Internal
    private NaturalNoiseSystem naturalNoiseSystem;

    // Internal \\

    @Override
    protected void create() {
        this.naturalNoiseSystem = create(NaturalNoiseSystem.class);
    }

    // Accessible \\

    public void sampleJitter(double worldX, double worldZ, Vector3 out) {
        naturalNoiseSystem.sampleJitter(worldX, worldZ, out);
    }

    public boolean isWithinNearTessellationRing(WorldHandle worldHandle, long entityChunkCoordinate,
            long blockChunkCoordinate) {
        return naturalNoiseSystem.isWithinNearTessellationRing(worldHandle, entityChunkCoordinate,
                blockChunkCoordinate);
    }
}