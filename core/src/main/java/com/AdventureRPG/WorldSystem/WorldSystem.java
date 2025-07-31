package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.Loader;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.AdventureRPG.WorldSystem.Grid.Grid;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class WorldSystem {

    // Game
    public final GameManager GameManager;
    public final SaveSystem SaveSystem;
    public final UISystem UISystem;
    public final Settings settings;

    // World System
    public final WorldTick WorldTick;
    public final WorldReader WorldReader;
    public final WorldGenerator WorldGenerator;
    public final BiomeSystem BiomeSystem;
    public final ChunkSystem ChunkSystem;
    public final Grid Grid;

    // Blocks
    public final Block[] blocks;

    // Dependencies
    public final Vector2Int WORLD_Scale;

    public WorldSystem(GameManager GameManager) {

        // Blocks
        this.blocks = Loader.LoadBlocks(); // This needs to be called as soon as possible

        // Game
        this.GameManager = GameManager;
        this.SaveSystem = GameManager.SaveSystem;
        this.UISystem = GameManager.UISystem;
        this.settings = GameManager.settings;

        // World System
        this.WorldTick = new WorldTick(this);
        this.WorldReader = new WorldReader(this);
        WORLD_Scale = WorldReader.GetWorldScale(); // This needs to be called as soon as possible
        this.WorldGenerator = new WorldGenerator(this);
        this.BiomeSystem = new BiomeSystem(GameManager);
        this.ChunkSystem = new ChunkSystem(this);
        this.Grid = new Grid(this);
    }

    public void Update() {
        WorldTick.Update();
        ChunkSystem.Update();
    }

    // Blocks

    public Block getBlockByName(String name) {

        for (Block block : blocks) {
            if (block != null && block.name.equalsIgnoreCase(name)) {
                return block;
            }
        }

        throw new RuntimeException("Block not found: " + name);
    }

    public Block getBlockByID(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id] : null;
    }

    // Movement

    public void Move(Vector3 input) {
        Grid.MoveTo(input);
    }

    // Wrap Logic

    public Vector3 WrapAroundBlock(Vector3 input) {
        return new Vector3(
                WrapAxisAroundBlock(input.x),
                WrapAxisAroundBlock(input.y),
                WrapAxisAroundBlock(input.z));
    }

    private float WrapAxisAroundBlock(float value) {
        value = value % settings.BLOCK_SIZE;

        if (value < 0)
            value += settings.BLOCK_SIZE;

        return value;
    }

    public Vector3 WrapAroundChunk(Vector3 input) {
        return new Vector3(
                WrapAxisAroundChunk(input.x),
                WrapAxisAroundChunk(input.y),
                WrapAxisAroundChunk(input.z));
    }

    private float WrapAxisAroundChunk(float value) {
        value = value % settings.CHUNK_SIZE;

        if (value < 0)
            value += settings.CHUNK_SIZE;

        return value;
    }

    public Vector3Int WrapChunksAroundWorld(Vector3 input) {
        int X = (int) Math.floor(input.x);
        int Y = (int) Math.floor(input.y);
        int Z = (int) Math.floor(input.z);

        X = X % (WORLD_Scale.x / settings.CHUNK_SIZE);
        if (X < 0)
            X += (WORLD_Scale.x / settings.CHUNK_SIZE);

        Z = Z % (WORLD_Scale.y / settings.CHUNK_SIZE);
        if (Z < 0)
            Z += (WORLD_Scale.y / settings.CHUNK_SIZE);

        return new Vector3Int(X, Y, Z);
    }

    public Vector2Int WrapAroundImageRegion(Vector2 input) {
        int scaleX = (WORLD_Scale.x / settings.CHUNKS_PER_PIXEL / settings.CHUNK_SIZE);
        int scaleY = (WORLD_Scale.y / settings.CHUNKS_PER_PIXEL / settings.CHUNK_SIZE);

        int wrappedX = (int) Math.floor(input.x) % scaleX;
        if (wrappedX < 0)
            wrappedX += scaleX;

        int wrappedZ = (int) Math.floor(input.y) % scaleY;
        if (wrappedZ < 0)
            wrappedZ += scaleY;

        return new Vector2Int(wrappedX, wrappedZ);
    }

    public Vector3Int WrapAroundWorld(Vector3Int input) {
        int wrappedX = input.x % WORLD_Scale.x;
        if (wrappedX < 0)
            wrappedX += WORLD_Scale.x;

        int wrappedZ = input.z % WORLD_Scale.y;
        if (wrappedZ < 0)
            wrappedZ += WORLD_Scale.y;

        return new Vector3Int(wrappedX, input.y, wrappedZ);
    }

}
