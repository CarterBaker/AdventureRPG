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
     * Bakes the near-terrain surface jitter lattice once at bootstrap using
     * the exact hash formula StandardSurfaceShader.tes used to evaluate live,
     * then pushes it into NaturalNoiseData so the vertex/tessellation stages
     * read that same table instead of calling sin() themselves — see
     * NaturalNoiseUtility for the shared math both the shader and
     * sampleJitter()/isWithinNearTessellationRing() run against it.
     */

    // Internal
    private UBOManager uboManager;

    // Baked
    private float[] lattice;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        this.lattice = NaturalNoiseUtility.bakeLattice();

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.NATURAL_NOISE_UBO);
        Vector4[] packedLattice = new Vector4[EngineSetting.NATURAL_NOISE_LATTICE_VEC4_COUNT];

        for (int i = 0; i < packedLattice.length; i++) {
            int base = i * 4;
            packedLattice[i] = new Vector4(
                    latticeValueOrZero(base),
                    latticeValueOrZero(base + 1),
                    latticeValueOrZero(base + 2),
                    latticeValueOrZero(base + 3));
        }

        ubo.updateUniform("u_naturalNoiseLattice", packedLattice);
        uboManager.push(ubo);
    }

    private float latticeValueOrZero(int index) {
        return index < lattice.length ? lattice[index] : 0f;
    }

    // Sampling \\

    void sampleJitter(double worldX, double worldZ, Vector3 out) {
        NaturalNoiseUtility.sampleJitter(worldX, worldZ, lattice, out);
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