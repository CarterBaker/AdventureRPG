package application.bootstrap.physicspipeline.worlddistortionsystem;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

public class WorldDistortionSystem extends SystemPackage {

    /*
     * Generates the world-warp noise lattice once at bootstrap: one (x, y, z)
     * offset per block column, seamless across EngineSetting.CHUNK_SIZE in
     * both X and Z since every cell is hashed from its own coordinate already
     * wrapped into [0, CHUNK_SIZE) — a chunk's near-ring terrain tessellates
     * into its neighbor with no seam, the same guarantee TerrainWrapNoiseUtility
     * gives macro terrain shape, just at block-lattice granularity. Pushed
     * once to WorldDistortionData on awake — the surface shader bilinearly
     * samples that same UBO instead of hashing noise per vertex, and
     * BlockCollisionBranch reads the identical cached Vector3 per cell so a
     * natural block's collision volume is offset by precisely the amount its
     * geometry is rendered displaced by. Lives in the physics pipeline, not
     * the render pipeline, because gameplay collision is the consumer that
     * actually needs this data structurally owned outside GL-only code.
     */

    // Internal
    private UBOManager uboManager;
    private UBOHandle worldDistortionUBO;

    // Grid
    private int gridSize;
    private int gridMask;
    private Vector3[] distortionGrid;

    // Internal \\

    @Override
    protected void create() {

        this.gridSize = EngineSetting.CHUNK_SIZE;

        if ((gridSize & (gridSize - 1)) != 0)
            throwException("WorldDistortionManager requires EngineSetting.CHUNK_SIZE to be a power of two "
                    + "for seamless wraparound lookup — got " + gridSize);

        this.gridMask = gridSize - 1;
        this.distortionGrid = new Vector3[gridSize * gridSize];

        for (int i = 0; i < distortionGrid.length; i++)
            distortionGrid[i] = new Vector3();

        generateDistortionGrid();
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.worldDistortionUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.WORLD_DISTORTION_UBO);
        pushDistortionGrid();
    }

    // Generation \\

    /*
     * Every cell is hashed directly from its own (x, z) lattice coordinate,
     * already wrapped into [0, gridSize) before hashing, so cell (0, z) and
     * cell (gridSize, z) resolve to the identical hash input by construction
     * — there is no seam to hide, the field is periodic because the sample
     * domain itself never leaves one period.
     */
    private void generateDistortionGrid() {

        for (int z = 0; z < gridSize; z++) {
            for (int x = 0; x < gridSize; x++) {

                float nX = sampleLatticeHash(x, z, EngineSetting.WORLD_DISTORTION_SEED_SALT_X) - 0.5f;
                float nY = sampleLatticeHash(x, z, EngineSetting.WORLD_DISTORTION_SEED_SALT_Y) - 0.5f;
                float nZ = sampleLatticeHash(x, z, EngineSetting.WORLD_DISTORTION_SEED_SALT_Z) - 0.5f;

                distortionGrid[z * gridSize + x].set(
                        nX * EngineSetting.WORLD_DISTORTION_STRENGTH_XZ,
                        nY * EngineSetting.WORLD_DISTORTION_STRENGTH_Y,
                        nZ * EngineSetting.WORLD_DISTORTION_STRENGTH_XZ);
            }
        }
    }

    private float sampleLatticeHash(int x, int z, long seedSalt) {

        long h = seedSalt;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);

        return (h & 0xFFFFFFL) / (float) 0xFFFFFF;
    }

    // Push \\

    private void pushDistortionGrid() {

        Vector4[] packed = new Vector4[distortionGrid.length];

        for (int i = 0; i < distortionGrid.length; i++) {
            Vector3 cell = distortionGrid[i];
            packed[i] = new Vector4(cell.x, cell.y, cell.z, 0f);
        }

        worldDistortionUBO.updateUniform("u_worldDistortion", packed);
        uboManager.push(worldDistortionUBO);
    }

    // Accessible \\

    /*
     * Returns the exact same cached offset the shader bilinearly blends
     * against its neighbors for rendering — physics reads it at whole-block
     * granularity, since a block's collision volume is a single AABB with no
     * interior to interpolate across. The returned Vector3 is the live cached
     * cell — callers must treat it as read-only.
     */
    public Vector3 getDistortionOffset(int localX, int localZ) {
        int x = localX & gridMask;
        int z = localZ & gridMask;
        return distortionGrid[z * gridSize + x];
    }
}