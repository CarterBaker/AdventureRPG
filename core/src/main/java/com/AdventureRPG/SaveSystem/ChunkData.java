package com.AdventureRPG.SaveSystem;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

public class ChunkData {

    // Save System
    private final Settings Settings;
    private final File path;

    // ChunkData
    private File ChunkDataFile;
    private boolean usingTempData;

    // Internal state
    private final int CHUNK_SIZE;
    private final int BLOCKS_PER_CHUNK;
    private final int BLOCK_BYTES;
    private final int CHUNK_BYTES;

    private RandomAccessFile raf;
    private FileChannel channel;

    public ChunkData(SaveSystem SaveSystem) {

        // Save System
        this.Settings = SaveSystem.GameManager.settings;
        this.path = SaveSystem.path;

        // Internal state
        CHUNK_SIZE = Settings.CHUNK_SIZE;
        BLOCKS_PER_CHUNK = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;
        BLOCK_BYTES = 12;
        CHUNK_BYTES = BLOCKS_PER_CHUNK * BLOCK_BYTES;
    }

    public void LoadChunkData(File Save) {
        // Not implemented yet
    }

    public void CreateTemporaryChunkData() {
        ChunkDataFile = new File(path, "ChunkData.db");
        usingTempData = true;

        try {
            if (ChunkDataFile.createNewFile())
                System.out.println("Temporary chunk file created: " + ChunkDataFile.getAbsolutePath());
            else
                System.out.println("Temporary chunk file already exists: " + ChunkDataFile.getAbsolutePath());

            raf = new RandomAccessFile(ChunkDataFile, "rw");
            channel = raf.getChannel();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void WriteChunk(Chunk chunk) {
        try {
            long index = packCoordinates(chunk.x, chunk.y, chunk.z);
            long offset = index * CHUNK_BYTES;

            ByteBuffer buffer = ByteBuffer.allocate(CHUNK_BYTES);

            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block == null) {
                            buffer.putInt(0);
                            buffer.putInt(0);
                            buffer.putInt(0);
                        } else {
                            buffer.putInt(block.ID);
                            buffer.putInt(block.top);
                            buffer.putInt((block.side << 16) | (block.bottom & 0xFFFF));
                        }
                    }
                }
            }

            buffer.flip();
            channel.write(buffer, offset);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Chunk ReadChunk(int x, int y, int z) {
        try {
            long index = packCoordinates(x, y, z);
            long offset = index * CHUNK_BYTES;

            ByteBuffer buffer = ByteBuffer.allocate(CHUNK_BYTES);
            int read = channel.read(buffer, offset);
            if (read < CHUNK_BYTES)
                return null; // Chunk not written

            buffer.flip();
            Chunk chunk = new Chunk(x, y, z, CHUNK_SIZE);

            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                for (int ly = 0; ly < CHUNK_SIZE; ly++) {
                    for (int lz = 0; lz < CHUNK_SIZE; lz++) {
                        int id = buffer.getInt();
                        int top = buffer.getInt();
                        int sideBottom = buffer.getInt();
                        int side = (sideBottom >> 16) & 0xFFFF;
                        int bottom = sideBottom & 0xFFFF;

                        if (id != 0) {
                            Block block = new Block();
                            block.ID = id;
                            block.top = top;
                            block.side = side;
                            block.bottom = bottom;
                            chunk.setBlock(lx, ly, lz, block);
                        }
                    }
                }
            }

            chunk.markClean();
            return chunk;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long packCoordinates(int x, int y, int z) {
        long lx = ((long) x & 0x1FFFFF);
        long ly = ((long) y & 0x1FFFFF);
        long lz = ((long) z & 0x1FFFFF);
        return (lx << 42) | (ly << 21) | lz;
    }

    public void dispose() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (usingTempData)
            DeleteTempFile();
    }

    private void DeleteTempFile() {
        ChunkDataFile.delete();
        usingTempData = false;

        System.out.println("Temporary chunk file deleted");
    }
}
