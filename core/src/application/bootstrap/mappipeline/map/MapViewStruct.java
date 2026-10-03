package application.bootstrap.mappipeline.map;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MapViewStruct extends StructPackage {

    /*
     * One window onto the world map, owned by whatever shows it. The owner
     * sets the world, the block at the view's centre, the zoom in blocks per
     * pixel and the size in pixels; MapManager.resolveView() answers with the
     * quads to draw this frame and the pyramid level they come from. The draw
     * list is reused frame to frame, so resolving a view allocates nothing
     * once it has grown to the most quads it needs.
     */

    // View
    private WorldHandle worldHandle;
    private double centerX;
    private double centerZ;
    private double blocksPerPixel;
    private float width;
    private float height;

    // Draws
    private final ObjectArrayList<MapDrawStruct> draws = new ObjectArrayList<>();
    private int drawCount;
    private int level;

    // Management \\

    public void set(
            WorldHandle worldHandle,
            double centerX,
            double centerZ,
            double blocksPerPixel,
            float width,
            float height) {

        this.worldHandle = worldHandle;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.blocksPerPixel = blocksPerPixel;
        this.width = width;
        this.height = height;
    }

    public void beginDraws(int level) {
        this.level = level;
        this.drawCount = 0;
    }

    public MapDrawStruct nextDraw() {

        if (drawCount == draws.size())
            draws.add(new MapDrawStruct());

        return draws.get(drawCount++);
    }

    // Conversion — view pixels, y up, to world blocks and back \\

    public double screenToWorldX(float screenX) {
        return centerX + (screenX - width * 0.5) * blocksPerPixel;
    }

    public double screenToWorldZ(float screenY) {
        return centerZ - (screenY - height * 0.5) * blocksPerPixel;
    }

    public float worldToScreenX(double worldX) {
        return (float) ((worldX - centerX) / blocksPerPixel + width * 0.5);
    }

    public float worldToScreenY(double worldZ) {
        return (float) (height * 0.5 - (worldZ - centerZ) / blocksPerPixel);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getBlocksPerPixel() {
        return blocksPerPixel;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int getDrawCount() {
        return drawCount;
    }

    public MapDrawStruct getDraw(int index) {
        return draws.get(index);
    }

    public int getLevel() {
        return level;
    }
}
