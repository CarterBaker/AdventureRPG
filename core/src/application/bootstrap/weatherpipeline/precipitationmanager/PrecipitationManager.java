package application.bootstrap.weatherpipeline.precipitationmanager;

import application.bootstrap.weatherpipeline.precipitation.PrecipitationInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PrecipitationManager extends ManagerPackage {

    /*
     * Drives rain and snow for every grid. Each grid owns its own
     * PrecipitationInstance, handed out here: PrecipitationBufferSystem
     * resolves what is falling from that grid's local weather, temperature,
     * and wind, and PrecipitationOcclusionBranch keeps its map of column tops
     * current while anything is falling, so the precipitation pass can stop
     * every drop at the first block above it. Relies on WeatherPipeline
     * registering this after WeatherPatternManager and WindManager so the
     * local weather, temperature, and wind it reads are this frame's.
     */

    // Internal
    private WorldStreamManager worldStreamManager;

    // Branches
    private PrecipitationOcclusionBranch precipitationOcclusionBranch;
    private PrecipitationBufferSystem precipitationBufferSystem;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.precipitationOcclusionBranch = create(PrecipitationOcclusionBranch.class);
        this.precipitationBufferSystem = create(PrecipitationBufferSystem.class);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            updateGrid((GridInstance) elements[i]);
    }

    private void updateGrid(GridInstance grid) {

        PrecipitationInstance precipitation = grid.getPrecipitationInstance();

        precipitationBufferSystem.resolveState(grid);

        if (precipitation.isFalling())
            precipitationOcclusionBranch.updateOcclusion(grid, precipitation);

        precipitationBufferSystem.pushData(grid);
    }

    // Grid Factory \\

    public PrecipitationInstance createPrecipitationInstance() {

        PrecipitationInstance instance = create(PrecipitationInstance.class);
        instance.constructor();

        return instance;
    }
}
