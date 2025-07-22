package com.AdventureRPG.WorldSystem;

import com.badlogic.gdx.math.Vector3;

import com.AdventureRPG.Util.*;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

import com.AdventureRPG.WorldSystem.Chunks.ChunkManager;
import com.AdventureRPG.WorldSystem.Regions.RegionManager;
import com.AdventureRPG.WorldSystem.Data.PNGReader;
import com.AdventureRPG.WorldSystem.Grid.Grid;

public class WorldSystem {

    // Game Manager
    public final GameManager GameManager;

    // Settings
    public final Settings settings;

    // World System
    public final ChunkManager ChunkManager;
    public final RegionManager RegionManager;
    public final PNGReader PNGReader;
    public final Grid Grid;

    // World Loading
    public final WorldLoader WorldLoader;

    // Dependencies
    public final Vector2Int WORLD_Scale;

    public WorldSystem(GameManager GameManager, Settings settings) {

        // Game Manager
        this.GameManager = GameManager;

        // Settings
        this.settings = settings;

        // World System
        this.ChunkManager = new ChunkManager();
        this.RegionManager = new RegionManager();
        this.PNGReader = new PNGReader(settings);
        this.Grid = new Grid(this);

        // World Loading
        this.WorldLoader = new WorldLoader(GameManager);

        // Dependencies
        WORLD_Scale = PNGReader.GetWorldScale();
    }

    public void Update() {

    }

    // Movement

    public void Move(Vector3 input) {
        Grid.SetPosition(input);
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
            value += settings.BLOCK_SIZE; // Ensure positive

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

        // Wrap X and Z coordinates around the WORLD_Scale (Y is usually vertical and
        // unwrapped)
        X = X % (WORLD_Scale.x / settings.CHUNK_SIZE);
        if (X < 0)
            X += (WORLD_Scale.x / settings.CHUNK_SIZE);

        Y = Y % settings.CHUNK_SIZE;
        if (Y < 0)
            Y += settings.CHUNK_SIZE;

        Z = Z % (WORLD_Scale.y / settings.CHUNK_SIZE);
        if (Z < 0)
            Z += (WORLD_Scale.y / settings.CHUNK_SIZE);

        return new Vector3Int(X, Y, Z);
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
