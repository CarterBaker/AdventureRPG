package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SaveSystem.ChunkData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.WorldTick;
import com.badlogic.gdx.math.Vector3;

public class Loader {

    // Chunk System
    private final ChunkData ChunkData;
    private final Settings settings;
    private final UISystem UISystem;
    private final WorldSystem WorldSystem;
    private final WorldTick WorldTick;

    // Data
    private final int LOD_START_DISTANCE;
    private final int MAX_LOD_DISTANCE;
    private final int MAX_CHUNK_LOADS_PER_FRAME;
    private final int MAX_CHUNK_LOADS_PER_TICK;

    // Variables
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
        UISystem.Open(Menu.LoadScreen);
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

        if (loadedChunks[x][y][z] != null)
            return;

        loadedChunks[x][y][z] = chunkQueue[x][y][z];

        int posX = (x * size) - ((range * size) / 2);
        int posY = (y * size) - ((range * size) / 2);
        int posZ = (z * size) - ((range * size) / 2);

        Vector3 position = new Vector3(posX, posY, posZ);
        Vector3Int wrappedPosition = WorldSystem.WrapChunksAroundWorld(position);

        LoadChunk(loadedChunks[x][y][z], wrappedPosition);
    }

    private void FinalizeLoad() {

        if (totalLoadedChunks != maxChunks)
            return;

        UISystem.Close(Menu.LoadScreen);
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

    private void LoadChunk(Vector3Int chunkToLoad, Vector3Int position) {
        // System.out.println("Loading chunk at: " + chunkToLoad + " for position: " +
        // position);
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
