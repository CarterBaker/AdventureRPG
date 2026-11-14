package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.wind.WindData;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
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

        uboManager.push(windData);
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