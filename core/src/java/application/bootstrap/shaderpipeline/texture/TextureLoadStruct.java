package application.bootstrap.shaderpipeline.texture;

import java.io.File;
import java.util.concurrent.Future;

import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import engine.root.StructPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

public class TextureLoadStruct extends StructPackage implements Runnable {

    /*
     * One tier of one tile being read off the main thread. The streamer keeps
     * a fixed set of these and reuses each: it names the tile, the tier, its
     * file and the pool generation it was asked under, submits the struct
     * itself to the thread pipeline, and uploads the maps once it reads as
     * done. A failure carries its reason back to the main thread.
     */

    // Request
    private int tileID;
    private int tier;
    private int generation;
    private File file;
    private Future<?> future;

    // Result
    private final byte[][] maps;
    private volatile boolean done;
    private volatile String failure;

    // Constructor \\

    public TextureLoadStruct() {
        this.maps = new byte[TextureMapType.TIERED_COUNT][];
    }

    // Request \\

    public void begin(int tileID, int tier, int generation, File file) {
        this.tileID = tileID;
        this.tier = tier;
        this.generation = generation;
        this.file = file;
        this.done = false;
        this.failure = null;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    // Read \\

    @Override
    public void run() {

        try {

            ArpgObjectStruct arpg = ArpgUtility.readObject(file);

            for (int m = 0; m < maps.length; m++)
                maps[m] = TextureBakeUtility.readMap(arpg, TextureMapType.VALUES[m]);
        } catch (InternalException e) {
            failure = file.getPath() + ": " + e.getMessage();
        } finally {
            done = true;
        }
    }

    // Releases the maps once they are uploaded or dropped, so nothing outlives the upload
    public void finish() {

        for (int m = 0; m < maps.length; m++)
            maps[m] = null;

        this.file = null;
        this.future = null;
    }

    // Accessible \\

    public int getTileID() {
        return tileID;
    }

    public int getTier() {
        return tier;
    }

    public int getGeneration() {
        return generation;
    }

    public Future<?> getFuture() {
        return future;
    }

    public byte[][] getMaps() {
        return maps;
    }

    public boolean isDone() {
        return done;
    }

    public String getFailure() {
        return failure;
    }
}
