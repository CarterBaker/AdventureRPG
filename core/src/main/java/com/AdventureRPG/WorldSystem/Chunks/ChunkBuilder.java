package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Direction;
import com.AdventureRPG.Util.MeshData;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockAtlas;
import com.AdventureRPG.WorldSystem.Blocks.State;

public class ChunkBuilder {

    private static final int CHUNK_SIZE = 16;

    public static MeshData build(BlockAtlas blockAtlas, MeshData meshData, Chunk chunk) {

        if (meshData == null)
            meshData = new MeshData();

        meshData.clear();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block == null || block.state == State.VACUUM || block.state == State.GAS
                            || block.state == State.ENERGY) {
                        continue; // Skip rendering nothing
                    }

                    for (Direction dir : Direction.values()) {
                        Vector3Int adj = new Vector3Int(x + dir.x, y + dir.y, z + dir.z);
                        Block neighbor = null;

                        // In-bounds: use same chunk
                        if (adj.x >= 0 && adj.x < CHUNK_SIZE &&
                                adj.y >= 0 && adj.y < CHUNK_SIZE &&
                                adj.z >= 0 && adj.z < CHUNK_SIZE) {

                            neighbor = chunk.getBlock(adj.x, adj.y, adj.z);

                            // Out-of-bounds: use neighbor chunk
                        } else {
                            Chunk neighborChunk = chunk.getNeighbors().get(dir);
                            if (neighborChunk != null) {
                                int nx = (adj.x + CHUNK_SIZE) % CHUNK_SIZE;
                                int ny = (adj.y + CHUNK_SIZE) % CHUNK_SIZE;
                                int nz = (adj.z + CHUNK_SIZE) % CHUNK_SIZE;

                                // Double-check the wrapped index is still valid
                                if (nx >= 0 && nx < CHUNK_SIZE &&
                                        ny >= 0 && ny < CHUNK_SIZE &&
                                        nz >= 0 && nz < CHUNK_SIZE) {

                                    neighbor = neighborChunk.getBlock(nx, ny, nz);
                                }
                            }
                        }

                        if (shouldRenderFace(block.state, neighbor)) {
                            int texID = getTextureIDForFace(block, dir);
                            float[] uvs = blockAtlas.getUV(texID);
                            meshData.addFace(x, y, z, dir, uvs);
                        }
                    }
                }
            }
        }

        return meshData;
    }

    private static boolean shouldRenderFace(State current, Block neighbor) {
        if (neighbor == null)
            return true;
        return neighbor.state != State.SOLID && neighbor.state != State.LIQUID;
    }

    private static int getTextureIDForFace(Block block, Direction face) {
        switch (face) {
            case UP:
                return block.top;
            case DOWN:
                return block.bottom;
            default:
                return block.side;
        }
    }
}
