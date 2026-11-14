package application.bootstrap.oceanpipeline.wavemanager;

import application.bootstrap.oceanpipeline.exposure.ExposureInstance;
import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.oceanpipeline.turbulencemanager.TurbulenceManager;
import application.bootstrap.oceanpipeline.wave.WaveInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleHullMaskSystem;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WaveBufferSystem extends SystemPackage {

    /*
     * Mirrors everything the water shaders need into each grid's own OceanData
     * UBO every frame: the wave set with this grid's phases, the turbulence
     * cells with their strengths packed four to a vector, the exposure window
     * packed the same way, the sea-state noise lattice, the surface, the
     * camera's submersion and the masks VehicleHullMaskSystem writes to keep
     * the sea out of hulls. Every tuning value the CPU sampler also reads is
     * sent from EngineSetting here, so the shader never carries its own copy.
     * Runs in LATE_UPDATE after WaveManager, for the same reason
     * WeatherMapBufferSystem does: every position is relative to a reference
     * chunk that is only final once FIXED_UPDATE has moved the player.
     */

    // Internal
    private WaveManager waveManager;
    private TurbulenceManager turbulenceManager;
    private TideManager tideManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private VehicleHullMaskSystem vehicleHullMaskSystem;

    // Buffer
    private Vector4[] waves;
    private Vector4[] cells;
    private Vector4[] strengths;
    private Vector4[] exposure;
    private Vector4 surface;
    private Vector4 waveScale;
    private Vector4 seaState;
    private Vector4 whitecap;
    private Vector4 noise;
    private Vector4 noiseShape;
    private Vector2 noisePeriod;
    private Vector4 exposureGrid;
    private Vector4 camera;
    private Vector4 tessellation;
    private Vector4[] hulls;

    // Base \\

    @Override
    protected void create() {

        // Buffer
        this.waves = allocate(EngineSetting.OCEAN_WAVE_COUNT);
        this.cells = allocate(EngineSetting.OCEAN_TURBULENCE_UBO_MAX_ENTRIES);
        this.strengths = allocate(
                EngineSetting.OCEAN_TURBULENCE_UBO_MAX_ENTRIES / EngineSetting.OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR);
        this.exposure = allocate(EngineSetting.OCEAN_EXPOSURE_GRID_SIZE * EngineSetting.OCEAN_EXPOSURE_GRID_SIZE
                / EngineSetting.OCEAN_EXPOSURE_VALUES_PER_VECTOR);
        this.surface = new Vector4();
        this.waveScale = new Vector4(
                EngineSetting.OCEAN_WAVE_CHOP_AMPLITUDE_PER_SEA_STATE,
                EngineSetting.OCEAN_WAVE_CHOP_MAX_AMPLITUDE_BLOCKS,
                EngineSetting.OCEAN_WAVE_SWELL_AMPLITUDE_PER_SEA_STATE,
                EngineSetting.OCEAN_WAVE_SWELL_MAX_AMPLITUDE_BLOCKS);
        this.seaState = new Vector4();
        this.whitecap = new Vector4(
                EngineSetting.OCEAN_WHITECAP_SEA_STATE_START,
                EngineSetting.OCEAN_WHITECAP_SEA_STATE_FULL,
                EngineSetting.OCEAN_WHITECAP_CREST_START,
                EngineSetting.OCEAN_WAVE_AMPLITUDE_EPSILON);
        this.noise = new Vector4();
        this.noiseShape = new Vector4(
                EngineSetting.OCEAN_SEA_NOISE_MIN,
                EngineSetting.OCEAN_SEA_NOISE_MAX,
                EngineSetting.OCEAN_SEA_NOISE_CONTRAST,
                EngineSetting.OCEAN_SEA_NOISE_DETAIL_WEIGHT);
        this.noisePeriod = new Vector2();
        this.exposureGrid = new Vector4();
        this.camera = new Vector4();
        this.tessellation = new Vector4(
                EngineSetting.OCEAN_TESSELLATION_NEAR_RADIUS_CHUNKS,
                EngineSetting.OCEAN_TESSELLATION_MID_RADIUS_CHUNKS,
                EngineSetting.OCEAN_TESSELLATION_FAR_RADIUS_CHUNKS,
                EngineSetting.OCEAN_TESSELLATION_FADE_CHUNKS);
        this.hulls = allocate(EngineSetting.OCEAN_HULL_MAX_ENTRIES * EngineSetting.OCEAN_HULL_VECTORS_PER_ENTRY);
    }

    @Override
    protected void get() {

        // Internal
        this.waveManager = get(WaveManager.class);
        this.turbulenceManager = get(TurbulenceManager.class);
        this.tideManager = get(TideManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.vehicleHullMaskSystem = get(VehicleHullMaskSystem.class);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            pushGrid((GridInstance) elements[i]);
    }

    private void pushGrid(GridInstance grid) {

        TurbulenceInstance turbulence = grid.getTurbulenceInstance();
        WaveInstance wave = grid.getWaveInstance();

        writeWaves(wave);
        writeExposure(grid.getExposureInstance());
        int cellCount = writeCells(turbulence);
        int hullCount = vehicleHullMaskSystem.writeHullMasks(grid, hulls);

        surface.set(
                tideManager.getSurfaceHeightBlocks(),
                tideManager.getTideOffsetBlocks(),
                turbulence.getAmbientStrength(),
                (float) (turbulenceManager.getElapsedSeconds() % EngineSetting.OCEAN_WAVE_TIME_WRAP_SECONDS));
        seaState.set(
                EngineSetting.OCEAN_SEA_STATE_CALM,
                EngineSetting.OCEAN_WAVE_SWELL_SEA_STATE_START,
                EngineSetting.OCEAN_WAVE_SHARPNESS,
                waveManager.getShapeMean());
        noise.set(
                wave.getNoiseOffsetX(),
                wave.getNoiseOffsetZ(),
                waveManager.getNoiseCellsPerBlockX(),
                waveManager.getNoiseCellsPerBlockZ());
        noisePeriod.set(waveManager.getNoisePeriodX(), waveManager.getNoisePeriodZ());
        camera.set(
                wave.isCameraSubmerged() ? 1f : 0f,
                wave.getCameraSurfaceHeightBlocks(),
                wave.getCameraDepthBlocks(),
                0f);

        UBOInstance oceanDataUBO = grid.getOceanDataUBO();

        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_WAVES, waves);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_CELLS, cells);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_STRENGTHS, strengths);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_EXPOSURE, exposure);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_SURFACE, surface);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_WAVE_SCALE, waveScale);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_SEA_STATE, seaState);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_WHITECAP, whitecap);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_NOISE, noise);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_NOISE_SHAPE, noiseShape);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_EXPOSURE_GRID, exposureGrid);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_CAMERA, camera);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TESSELLATION, tessellation);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_NOISE_PERIOD, noisePeriod);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_COUNT, cellCount);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_HULLS, hulls);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_HULL_COUNT, hullCount);

        uboManager.push(oceanDataUBO);
    }

    private void writeWaves(WaveInstance wave) {

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++)
            waves[i].set(
                    waveManager.getWaveVectorX(i),
                    waveManager.getWaveVectorZ(i),
                    wave.getWavePhase(i),
                    waveManager.getWaveShare(i));
    }

    private void writeExposure(ExposureInstance exposureInstance) {

        int valueCount = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE * EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;

        for (int i = 0; i < valueCount; i++)
            setPacked(exposure, i, EngineSetting.OCEAN_EXPOSURE_VALUES_PER_VECTOR, exposureInstance.getExposure(i));

        exposureGrid.set(
                exposureInstance.getOriginXBlocks(),
                exposureInstance.getOriginZBlocks(),
                EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS,
                EngineSetting.OCEAN_EXPOSURE_GRID_SIZE);
    }

    private int writeCells(TurbulenceInstance turbulence) {

        int cellCount = turbulence.getCellCount();

        for (int i = 0; i < cellCount; i++) {

            cells[i].set(
                    turbulence.getCellCenterX(i),
                    turbulence.getCellCenterZ(i),
                    turbulence.getCellRadius(i),
                    turbulence.getCellWeight(i));

            setPacked(strengths, i, EngineSetting.OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR, turbulence.getCellStrength(i));
        }

        return cellCount;
    }

    private void setPacked(Vector4[] packedArray, int valueIndex, int valuesPerVector, float value) {

        Vector4 packed = packedArray[valueIndex / valuesPerVector];

        switch (valueIndex % valuesPerVector) {
            case 0 -> packed.x = value;
            case 1 -> packed.y = value;
            case 2 -> packed.z = value;
            default -> packed.w = value;
        }
    }

    // Utility \\

    private static Vector4[] allocate(int size) {

        Vector4[] array = new Vector4[size];

        for (int i = 0; i < size; i++)
            array[i] = new Vector4();

        return array;
    }
}
