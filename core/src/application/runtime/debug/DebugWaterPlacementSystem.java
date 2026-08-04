package application.runtime.debug;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.runtime.input.InputSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

public class DebugWaterPlacementSystem extends SystemPackage {

    /*
     * Debug-only tool for exercising the liquid pipeline: a right click drops
     * a Water block wherever the hovered window's player is aiming, reusing
     * the same raycast-and-rebuild plumbing as real block placement but
     * skipping inventory/tool rules entirely. Everything this needs lives in
     * the runtime layer — no bootstrap class had to change to add it. Flip
     * ENABLED to false, or remove the create() call in RuntimeContext, to
     * take this out entirely; that is the whole undo path.
     */

    private static final boolean ENABLED = true;
    private static final String WATER_BLOCK_NAME = "TerraArcanaBlocks/Water";

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputSystem;
    private RaycastManager raycastManager;
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Settings
    private int chunkSize;
    private int worldHeight;

    // Block ID
    private short waterBlockID;

    // Scratch
    private final BlockCastStruct castStruct = new BlockCastStruct();
    private final Vector3 eyePosition = new Vector3();
    private final Vector3 eyeOffset = new Vector3();

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Internal
        this.playerManager = get(PlayerManager.class);
        this.inputSystem = get(InputSystem.class);
        this.raycastManager = get(RaycastManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.waterBlockID = (short) blockManager.getBlockIDFromBlockName(WATER_BLOCK_NAME);
    }

    // Update \\

    @Override
    protected void update() {

        if (!ENABLED)
            return;

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        if (context.getWindow().getMenuListHandle().isInputLocked())
            return;

        if (!inputSystem.getRawInputHandle().isBindingClicked(KeyBindings.SECONDARY))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        if (camera == null)
            return;

        if (!raycastToTarget(player, camera))
            return;

        placeWaterBlock();
    }

    // Raycast \\

    private boolean raycastToTarget(EntityInstance player, CameraInstance camera) {

        eyeOffset.set(player.getSize().x / 2, player.getEyeHeight(), player.getSize().z / 2);
        eyePosition.set(player.getWorldPositionStruct().getPosition());
        eyePosition.add(eyeOffset);

        float reach = player.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;

        raycastManager.castBlock(
                player.getWorldPositionStruct().getChunkCoordinate(),
                eyePosition,
                camera.getDirection(),
                reach,
                castStruct);

        return castStruct.isHit();
    }

    // Placement \\

    private void placeWaterBlock() {

        Direction3Vector hitFace = castStruct.getHitFace();

        int placeX = castStruct.getBlockX() + hitFace.x;
        int placeY = castStruct.getBlockY() + hitFace.y;
        int placeZ = castStruct.getBlockZ() + hitFace.z;
        int placeSubChunkY = castStruct.getSubChunkY();

        int placeChunkX = Coordinate2Long.unpackX(castStruct.getChunkCoordinate());
        int placeChunkZ = Coordinate2Long.unpackY(castStruct.getChunkCoordinate());

        if (placeX < 0) {
            placeChunkX--;
            placeX += chunkSize;
        } else if (placeX >= chunkSize) {
            placeChunkX++;
            placeX -= chunkSize;
        }

        if (placeZ < 0) {
            placeChunkZ--;
            placeZ += chunkSize;
        } else if (placeZ >= chunkSize) {
            placeChunkZ++;
            placeZ -= chunkSize;
        }

        if (placeY < 0) {
            placeSubChunkY--;
            placeY += chunkSize;
        } else if (placeY >= chunkSize) {
            placeSubChunkY++;
            placeY -= chunkSize;
        }

        if (placeSubChunkY < 0 || placeSubChunkY >= worldHeight)
            return;

        long placeChunkCoord = Coordinate2Long.pack(placeChunkX, placeChunkZ);
        ChunkInstance placeChunk = worldStreamManager.getChunkInstance(placeChunkCoord);

        if (placeChunk == null)
            return;

        SubChunkInstance subChunk = placeChunk.getSubChunk(placeSubChunkY);

        if (subChunk == null)
            return;

        subChunk.getBlockPaletteHandle().setBlock(placeX, placeY, placeZ, waterBlockID);

        rebuildAffected(placeChunk, placeChunkCoord, placeX, placeY, placeZ, placeSubChunkY);
    }

    // Rebuild \\

    private void rebuildAffected(
            ChunkInstance chunk,
            long chunkCoordinate,
            int blockX, int blockY, int blockZ,
            int subChunkY) {

        rebuildSubChunk(chunk, subChunkY);

        if (blockY == 0 && subChunkY > 0)
            rebuildSubChunk(chunk, subChunkY - 1);

        if (blockY == chunkSize - 1 && subChunkY < worldHeight - 1)
            rebuildSubChunk(chunk, subChunkY + 1);

        mergeAndRender(chunk, chunkCoordinate);

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        if (blockX == 0)
            rebuildNeighbour(chunkX - 1, chunkZ, subChunkY);

        if (blockX == chunkSize - 1)
            rebuildNeighbour(chunkX + 1, chunkZ, subChunkY);

        if (blockZ == 0)
            rebuildNeighbour(chunkX, chunkZ - 1, subChunkY);

        if (blockZ == chunkSize - 1)
            rebuildNeighbour(chunkX, chunkZ + 1, subChunkY);
    }

    private void rebuildSubChunk(ChunkInstance chunk, int subChunkY) {
        chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
    }

    private void rebuildNeighbour(int chunkX, int chunkZ, int subChunkY) {

        long coord = Coordinate2Long.pack(chunkX, chunkZ);
        ChunkInstance neighbour = worldStreamManager.getChunkInstance(coord);

        if (neighbour == null)
            return;

        rebuildSubChunk(neighbour, subChunkY);
        mergeAndRender(neighbour, coord);
    }

    private void mergeAndRender(ChunkInstance chunk, long chunkCoordinate) {
        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        worldStreamManager.invalidateChunkBatch(chunkCoordinate);
    }
}