package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.InstancePackage;

public class WorldRenderInstance extends InstancePackage {

    /*
     * Base class for all world geometry instances. Owns a DynamicPacketInstance
     * for GPU-ready geometry and holds the coordinate, world handle, and render
     * type needed for registration and disposal. Extended by ChunkInstance,
     * SubChunkInstance, and MegaChunkInstance.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;
    private WorldHandle worldHandle;
    private RenderType renderType;
    private long coordinate;
    private DynamicPacketInstance dynamicPacketInstance;

    // Internal \\

    @Override
    protected void create() {
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderSystem,
            WorldHandle worldHandle,
            RenderType renderType,
            long coordinate,
            VAOHandle vaoHandle) {

        this.worldRenderSystem = worldRenderSystem;
        this.worldHandle = worldHandle;
        this.renderType = renderType;
        this.coordinate = coordinate;
        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    // Dispose \\

    public void dispose() {
        if (renderType == RenderType.INDIVIDUAL)
            worldRenderSystem.removeChunkInstance(coordinate);
        else
            worldRenderSystem.removeMegaInstance(coordinate);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getCoordinate() {
        return coordinate;
    }

    public DynamicPacketInstance getDynamicPacketInstance() {
        return dynamicPacketInstance;
    }

    protected DynamicPacketInstance getDynamicPacket() {
        return dynamicPacketInstance;
    }
}