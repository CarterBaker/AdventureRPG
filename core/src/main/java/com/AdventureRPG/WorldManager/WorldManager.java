package com.AdventureRPG.WorldManager;

import com.AdventureRPG.Core.Root.ManagerFrame;
import com.AdventureRPG.MaterialSystem.MaterialSystem;
import com.AdventureRPG.PlayerSystem.PlayerManager;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.TextureSystem.TextureSystem;
import com.AdventureRPG.ThreadSystem.ThreadSystem;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldManager.BatchSystem.BatchSystem;
import com.AdventureRPG.WorldManager.Biomes.BiomeSystem;
import com.AdventureRPG.WorldManager.Blocks.Block;
import com.AdventureRPG.WorldManager.Blocks.Loader;
import com.AdventureRPG.WorldManager.Blocks.Type;
import com.AdventureRPG.WorldManager.QueueSystem.QueueSystem;
import com.AdventureRPG.WorldManager.Util.PackedCoordinate3Int;
import com.badlogic.gdx.math.Vector3;
import com.google.gson.Gson;

public class WorldManager extends ManagerFrame {

    // Root
    public Gson gson;
    public ThreadSystem threadSystem;
    public TextureSystem textureSystem;
    public ShaderManager shaderManager;
    public MaterialSystem materialSystem;
    public SaveManager saveManager;
    public UISystem UISystem;
    public PlayerManager playerManager;

    // Settings
    private int maxRenderDistance;
    private int CHUNKS_PER_PIXEL;
    private int CHUNK_SIZE;
    public Vector2Int WORLD_SCALE;

    // Block Management
    private Block[] blocks;

    // World System
    public PackedCoordinate3Int packedCoordinate3Int;
    public WorldGenerator worldGenerator;
    public WorldTick worldTick;
    public WorldReader worldReader;
    public QueueSystem queueSystem;
    public BatchSystem batchSystem;
    public BiomeSystem biomeSystem;

    // Position
    private Vector3 currentPosition;
    private Vector2Int chunkCoordinate;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;
        this.CHUNKS_PER_PIXEL = GlobalConstant.CHUNKS_PER_PIXEL;
        this.CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;

        // World System
        this.packedCoordinate3Int = (PackedCoordinate3Int) register(new PackedCoordinate3Int());
        this.worldGenerator = (WorldGenerator) register(new WorldGenerator());
        this.worldTick = (WorldTick) register(new WorldTick());
        this.worldReader = (WorldReader) register(new WorldReader());
        this.queueSystem = (QueueSystem) register(new QueueSystem());
        this.batchSystem = queueSystem.batchSystem; // TODO: Needs to be updated
        this.biomeSystem = (BiomeSystem) register(new BiomeSystem());

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector2Int();
    }

    @Override
    protected void init() {

        // Settings
        this.WORLD_SCALE = worldReader.getWorldScale();

        // Root
        this.gson = rootManager.gson;
        this.threadSystem = rootManager.get(ThreadSystem.class);
        this.textureSystem = rootManager.get(TextureSystem.class);
        this.shaderManager = rootManager.get(ShaderManager.class);
        this.materialSystem = rootManager.get(MaterialSystem.class);
        this.saveManager = rootManager.get(SaveManager.class);
        this.UISystem = rootManager.get(UISystem.class);
        this.playerManager = rootManager.get(PlayerManager.class);

        // Block Management
        this.blocks = Loader.LoadBlocks(rootManager.gson, this); // TODO: Add block system
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

        loadChunks();
    }

    public void loadChunks() {
        queueSystem.updateChunksInGrid(chunkCoordinate);
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

    public Type getBlockType(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id].type : null;
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

    // Accessible \\

    public void rebuildGrid() {

        queueSystem.rebuildGrid();
    }
}
