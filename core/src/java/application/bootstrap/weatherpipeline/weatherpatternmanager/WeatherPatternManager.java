package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.temperature.TemperatureInstance;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.weather.WeatherWindowStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
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
     * Owns the live weather pattern. The weather image, divided into cells
     * fixed in noise space, slides across the world with WeatherManager's flow.
     * Each grid reads a window of cells above it, cells are pooled as windows
     * reach them and cross-fade when their biome changes, and each grid keeps a
     * local WeatherInstance following the cell overhead. The weather over any
     * point of noise space can also be sampled, from its live cell or resolved
     * exactly as that cell would be, so a map shows what every player sees.
     * A globe has no wrap of its own, so its pattern repeats over a square
     * as wide as its strip, far wider than any window, laid over the chart
     * of the grid reading it: a cell's weather is the biome's where that
     * chart lays the copy of the cell nearest the grid, on the cube.
     */

    // Internal
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private TemperatureSystem temperatureSystem;

    // Map Geometry
    private WorldHandle mapWorld;
    private int mapResolution;
    private double cellSizeChunks;
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
    private final GlobeFrameStruct frameScratch = new GlobeFrameStruct();

    // Base \\

    @Override
    protected void create() {

        // Map Geometry
        this.mapResolution = EngineSetting.WEATHER_MAP_RESOLUTION;

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

        int pixelBlocks = EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;
        int pixelCountX = Math.max(1, activeWorld.getWorldScale().x / pixelBlocks);
        int pixelCountZ = activeWorld.isGlobe()
                ? pixelCountX
                : Math.max(1, activeWorld.getWorldScale().y / pixelBlocks);
        int cellPixels = resolveLargestDivisor(
                greatestCommonDivisor(pixelCountX, pixelCountZ), EngineSetting.WEATHER_CELL_SIZE_PIXELS);

        this.mapWorld = activeWorld;
        this.worldWidthBlocks = activeWorld.getWorldScale().x;
        this.worldHeightBlocks = (double) pixelCountZ * pixelBlocks;
        this.cellSizeChunks = (double) cellPixels * EngineSetting.CHUNKS_PER_PIXEL;
        this.cellSizeBlocks = cellSizeChunks * EngineSetting.CHUNK_SIZE;
        this.worldCellCountX = pixelCountX / cellPixels;
        this.worldCellCountZ = pixelCountZ / cellPixels;
        this.shapePeriodBlocks = cellSizeBlocks * resolveLargestDivisor(
                greatestCommonDivisor(worldCellCountX, worldCellCountZ),
                EngineSetting.WEATHER_MAP_SHAPE_PERIOD_MAX_CELLS);
    }

    // Cells and the cloud shape period both have to divide the world's axes
    // exactly, or the weather and the cloud shapes would seam where the world
    // wraps.
    private int resolveLargestDivisor(int value, int maximum) {

        for (int divisor = Math.min(value, maximum); divisor > 1; divisor--)
            if (value % divisor == 0)
                return divisor;

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

        float noisePercentile = sampleCellPercentile(cellX, cellZ);

        cell.assignCell(cellKey, cellX, cellZ, noisePercentile);
        cell.constructor(resolveCellWeather(cellX, cellZ, noisePercentile));

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
            WeatherHandle resolved = resolveCellWeather(cell.getCellX(), cell.getCellZ(), cell.getNoisePercentile());

            if (resolved != cell.getWeatherHandle())
                cell.beginWeatherTransition(resolved);
        }
    }

    private float sampleCellPercentile(int cellX, int cellZ) {
        return weatherManager.sampleNoisePercentile((cellX + 0.5) * cellSizeChunks, (cellZ + 0.5) * cellSizeChunks);
    }

    private WeatherHandle resolveCellWeather(int cellX, int cellZ, float noisePercentile) {

        double worldXBlocks = wrap(
                (cellX + 0.5) * cellSizeBlocks + weatherManager.getFlowOffsetXBlocks(), worldWidthBlocks);
        double worldZBlocks = wrap(
                (cellZ + 0.5) * cellSizeBlocks + weatherManager.getFlowOffsetZBlocks(), worldHeightBlocks);

        long worldChunkCoordinate = Coordinate2Long.pack(
                (int) Math.floor(worldXBlocks / EngineSetting.CHUNK_SIZE),
                (int) Math.floor(worldZBlocks / EngineSetting.CHUNK_SIZE));

        return weatherManager.resolveWeather(resolveGlobeChunk(worldChunkCoordinate), noisePercentile);
    }

    // The face cell a chunk of the pattern stands over on a globe, through the chart of the first grid on it: the
    // copy of the chunk nearest that grid's active chunk, folded onto the cube; a torus chunk is its own
    private long resolveGlobeChunk(long patternChunk) {

        if (!mapWorld.isGlobe())
            return patternChunk;

        GridInstance grid = findMapGrid();

        if (grid == null)
            return patternChunk;

        long reference = grid.getActiveChunkCoordinate();
        int periodXChunks = (int) (worldWidthBlocks / EngineSetting.CHUNK_SIZE);
        int periodZChunks = (int) (worldHeightBlocks / EngineSetting.CHUNK_SIZE);

        grid.resolveFrame(
                Coordinate2Long.pack(
                        nearestCopy(Coordinate2Long.unpackX(patternChunk), Coordinate2Long.unpackX(reference),
                                periodXChunks),
                        nearestCopy(Coordinate2Long.unpackY(patternChunk), Coordinate2Long.unpackY(reference),
                                periodZChunks)),
                frameScratch);

        return frameScratch.getCanonicalCoordinate();
    }

    private GridInstance findMapGrid() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];

            if (grid.getWorldHandle() == mapWorld)
                return grid;
        }

        return null;
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

    // Sampling \\

    // Only valid while the map is resolved for the world asked about, see getMapWorld()
    public WeatherHandle sampleNoiseWeather(double noiseXBlocks, double noiseZBlocks) {

        int cellX = Math.floorMod((int) Math.floor(noiseXBlocks / cellSizeBlocks), worldCellCountX);
        int cellZ = Math.floorMod((int) Math.floor(noiseZBlocks / cellSizeBlocks), worldCellCountZ);
        WeatherInstance cell = cellKey2WeatherInstance.get(Coordinate2Long.pack(cellX, cellZ));

        if (cell != null)
            return cell.getWeatherHandle();

        return resolveCellWeather(cellX, cellZ, sampleCellPercentile(cellX, cellZ));
    }

    // Utility \\

    // The copy of a value repeating every period that lies nearest a reference
    private int nearestCopy(int value, int reference, int period) {
        return reference + Math.floorMod(value - reference + period / 2, period) - period / 2;
    }

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

    public WorldHandle getMapWorld() {
        return mapWorld;
    }

    public int getMapResolution() {
        return mapResolution;
    }

    public float getCellSizeBlocks() {
        return (float) cellSizeBlocks;
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
