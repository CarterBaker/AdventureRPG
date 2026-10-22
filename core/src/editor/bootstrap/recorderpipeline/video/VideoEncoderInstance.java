package editor.bootstrap.recorderpipeline.video;

import java.io.File;

import engine.root.InstancePackage;

public abstract class VideoEncoderInstance extends InstancePackage {

    /*
     * One video being written, frame by frame. open() starts the file at a
     * size and frame rate, encodeFrame() — run on the encoding thread, one
     * frame at a time and in order — takes a frame of 24-bit pixels, blue
     * first and rows top down, and close() finishes the file once no frame is
     * left in flight. The first failure is kept as a message and every frame
     * after it is skipped, so the export can stop and say why; abort() lets go
     * of a file half written.
     */

    // Video
    protected File file;
    protected int width;
    protected int height;
    protected int frameRate;

    // Failure
    private volatile String failure;

    // Management \\

    public final boolean open(File file, int width, int height, int frameRate) {

        this.file = file;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.failure = null;

        return openFile();
    }

    protected abstract boolean openFile();

    public abstract void encodeFrame(byte[] frame);

    public abstract boolean close();

    public abstract void abort();

    // Failure \\

    protected final void fail(String message) {
        if (failure == null)
            this.failure = message;
    }

    public final boolean hasFailed() {
        return failure != null;
    }

    public final String getFailure() {
        return failure;
    }
}
