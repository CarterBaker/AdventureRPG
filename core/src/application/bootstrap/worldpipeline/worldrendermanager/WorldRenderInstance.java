package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.InstancePackage;

public class WorldRenderInstance extends InstancePackage {

    /*
     * Base class for all world geometry instances. Owns a QuadPacketInstance
     * for its packed terrain and water records and holds the coordinate, world
     * handle, and render type needed for registration and disposal. Extended
     * by ChunkInstance, SubChunkInstance, and MegaChunkInstance.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;
    private WorldHandle worldHandle;
    private RenderType renderType;
    private long coordinate;
    private QuadPacketInstance quadPacketInstance;

    // Internal \\

    @Override
    protected void create() {
        this.quadPacketInstance = create(QuadPacketInstance.class);
    }

    // Constructor \\

    public void constructor(
            WorldRenderManager worldRenderSystem,
            WorldHandle worldHandle,
            RenderType renderType,
            long coordinate) {

        this.worldRenderSystem = worldRenderSystem;
        this.worldHandle = worldHandle;
        this.renderType = renderType;
        this.coordinate = coordinate;
        this.quadPacketInstance.clear();
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

    public QuadPacketInstance getQuadPacketInstance() {
        return quadPacketInstance;
    }

    protected QuadPacketInstance getQuadPacket() {
        return quadPacketInstance;
    }
}