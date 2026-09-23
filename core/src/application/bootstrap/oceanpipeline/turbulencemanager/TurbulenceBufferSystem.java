package application.bootstrap.oceanpipeline.turbulencemanager;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TurbulenceBufferSystem extends SystemPackage {

    /*
     * Mirrors each grid's TurbulenceInstance and the live tide into that
     * grid's own OceanData UBO every frame: the wave set with this grid's
     * phases, every turbulence cell with its strength packed four to a
     * vector, and the surface height, tide offset, baseline turbulence, and
     * wrapped clock the water shader animates against. Runs in LATE_UPDATE
     * for the same reason WeatherMapBufferSystem does — a grid's reference
     * chunk is only final once FIXED_UPDATE has moved the player, and every
     * position in the buffer is relative to it.
     */

    // Internal
    private TurbulenceManager turbulenceManager;
    private TideManager tideManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;

    // Buffer
    private Vector4[] waves;
    private Vector4[] cells;
    private Vector4[] strengths;
    private Vector4 surface;
    private Vector2 waveScale;

    // Base \\

    @Override
    protected void create() {

        // Buffer
        this.waves = allocate(EngineSetting.OCEAN_WAVE_COUNT);
        this.cells = allocate(EngineSetting.OCEAN_TURBULENCE_UBO_MAX_ENTRIES);
        this.strengths = allocate(
                EngineSetting.OCEAN_TURBULENCE_UBO_MAX_ENTRIES / EngineSetting.OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR);
        this.surface = new Vector4();
        this.waveScale = new Vector2(
                EngineSetting.OCEAN_WAVE_AMPLITUDE_PER_TURBULENCE_BLOCKS,
                EngineSetting.OCEAN_WAVE_MAX_AMPLITUDE_BLOCKS);
    }

    @Override
    protected void get() {

        // Internal
        this.turbulenceManager = get(TurbulenceManager.class);
        this.tideManager = get(TideManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
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

        writeWaves(turbulence);
        int cellCount = writeCells(turbulence);

        surface.set(
                tideManager.getSurfaceHeightBlocks(),
                tideManager.getTideOffsetBlocks(),
                turbulence.getAmbientStrength(),
                (float) (turbulenceManager.getElapsedSeconds() % EngineSetting.OCEAN_WAVE_TIME_WRAP_SECONDS));

        UBOInstance oceanDataUBO = grid.getOceanDataUBO();

        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_WAVES, waves);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_CELLS, cells);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_STRENGTHS, strengths);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_SURFACE, surface);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_WAVE_SCALE, waveScale);
        oceanDataUBO.updateUniform(EngineSetting.UNIFORM_OCEAN_TURBULENCE_COUNT, cellCount);

        uboManager.push(oceanDataUBO);
    }

    private void writeWaves(TurbulenceInstance turbulence) {

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++)
            waves[i].set(
                    turbulenceManager.getWaveVectorX(i),
                    turbulenceManager.getWaveVectorZ(i),
                    turbulence.getWavePhase(i),
                    turbulenceManager.getWaveAmplitudeShare(i));
    }

    private int writeCells(TurbulenceInstance turbulence) {

        int cellCount = turbulence.getCellCount();

        for (int i = 0; i < cellCount; i++) {

            cells[i].set(
                    turbulence.getCellCenterX(i),
                    turbulence.getCellCenterZ(i),
                    turbulence.getCellRadius(i),
                    turbulence.getCellWeight(i));

            setPackedStrength(i, turbulence.getCellStrength(i));
        }

        return cellCount;
    }

    private void setPackedStrength(int cellIndex, float strength) {

        Vector4 packed = strengths[cellIndex / EngineSetting.OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR];

        switch (cellIndex % EngineSetting.OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR) {
            case 0 -> packed.x = strength;
            case 1 -> packed.y = strength;
            case 2 -> packed.z = strength;
            default -> packed.w = strength;
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
