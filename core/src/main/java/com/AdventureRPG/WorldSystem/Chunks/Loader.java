package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldGenerator;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;

public class Loader {

    // Chunk System
    private final ChunkData ChunkData;
    private final Settings settings;
    private final UISystem UISystem;
    private final WorldSystem WorldSystem;
    private final WorldTick WorldTick;
    public final WorldGenerator WorldGenerator;
    private final ChunkSystem ChunkSystem;

    // Data
    private final int LOD_START_DISTANCE;
    private final int MAX_LOD_DISTANCE;
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Loading
    private LoadScreen loadScreen;

    private boolean loading;

    private final int range;
    private final int height;
    private final int size;

    private int totalLoadedChunks;
    private int loadedChunksThisFrame;
    private int loadedChunksThisTick;
    private final int maxChunks;

    private Chunk[][][] chunks;
    private Vector3Int[][][] loadedChunks;
    private Vector3Int[][][] chunkQueue;

    public Loader(WorldSystem WorldSystem) {

        // Chunk System
        this.ChunkData = WorldSystem.SaveSystem.ChunkData;
        this.settings = WorldSystem.settings;
        this.UISystem = WorldSystem.UISystem;
        this.WorldSystem = WorldSystem;
        this.WorldTick = WorldSystem.WorldTick;
        this.WorldGenerator = new WorldGenerator(WorldSystem);
        this.ChunkSystem = WorldSystem.ChunkSystem;

        // Data
        this.LOD_START_DISTANCE = settings.LOD_START_DISTANCE;
        this.MAX_LOD_DISTANCE = settings.MAX_LOD_DISTANCE;
        this.MAX_CHUNK_LOADS_PER_FRAME = settings.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = settings.MAX_CHUNK_LOADS_PER_TICK;

        // Variables
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
        this.size = settings.CHUNK_SIZE;

        this.maxChunks = range * height * range;

        this.chunks = new Chunk[range][height][range];
        this.loadedChunks = new Vector3Int[range][height][range];
        this.chunkQueue = new Vector3Int[range][height][range];
    }

    public void Update() {

        if (loading)
            Load();

        if (WorldTick.Tick())
            ResetTick();
    }

    private void ResetTick() {

        loadedChunksThisFrame = 0;
        loadedChunksThisTick = 0;
    }

    // Load

    public void LoadChunks(Vector3Int[][][] chunks) {

        this.chunkQueue = chunks;

        loading = true;
        this.loadScreen = (LoadScreen) UISystem.Open(Menu.LoadScreen);
        loadScreen.SetMaxProgrss(range * height * range);

    }

    private int currentX = 0;
    private int currentY = 0;
    private int currentZ = 0;

    private void Load() {

        if (loadedChunksThisTick == MAX_CHUNK_LOADS_PER_TICK)
            return;

        for (; currentX < range; currentX++) {
            for (; currentY < height; currentY++) {
                for (; currentZ < range; currentZ++) {

                    if (loadedChunksThisFrame == MAX_CHUNK_LOADS_PER_FRAME ||
                            loadedChunksThisTick == MAX_CHUNK_LOADS_PER_TICK)
                        return;

                    CheckChunkToLoad(new Vector3Int(currentX, currentY, currentZ));
                }
                currentZ = 0;
            }
            currentY = 0;
        }

        FinalizeLoad();
    }

    private void CheckChunkToLoad(Vector3Int input) {

        int x = input.x;
        int y = input.y;
        int z = input.z;

        // 1. Skip if the chunk was already loaded
        if (loadedChunks[x][y][z] != null)
            return;

        // 2. Log the loaded chunk into the chunk cache
        loadedChunks[x][y][z] = chunkQueue[x][y][z];

        // 3. Position should be subtract half the range multiplied by size
        int posX = (x - (range / 2)) * size;
        int posY = (y - (height / 2)) * size;
        int posZ = (z - (range / 2)) * size;

        // 4. Assemble the chunkCoord and the gridPosition to LoadChunk
        Vector3Int chunkCoord = loadedChunks[x][y][z];
        Vector3Int gridPosition = new Vector3Int(posX, posY, posZ);
        LoadChunk(chunkCoord, gridPosition);

        // 5. Log progress to the loading screen
        loadScreen.IncreaseProgrss(1);
    }

    private void FinalizeLoad() {

        if (totalLoadedChunks != maxChunks)
            return;

        UISystem.Close(loadScreen);
        loading = false;

        // Reset loop state
        currentX = 0;
        currentY = 0;
        currentZ = 0;
    }

    // Update

    public void UpdateChunks(Vector3Int chunkShift) {

    }

    // Base

    private void LoadChunk(Vector3Int chunkCoord, Vector3Int gridPosition) {

        Chunk chunk = null;

        // 2. After that attempt to load from file
        chunk = ChunkData.ReadChunk(chunkCoord);

        // 3. If the chunk could not be loaded it is null and needs to be generated
        if (chunk == null)
            chunk = WorldGenerator.GenerateChunk(chunkCoord);

        // 3. Render each chunk
        chunk.Render(gridPosition);

        // 4. Increase chunk count after a successful chunk load
        ChangeChunkCountBy(1);
    }

    private void UnloadChunk(Vector3Int chunkToLoad, Vector3Int position) {

        ChangeChunkCountBy(-1);
    }

    private void ChangeChunkCountBy(int input) {

        loadedChunksThisFrame += 1;
        loadedChunksThisTick += input;
        totalLoadedChunks += input;
    }

}
