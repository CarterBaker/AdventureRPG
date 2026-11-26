package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.wind.WindData;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindManager extends ManagerPackage {

    /*
     * Owns the global prevailing wind and each grid's local wind, blended per
     * frame from its season and weather by LocalWindBranch. Also pushes the
     * grid's ambient temperature into the same WindData UBO;
     * WeatherPatternManager is registered first so both are current. Gusts
     * run on their own time, which stands still while the game is paused and
     * can be held at any moment so a recording replays them.
     */

    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private ClockManager clockManager;

    private GlobalWindBranch globalWindBranch;
    private LocalWindBranch localWindBranch;

    private WindHandle windHandle;

    // Hold
    private boolean gustHeld;

    @Override
    protected void create() {

        this.globalWindBranch = create(GlobalWindBranch.class);
        this.localWindBranch = create(LocalWindBranch.class);

        this.windHandle = create(WindHandle.class);
        this.windHandle.constructor(new WindData());
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        globalWindBranch.assignData(windHandle);
        localWindBranch.assignGlobalWind(windHandle);
    }

    @Override
    protected void update() {

        float deltaTime = gustHeld || clockManager.isPaused() ? 0f : internal.getDeltaTime();
        localWindBranch.advanceTime(deltaTime);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];
            WindInstance windInstance = grid.getWindInstance();

            localWindBranch.updateLocalWind(windInstance, grid.getWeatherInstance(), grid.getClockInstance());
            pushWindData(grid, windInstance);
        }
    }

    // GPU Push \\

    private void pushWindData(GridInstance grid, WindInstance windInstance) {

        UBOInstance windData = grid.getWindDataUBO();

        windData.updateUniform(EngineSetting.UNIFORM_WIND_DIRECTION, windInstance.getLocalWindDirection());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_SPEED, windInstance.getLocalWindSpeed());
        windData.updateUniform(EngineSetting.UNIFORM_TEMPERATURE, grid.getTemperatureInstance().getTemperature());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_FOLIAGE, resolveFoliage(grid));

        uboManager.push(windData);
    }

    // How thick the trees stand about the grid's active chunk, from none to WIND_FOLIAGE_TREES and more in it and the
    // chunks around it, for the leaves the wind tears loose
    private float resolveFoliage(GridInstance grid) {

        long center = grid.getActiveChunkCoordinate();
        int centerX = Coordinate2Long.unpackX(center);
        int centerZ = Coordinate2Long.unpackY(center);
        int trees = 0;

        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(
                        Coordinate2Long.pack(centerX + dx, centerZ + dz));

                if (chunk != null)
                    trees += chunk.getTreePaletteHandle().getTrees().length;
            }

        return Math.min(1f, trees / (float) EngineSetting.WIND_FOLIAGE_TREES);
    }

    // Hold \\

    public void holdGustTime(float gustTime) {
        this.gustHeld = true;
        localWindBranch.setElapsedTime(gustTime);
    }

    public void releaseGustTime() {
        this.gustHeld = false;
    }

    public float getGustTime() {
        return localWindBranch.getElapsedTime();
    }

    // Grid Factory \\

    public WindInstance createWindInstance() {
        WindInstance instance = create(WindInstance.class);
        instance.constructor();
        return instance;
    }

    // Accessible \\

    public WindHandle getWindHandle() {
        return windHandle;
    }
}