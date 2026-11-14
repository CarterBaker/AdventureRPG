package application.bootstrap.physicspipeline.physicsnoisemanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class PhysicsNoiseManager extends ManagerPackage {

    /*
     * Gives NaturalNoiseSystem a home inside the physics pipeline's own
     * dependency graph, since movement and collision reach it every frame,
     * and exposes its CPU-side sampling to the rest of physics: the near-ring
     * detail vector a natural top face is lifted by, and the edge warp every
     * natural vertex is drawn at, both read from the very lattice the
     * tessellation stages displace natural terrain with.
     */

    // Internal
    private NaturalNoiseSystem naturalNoiseSystem;

    // Internal \\

    @Override
    protected void create() {
        this.naturalNoiseSystem = create(NaturalNoiseSystem.class);
    }

    // Accessible \\

    // Detail and warp at a chunk-local position — see NaturalNoiseUtility.sampleFields()
    public void sampleFields(
            long chunkCoordinate,
            float localX, float localY, float localZ,
            Vector3 outDetail,
            Vector3 outWarp) {
        naturalNoiseSystem.sampleFields(chunkCoordinate, localX, localY, localZ, outDetail, outWarp);
    }

    public boolean isWithinNearTessellationRing(WorldHandle worldHandle, long entityChunkCoordinate,
            long blockChunkCoordinate) {
        return naturalNoiseSystem.isWithinNearTessellationRing(worldHandle, entityChunkCoordinate,
                blockChunkCoordinate);
    }
}