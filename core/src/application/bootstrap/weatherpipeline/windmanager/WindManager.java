package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.wind.WindData;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindManager extends ManagerPackage {

    /*
     * Owns the world's global prevailing wind, resolved once by
     * GlobalWindBranch. Local wind is computed every frame per grid by
     * LocalWindBranch, blending that global airflow with each grid's own
     * season and active weather, and lives on that grid's own WindInstance
     * — never a single shared value — so every window's location tracks
     * its own wind and sky-dome drift independently.
     */

    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;

    private GlobalWindBranch globalWindBranch;
    private LocalWindBranch localWindBranch;

    private WindHandle windHandle;

    private final Vector2 windDriftOffsetScratch = new Vector2();

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
    }

    @Override
    protected void awake() {
        globalWindBranch.assignData(windHandle);
        localWindBranch.assignGlobalWind(windHandle);
    }

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();
        localWindBranch.advanceTime(deltaTime);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];
            WindInstance windInstance = grid.getWindInstance();

            localWindBranch.updateLocalWind(windInstance, grid.getWeatherInstance(), grid.getClockInstance());
            windInstance.advanceSkyDrift(deltaTime);
            pushWindData(grid, windInstance);
        }
    }

    // GPU Push \\

    private void pushWindData(GridInstance grid, WindInstance windInstance) {

        UBOInstance windData = grid.getWindDataUBO();

        windData.updateUniform(EngineSetting.UNIFORM_WIND_DIRECTION, windInstance.getLocalWindDirection());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_SPEED, windInstance.getLocalWindSpeed());

        windDriftOffsetScratch.set((float) windInstance.getSkyDriftX(), (float) windInstance.getSkyDriftZ());
        windData.updateUniform(EngineSetting.UNIFORM_WIND_DRIFT_OFFSET, windDriftOffsetScratch);

        uboManager.push(windData);
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