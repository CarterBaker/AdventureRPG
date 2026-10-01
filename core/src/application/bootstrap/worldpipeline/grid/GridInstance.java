package application.bootstrap.worldpipeline.grid;

import application.bootstrap.calendarpipeline.clock.ClockInstance;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.oceanpipeline.exposure.ExposureInstance;
import application.bootstrap.oceanpipeline.turbulence.TurbulenceInstance;
import application.bootstrap.oceanpipeline.wave.WaveInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.weatherpipeline.precipitation.PrecipitationInstance;
import application.bootstrap.weatherpipeline.temperature.TemperatureInstance;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridInstance extends InstancePackage {

    /*
     * The streaming grid around one window's focal entity. Owns load order,
     * slots, active chunks, megas and macros, pending requests, render queues,
     * the macro ring anchored to the active chunk, and the window's own
     * location state and UBO instances. rebuildSlots() swaps the layout in
     * place so holders stay valid. Each render queue rebuild is diffed against
     * the last one, and every chunk and mega whose representation changed is
     * promoted to the front of its assessment order so the switch lands within
     * frames instead of waiting out a full streaming pass.
     */

    // Focal
    private EntityInstance focalEntity;

    // Window
    private WindowInstance windowInstance;
    private FBOInstance renderTargetFbo;
    private WaterTargetStruct waterTarget;

    // Grid
    private int totalSlots;
    private long[] loadOrder;
    private int immediateSlotCount;
    private LongOpenHashSet gridCoordinates;
    private Long2ObjectOpenHashMap<GridSlotHandle> gridSlots;
    private float radiusSquared;

    // Active State
    private long activeChunkCoordinate;

    // Location Time
    private ClockInstance clockInstance;

    // Location Lighting UBOs
    private UBOInstance timeDataUBO;
    private UBOInstance sunLightUBO;
    private UBOInstance moonLightUBO;
    private UBOInstance skyColorUBO;

    // Weather Map
    private UBOInstance weatherMapUBO;

    // Weather / Temperature
    private WeatherInstance weatherInstance;
    private TemperatureInstance temperatureInstance;

    // Wind
    private WindInstance windInstance;
    private UBOInstance windDataUBO;

    // Precipitation
    private PrecipitationInstance precipitationInstance;
    private UBOInstance precipitationDataUBO;

    // Ocean
    private TurbulenceInstance turbulenceInstance;
    private ExposureInstance exposureInstance;
    private WaveInstance waveInstance;
    private UBOInstance oceanDataUBO;

    // Chunk State
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;
    private LongLinkedOpenHashSet loadRequests;
    private LongLinkedOpenHashSet unloadRequests;

    // Macro State — wanted macro coordinates, near to far, for the anchor chunk
    private Long2ObjectLinkedOpenHashMap<MacroChunkInstance> activeMacroChunks;
    private LongArrayList macroLoadOrder;
    private LongOpenHashSet macroCoordinates;
    private long macroAnchorCoordinate;
    private int macroAdmitCursor;

    // Render Queues — chunk/mega world coordinate → slot handle
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkRenderQueue;
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaRenderQueue;
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> previousChunkRenderQueue;
    private Long2ObjectLinkedOpenHashMap<GridSlotHandle> previousMegaRenderQueue;

    // Settings
    private int batchedChunks;

    // Scan cursor
    private int scanCursor;

    // Constructor \\

    public void constructor(
            EntityInstance focalEntity,
            WindowInstance windowInstance,
            FBOInstance renderTargetFbo,
            int totalSlots,
            long[] loadOrder,
            int immediateSlotCount,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared,
            int maxChunks,
            ClockInstance clockInstance,
            UBOInstance timeDataUBO,
            UBOInstance sunLightUBO,
            UBOInstance moonLightUBO,
            UBOInstance skyColorUBO,
            UBOInstance weatherMapUBO,
            WeatherInstance weatherInstance,
            TemperatureInstance temperatureInstance,
            WindInstance windInstance,
            UBOInstance windDataUBO,
            PrecipitationInstance precipitationInstance,
            UBOInstance precipitationDataUBO,
            TurbulenceInstance turbulenceInstance,
            ExposureInstance exposureInstance,
            WaveInstance waveInstance,
            UBOInstance oceanDataUBO) {

        // Focal
        this.focalEntity = focalEntity;

        // Window
        this.windowInstance = windowInstance;
        this.renderTargetFbo = renderTargetFbo;
        this.waterTarget = null;

        // Grid
        assignSlots(totalSlots, loadOrder, immediateSlotCount, gridCoordinates, gridSlots, radiusSquared);

        // Active State
        this.activeChunkCoordinate = Coordinate2Long.pack(-1, -1);

        // Location Time
        this.clockInstance = clockInstance;

        // Location Lighting UBOs
        this.timeDataUBO = timeDataUBO;
        this.sunLightUBO = sunLightUBO;
        this.moonLightUBO = moonLightUBO;
        this.skyColorUBO = skyColorUBO;

        // Weather Map
        this.weatherMapUBO = weatherMapUBO;

        // Weather / Temperature
        this.weatherInstance = weatherInstance;
        this.temperatureInstance = temperatureInstance;

        // Wind
        this.windInstance = windInstance;
        this.windDataUBO = windDataUBO;

        // Precipitation
        this.precipitationInstance = precipitationInstance;
        this.precipitationDataUBO = precipitationDataUBO;

        // Ocean
        this.turbulenceInstance = turbulenceInstance;
        this.exposureInstance = exposureInstance;
        this.waveInstance = waveInstance;
        this.oceanDataUBO = oceanDataUBO;

        // Chunk State
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>(maxChunks);
        this.activeMegaChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.loadRequests = new LongLinkedOpenHashSet();
        this.unloadRequests = new LongLinkedOpenHashSet();

        // Macro State
        this.activeMacroChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.macroLoadOrder = new LongArrayList();
        this.macroCoordinates = new LongOpenHashSet();
        resetMacroRing();

        // Render Queues
        this.chunkRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
        this.megaRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
        this.previousChunkRenderQueue = new Long2ObjectLinkedOpenHashMap<>();
        this.previousMegaRenderQueue = new Long2ObjectLinkedOpenHashMap<>();

        // Settings
        this.batchedChunks = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;

        this.scanCursor = 0;
    }

    // Slots \\

    public void rebuildSlots(
            int totalSlots,
            long[] loadOrder,
            int immediateSlotCount,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared) {

        assignSlots(totalSlots, loadOrder, immediateSlotCount, gridCoordinates, gridSlots, radiusSquared);
        this.scanCursor = 0;
        rebuildRenderQueue();
    }

    private void assignSlots(
            int totalSlots,
            long[] loadOrder,
            int immediateSlotCount,
            LongOpenHashSet gridCoordinates,
            Long2ObjectOpenHashMap<GridSlotHandle> gridSlots,
            float radiusSquared) {

        this.totalSlots = totalSlots;
        this.loadOrder = loadOrder;
        this.immediateSlotCount = immediateSlotCount;
        this.gridCoordinates = gridCoordinates;
        this.gridSlots = gridSlots;
        this.radiusSquared = radiusSquared;
    }

    // Render Queue \\

    private void rebuildRenderQueue() {

        swapRenderQueues();

        for (int i = 0; i < totalSlots; i++) {

            long gridCoordinate = loadOrder[i];
            GridSlotHandle slot = gridSlots.get(gridCoordinate);
            long chunkCoordinate = getChunkCoordinateForSlot(gridCoordinate);

            queueChunk(slot, chunkCoordinate);

            if (slot.getDetailLevel().renderMode == RenderType.BATCHED)
                queueMega(slot, chunkCoordinate);
        }

        promoteChangedRepresentations();
    }

    private void swapRenderQueues() {

        Long2ObjectLinkedOpenHashMap<GridSlotHandle> chunkQueue = previousChunkRenderQueue;
        Long2ObjectLinkedOpenHashMap<GridSlotHandle> megaQueue = previousMegaRenderQueue;

        this.previousChunkRenderQueue = chunkRenderQueue;
        this.previousMegaRenderQueue = megaRenderQueue;
        this.chunkRenderQueue = chunkQueue;
        this.megaRenderQueue = megaQueue;

        chunkRenderQueue.clear();
        megaRenderQueue.clear();
    }

    private void queueChunk(GridSlotHandle slot, long chunkCoordinate) {

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        if (megaRenderQueue.containsKey(megaCoordinate))
            return;

        chunkRenderQueue.put(chunkCoordinate, slot);
    }

    private void queueMega(GridSlotHandle slot, long chunkCoordinate) {

        long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);

        if (chunkCoordinate != megaCoordinate)
            return;

        ObjectArrayList<GridSlotHandle> coveredSlots = slot.getCoveredSlots();

        if (coveredSlots.size() != batchedChunks)
            return;

        for (int i = 0; i < coveredSlots.size(); i++)
            if (coveredSlots.get(i).getDetailLevel().renderMode != RenderType.BATCHED)
                return;

        megaRenderQueue.put(megaCoordinate, slot);

        for (int i = 0; i < coveredSlots.size(); i++) {
            long coveredChunk = getChunkCoordinateForSlot(coveredSlots.get(i).getGridCoordinate());
            chunkRenderQueue.remove(coveredChunk);
        }
    }

    // Promotion \\

    // Walked far to near so the nearest changed chunks end up first
    private void promoteChangedRepresentations() {

        for (int i = totalSlots - 1; i >= 0; i--) {

            long chunkCoordinate = getChunkCoordinateForSlot(loadOrder[i]);

            if (chunkRenderQueue.containsKey(chunkCoordinate)
                    && !previousChunkRenderQueue.containsKey(chunkCoordinate))
                promoteChunk(chunkCoordinate);

            if (megaRenderQueue.containsKey(chunkCoordinate)
                    && !previousMegaRenderQueue.containsKey(chunkCoordinate))
                promoteMegaBlock(megaRenderQueue.get(chunkCoordinate), chunkCoordinate);
        }
    }

    private void promoteMegaBlock(GridSlotHandle megaSlot, long megaCoordinate) {

        ObjectArrayList<GridSlotHandle> coveredSlots = megaSlot.getCoveredSlots();

        for (int i = 0; i < coveredSlots.size(); i++)
            promoteChunk(getChunkCoordinateForSlot(coveredSlots.get(i).getGridCoordinate()));

        promoteMega(megaCoordinate);
    }

    public void promoteChunk(long chunkCoordinate) {
        activeChunks.getAndMoveToFirst(chunkCoordinate);
    }

    public void promoteMega(long megaCoordinate) {
        activeMegaChunks.getAndMoveToFirst(megaCoordinate);
    }

    // Active State \\

    public boolean updateActiveChunkCoordinate() {

        long entityChunkCoordinate = focalEntity
                .getWorldPositionStruct()
                .getChunkCoordinate();

        if (activeChunkCoordinate == entityChunkCoordinate)
            return false;

        activeChunkCoordinate = entityChunkCoordinate;
        rebuildRenderQueue();
        return true;
    }

    public long getActiveChunkCoordinate() {
        return activeChunkCoordinate;
    }

    // Macro Ring \\

    public void resetMacroRing() {
        macroLoadOrder.clear();
        macroCoordinates.clear();
        this.macroAnchorCoordinate = Coordinate2Long.pack(-1, -1);
        this.macroAdmitCursor = 0;
    }

    public void anchorMacroRing(long anchorCoordinate) {
        this.macroAnchorCoordinate = anchorCoordinate;
        this.macroAdmitCursor = 0;
    }

    public long getMacroAnchorCoordinate() {
        return macroAnchorCoordinate;
    }

    public int getMacroAdmitCursor() {
        return macroAdmitCursor;
    }

    public void setMacroAdmitCursor(int macroAdmitCursor) {
        this.macroAdmitCursor = macroAdmitCursor;
    }

    // Scan Iteration \\

    public GridSlotHandle getNextScanSlot() {

        if (scanCursor >= totalSlots)
            scanCursor = 0;

        long gridCoordinate = loadOrder[scanCursor];
        scanCursor++;

        return gridSlots.get(gridCoordinate);
    }

    // Computed Slot Lookups \\

    public long getChunkCoordinateForSlot(long gridCoordinate) {
        long raw = Coordinate2Long.add(activeChunkCoordinate, gridCoordinate);
        return WorldWrapUtility.wrapAroundWorld(getWorldHandle(), raw);
    }

    public long getMegaCoordinateForSlot(long gridCoordinate) {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinateForSlot(gridCoordinate));
    }

    public GridSlotHandle getGridSlotForChunk(long chunkCoordinate) {

        long gridCoordinate = WorldWrapUtility.unwrapToGridCoordinate(
                getWorldHandle(),
                activeChunkCoordinate,
                chunkCoordinate);

        return gridSlots.get(gridCoordinate);
    }

    // Accessible \\

    public EntityInstance getFocalEntity() {
        return focalEntity;
    }

    public WindowInstance getWindowInstance() {
        return windowInstance;
    }

    public FBOInstance getRenderTargetFbo() {
        return renderTargetFbo;
    }

    public WaterTargetStruct getWaterTarget() {
        return waterTarget;
    }

    public void setWaterTarget(WaterTargetStruct waterTarget) {
        this.waterTarget = waterTarget;
    }

    public WorldHandle getWorldHandle() {
        return focalEntity.getWorldHandle();
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public long[] getLoadOrder() {
        return loadOrder;
    }

    public int getImmediateSlotCount() {
        return immediateSlotCount;
    }

    public long getGridCoordinate(int i) {
        return loadOrder[i];
    }

    public LongOpenHashSet getGridCoordinates() {
        return gridCoordinates;
    }

    public GridSlotHandle getGridSlot(long gridCoordinate) {
        return gridSlots.get(gridCoordinate);
    }

    public Long2ObjectOpenHashMap<GridSlotHandle> getGridSlots() {
        return gridSlots;
    }

    public float getRadiusSquared() {
        return radiusSquared;
    }

    public ClockInstance getClockInstance() {
        return clockInstance;
    }

    public UBOInstance getTimeDataUBO() {
        return timeDataUBO;
    }

    public UBOInstance getSunLightUBO() {
        return sunLightUBO;
    }

    public UBOInstance getMoonLightUBO() {
        return moonLightUBO;
    }

    public UBOInstance getSkyColorUBO() {
        return skyColorUBO;
    }

    public UBOInstance getWeatherMapUBO() {
        return weatherMapUBO;
    }

    public WeatherInstance getWeatherInstance() {
        return weatherInstance;
    }

    public TemperatureInstance getTemperatureInstance() {
        return temperatureInstance;
    }

    public WindInstance getWindInstance() {
        return windInstance;
    }

    public UBOInstance getWindDataUBO() {
        return windDataUBO;
    }

    public PrecipitationInstance getPrecipitationInstance() {
        return precipitationInstance;
    }

    public UBOInstance getPrecipitationDataUBO() {
        return precipitationDataUBO;
    }

    public TurbulenceInstance getTurbulenceInstance() {
        return turbulenceInstance;
    }

    public ExposureInstance getExposureInstance() {
        return exposureInstance;
    }

    public WaveInstance getWaveInstance() {
        return waveInstance;
    }

    public UBOInstance getOceanDataUBO() {
        return oceanDataUBO;
    }

    public Long2ObjectLinkedOpenHashMap<ChunkInstance> getActiveChunks() {
        return activeChunks;
    }

    public Long2ObjectLinkedOpenHashMap<MegaChunkInstance> getActiveMegaChunks() {
        return activeMegaChunks;
    }

    public Long2ObjectLinkedOpenHashMap<MacroChunkInstance> getActiveMacroChunks() {
        return activeMacroChunks;
    }

    public LongArrayList getMacroLoadOrder() {
        return macroLoadOrder;
    }

    public LongOpenHashSet getMacroCoordinates() {
        return macroCoordinates;
    }

    public LongLinkedOpenHashSet getLoadRequests() {
        return loadRequests;
    }

    public LongLinkedOpenHashSet getUnloadRequests() {
        return unloadRequests;
    }

    public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getChunkRenderQueue() {
        return chunkRenderQueue;
    }

    public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getMegaRenderQueue() {
        return megaRenderQueue;
    }
}