package application.bootstrap.mappipeline.map;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class MapTileInstance extends InstancePackage {

    /*
     * One square of the world map at one zoom level, MAP_TILE_TEXELS on a side.
     * Pooled by MapManager with the texture it draws from, so a tile
     * reused for another place uploads into the texture it already owns. A
     * worker fills the pixels and then raises the generated flag, which the
     * main thread reads before uploading, so the pixels are always whole when
     * they reach the GPU, and a tile stays generating until then, so it is
     * never recycled with a worker still writing into it. A tile at the
     * finest level also remembers, chunk by chunk, which version of a real
     * chunk it was last drawn from, and the frame a view last showed it at
     * that level, so its chunks are refreshed once per frame however many
     * views show it.
     */

    // Identity
    private int level;
    private int tileX;
    private int tileZ;
    private long key;

    // GPU
    private int texture;
    private boolean uploaded;

    // Generation
    private byte[] pixels;
    private boolean generating;
    private volatile boolean generated;

    // Use
    private long lastUsedFrame;
    private long shownFinestFrame;

    // Chunks
    private long[] chunkVersions;

    // Internal \\

    @Override
    protected void create() {

        int chunksPerSide = EngineSetting.MACRO_TILE_SIZE_BLOCKS / EngineSetting.CHUNK_SIZE;

        this.pixels = new byte[EngineSetting.MAP_TILE_TEXELS * EngineSetting.MAP_TILE_TEXELS
                * EngineSetting.COLOR_CHANNEL_COUNT];
        this.chunkVersions = new long[chunksPerSide * chunksPerSide];
    }

    // Constructor \\

    public void constructor(int level, int tileX, int tileZ, long key, int texture) {

        this.level = level;
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.key = key;
        this.texture = texture;
        this.uploaded = false;
        this.generating = false;
        this.generated = false;
        this.shownFinestFrame = EngineSetting.INDEX_NOT_FOUND;

        Arrays.fill(chunkVersions, EngineSetting.MAP_CHUNK_VERSION_NONE);
    }

    // Generation \\

    public void beginGeneration() {
        this.generating = true;
    }

    public void finishGeneration() {
        this.generated = true;
    }

    public boolean isGenerating() {
        return generating;
    }

    public boolean isAwaitingUpload() {
        return generated && !uploaded;
    }

    public void markUploaded() {
        this.uploaded = true;
        this.generating = false;
    }

    // Use \\

    public void touch(long frame) {
        this.lastUsedFrame = frame;
    }

    // True only the first time a view shows this tile at the finest level in a frame
    public boolean markShownFinest(long frame) {

        if (shownFinestFrame == frame)
            return false;

        this.shownFinestFrame = frame;
        return true;
    }

    // Chunks \\

    public long getChunkVersion(int chunkIndex) {
        return chunkVersions[chunkIndex];
    }

    public void setChunkVersion(int chunkIndex, long version) {
        chunkVersions[chunkIndex] = version;
    }

    // Accessible \\

    public int getLevel() {
        return level;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileZ() {
        return tileZ;
    }

    public long getKey() {
        return key;
    }

    public int getTexture() {
        return texture;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public boolean isRequested() {
        return generating || uploaded;
    }

    public byte[] getPixels() {
        return pixels;
    }

    public long getLastUsedFrame() {
        return lastUsedFrame;
    }
}
