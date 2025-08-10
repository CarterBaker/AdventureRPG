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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class WorldSystem {

    private final boolean debug = true; // TODO: Remove debug line

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
    private final int CHUNK_SIZE;
    private final int CHUNKS_PER_PIXEL;
    public final Vector2Int WORLD_SCALE;

    private final int range;
    private final int height;

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
        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.CHUNKS_PER_PIXEL = settings.CHUNKS_PER_PIXEL;
        this.WORLD_SCALE = worldReader.GetWorldScale();

        this.range = settings.MAX_RENDER_DISTANCE;
        this.height = settings.MAX_RENDER_HEIGHT;
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

        for (Block block : blocks) {
            if (block != null && block.name.equalsIgnoreCase(name)) {
                return block;
            }
        }

        throw new RuntimeException("Block not found: " + name);
    }

    // Wrap Logic \\ //TODO: I am returning a lot of new Vectors here

    public Vector3 WrapAroundChunk(Vector3 input) {

        float x = input.x % CHUNK_SIZE;
        if (x < 0)
            x += CHUNK_SIZE;

        float y = input.y % CHUNK_SIZE;
        if (y < 0)
            y += CHUNK_SIZE;

        float z = input.z % CHUNK_SIZE;
        if (z < 0)
            z += CHUNK_SIZE;

        return new Vector3(x, y, z);
    }

    public Vector3Int WrapAroundWorld(Vector3Int input) {

        int X = input.x % (WORLD_SCALE.x / CHUNK_SIZE);
        if (X < 0)
            X += (WORLD_SCALE.x / CHUNK_SIZE);

        int Y = input.y;

        int Z = input.z % (WORLD_SCALE.y / CHUNK_SIZE);
        if (Z < 0)
            Z += (WORLD_SCALE.y / CHUNK_SIZE);

        return new Vector3Int(X, Y, Z);
    }

    public Vector3 WrapAroundGrid(Vector3 input) {

        float maxX = range * CHUNK_SIZE;
        float maxY = height * CHUNK_SIZE;
        float maxZ = range * CHUNK_SIZE;

        float X = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        float Y = ((input.y + maxY / 2) % maxY + maxY) % maxY - maxY / 2;
        float Z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return new Vector3(X, Y, Z);
    }

    public Vector2Int WrapAroundImageRegion(Vector2 input) {

        int scaleX = (WORLD_SCALE.x / CHUNKS_PER_PIXEL / CHUNK_SIZE);
        int scaleY = (WORLD_SCALE.y / CHUNKS_PER_PIXEL / CHUNK_SIZE);

        int wrappedX = (int) Math.floor(input.x) % scaleX;
        if (wrappedX < 0)
            wrappedX += scaleX;

        int wrappedZ = (int) Math.floor(input.y) % scaleY;
        if (wrappedZ < 0)
            wrappedZ += scaleY;

        return new Vector2Int(wrappedX, wrappedZ);
    }
    // Debug \\

    private void Debug() { // TODO: Remove debug line

        System.out.print("\rCurrect Chunk Coordinate: " + chunkCoordinate.toString());
        System.out.flush();
    }
}
