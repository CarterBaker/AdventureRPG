package application.bootstrap.oceanpipeline.exposure;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class ExposureInstance extends InstancePackage {

    /*
     * One grid's window of open-water exposure: a square of world-aligned
     * cells centred on the grid's reference chunk, each holding how open the
     * water around it is, from 0 in a pond to 1 on the open sea. Values ease
     * toward their targets over time and survive a window shift, so a cell
     * resolving late never pops the waves. Sampled bilinearly between cell
     * centres, exactly as OceanSurface.glsl reads the OceanData copy.
     */

    // Window
    private int originCellX;
    private int originCellZ;
    private float originXBlocks;
    private float originZBlocks;
    private boolean placed;

    // Values
    private float[] exposure;
    private float[] shiftScratch;

    // Internal \\

    @Override
    protected void create() {

        int cellCount = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE * EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;

        // Values
        this.exposure = new float[cellCount];
        this.shiftScratch = new float[cellCount];
    }

    // Constructor \\

    public void constructor() {

        this.originCellX = 0;
        this.originCellZ = 0;
        this.originXBlocks = 0f;
        this.originZBlocks = 0f;
        this.placed = false;
        Arrays.fill(exposure, Float.NaN);
    }

    // Management \\

    public void placeWindow(int originCellX, int originCellZ, float originXBlocks, float originZBlocks) {

        if (placed)
            shiftValues(originCellX - this.originCellX, originCellZ - this.originCellZ);

        this.originCellX = originCellX;
        this.originCellZ = originCellZ;
        this.originXBlocks = originXBlocks;
        this.originZBlocks = originZBlocks;
        this.placed = true;
    }

    private void shiftValues(int shiftX, int shiftZ) {

        if (shiftX == 0 && shiftZ == 0)
            return;

        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                int sourceX = x + shiftX;
                int sourceZ = z + shiftZ;
                boolean inside = sourceX >= 0 && sourceX < size && sourceZ >= 0 && sourceZ < size;

                shiftScratch[z * size + x] = inside ? exposure[sourceZ * size + sourceX] : Float.NaN;
            }
        }

        float[] swap = exposure;
        exposure = shiftScratch;
        shiftScratch = swap;
    }

    public void easeToward(int index, float target, float response) {

        float current = exposure[index];

        exposure[index] = Float.isNaN(current) ? target : current + (target - current) * response;
    }

    // Sample \\

    public float sample(float relativeX, float relativeZ) {

        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;
        float cellBlocks = EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS;
        float maxCell = size - 1;

        float cellX = Math.max(0f, Math.min(maxCell, (relativeX - originXBlocks) / cellBlocks - 0.5f));
        float cellZ = Math.max(0f, Math.min(maxCell, (relativeZ - originZBlocks) / cellBlocks - 0.5f));

        int x0 = (int) Math.floor(cellX);
        int z0 = (int) Math.floor(cellZ);
        int x1 = Math.min(x0 + 1, size - 1);
        int z1 = Math.min(z0 + 1, size - 1);
        float tx = cellX - x0;
        float tz = cellZ - z0;

        float top = getExposure(z0 * size + x0) + (getExposure(z0 * size + x1) - getExposure(z0 * size + x0)) * tx;
        float bottom = getExposure(z1 * size + x0)
                + (getExposure(z1 * size + x1) - getExposure(z1 * size + x0)) * tx;

        return top + (bottom - top) * tz;
    }

    // Accessible \\

    public int getOriginCellX() {
        return originCellX;
    }

    public int getOriginCellZ() {
        return originCellZ;
    }

    public float getOriginXBlocks() {
        return originXBlocks;
    }

    public float getOriginZBlocks() {
        return originZBlocks;
    }

    public float getExposure(int index) {

        float value = exposure[index];

        return Float.isNaN(value) ? 0f : value;
    }
}
