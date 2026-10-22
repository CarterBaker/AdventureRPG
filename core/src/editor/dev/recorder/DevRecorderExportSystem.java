package editor.dev.recorder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.pbo.PBOInstance;
import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.bootstrap.recorderpipeline.util.VideoFrameUtility;
import editor.bootstrap.recorderpipeline.video.AviVideoEncoderInstance;
import editor.bootstrap.recorderpipeline.video.FfmpegVideoEncoderInstance;
import editor.bootstrap.recorderpipeline.video.VideoEncoderInstance;
import editor.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class DevRecorderExportSystem extends SystemPackage {

    /*
     * Renders the recorder's export in this Dev window, frame by frame, as
     * slowly as each frame takes. While it runs the engine's clock steps each
     * frame by exactly the time between video frames, so nothing is lost or
     * rushed however long a frame takes to make, and the window renders at the
     * export's size whatever the size of its tab. Before the first frame the
     * world is given time to stream in around the camera, and before each
     * frame until the streaming threads have nothing left in flight, the clock
     * standing still meanwhile. Every layer the window draws, up to the HUD
     * when it is left out, is gathered into one capture target and read back
     * a frame later without stalling. Motion blur renders several moments
     * across a 180 degree shutter and supersampling renders at twice the size;
     * both are averaged down into each video frame, which goes to the encoder
     * on its own thread while the next renders. This system updates before
     * DevRecorderSystem, so the playhead it sets is the one played that frame.
     */

    // Internal
    private RecorderManager recorderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private PBOManager pboManager;
    private WindowManager windowManager;
    private ThreadHandle encodingThread;
    private ThreadHandle worldStreamingThread;
    private ThreadHandle macroStreamingThread;

    // Targets — made once for this window and resized with it
    private FBOInstance captureFbo;
    private PBOInstance pbo;

    // Export
    private boolean exporting;
    private VideoEncoderInstance encoder;
    private int outputWidth;
    private int outputHeight;
    private int renderWidth;
    private int renderHeight;
    private int scale;
    private int subframes;
    private int frameCount;
    private float startTime;
    private float frameSeconds;
    private float subframeSeconds;

    // Progress
    private boolean warming;
    private boolean finishing;
    private int idleFrames;
    private int settleFrames;
    private int nextFrame;
    private int nextSubframe;
    private boolean renderedCapture;
    private int renderedSubframe;

    // Read Back — captures arrive one frame behind, oldest first
    private ByteBuffer readback;
    private final int[] pendingSubframes = new int[EditorSetting.RECORDER_EXPORT_FRAME_SLOTS];
    private int pendingCount;

    // Frames
    private int[] frameSum;
    private byte[][] frames;
    private Future<?>[] frameFutures;
    private int frameCursor;
    private int encodedFrames;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.pboManager = get(PBOManager.class);
        this.windowManager = get(WindowManager.class);
        this.encodingThread = getThreadHandleFromThreadName(EditorSetting.RECORDER_ENCODING_THREAD_NAME);
        this.worldStreamingThread = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.macroStreamingThread = getThreadHandleFromThreadName(EngineSetting.MACRO_STREAMING_THREAD_NAME);
    }

    @Override
    protected void dispose() {
        if (exporting)
            cancel();
    }

    // Update \\

    @Override
    protected void update() {

        boolean wanted = recorderManager.isSessionFor((DevContext) context)
                && recorderManager.getState() == RecorderState.EXPORTING;

        if (!wanted) {

            if (exporting)
                cancel();

            return;
        }

        if (!exporting && !begin())
            return;

        readBack();

        if (exporting && encoder.hasFailed())
            fail(encoder.getFailure());

        if (exporting)
            step();
    }

    // Begin \\

    private boolean begin() {

        RecordingInstance recording = recorderManager.getRecording();
        RecordingExportStruct settings = recording.getExportStruct();
        WindowInstance window = context.getWindow();

        this.scale = settings.isSupersampled() ? EditorSetting.RECORDER_EXPORT_SUPERSAMPLE_SCALE : 1;
        this.subframes = settings.getMotionBlurSamples();
        this.outputWidth = settings.getWidth();
        this.outputHeight = settings.getHeight();
        this.renderWidth = outputWidth * scale;
        this.renderHeight = outputHeight * scale;
        this.frameCount = recorderManager.getExportFrameCount();
        this.startTime = settings.resolveStart(recording.getDuration());
        this.frameSeconds = 1f / settings.getFrameRate();
        this.subframeSeconds = frameSeconds * EditorSetting.RECORDER_EXPORT_SHUTTER / subframes;

        this.encoder = createEncoder(settings.getFormat());

        if (encoder == null || !encoder.open(
                recorderManager.getExportFile(), outputWidth, outputHeight, settings.getFrameRate())) {
            recorderManager.finishExport(false, encoder == null
                    ? EditorSetting.RECORDER_STATUS_NO_FFMPEG
                    : encoder.getFailure());
            return false;
        }

        window.setFixedRenderSize(renderWidth, renderHeight);

        if (captureFbo == null)
            this.captureFbo = fboManager.cloneFbo(EditorSetting.RECORDER_CAPTURE_FBO, window);

        if (pbo == null)
            this.pbo = pboManager.createPbo(window.getGLWindow(), renderWidth, renderHeight);

        fboManager.resize(captureFbo, renderWidth, renderHeight);
        fboRenderSystem.setCaptureTarget(window, captureFbo, settings.isHud()
                ? EditorSetting.RECORDER_EXPORT_HUD_LAYER_LIMIT
                : EditorSetting.RECORDER_EXPORT_SCENE_LAYER_LIMIT);

        int frameBytes = outputWidth * outputHeight * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

        this.readback = ByteBuffer.allocateDirect(renderWidth * renderHeight * EngineSetting.BYTES_PER_PIXEL_BGRA);
        this.frameSum = new int[frameBytes];
        this.frames = new byte[EditorSetting.RECORDER_EXPORT_FRAME_SLOTS][frameBytes];
        this.frameFutures = new Future<?>[EditorSetting.RECORDER_EXPORT_FRAME_SLOTS];
        this.frameCursor = 0;
        this.encodedFrames = 0;

        this.exporting = true;
        this.warming = true;
        this.finishing = false;
        this.idleFrames = 0;
        this.settleFrames = 0;
        this.nextFrame = 0;
        this.nextSubframe = 0;
        this.renderedCapture = false;
        this.pendingCount = 0;

        internal.beginFixedClock(0f);
        recorderManager.setExportPlayhead(startTime);
        return true;
    }

    private VideoEncoderInstance createEncoder(VideoFormat format) {

        if (!format.needsFfmpeg())
            return create(AviVideoEncoderInstance.class);

        File executable = FfmpegUtility.findExecutable(internal.path);

        if (executable == null)
            return null;

        FfmpegVideoEncoderInstance ffmpegEncoder = create(FfmpegVideoEncoderInstance.class);
        ffmpegEncoder.constructor(executable, format.getFfmpegArguments());
        return ffmpegEncoder;
    }

    // Step \\

    // Settles or renders a capture this frame, and sets how far the clock moves into the next
    private void step() {

        if (finishing) {
            finish();
            return;
        }

        float time = resolveTime(nextFrame, nextSubframe);
        boolean idle = isStreamingIdle();

        recorderManager.setExportPlayhead(time);
        this.renderedCapture = false;

        if (warming) {

            this.idleFrames = idle ? idleFrames + 1 : 0;
            this.settleFrames++;

            if (idleFrames < EditorSetting.RECORDER_EXPORT_WARMUP_IDLE_FRAMES
                    && settleFrames < EditorSetting.RECORDER_EXPORT_WARMUP_LIMIT)
                return;

            this.warming = false;
            this.settleFrames = 0;
        }

        if (!idle && settleFrames < EditorSetting.RECORDER_EXPORT_SETTLE_LIMIT) {
            this.settleFrames++;
            internal.setFixedFrameSeconds(0f);
            return;
        }

        this.settleFrames = 0;
        this.renderedCapture = true;
        this.renderedSubframe = nextSubframe;

        this.nextSubframe++;

        if (nextSubframe == subframes) {
            this.nextSubframe = 0;
            this.nextFrame++;
        }

        if (nextFrame >= frameCount) {
            this.finishing = true;
            internal.setFixedFrameSeconds(0f);
            return;
        }

        internal.setFixedFrameSeconds(resolveTime(nextFrame, nextSubframe) - time);
    }

    // A moment of the export — its frame's start, and each motion blur sample spread across the shutter
    private float resolveTime(int frame, int subframe) {
        return startTime + frame * frameSeconds + subframe * subframeSeconds;
    }

    private boolean isStreamingIdle() {
        return worldStreamingThread.getInFlightCount() == 0 && macroStreamingThread.getInFlightCount() == 0;
    }

    // Read Back \\

    // Queues last frame's capture and takes in the one queued before it
    private void readBack() {

        if (!renderedCapture)
            return;

        WindowInstance previous = enterContext();
        boolean retrieved = pbo.captureFbo(context.getWindow().getGLWindow(), captureFbo, readback);

        exitContext(previous);
        pendingSubframes[pendingCount++] = renderedSubframe;
        this.renderedCapture = false;

        if (retrieved)
            consume();
    }

    // The oldest capture read back joins its frame, and a frame whose last moment it was goes to the encoder
    private void consume() {

        int subframe = pendingSubframes[0];

        for (int i = 1; i < pendingCount; i++)
            pendingSubframes[i - 1] = pendingSubframes[i];

        this.pendingCount--;

        VideoFrameUtility.accumulate(
                readback, renderWidth, renderHeight, scale, frameSum, outputWidth, outputHeight);

        if (subframe < subframes - 1)
            return;

        int slot = frameCursor;

        if (!awaitSlot(slot))
            return;

        byte[] frame = frames[slot];
        VideoEncoderInstance frameEncoder = encoder;

        VideoFrameUtility.resolve(frameSum, subframes * scale * scale, frame);
        frameFutures[slot] = executeAsync(encodingThread, () -> frameEncoder.encodeFrame(frame));

        this.frameCursor = (slot + 1) % EditorSetting.RECORDER_EXPORT_FRAME_SLOTS;
        this.encodedFrames++;
        recorderManager.reportExportProgress(encodedFrames);
    }

    // Waits for the frame still encoding from the slot, false once the export has failed
    private boolean awaitSlot(int slot) {

        Future<?> future = frameFutures[slot];

        if (future == null)
            return true;

        try {
            future.get();
            frameFutures[slot] = null;
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(EditorSetting.RECORDER_MESSAGE_INTERRUPTED);
            return false;
        } catch (ExecutionException e) {
            fail(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getCause().getMessage());
            return false;
        }
    }

    private WindowInstance enterContext() {
        WindowInstance previous = windowManager.getContextWindow();
        internal.windowPlatform.makeContextCurrent(context.getWindow().getGLWindow());
        return previous;
    }

    private void exitContext(WindowInstance previous) {
        if (previous != null)
            internal.windowPlatform.makeContextCurrent(previous.getGLWindow());
        else
            internal.windowPlatform.restoreMainContext();
    }

    // End \\

    // Takes in the last capture, lets every frame finish encoding and closes the video
    private void finish() {

        WindowInstance previous = enterContext();

        if (pendingCount > 0 && pbo.tryRetrieve(readback)) {
            exitContext(previous);
            consume();
        } else
            exitContext(previous);

        if (!exporting || !awaitAll())
            return;

        boolean closed = encoder.close();
        String failure = encoder.getFailure();

        releaseExport();
        recorderManager.finishExport(closed, failure);
    }

    private boolean awaitAll() {

        for (int slot = 0; slot < EditorSetting.RECORDER_EXPORT_FRAME_SLOTS; slot++)
            if (!awaitSlot(slot))
                return false;

        return true;
    }

    private void fail(String message) {

        awaitQuietly();
        encoder.abort();
        releaseExport();
        recorderManager.finishExport(false, message);
    }

    // Stopped from outside — the session already ended, so the half-written video is let go and removed here
    private void cancel() {

        awaitQuietly();
        encoder.abort();
        releaseExport();

        File file = recorderManager.getExportFile();

        if (file != null)
            file.delete();
    }

    // Lets frames still encoding finish before the file is let go, whatever they end in
    private void awaitQuietly() {

        for (int slot = 0; slot < EditorSetting.RECORDER_EXPORT_FRAME_SLOTS; slot++) {

            Future<?> future = frameFutures[slot];

            if (future == null)
                continue;

            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                errorLog(EditorSetting.RECORDER_MESSAGE_WRITE_FAILED + e.getCause().getMessage());
            }

            frameFutures[slot] = null;
        }
    }

    // The window renders at its own size again and nothing the export held is kept
    private void releaseExport() {

        WindowInstance window = context.getWindow();

        this.exporting = false;
        internal.endFixedClock();
        fboRenderSystem.clearCaptureTarget(window);
        window.clearFixedRenderSize();

        this.readback = null;
        this.frameSum = null;
        this.frames = null;
    }
}
