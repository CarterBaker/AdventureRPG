package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;

public class ChunkSystem {

    // Chunk System
    private final Settings settings;
    private final Loader Loader;

    // Rendered Chunks
    private final int RenderRange;
    private Vector3Int[][][] renderedChunks;

    public ChunkSystem(GameManager GameManager) {

        // Chunk System
        this.settings = GameManager.settings;
        this.Loader = new Loader(GameManager);

        // Rendered Chunks
        RenderRange = settings.MAX_RENDER_DISTANCE;
        renderedChunks = new Vector3Int[RenderRange][RenderRange][RenderRange];
    }

    public void ReloadChunks(Vector3Int currentChunk) {
        if (!settings.debug)
            return;

        int range = settings.MAX_RENDER_DISTANCE;
        int size = settings.CHUNK_SIZE;

        for (int x = 0; x <= range; x++) {
            for (int y = 0; y <= range; y++) {
                for (int z = 0; z <= range; z++) {
                    int cx = (x - (size / 2)) * size;
                    int cy = (y - (size / 2)) * size;
                    int cz = (z - (size / 2)) * size;

                    int fx = currentChunk.x + cx;
                    int fy = currentChunk.y + cy;
                    int fz = currentChunk.z + cz;

                    renderedChunks[x][y][z] = new Vector3Int(fx, fy, fz);
                }
            }
        }
    }

}
