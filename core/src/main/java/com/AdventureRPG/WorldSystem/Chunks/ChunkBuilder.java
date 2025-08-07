package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Direction;
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

                    if (block == null || isNonMaterial(block.state))
                        continue; // Never render non-material blocks

                    for (Direction dir : Direction.values()) {

                        Vector3Int adj = new Vector3Int(x + dir.x, y + dir.y, z + dir.z);
                        Block neighbor = null;

                        // In-bounds neighbor
                        if (adj.x >= 0 && adj.x < CHUNK_SIZE &&
                                adj.y >= 0 && adj.y < CHUNK_SIZE &&
                                adj.z >= 0 && adj.z < CHUNK_SIZE) {

                            neighbor = chunk.getBlock(adj.x, adj.y, adj.z);

                        } else {

                            // Neighbor from adjacent chunk
                            Chunk neighborChunk = chunk.getNeighbors().get(dir);

                            if (neighborChunk == null)
                                continue;

                            // Local-space wrap for neighbor chunk access
                            int nx = adj.x;
                            int ny = adj.y;
                            int nz = adj.z;

                            if (nx < 0 || nx >= CHUNK_SIZE ||
                                    ny < 0 || ny >= CHUNK_SIZE ||
                                    nz < 0 || nz >= CHUNK_SIZE) {

                                nx = (nx + CHUNK_SIZE) % CHUNK_SIZE;
                                ny = (ny + CHUNK_SIZE) % CHUNK_SIZE;
                                nz = (nz + CHUNK_SIZE) % CHUNK_SIZE;

                                neighbor = neighborChunk.getBlock(nx, ny, nz);
                            }

                            neighbor = neighborChunk.getBlock(nx, ny, nz);
                        }

                        if (neighbor == null)
                            continue;

                        if (shouldRenderFace(block.state, neighbor.state)) {

                            int texID = getTextureIDForFace(block, dir);

                            if (texID >= 0) {

                                float[] uvs = blockAtlas.getUV(texID);
                                meshData.addFace(x, y, z, dir, uvs);
                            }
                        }
                    }
                }
            }
        }

        return meshData;
    }

    private static boolean shouldRenderFace(State block, State neighbor) {

        State a = isNonMaterial(block) ? State.GAS : block;
        State b = isNonMaterial(neighbor) ? State.GAS : neighbor;

        return (a != b);
    }

    // Treat all non physical blocks the same
    private static boolean isNonMaterial(State state) {
        return state == State.VACUUM || state == State.GAS || state == State.ENERGY;
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
