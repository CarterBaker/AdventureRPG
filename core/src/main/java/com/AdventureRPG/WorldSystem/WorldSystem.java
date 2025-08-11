package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockAtlas;
import com.AdventureRPG.WorldSystem.Blocks.Loader;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;

public class WorldSystem {

    private final boolean debug = false; // TODO: Remove debug line

    // Game Manager
    public final GameManager gameManager;
    public final SaveSystem saveSystem;
    public final UISystem UISystem;
    public final Settings settings;

    // Block Management
    private final Block[] blocks;
    private final String BLOCK_TEXTURE_PATH;
    private final int BLOCK_TEXTURE_SIZE;
    private final int BLOCK_ATLAS_PADDING;
    public final BlockAtlas BlockAtlas;

    // World System
    public final WorldTick worldTick;
    public final WorldReader worldReader;
    public final ChunkSystem chunkSystem;
    public final BiomeSystem biomeSystem;
    public final WorldGenerator worldGenerator;

    // Position
    private Vector3 currentPosition;
    private Vector3Int chunkCoordinate;

    // Settings
    private final int CHUNKS_PER_PIXEL;
    public final Vector2Int WORLD_SCALE;

    private int range;
    private int height;
    private final int size;

    // Base \\

    public WorldSystem(GameManager gameManager) {

        // Game Manager
        this.gameManager = gameManager;
        this.saveSystem = gameManager.saveSystem;
        this.UISystem = gameManager.UISystem;
        this.settings = gameManager.settings;

        // Block Management
        this.blocks = Loader.LoadBlocks();
        this.BLOCK_TEXTURE_PATH = settings.BLOCK_TEXTURE_PATH;
        this.BLOCK_TEXTURE_SIZE = settings.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = settings.BLOCK_ATLAS_PADDING;
        this.BlockAtlas = new BlockAtlas(BLOCK_TEXTURE_PATH, BLOCK_TEXTURE_SIZE, BLOCK_ATLAS_PADDING, settings.debug);

        // World System
        this.worldTick = new WorldTick(this);
        this.worldReader = new WorldReader(this);
        this.chunkSystem = new ChunkSystem(this);
        this.biomeSystem = new BiomeSystem(gameManager);
        this.worldGenerator = new WorldGenerator(this);

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector3Int();

        // Settings
        this.CHUNKS_PER_PIXEL = settings.CHUNKS_PER_PIXEL;
        this.WORLD_SCALE = worldReader.GetWorldScale();

        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
        this.size = settings.CHUNK_SIZE;
    }

    public void Awake() {

        chunkSystem.Awake();
        biomeSystem.Awake();
    }

    public void Start() {

        chunkSystem.Start();
        biomeSystem.Start();
    }

    public void Update() {

        worldTick.Update();

        chunkSystem.Update();
        biomeSystem.Update();
    }

    public void Render(ModelBatch modelBatch) {

        chunkSystem.Render(modelBatch);
        biomeSystem.Render();
    }

    // Initialize \\

    public void RebuildGrid() {

        // Reset render distances
        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;

        // Rebuild the grid
        chunkSystem.RebuildGrid();
    }

    // Movement \\

    public Vector3 Position() {
        return currentPosition;
    }

    public Vector3Int Chunk() {
        return chunkCoordinate;
    }

    public void UpdatePosition(Vector3 currentPosition, Vector3Int chunkCoordinate) {

        this.currentPosition.set(currentPosition);

        if (this.chunkCoordinate.equals(chunkCoordinate))
            return;

        this.chunkCoordinate.set(chunkCoordinate);

        if (debug) // Remove debug line
            Debug();

        LoadChunks();
    }

    public void LoadChunks() {
        chunkSystem.LoadChunks(chunkCoordinate);
    }

    // Block Management \\

    public Block GetBlockByID(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id] : null;
    }

    public Block GetBlockByName(String name) {

        for (Block block : blocks)
            if (block != null && block.name.equalsIgnoreCase(name))
                return block;

        throw new RuntimeException("Block not found: " + name);
    }

    // Wrap Logic \\

    public Vector3 WrapAroundChunk(Vector3 input) {

        float x = input.x % size;
        if (x < 0)
            x += size;

        float y = input.y % size;
        if (y < 0)
            y += size;

        float z = input.z % size;
        if (z < 0)
            z += size;

        input.x = x;
        input.y = y;
        input.z = z;

        return input;
    }

    public Vector3Int WrapAroundWorld(Vector3Int input) {

        int maxX = WORLD_SCALE.x / size;
        int maxZ = WORLD_SCALE.y / size;

        int x = input.x % maxX;

        if (x < 0)
            x += maxX;

        int y = input.y;

        int z = input.z % maxZ;

        if (z < 0)
            z += maxZ;

        input.x = x;
        input.y = y;
        input.z = z;

        return input;
    }

    public Vector3 WrapAroundGrid(Vector3 input) {

        float maxX = range * size;
        float maxY = height * size;
        float maxZ = range * size;

        input.x = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        input.y = ((input.y + maxY / 2) % maxY + maxY) % maxY - maxY / 2;
        input.z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return input;
    }

    public Vector2Int WrapAroundImageRegion(Vector2Int input) {

        int maxX = (WORLD_SCALE.x / CHUNKS_PER_PIXEL / size);
        int maxY = (WORLD_SCALE.y / CHUNKS_PER_PIXEL / size);

        int x = (int) Math.floor(input.x) % maxX;

        if (x < 0)
            x += maxX;

        int y = (int) Math.floor(input.y) % maxY;

        if (y < 0)
            y += maxY;

        input.x = x;
        input.y = y;

        return input;
    }

    // Debug \\

    private void Debug() { // TODO: Remove debug line

        System.out.print("\rCurrect Chunk Coordinate: " + chunkCoordinate.toString());
        System.out.flush();
    }
}
