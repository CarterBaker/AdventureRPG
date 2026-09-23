package application.bootstrap.oceanpipeline.turbulence;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class TurbulenceInstance extends InstancePackage {

    /*
     * One grid's resolved ocean turbulence field. A baseline strength from the
     * grid's own local weather sits under up to
     * OCEAN_TURBULENCE_UBO_MAX_ENTRIES cells, one per nearby weather pattern,
     * each centered in blocks relative to the grid's reference chunk exactly
     * as the shader sees its fragments. A position's strength is the weighted
     * mean of the baseline and every cell reaching it, so storms raise the
     * sea inside their own footprint and fade smoothly into calmer water
     * around them. Also holds this grid's current phase for every wave
     * component. TurbulenceManager rewrites all of it every frame and
     * TurbulenceBufferSystem mirrors it into the grid's OceanData UBO, so the
     * CPU and the GPU always read the same field.
     */

    // Baseline
    private float ambientStrength;

    // Cells
    private int cellCount;
    private float[] cellCenterX;
    private float[] cellCenterZ;
    private float[] cellRadius;
    private float[] cellWeight;
    private float[] cellStrength;

    // Waves
    private float[] wavePhases;

    // Internal \\

    @Override
    protected void create() {

        int capacity = EngineSetting.OCEAN_TURBULENCE_UBO_MAX_ENTRIES;

        // Cells
        this.cellCenterX = new float[capacity];
        this.cellCenterZ = new float[capacity];
        this.cellRadius = new float[capacity];
        this.cellWeight = new float[capacity];
        this.cellStrength = new float[capacity];

        // Waves
        this.wavePhases = new float[EngineSetting.OCEAN_WAVE_COUNT];
    }

    // Constructor \\

    public void constructor() {

        this.ambientStrength = 0f;
        this.cellCount = 0;
        Arrays.fill(wavePhases, 0f);
    }

    // Management \\

    public void beginField(float ambientStrength) {
        this.ambientStrength = ambientStrength;
        this.cellCount = 0;
    }

    public boolean addCell(float centerX, float centerZ, float radius, float weight, float strength) {

        if (cellCount >= cellCenterX.length)
            return false;

        cellCenterX[cellCount] = centerX;
        cellCenterZ[cellCount] = centerZ;
        cellRadius[cellCount] = radius;
        cellWeight[cellCount] = weight;
        cellStrength[cellCount] = strength;
        cellCount++;

        return true;
    }

    public void setWavePhase(int waveIndex, float phase) {
        wavePhases[waveIndex] = phase;
    }

    // Sample \\

    public float sampleStrength(float relativeX, float relativeZ) {

        float weightedStrength = ambientStrength;
        float totalWeight = 1f;

        for (int i = 0; i < cellCount; i++) {

            float dx = relativeX - cellCenterX[i];
            float dz = relativeZ - cellCenterZ[i];
            float t = 1f - (float) Math.sqrt(dx * dx + dz * dz) / cellRadius[i];

            if (t <= 0f)
                continue;

            float influence = t * t * (3f - 2f * t) * cellWeight[i];

            weightedStrength += cellStrength[i] * influence;
            totalWeight += influence;
        }

        return weightedStrength / totalWeight;
    }

    public float sampleWaveAmplitudeBlocks(float relativeX, float relativeZ) {
        return Math.min(
                sampleStrength(relativeX, relativeZ) * EngineSetting.OCEAN_WAVE_AMPLITUDE_PER_TURBULENCE_BLOCKS,
                EngineSetting.OCEAN_WAVE_MAX_AMPLITUDE_BLOCKS);
    }

    // Accessible \\

    public float getAmbientStrength() {
        return ambientStrength;
    }

    public int getCellCount() {
        return cellCount;
    }

    public float getCellCenterX(int index) {
        return cellCenterX[index];
    }

    public float getCellCenterZ(int index) {
        return cellCenterZ[index];
    }

    public float getCellRadius(int index) {
        return cellRadius[index];
    }

    public float getCellWeight(int index) {
        return cellWeight[index];
    }

    public float getCellStrength(int index) {
        return cellStrength[index];
    }

    public float getWavePhase(int waveIndex) {
        return wavePhases[waveIndex];
    }
}
