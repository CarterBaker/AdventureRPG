package application.benchmark.stream;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import com.sun.management.ThreadMXBean;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.chunkstreammanager.GenerationBranch;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.kernel.kernelpipeline.kernelmanager.KernelManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class StreamBenchmarkSystem extends SystemPackage {

    /*
     * Measures the world streaming routines on the first frame, then closes
     * the engine. Every corpus chunk and the ring around it is generated
     * exactly as the stream generates it, through GenerationBranch on a chunk
     * map of its own; then the generation and the geometry build of each
     * corpus chunk are timed over warm-up and measured passes, reporting the
     * median and 95th percentile time per call, the bytes allocated per call
     * and a checksum of the output, so two builds or two kernel variants are
     * proven identical before their times are compared. Generation calls the
     * native kernels through KernelManager at the CPU level it resolved.
     */

    // Internal
    private WorldManager worldManager;
    private GenerationBranch generationBranch;
    private DynamicGeometryManager dynamicGeometryManager;
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderManager worldRenderManager;
    private BlockManager blockManager;
    private TreeManager treeManager;
    private KernelManager kernelManager;
    private ThreadMXBean threadMXBean;

    // Corpus
    private WorldHandle worldHandle;
    private short airBlockId;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private ChunkInstance[] corpusChunks;

    // Measurement
    private long[] callNanos;
    private long allocatedBytes;

    // Report
    private StringBuilder report;
    private long generationChecksum;
    private long meshingChecksum;
    private boolean measured;

    // Base \\

    @Override
    protected void create() {

        // Corpus
        this.activeChunks = new Long2ObjectLinkedOpenHashMap<>();
        this.corpusChunks = new ChunkInstance[EngineSetting.BENCHMARK_CHUNK_COORDINATES.length / 2];

        // Measurement
        this.callNanos = new long[corpusChunks.length * EngineSetting.BENCHMARK_MEASURED_PASSES];

        // Report
        this.report = new StringBuilder();
    }

    @Override
    protected void get() {

        // Internal
        this.worldManager = get(WorldManager.class);
        this.generationBranch = get(GenerationBranch.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.blockManager = get(BlockManager.class);
        this.treeManager = get(TreeManager.class);
        this.kernelManager = get(KernelManager.class);
        this.threadMXBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    }

    @Override
    protected void update() {

        if (measured)
            return;

        this.measured = true;

        buildCorpus();
        measureGeneration();
        measureMeshing();

        internal.windowPlatform.exit();
    }

    // Corpus \\

    // Every corpus chunk and its eight neighbors, generated once, so each corpus chunk builds against real neighbors
    private void buildCorpus() {

        this.worldHandle = worldManager.getWorldHandleFromWorldName(EngineSetting.BENCHMARK_WORLD_NAME);
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);

        for (int i = 0; i < corpusChunks.length; i++) {

            int chunkX = EngineSetting.BENCHMARK_CHUNK_COORDINATES[i * 2];
            int chunkZ = EngineSetting.BENCHMARK_CHUNK_COORDINATES[i * 2 + 1];

            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
                for (int offsetX = -1; offsetX <= 1; offsetX++)
                    resolveChunk(chunkX + offsetX, chunkZ + offsetZ);

            corpusChunks[i] = resolveChunk(chunkX, chunkZ);
        }

        report.append(String.format(
                EngineSetting.BENCHMARK_HEADER_FORMAT,
                worldHandle.getWorldName(),
                corpusChunks.length,
                activeChunks.size(),
                EngineSetting.BENCHMARK_WARMUP_PASSES,
                EngineSetting.BENCHMARK_MEASURED_PASSES,
                kernelManager.getCpuLevel()));
    }

    private ChunkInstance resolveChunk(int chunkX, int chunkZ) {

        long coordinate = WorldWrapUtility.wrapAroundWorld(worldHandle, Coordinate2Long.pack(chunkX, chunkZ));
        ChunkInstance chunkInstance = activeChunks.get(coordinate);

        if (chunkInstance != null)
            return chunkInstance;

        chunkInstance = create(ChunkInstance.class);
        configureChunk(chunkInstance, coordinate);
        activeChunks.put(coordinate, chunkInstance);
        generationBranch.generateChunk(chunkInstance);

        return chunkInstance;
    }

    // The stream's own configuration of a fresh or pooled chunk
    private void configureChunk(ChunkInstance chunkInstance, long coordinate) {
        chunkInstance.constructor(
                worldRenderManager,
                worldHandle,
                coordinate,
                chunkStreamManager.getChunkVAO(),
                airBlockId,
                blockManager,
                treeManager,
                activeChunks);
    }

    // Generation \\

    // Each pass returns every corpus chunk to the pool state and generates it again, as the stream reuses a chunk
    private void measureGeneration() {

        int passCount = EngineSetting.BENCHMARK_WARMUP_PASSES + EngineSetting.BENCHMARK_MEASURED_PASSES;

        this.allocatedBytes = 0L;

        for (int pass = 0; pass < passCount; pass++) {

            boolean recorded = pass >= EngineSetting.BENCHMARK_WARMUP_PASSES;

            for (int i = 0; i < corpusChunks.length; i++) {

                ChunkInstance chunkInstance = corpusChunks[i];

                chunkInstance.reset();
                configureChunk(chunkInstance, chunkInstance.getCoordinate());

                long allocationStart = threadMXBean.getCurrentThreadAllocatedBytes();
                long start = System.nanoTime();

                generationBranch.generateChunk(chunkInstance);

                long elapsed = System.nanoTime() - start;
                long allocated = threadMXBean.getCurrentThreadAllocatedBytes() - allocationStart;

                if (recorded)
                    record(pass - EngineSetting.BENCHMARK_WARMUP_PASSES, i, elapsed, allocated);
            }
        }

        this.generationChecksum = hashGeneration();
        appendRow(
                String.format(EngineSetting.BENCHMARK_ROUTINE_GENERATION_FORMAT, kernelManager.getCpuLevel()),
                generationChecksum);
    }

    // Meshing \\

    private void measureMeshing() {

        DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer = dynamicGeometryManager
                .getDynamicGeometryAsyncInstance();
        int passCount = EngineSetting.BENCHMARK_WARMUP_PASSES + EngineSetting.BENCHMARK_MEASURED_PASSES;

        this.allocatedBytes = 0L;

        for (int pass = 0; pass < passCount; pass++) {

            boolean recorded = pass >= EngineSetting.BENCHMARK_WARMUP_PASSES;

            for (int i = 0; i < corpusChunks.length; i++) {

                long allocationStart = threadMXBean.getCurrentThreadAllocatedBytes();
                long start = System.nanoTime();

                dynamicGeometryManager.build(dynamicGeometryAsyncContainer, corpusChunks[i]);

                long elapsed = System.nanoTime() - start;
                long allocated = threadMXBean.getCurrentThreadAllocatedBytes() - allocationStart;

                if (recorded)
                    record(pass - EngineSetting.BENCHMARK_WARMUP_PASSES, i, elapsed, allocated);
            }
        }

        this.meshingChecksum = hashMeshing();
        appendRow(EngineSetting.BENCHMARK_ROUTINE_MESHING, meshingChecksum);
    }

    // Measurement \\

    private void record(int pass, int chunkIndex, long elapsedNanos, long allocated) {
        callNanos[pass * corpusChunks.length + chunkIndex] = elapsedNanos;
        allocatedBytes += allocated;
    }

    private void appendRow(String routineName, long checksum) {

        Arrays.sort(callNanos);

        int percentileIndex = (int) Math.ceil(callNanos.length * EngineSetting.BENCHMARK_PERCENTILE) - 1;
        double medianMillis = (double) callNanos[callNanos.length / 2] / EngineSetting.NANOS_PER_MILLI;
        double percentileMillis = (double) callNanos[percentileIndex] / EngineSetting.NANOS_PER_MILLI;

        report.append(String.format(
                EngineSetting.BENCHMARK_ROW_FORMAT,
                routineName,
                medianMillis,
                percentileMillis,
                allocatedBytes / callNanos.length,
                checksum));
    }

    // Checksum \\

    // Every block, biome and orientation of every corpus subchunk, in coordinate order
    private long hashGeneration() {

        long hash = EngineSetting.BENCHMARK_HASH_SEED;

        for (ChunkInstance chunkInstance : corpusChunks)
            for (SubChunkInstance subChunkInstance : chunkInstance.getSubChunks()) {

                BlockPaletteHandle blockPaletteHandle = subChunkInstance.getBlockPaletteHandle();
                BlockPaletteHandle biomePaletteHandle = subChunkInstance.getBiomePaletteHandle();
                BlockPaletteHandle rotationPaletteHandle = subChunkInstance.getBlockRotationPaletteHandle();

                for (int i = 0; i < ChunkCoordinateUtility.BLOCK_COORDINATE_COUNT; i++) {

                    int xyz = ChunkCoordinateUtility.getBlockCoordinate(i);

                    hash = mix(hash, blockPaletteHandle.getBlock(xyz));
                    hash = mix(hash, blockPaletteHandle.getSubBlockMask(xyz));
                    hash = mix(hash, biomePaletteHandle.getBlock(xyz));
                    hash = mix(hash, rotationPaletteHandle.getBlock(xyz));
                }
            }

        return hash;
    }

    // Every record of every bucket of every corpus subchunk's quad packet, in bucket order
    private long hashMeshing() {

        long hash = EngineSetting.BENCHMARK_HASH_SEED;

        for (ChunkInstance chunkInstance : corpusChunks)
            for (SubChunkInstance subChunkInstance : chunkInstance.getSubChunks()) {

                QuadPacketInstance quadPacket = subChunkInstance.getQuadPacketInstance();

                for (int bucket = 0; bucket < quadPacket.getBucketCount(); bucket++) {

                    IntArrayList records = quadPacket.getBucketRecords(bucket);
                    int[] elements = records.elements();

                    hash = mix(hash, quadPacket.getBucketMaterialID(bucket));
                    hash = mix(hash, records.size());

                    for (int i = 0; i < records.size(); i++)
                        hash = mix(hash, elements[i]);
                }
            }

        return hash;
    }

    private long mix(long hash, int value) {
        return (hash ^ value) * EngineSetting.BENCHMARK_HASH_PRIME;
    }

    // Accessible \\

    public String getReport() {
        return report.toString();
    }

    public long getGenerationChecksum() {
        return generationChecksum;
    }

    public long getMeshingChecksum() {
        return meshingChecksum;
    }
}
