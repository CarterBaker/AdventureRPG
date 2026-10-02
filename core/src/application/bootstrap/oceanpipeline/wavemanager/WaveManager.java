package application.bootstrap.oceanpipeline.wavemanager;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulencemanager.TurbulenceManager;
import application.bootstrap.oceanpipeline.util.OceanWaveUtility;
import application.bootstrap.oceanpipeline.wave.WaveInstance;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WaveManager extends ManagerPackage {

    /*
     * Owns the sea surface. The wave set follows the prevailing wind, long
     * swell and short chop each snapped to the world's wrap period, and every
     * grid gets its phases, its sea-state noise offset and its camera's
     * submersion each frame. The surface at any point is the live tide plus
     * the wave sum, scaled by a sea state built from weather turbulence, the
     * drifting sea noise and the exposure of the water there. The sample
     * methods are the CPU copy of exactly what OceanSurface.glsl tessellates,
     * for physics and gameplay: sampleWaterSurface() is the surface a swimmer
     * floats on, and sampleSurfaceMotion() how fast that surface rises and how
     * the water under it moves, the orbit every wave drives through the water
     * fading with depth. Resolves in LATE_UPDATE, after turbulence and
     * exposure, once every grid's reference chunk is final.
     */

    // Internal
    private TurbulenceManager turbulenceManager;
    private TideManager tideManager;
    private WindManager windManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Waves
    private WorldHandle waveWorldHandle;
    private double[] waveVectorX;
    private double[] waveVectorZ;
    private double[] waveAngularSpeed;
    private double[] waveNumber;
    private float[] waveShare;
    private float shapeMean;

    // Sea Noise
    private int noisePeriodX;
    private int noisePeriodZ;
    private double noiseCellsPerBlockX;
    private double noiseCellsPerBlockZ;
    private double driftDirectionX;
    private double driftDirectionZ;

    // Base \\

    @Override
    protected void create() {

        // Waves
        this.waveVectorX = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveVectorZ = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveAngularSpeed = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveNumber = new double[EngineSetting.OCEAN_WAVE_COUNT];
        this.waveShare = new float[EngineSetting.OCEAN_WAVE_COUNT];
        this.shapeMean = OceanWaveUtility.computeShapeMean(EngineSetting.OCEAN_WAVE_SHARPNESS);

        create(WaveBufferSystem.class);
    }

    @Override
    protected void get() {

        // Internal
        this.turbulenceManager = get(TurbulenceManager.class);
        this.tideManager = get(TideManager.class);
        this.windManager = get(WindManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == null)
            return;

        if (activeWorld != waveWorldHandle)
            resolveWaveSet(activeWorld);

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            resolveGrid((GridInstance) elements[i]);
    }

    private void resolveGrid(GridInstance grid) {

        WaveInstance wave = grid.getWaveInstance();
        long reference = grid.getActiveChunkCoordinate();
        double originX = (double) Coordinate2Long.unpackX(reference) * EngineSetting.CHUNK_SIZE;
        double originZ = (double) Coordinate2Long.unpackY(reference) * EngineSetting.CHUNK_SIZE;

        resolveWavePhases(wave, originX, originZ);
        resolveNoiseOffset(wave, originX, originZ);
        resolveCamera(grid, wave);
    }

    // Waves \\

    private void resolveWaveSet(WorldHandle activeWorld) {

        Vector3 windDirection = windManager.getWindHandle().getGlobalWindDirection();

        double baseAngle = Math.atan2(windDirection.z, windDirection.x);
        double worldWidthBlocks = activeWorld.getWorldScale().x;
        double worldHeightBlocks = activeWorld.getWorldScale().y;
        float swellShareSum = 0f;
        float chopShareSum = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            double angle = baseAngle + Math.toRadians(EngineSetting.OCEAN_WAVE_ANGLES_DEGREES[i]);
            double targetWaveNumber = Math.PI * 2.0 / EngineSetting.OCEAN_WAVE_WAVELENGTHS_BLOCKS[i];

            waveVectorX[i] = snapToWrapPeriod(Math.cos(angle) * targetWaveNumber, worldWidthBlocks);
            waveVectorZ[i] = snapToWrapPeriod(Math.sin(angle) * targetWaveNumber, worldHeightBlocks);

            waveNumber[i] = Math.sqrt(waveVectorX[i] * waveVectorX[i] + waveVectorZ[i] * waveVectorZ[i]);
            waveAngularSpeed[i] = Math.sqrt(EngineSetting.GRAVITY_FORCE * waveNumber[i])
                    * EngineSetting.OCEAN_WAVE_SPEED_SCALE;
            waveShare[i] = EngineSetting.OCEAN_WAVE_AMPLITUDE_RATIOS[i];

            if (isSwell(i))
                swellShareSum += waveShare[i];
            else
                chopShareSum += waveShare[i];
        }

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++)
            waveShare[i] /= isSwell(i) ? swellShareSum : chopShareSum;

        resolveSeaNoise(windDirection, worldWidthBlocks, worldHeightBlocks);

        waveWorldHandle = activeWorld;
    }

    private void resolveSeaNoise(Vector3 windDirection, double worldWidthBlocks, double worldHeightBlocks) {

        double horizontalLength = Math.sqrt(windDirection.x * windDirection.x + windDirection.z * windDirection.z);

        driftDirectionX = horizontalLength > 0.0 ? windDirection.x / horizontalLength : 1.0;
        driftDirectionZ = horizontalLength > 0.0 ? windDirection.z / horizontalLength : 0.0;

        noisePeriodX = Math.max(1, (int) Math.round(worldWidthBlocks / EngineSetting.OCEAN_SEA_NOISE_CELL_BLOCKS));
        noisePeriodZ = Math.max(1, (int) Math.round(worldHeightBlocks / EngineSetting.OCEAN_SEA_NOISE_CELL_BLOCKS));
        noiseCellsPerBlockX = noisePeriodX / worldWidthBlocks;
        noiseCellsPerBlockZ = noisePeriodZ / worldHeightBlocks;
    }

    private double snapToWrapPeriod(double waveVectorComponent, double periodBlocks) {

        double cyclesPerPeriod = Math.PI * 2.0 / periodBlocks;

        return Math.round(waveVectorComponent / cyclesPerPeriod) * cyclesPerPeriod;
    }

    private void resolveWavePhases(WaveInstance wave, double originX, double originZ) {

        double elapsedSeconds = turbulenceManager.getElapsedSeconds();

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            double phase = waveVectorX[i] * originX + waveVectorZ[i] * originZ
                    - waveAngularSpeed[i] * elapsedSeconds;

            wave.setWavePhase(i, (float) (phase - Math.floor(phase / (Math.PI * 2.0)) * (Math.PI * 2.0)));
        }
    }

    private void resolveNoiseOffset(WaveInstance wave, double originX, double originZ) {

        double driftBlocks = turbulenceManager.getElapsedSeconds()
                * EngineSetting.OCEAN_SEA_NOISE_DRIFT_BLOCKS_PER_SECOND;
        double latticeX = (originX - driftDirectionX * driftBlocks) * noiseCellsPerBlockX;
        double latticeZ = (originZ - driftDirectionZ * driftBlocks) * noiseCellsPerBlockZ;

        wave.setNoiseOffset(
                (float) (latticeX - Math.floor(latticeX / noisePeriodX) * noisePeriodX),
                (float) (latticeZ - Math.floor(latticeZ / noisePeriodZ) * noisePeriodZ));
    }

    private boolean isSwell(int waveIndex) {
        return waveIndex < EngineSetting.OCEAN_WAVE_SWELL_COUNT;
    }

    // Camera \\

    private void resolveCamera(GridInstance grid, WaveInstance wave) {

        WindowInstance window = grid.getWindowInstance();
        float tideSurfaceHeight = tideManager.getSurfaceHeightBlocks();

        if (window == null || window.getActiveCamera() == null) {
            wave.setCamera(false, tideSurfaceHeight, 0f);
            return;
        }

        Vector3 cameraPosition = window.getActiveCamera().getPosition();
        float oceanSurfaceHeight = sampleSurfaceHeightRelative(grid, cameraPosition.x, cameraPosition.z);
        float submergedSurfaceHeight = resolveSubmergedSurface(grid, cameraPosition, oceanSurfaceHeight);

        if (Float.isNaN(submergedSurfaceHeight)) {
            wave.setCamera(false, oceanSurfaceHeight, 0f);
            return;
        }

        wave.setCamera(true, submergedSurfaceHeight, Math.max(0f, submergedSurfaceHeight - cameraPosition.y));
    }

    // The surface above a submerged position, or NaN when the position is not under water. A tidal column is
    // under the waves wherever the wave surface passes over it, even inside an air block a trough left open.
    private float resolveSubmergedSurface(GridInstance grid, Vector3 position, float oceanSurfaceHeight) {

        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int totalY = (int) Math.floor(position.y);

        long chunkCoordinate = WorldWrapUtility.wrapAroundWorld(
                grid.getWorldHandle(),
                Coordinate2Long.add(
                        grid.getActiveChunkCoordinate(),
                        Math.floorDiv(blockX, EngineSetting.CHUNK_SIZE),
                        Math.floorDiv(blockZ, EngineSetting.CHUNK_SIZE)));
        ChunkInstance chunk = grid.getActiveChunks().get(chunkCoordinate);

        if (chunk == null)
            return EngineSetting.LIQUID_NO_SURFACE;

        int localX = Math.floorMod(blockX, EngineSetting.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, EngineSetting.CHUNK_SIZE);
        int waterY = LiquidColumnUtility.findWaterY(
                chunk, blockManager, localX, totalY, localZ, tideManager.getSurfaceHeightBlocks());

        if (waterY == LiquidColumnUtility.NO_WATER)
            return EngineSetting.LIQUID_NO_SURFACE;

        float surfaceHeight = resolveWaterSurface(chunk, localX, waterY, localZ, oceanSurfaceHeight);

        return position.y < surfaceHeight ? surfaceHeight : EngineSetting.LIQUID_NO_SURFACE;
    }

    // The surface of the liquid at waterY: the sea over a tidal block, the still surface over any other
    private float resolveWaterSurface(
            ChunkInstance chunk,
            int blockX,
            int waterY,
            int blockZ,
            float oceanSurfaceHeight) {

        if (LiquidColumnUtility.isTidal(chunk, blockX, waterY, blockZ))
            return oceanSurfaceHeight;

        return LiquidColumnUtility.findSurfaceHeight(chunk, blockManager, blockX, waterY, blockZ);
    }

    // Sample \\

    private float sampleSeaStateRelative(GridInstance grid, float relativeX, float relativeZ) {

        WaveInstance wave = grid.getWaveInstance();

        float turbulence = grid.getTurbulenceInstance().sampleStrength(relativeX, relativeZ);
        float exposure = grid.getExposureInstance().sample(relativeX, relativeZ);
        float noiseMultiplier = OceanWaveUtility.sampleSeaNoiseMultiplier(
                relativeX * (float) noiseCellsPerBlockX + wave.getNoiseOffsetX(),
                relativeZ * (float) noiseCellsPerBlockZ + wave.getNoiseOffsetZ(),
                noisePeriodX,
                noisePeriodZ);

        return OceanWaveUtility.resolveSeaState(turbulence, noiseMultiplier, exposure);
    }

    private float sampleDisplacement(WaveInstance wave, float relativeX, float relativeZ, float seaState) {

        float chopAmplitude = OceanWaveUtility.resolveChopAmplitudeBlocks(seaState);
        float swellAmplitude = OceanWaveUtility.resolveSwellAmplitudeBlocks(seaState);
        float displacement = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            float amplitude = (isSwell(i) ? swellAmplitude : chopAmplitude) * waveShare[i];

            displacement += amplitude * OceanWaveUtility.shape(
                    resolveTheta(wave, i, relativeX, relativeZ),
                    EngineSetting.OCEAN_WAVE_SHARPNESS,
                    shapeMean);
        }

        return displacement;
    }

    private void sampleSlope(WaveInstance wave, float relativeX, float relativeZ, float seaState, Vector2 outSlope) {

        float chopAmplitude = OceanWaveUtility.resolveChopAmplitudeBlocks(seaState);
        float swellAmplitude = OceanWaveUtility.resolveSwellAmplitudeBlocks(seaState);
        float slopeX = 0f;
        float slopeZ = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            float amplitude = (isSwell(i) ? swellAmplitude : chopAmplitude) * waveShare[i];
            float derivative = amplitude * OceanWaveUtility.shapeSlope(
                    resolveTheta(wave, i, relativeX, relativeZ),
                    EngineSetting.OCEAN_WAVE_SHARPNESS,
                    shapeMean);

            slopeX += derivative * (float) waveVectorX[i];
            slopeZ += derivative * (float) waveVectorZ[i];
        }

        outSlope.set(slopeX, slopeZ);
    }

    // The surface's rise in y and the orbital current depthBlocks beneath it in x and z, in blocks per second.
    // Every wave turns the water under it through a circle as it passes, forward under its crest and back under
    // its trough, at the speed its own height sets, shrinking with depth by its wave number.
    private void sampleMotion(
            WaveInstance wave,
            float relativeX,
            float relativeZ,
            float seaState,
            float depthBlocks,
            Vector3 outMotion) {

        float chopAmplitude = OceanWaveUtility.resolveChopAmplitudeBlocks(seaState);
        float swellAmplitude = OceanWaveUtility.resolveSwellAmplitudeBlocks(seaState);
        float rise = 0f;
        float currentX = 0f;
        float currentZ = 0f;

        for (int i = 0; i < EngineSetting.OCEAN_WAVE_COUNT; i++) {

            float amplitude = (isSwell(i) ? swellAmplitude : chopAmplitude) * waveShare[i];
            float theta = resolveTheta(wave, i, relativeX, relativeZ);
            float angularSpeed = (float) waveAngularSpeed[i];

            rise -= amplitude * angularSpeed
                    * OceanWaveUtility.shapeSlope(theta, EngineSetting.OCEAN_WAVE_SHARPNESS, shapeMean);

            if (waveNumber[i] <= 0.0)
                continue;

            float orbital = amplitude * angularSpeed
                    * OceanWaveUtility.shape(theta, EngineSetting.OCEAN_WAVE_SHARPNESS, shapeMean)
                    * (float) (Math.exp(-waveNumber[i] * depthBlocks) / waveNumber[i]);

            currentX += orbital * (float) waveVectorX[i];
            currentZ += orbital * (float) waveVectorZ[i];
        }

        outMotion.set(currentX, rise, currentZ);
    }

    private float resolveTheta(WaveInstance wave, int waveIndex, float relativeX, float relativeZ) {
        return (float) waveVectorX[waveIndex] * relativeX + (float) waveVectorZ[waveIndex] * relativeZ
                + wave.getWavePhase(waveIndex);
    }

    private float sampleSurfaceHeightRelative(GridInstance grid, float relativeX, float relativeZ) {

        float seaState = sampleSeaStateRelative(grid, relativeX, relativeZ);

        return tideManager.getSurfaceHeightBlocks()
                + sampleDisplacement(grid.getWaveInstance(), relativeX, relativeZ, seaState);
    }

    // Grid \\

    // The grid that streams a chunk, whose weather and sea state the water there follows; null when none does
    private GridInstance findGrid(long chunkCoordinate) {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];

            if (grid.getGridSlotForChunk(chunkCoordinate) != null)
                return grid;
        }

        return null;
    }

    private double toWorldBlockX(long chunkCoordinate, float localX) {
        return (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE + localX;
    }

    private double toWorldBlockZ(long chunkCoordinate, float localZ) {
        return (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE + localZ;
    }

    // On-Demand \\

    // The surface of the liquid a chunk-local position's column holds at waterY: the live sea over a tidal block,
    // the bare tide while no grid streams the chunk, and the still surface over any other liquid
    public float sampleWaterSurface(
            ChunkInstance chunk,
            long chunkCoordinate,
            int blockX,
            int waterY,
            int blockZ,
            float localX,
            float localZ) {

        GridInstance grid = findGrid(chunkCoordinate);
        float oceanSurfaceHeight = grid == null
                ? tideManager.getSurfaceHeightBlocks()
                : sampleSurfaceHeightBlocks(
                        grid,
                        toWorldBlockX(chunkCoordinate, localX),
                        toWorldBlockZ(chunkCoordinate, localZ));

        return resolveWaterSurface(chunk, blockX, waterY, blockZ, oceanSurfaceHeight);
    }

    // The sea's rise and current at a chunk-local position depthBlocks under its surface; still while no grid
    // streams the chunk
    public void sampleSurfaceMotion(
            long chunkCoordinate,
            float localX,
            float localZ,
            float depthBlocks,
            Vector3 outMotion) {

        GridInstance grid = findGrid(chunkCoordinate);

        if (grid == null) {
            outMotion.set(0f, 0f, 0f);
            return;
        }

        float relativeX = turbulenceManager.toRelativeX(grid, toWorldBlockX(chunkCoordinate, localX));
        float relativeZ = turbulenceManager.toRelativeZ(grid, toWorldBlockZ(chunkCoordinate, localZ));

        sampleMotion(
                grid.getWaveInstance(),
                relativeX,
                relativeZ,
                sampleSeaStateRelative(grid, relativeX, relativeZ),
                Math.max(0f, depthBlocks),
                outMotion);
    }

    public float sampleSurfaceHeightBlocks(GridInstance grid, double worldBlockX, double worldBlockZ) {
        return sampleSurfaceHeightRelative(
                grid,
                turbulenceManager.toRelativeX(grid, worldBlockX),
                turbulenceManager.toRelativeZ(grid, worldBlockZ));
    }

    public void sampleSurfaceSlope(GridInstance grid, double worldBlockX, double worldBlockZ, Vector2 outSlope) {

        float relativeX = turbulenceManager.toRelativeX(grid, worldBlockX);
        float relativeZ = turbulenceManager.toRelativeZ(grid, worldBlockZ);

        sampleSlope(
                grid.getWaveInstance(),
                relativeX,
                relativeZ,
                sampleSeaStateRelative(grid, relativeX, relativeZ),
                outSlope);
    }

    public float sampleSeaState(GridInstance grid, double worldBlockX, double worldBlockZ) {
        return sampleSeaStateRelative(
                grid,
                turbulenceManager.toRelativeX(grid, worldBlockX),
                turbulenceManager.toRelativeZ(grid, worldBlockZ));
    }

    public float sampleWhitecap(GridInstance grid, double worldBlockX, double worldBlockZ) {

        float relativeX = turbulenceManager.toRelativeX(grid, worldBlockX);
        float relativeZ = turbulenceManager.toRelativeZ(grid, worldBlockZ);
        float seaState = sampleSeaStateRelative(grid, relativeX, relativeZ);
        float totalAmplitude = OceanWaveUtility.resolveChopAmplitudeBlocks(seaState)
                + OceanWaveUtility.resolveSwellAmplitudeBlocks(seaState);
        float crest = sampleDisplacement(grid.getWaveInstance(), relativeX, relativeZ, seaState)
                / Math.max(totalAmplitude, EngineSetting.OCEAN_WAVE_AMPLITUDE_EPSILON);

        return OceanWaveUtility.resolveWhitecap(seaState, crest);
    }

    // Grid Factory \\

    public WaveInstance createWaveInstance() {
        WaveInstance instance = create(WaveInstance.class);
        instance.constructor();
        return instance;
    }

    // Accessible \\

    public float getWaveVectorX(int waveIndex) {
        return (float) waveVectorX[waveIndex];
    }

    public float getWaveVectorZ(int waveIndex) {
        return (float) waveVectorZ[waveIndex];
    }

    public float getWaveShare(int waveIndex) {
        return waveShare[waveIndex];
    }

    public float getShapeMean() {
        return shapeMean;
    }

    public int getNoisePeriodX() {
        return noisePeriodX;
    }

    public int getNoisePeriodZ() {
        return noisePeriodZ;
    }

    public float getNoiseCellsPerBlockX() {
        return (float) noiseCellsPerBlockX;
    }

    public float getNoiseCellsPerBlockZ() {
        return (float) noiseCellsPerBlockZ;
    }
}
