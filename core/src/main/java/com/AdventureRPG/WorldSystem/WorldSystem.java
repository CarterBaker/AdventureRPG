package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockAtlas;
import com.AdventureRPG.WorldSystem.Blocks.Loader;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;

public class WorldSystem {

    // Debug
    private final boolean debug = false; // TODO: Remove debug line

    // Game Manager
    public final GameManager gameManager;
    public final SaveSystem saveSystem;
    public final UISystem UISystem;
    public final Settings settings;

    // Settings
    private int maxRenderDistance;

    private final int CHUNKS_PER_PIXEL;

    private final int CHUNK_SIZE;

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
    public final Vector2Int WORLD_SCALE;

    // Position
    private Vector3 currentPosition;
    private Vector2Int chunkCoordinate;

    // Base \\

    public WorldSystem(GameManager gameManager) {

        // Game Manager
        this.gameManager = gameManager;
        this.saveSystem = gameManager.saveSystem;
        this.UISystem = gameManager.UISystem;
        this.settings = gameManager.settings;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;

        this.CHUNKS_PER_PIXEL = settings.CHUNKS_PER_PIXEL;

        this.CHUNK_SIZE = settings.CHUNK_SIZE;

        // Block Management
        this.blocks = Loader.LoadBlocks();
        this.BLOCK_TEXTURE_PATH = settings.BLOCK_TEXTURE_PATH;
        this.BLOCK_TEXTURE_SIZE = settings.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = settings.BLOCK_ATLAS_PADDING;
        this.BlockAtlas = new BlockAtlas(BLOCK_TEXTURE_PATH, BLOCK_TEXTURE_SIZE, BLOCK_ATLAS_PADDING);

        // World System
        this.worldTick = new WorldTick(this);
        this.worldReader = new WorldReader(this);
        this.chunkSystem = new ChunkSystem(this);
        this.biomeSystem = new BiomeSystem(gameManager);
        this.worldGenerator = new WorldGenerator(this);
        this.WORLD_SCALE = worldReader.getWorldScale();

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector2Int();
    }

    public void awake() {

        chunkSystem.awake();
        biomeSystem.awake();
    }

    public void start() {

        chunkSystem.start();
        biomeSystem.start();
    }

    public void update() {

        worldTick.update();

        chunkSystem.update();
        biomeSystem.update();
    }

    public void render(ModelBatch modelBatch) {

        chunkSystem.render(modelBatch);
        biomeSystem.render();
    }

    // Movement \\

    public Vector3 position() {
        return currentPosition;
    }

    public Vector2Int chunk() {
        return chunkCoordinate;
    }

    public void updatePosition(Vector3 currentPosition, Vector2Int chunkCoordinate) {

        this.currentPosition.set(currentPosition);

        if (this.chunkCoordinate.equals(chunkCoordinate))
            return;

        this.chunkCoordinate.set(chunkCoordinate);

        if (debug) // Remove debug line
            debug();

        loadChunks();
    }

    public void loadChunks() {
        chunkSystem.loadChunks(chunkCoordinate);
    }

    // Block Management \\

    public Block getBlockByID(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id] : null;
    }

    public Block getBlockByName(String name) {

        for (Block block : blocks)
            if (block != null && block.name.equalsIgnoreCase(name))
                return block;

        throw new RuntimeException("Block not found: " + name);
    }

    // Wrap Logic \\

    public Vector3 wrapAroundChunk(Vector3 input) {

        float x = input.x % CHUNK_SIZE;

        if (x < 0)
            x += CHUNK_SIZE;

        float z = input.z % CHUNK_SIZE;

        if (z < 0)
            z += CHUNK_SIZE;

        input.x = x;
        input.z = z;

        return input;
    }

    public long wrapAroundWorld(long input) {

        int maxX = WORLD_SCALE.x / CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / CHUNK_SIZE;

        int inputX = Coordinate2Int.unpackX(input);
        int inputY = Coordinate2Int.unpackY(input);

        int x = inputX % maxX;

        if (x < 0)
            x += maxX;

        int y = inputY % maxY;

        if (y < 0)
            y += maxY;

        inputX = x;
        inputY = y;

        return Coordinate2Int.pack(inputX, inputY);
    }

    public Vector2Int wrapAroundWorld(Vector2Int input) {

        int maxX = WORLD_SCALE.x / CHUNK_SIZE;
        int maxY = WORLD_SCALE.y / CHUNK_SIZE;

        int x = input.x % maxX;

        if (x < 0)
            x += maxX;

        int y = input.y % maxY;

        if (y < 0)
            y += maxY;

        input.x = x;
        input.y = y;

        return input;
    }

    public Vector3 wrapAroundGrid(Vector3 input) {

        float maxX = maxRenderDistance * CHUNK_SIZE;
        float maxZ = maxRenderDistance * CHUNK_SIZE;

        input.x = ((input.x + maxX / 2) % maxX + maxX) % maxX - maxX / 2;
        input.z = ((input.z + maxZ / 2) % maxZ + maxZ) % maxZ - maxZ / 2;

        return input;
    }

    public long wrapAroundImageRegion(long input) {

        int maxX = (WORLD_SCALE.x / CHUNKS_PER_PIXEL / CHUNK_SIZE);
        int maxY = (WORLD_SCALE.y / CHUNKS_PER_PIXEL / CHUNK_SIZE);

        int inputX = Coordinate2Int.unpackX(input);
        int inputY = Coordinate2Int.unpackY(input);

        int x = (int) Math.floor(inputX) % maxX;

        if (x < 0)
            x += maxX;

        int y = (int) Math.floor(inputY) % maxY;

        if (y < 0)
            y += maxY;

        inputX = x;
        inputY = y;

        return Coordinate2Int.pack(inputX, inputY);
    }

    // Debug \\

    private void debug() { // TODO: Remove debug line

        System.out.print("\rCurrect Chunk Coordinate: " + chunkCoordinate.toString());
        System.out.flush();
    }
}
