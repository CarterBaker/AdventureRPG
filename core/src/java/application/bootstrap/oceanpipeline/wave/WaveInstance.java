package application.bootstrap.oceanpipeline.wave;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class WaveInstance extends InstancePackage {

    /*
     * One grid's view of the shared wave set: every wave's phase with the
     * grid's reference chunk and the ocean clock folded in, the sea-state
     * noise lattice offset for the same reference, and whether this grid's
     * camera is under the water. Rewritten by WaveManager each frame and
     * mirrored into the grid's OceanData UBO.
     */

    // Waves
    private float[] wavePhases;

    // Sea Noise
    private float noiseOffsetX;
    private float noiseOffsetZ;

    // Camera
    private boolean cameraSubmerged;
    private float cameraSurfaceHeightBlocks;
    private float cameraDepthBlocks;

    // Internal \\

    @Override
    protected void create() {

        // Waves
        this.wavePhases = new float[EngineSetting.OCEAN_WAVE_COUNT];
    }

    // Constructor \\

    public void constructor() {

        Arrays.fill(wavePhases, 0f);
        this.noiseOffsetX = 0f;
        this.noiseOffsetZ = 0f;
        this.cameraSubmerged = false;
        this.cameraSurfaceHeightBlocks = 0f;
        this.cameraDepthBlocks = 0f;
    }

    // Management \\

    public void setWavePhase(int waveIndex, float phase) {
        wavePhases[waveIndex] = phase;
    }

    public void setNoiseOffset(float noiseOffsetX, float noiseOffsetZ) {
        this.noiseOffsetX = noiseOffsetX;
        this.noiseOffsetZ = noiseOffsetZ;
    }

    public void setCamera(boolean cameraSubmerged, float cameraSurfaceHeightBlocks, float cameraDepthBlocks) {
        this.cameraSubmerged = cameraSubmerged;
        this.cameraSurfaceHeightBlocks = cameraSurfaceHeightBlocks;
        this.cameraDepthBlocks = cameraDepthBlocks;
    }

    // Accessible \\

    public float getWavePhase(int waveIndex) {
        return wavePhases[waveIndex];
    }

    public float getNoiseOffsetX() {
        return noiseOffsetX;
    }

    public float getNoiseOffsetZ() {
        return noiseOffsetZ;
    }

    public boolean isCameraSubmerged() {
        return cameraSubmerged;
    }

    public float getCameraSurfaceHeightBlocks() {
        return cameraSurfaceHeightBlocks;
    }

    public float getCameraDepthBlocks() {
        return cameraDepthBlocks;
    }
}
