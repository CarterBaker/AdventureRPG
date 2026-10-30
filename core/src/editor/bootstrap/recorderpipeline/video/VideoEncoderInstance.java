package editor.bootstrap.recorderpipeline.video;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.InstancePackage;

public abstract class VideoEncoderInstance extends InstancePackage {

    /*
     * One video being written, frame by frame. open() starts the file at a
     * size and frame rate, with a stereo 16-bit sound track at the export's
     * sample rate when asked, encodeFrame() — run on the encoding thread, one
     * frame at a time and in order — takes a frame of 24-bit pixels, blue
     * first and rows top down, encodeAudio() right after it takes the same
     * frame's stretch of sound, and close() finishes the file once no frame is
     * left in flight. The first failure is kept as a message and every frame
     * after it is skipped, so the export can stop and say why; abort() lets go
     * of a file half written.
     */

    // Video
    protected File file;
    protected int width;
    protected int height;
    protected int frameRate;

    // Audio
    protected boolean audio;
    protected int audioFramesPerFrame;

    // Failure
    private volatile String failure;

    // Management \\

    public final boolean open(File file, int width, int height, int frameRate, boolean audio) {

        this.file = file;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.audio = audio;
        this.audioFramesPerFrame = EditorSetting.RECORDER_AUDIO_SAMPLE_RATE / frameRate;
        this.failure = null;

        return openFile();
    }

    protected abstract boolean openFile();

    public abstract void encodeFrame(byte[] frame);

    // A video frame's stretch of sound, both sides of each sound frame side by side; nothing for a video without sound
    public final void encodeAudio(short[] samples) {
        if (audio && !hasFailed())
            writeAudio(samples);
    }

    protected abstract void writeAudio(short[] samples);

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
