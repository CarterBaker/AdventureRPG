package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.temperature.TemperatureInstance;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherPatternManager extends ManagerPackage {

    /*
     * Owns the live weather pattern — the pixels of the static weather image
     * that any grid can currently see. The image is divided into cells one
     * world-map pixel wide, fixed in noise space, and WeatherManager's flow
     * slides the whole image across the world every frame, so a storm keeps
     * its shape and reaches every player beneath its path in turn. Each grid
     * reads a square window of cells centred above it; cells enter the pool
     * as a window reaches them, leave once no window holds them, and are
     * re-resolved a few per frame so a cell whose biome changes beneath it
     * cross-fades to its new weather. Each grid also keeps one local
     * WeatherInstance that follows the cell overhead, feeding wind,
     * temperature, and ocean turbulence.
     */

    // Internal
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private TemperatureSystem temperatureSystem;

    // Map Geometry
    private WorldHandle mapWorld;
    private int mapResolution;
    private double cellSizeBlocks;
    private double worldWidthBlocks;
    private double worldHeightBlocks;
    private int worldCellCountX;
    private int worldCellCountZ;
    private double shapePeriodBlocks;

    // Cells
    private Long2ObjectOpenHashMap<WeatherInstance> cellKey2WeatherInstance;
    private ObjectArrayList<WeatherInstance> activeCells;
    private ObjectArrayList<WeatherInstance> freeCells;
    private int resolveCursor;
    private long frameIndex;

    // Scratch
    private final WeatherWindowStruct windowScratch = new WeatherWindowStruct();

    // Base \\

    @Override
    protected void create() {

        // Map Geometry
        this.mapResolution = EngineSetting.WEATHER_MAP_RESOLUTION;
        this.cellSizeBlocks = (double) EngineSetting.WEATHER_CELL_SIZE_CHUNKS * EngineSetting.CHUNK_SIZE;

        // Cells
        int retainedSpan = mapResolution + EngineSetting.WEATHER_MAP_RETAIN_MARGIN_CELLS * 2;
        int initialCapacity = retainedSpan * retainedSpan;

        this.cellKey2WeatherInstance = new Long2ObjectOpenHashMap<>(initialCapacity);
        this.activeCells = new ObjectArrayList<>(initialCapacity);
        this.freeCells = new ObjectArrayList<>(initialCapacity);

        for (int i = 0; i < initialCapacity; i++)
            freeCells.add(create(WeatherInstance.class));

        this.temperatureSystem = create(TemperatureSystem.class);
        create(WeatherMapBufferSystem.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {

        if (!weatherManager.hasActiveWeatherPool())
            return;

        float deltaTime = internal.getDeltaTime();
        frameIndex++;

        resolveMapGeometry();

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int gridCount = grids.size();

        for (int i = 0; i < gridCount; i++)
            retainWindow((GridInstance) elements[i]);

        releaseCells();
        refreshCells();
        advanceCellTransitions(deltaTime);
        advanceLocalWeather(grids, deltaTime);
    }

    // Map Geometry \\

    private void resolveMapGeometry() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == mapWorld)
            return;

        releaseAllCells();

        this.mapWorld = activeWorld;
        this.worldWidthBlocks = activeWorld.getWorldScale().x;
        this.worldHeightBlocks = activeWorld.getWorldScale().y;
        this.worldCellCountX = Math.max(1, (int) Math.round(worldWidthBlocks / cellSizeBlocks));
        this.worldCellCountZ = Math.max(1, (int) Math.round(worldHeightBlocks / cellSizeBlocks));
        this.shapePeriodBlocks = cellSizeBlocks * resolveShapePeriodCells(worldCellCountX, worldCellCountZ);
    }

    // The cloud shape noise tiles at this period, so it has to divide both
    // world axes exactly or the shapes would seam where the world wraps.
    private int resolveShapePeriodCells(int cellCountX, int cellCountZ) {

        int divisor = greatestCommonDivisor(cellCountX, cellCountZ);

        for (int period = Math.min(divisor, EngineSetting.WEATHER_MAP_SHAPE_PERIOD_MAX_CELLS); period > 1; period--)
            if (divisor % period == 0)
                return period;

        return 1;
    }

    private int greatestCommonDivisor(int a, int b) {

        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }

        return a;
    }

    // Window \\

    public void resolveWindow(GridInstance grid, WeatherWindowStruct out) {

        long referenceCoordinate = grid.getActiveChunkCoordinate();

        double noiseXBlocks = wrap(
                (double) Coordinate2Long.unpackX(referenceCoordinate) * EngineSetting.CHUNK_SIZE
                        - weatherManager.getFlowOffsetXBlocks(),
                worldWidthBlocks);
        double noiseZBlocks = wrap(
                (double) Coordinate2Long.unpackY(referenceCoordinate) * EngineSetting.CHUNK_SIZE
                        - weatherManager.getFlowOffsetZBlocks(),
                worldHeightBlocks);

        int originCellX = (int) Math.floor(noiseXBlocks / cellSizeBlocks) - mapResolution / 2;
        int originCellZ = (int) Math.floor(noiseZBlocks / cellSizeBlocks) - mapResolution / 2;

        out.set(
                originCellX,
                originCellZ,
                (float) (noiseXBlocks - originCellX * cellSizeBlocks),
                (float) (noiseZBlocks - originCellZ * cellSizeBlocks));
    }

    public void resolveShapeOrigin(GridInstance grid, float driftSpeedScale, Vector2 out) {

        long referenceCoordinate = grid.getActiveChunkCoordinate();

        double shapeXBlocks = (double) Coordinate2Long.unpackX(referenceCoordinate) * EngineSetting.CHUNK_SIZE
                - weatherManager.getFlowOffsetXBlocks() * driftSpeedScale;
        double shapeZBlocks = (double) Coordinate2Long.unpackY(referenceCoordinate) * EngineSetting.CHUNK_SIZE
                - weatherManager.getFlowOffsetZBlocks() * driftSpeedScale;

        out.set((float) wrap(shapeXBlocks, shapePeriodBlocks), (float) wrap(shapeZBlocks, shapePeriodBlocks));
    }

    private void retainWindow(GridInstance grid) {

        resolveWindow(grid, windowScratch);

        int margin = EngineSetting.WEATHER_MAP_RETAIN_MARGIN_CELLS;

        for (int z = -margin; z < mapResolution + margin; z++) {
            for (int x = -margin; x < mapResolution + margin; x++) {

                int cellX = Math.floorMod(windowScratch.getOriginCellX() + x, worldCellCountX);
                int cellZ = Math.floorMod(windowScratch.getOriginCellZ() + z, worldCellCountZ);
                long cellKey = Coordinate2Long.pack(cellX, cellZ);

                WeatherInstance cell = cellKey2WeatherInstance.get(cellKey);

                if (cell == null)
                    cell = acquireCell(cellKey, cellX, cellZ);

                cell.retain(frameIndex);
            }
        }
    }

    // Management \\

    private WeatherInstance acquireCell(long cellKey, int cellX, int cellZ) {

        WeatherInstance cell = freeCells.isEmpty()
                ? create(WeatherInstance.class)
                : freeCells.remove(freeCells.size() - 1);

        double cellSizeChunks = EngineSetting.WEATHER_CELL_SIZE_CHUNKS;
        float noisePercentile = weatherManager.sampleNoisePercentile(
                (cellX + 0.5) * cellSizeChunks,
                (cellZ + 0.5) * cellSizeChunks);

        cell.assignCell(cellKey, cellX, cellZ, noisePercentile);
        cell.constructor(resolveCellWeather(cell));

        cellKey2WeatherInstance.put(cellKey, cell);
        activeCells.add(cell);

        return cell;
    }

    private void releaseCells() {

        for (int i = activeCells.size() - 1; i >= 0; i--) {

            WeatherInstance cell = activeCells.get(i);

            if (cell.getRetainedFrame() == frameIndex)
                continue;

            int last = activeCells.size() - 1;
            activeCells.set(i, activeCells.get(last));
            activeCells.remove(last);

            cellKey2WeatherInstance.remove(cell.getCellKey());
            freeCells.add(cell);
        }
    }

    private void releaseAllCells() {
        freeCells.addAll(activeCells);
        activeCells.clear();
        cellKey2WeatherInstance.clear();
    }

    // Update \\

    private void refreshCells() {

        int size = activeCells.size();

        if (size == 0)
            return;

        int count = Math.min(EngineSetting.WEATHER_CELL_RESOLVES_PER_FRAME, size);

        for (int i = 0; i < count; i++) {

            resolveCursor = (resolveCursor + 1) % size;

            WeatherInstance cell = activeCells.get(resolveCursor);
            WeatherHandle resolved = resolveCellWeather(cell);

            if (resolved != cell.getWeatherHandle())
                cell.beginWeatherTransition(resolved);
        }
    }

    private WeatherHandle resolveCellWeather(WeatherInstance cell) {

        double worldXBlocks = wrap(
                (cell.getCellX() + 0.5) * cellSizeBlocks + weatherManager.getFlowOffsetXBlocks(), worldWidthBlocks);
        double worldZBlocks = wrap(
                (cell.getCellZ() + 0.5) * cellSizeBlocks + weatherManager.getFlowOffsetZBlocks(), worldHeightBlocks);

        long worldChunkCoordinate = Coordinate2Long.pack(
                (int) Math.floor(worldXBlocks / EngineSetting.CHUNK_SIZE),
                (int) Math.floor(worldZBlocks / EngineSetting.CHUNK_SIZE));

        return weatherManager.resolveWeather(worldChunkCoordinate, cell.getNoisePercentile());
    }

    private void advanceCellTransitions(float deltaTime) {

        Object[] elements = activeCells.elements();
        int size = activeCells.size();

        for (int i = 0; i < size; i++)
            ((WeatherInstance) elements[i]).advanceWeatherTransition(deltaTime);
    }

    // Local Weather \\

    private void advanceLocalWeather(ObjectArrayList<GridInstance> grids, float deltaTime) {

        temperatureSystem.advanceClock();

        Object[] elements = grids.elements();
        int count = grids.size();
        int centerIndex = mapResolution / 2;

        for (int i = 0; i < count; i++) {

            GridInstance grid = (GridInstance) elements[i];
            WeatherInstance localWeather = grid.getWeatherInstance();

            resolveWindow(grid, windowScratch);
            WeatherHandle overheadWeather = getCell(windowScratch, centerIndex, centerIndex).getWeatherHandle();

            if (!localWeather.isConfigured())
                localWeather.constructor(overheadWeather);
            else {
                localWeather.advanceWeatherTransition(deltaTime);

                if (overheadWeather != localWeather.getWeatherHandle())
                    localWeather.beginWeatherTransition(overheadWeather);
            }

            double visualTimeOfDay = grid.getClockInstance().getVisualTimeOfDay();
            float temperature = temperatureSystem.computeTemperature(localWeather, visualTimeOfDay);
            grid.getTemperatureInstance().setTemperature(temperature);
        }
    }

    // Utility \\

    private double wrap(double value, double period) {

        double wrapped = value % period;

        return wrapped < 0.0 ? wrapped + period : wrapped;
    }

    // Accessible \\

    public WeatherInstance getCell(WeatherWindowStruct window, int windowX, int windowZ) {

        int cellX = Math.floorMod(window.getOriginCellX() + windowX, worldCellCountX);
        int cellZ = Math.floorMod(window.getOriginCellZ() + windowZ, worldCellCountZ);

        return cellKey2WeatherInstance.get(Coordinate2Long.pack(cellX, cellZ));
    }

    public boolean hasActiveMap() {
        return mapWorld != null;
    }

    public int getMapResolution() {
        return mapResolution;
    }

    public float getCellSizeBlocks() {
        return (float) cellSizeBlocks;
    }

    public float getDomeRangeBlocks() {
        return (float) (EngineSetting.WEATHER_MAP_DOME_RANGE_CELLS * cellSizeBlocks);
    }

    public float getShapePeriodBlocks() {
        return (float) shapePeriodBlocks;
    }

    // Grid Factory \\

    public WeatherInstance createLocalWeatherInstance() {
        return create(WeatherInstance.class);
    }

    public TemperatureInstance createTemperatureInstance() {
        TemperatureInstance instance = create(TemperatureInstance.class);
        instance.constructor();
        return instance;
    }
}
