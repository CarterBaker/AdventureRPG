package application.bootstrap.physicspipeline.physicsnoisemanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NaturalNoiseUtility;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

class NaturalNoiseSystem extends SystemPackage {

    /*
     * Bakes the natural noise lattice once at bootstrap and pushes it into
     * NaturalNoiseData, four channels to a vector exactly as the CPU stores
     * them, so the tessellation stages and physics read one table. Samples
     * the near-ring detail and the edge warp at any chunk-local position from
     * that same table: the detail lifts a natural top face, the warp bends
     * every natural edge, and both reach physics through PhysicsNoiseManager
     * so ground and walls sit where they are drawn.
     */

    // Internal
    private UBOManager uboManager;

    // Baked
    private float[] lattice;

    // Scratch
    private float[] channelScratch;

    // Internal \\

    @Override
    protected void create() {
        this.channelScratch = new float[EngineSetting.NATURAL_NOISE_CHANNELS * EngineSetting.NATURAL_NOISE_PLANES];
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        this.lattice = NaturalNoiseUtility.bakeLattice();

        int channels = EngineSetting.NATURAL_NOISE_CHANNELS;
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.NATURAL_NOISE_UBO);
        Vector4[] packedLattice = new Vector4[EngineSetting.NATURAL_NOISE_LATTICE_SIZE];

        for (int i = 0; i < packedLattice.length; i++) {
            int base = i * channels;
            packedLattice[i] = new Vector4(
                    lattice[base],
                    lattice[base + 1],
                    lattice[base + 2],
                    lattice[base + 3]);
        }

        ubo.updateUniform(EngineSetting.UNIFORM_NATURAL_NOISE_LATTICE, packedLattice);
        uboManager.push(ubo);
    }

    // Sampling \\

    void sampleFields(
            long chunkCoordinate,
            float localX, float localY, float localZ,
            Vector3 outDetail,
            Vector3 outWarp) {

        NaturalNoiseUtility.sampleFields(
                lattice,
                (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE + localX,
                localY,
                (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE + localZ,
                channelScratch,
                outDetail,
                outWarp);
    }

    boolean isWithinNearTessellationRing(WorldHandle worldHandle, long entityChunkCoordinate,
            long blockChunkCoordinate) {

        double deltaX = WorldWrapUtility.wrappedDeltaX(
                worldHandle,
                Coordinate2Long.unpackX(blockChunkCoordinate),
                Coordinate2Long.unpackX(entityChunkCoordinate));
        double deltaZ = WorldWrapUtility.wrappedDeltaZ(
                worldHandle,
                Coordinate2Long.unpackY(blockChunkCoordinate),
                Coordinate2Long.unpackY(entityChunkCoordinate));

        float distanceSqChunks = (float) (deltaX * deltaX + deltaZ * deltaZ);

        float tier0MaxSqDist = NaturalNoiseUtility.getTier0MaxSqDistChunks(
                (float) settings.nearTessellationRadius,
                (float) settings.maxRenderDistance,
                (float) EngineSetting.CHUNK_SIZE);

        return distanceSqChunks <= tier0MaxSqDist;
    }
}
