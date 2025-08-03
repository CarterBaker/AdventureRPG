package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class ChunkBuilder {

    public static ModelInstance build(Chunk chunk) {
        // Generate full-detail mesh using chunk.getBlock(...) etc.
        // Convert local blocks to mesh data
        // Assign mesh to rendering system
        return null;
    }

    public static ModelInstance destroy(Chunk chunk) {
        // Dispose of mesh resources or flag them as reusable
        // Remove from rendering scene
        return null;
    }
}
