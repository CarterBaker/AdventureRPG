package editor.runtime.dev.recorder;

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
import editor.bootstrap.recorderpipeline.util.VideoFrameUtility;
import editor.bootstrap.recorderpipeline.video.AviVideoEncoderInstance;
import editor.bootstrap.recorderpipeline.video.H264VideoEncoderInstance;
import editor.bootstrap.recorderpipeline.video.ProresVideoEncoderInstance;
import editor.bootstrap.recorderpipeline.video.VideoEncoderInstance;
import editor.runtime.dev.DevContext;
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
     * standing still meanwhile. Every layer the window draws, its interface
     * included, is gathered into one capture target, so the video shows
     * exactly what the window does, and read back a frame later without
     * stalling. Motion blur renders several moments across a 180 degree
     * shutter and supersampling renders at twice the size; both are averaged
     * down into each video frame, which goes to the encoder on its own thread
     * while the next renders, with the stretch of sound the timeline's sounds
     * mix to across it when the export range holds any. A frame also waits
     * until the
     * recorded interface shows the moment it is rendered at, and nothing is
     * rendered while the game is paused under the export or while the Dev tab
     * waits hidden behind another in its stack, since it then shows nothing to
     * capture. This system updates
     * before DevRecorderSystem, so the playhead it sets is the one played that
     * frame.
     */

    // Internal
    private RecorderManager recorderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private PBOManager pboManager;
    private WindowManager windowManager;
    private DevRecorderOverlaySystem devRecorderOverlaySystem;
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

    // Audio
    private boolean audio;
    private int audioFramesPerFrame;

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
    private short[][] audioFrames;
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
        this.devRecorderOverlaySystem = get(DevRecorderOverlaySystem.class);
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

        this.audio = recorderManager.prepareExportMix(EditorSetting.RECORDER_AUDIO_SAMPLE_RATE);
        this.audioFramesPerFrame = EditorSetting.RECORDER_AUDIO_SAMPLE_RATE / settings.getFrameRate();
        this.encoder = createEncoder(settings.getFormat());

        if (!encoder.open(
                recorderManager.getExportFile(), outputWidth, outputHeight, settings.getFrameRate(), audio)) {
            recorderManager.releaseExportMix();
            recorderManager.finishExport(false, encoder.getFailure());
            return false;
        }

        window.setFixedRenderSize(renderWidth, renderHeight);

        if (captureFbo == null)
            this.captureFbo = fboManager.cloneFbo(EditorSetting.RECORDER_CAPTURE_FBO, window);

        if (pbo == null)
            this.pbo = pboManager.createPbo(window.getGLWindow(), renderWidth, renderHeight);

        fboManager.resize(captureFbo, renderWidth, renderHeight);
        fboRenderSystem.setCaptureTarget(window, captureFbo);

        int frameBytes = outputWidth * outputHeight * EditorSetting.RECORDER_FRAME_BYTES_PER_PIXEL;

        this.readback = ByteBuffer.allocateDirect(renderWidth * renderHeight * EngineSetting.BYTES_PER_PIXEL_BGRA);
        this.frameSum = new int[frameBytes];
        this.frames = new byte[EditorSetting.RECORDER_EXPORT_FRAME_SLOTS][frameBytes];
        this.audioFrames = audio
                ? new short[EditorSetting.RECORDER_EXPORT_FRAME_SLOTS]
                        [audioFramesPerFrame * EditorSetting.RECORDER_AUDIO_CHANNELS]
                : null;
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
        return switch (format) {
            case MP4 -> create(H264VideoEncoderInstance.class);
            case MOV -> create(ProresVideoEncoderInstance.class);
            case AVI -> create(AviVideoEncoderInstance.class);
        };
    }

    // Step \\

    // Settles or renders a capture this frame, and sets how far the clock moves into the next
    private void step() {

        if (finishing) {
            finish();
            return;
        }

        float time = resolveTime(nextFrame, nextSubframe);

        recorderManager.setExportPlayhead(time);
        this.renderedCapture = false;

        if (recorderManager.isSuspended() || context.getWindow().isWithdrawn()) {
            internal.setFixedFrameSeconds(0f);
            return;
        }

        boolean idle = isStreamingIdle() && devRecorderOverlaySystem.prepare();

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

        WindowInstance previous = windowManager.enterGLContext(context.getWindow());
        boolean retrieved = pbo.captureFbo(context.getWindow().getGLWindow(), captureFbo, readback);

        windowManager.exitGLContext(previous);
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
        short[] audioFrame = audio ? audioFrames[slot] : null;
        VideoEncoderInstance frameEncoder = encoder;

        VideoFrameUtility.resolve(frameSum, subframes * scale * scale, frame);

        if (audio)
            recorderManager.mixExportAudio(resolveTime(encodedFrames, 0), audioFrame, audioFramesPerFrame);

        frameFutures[slot] = executeAsync(encodingThread, () -> {
            frameEncoder.encodeFrame(frame);
            frameEncoder.encodeAudio(audioFrame);
        });

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

    // End \\

    // Takes in the last capture, lets every frame finish encoding and closes the video
    private void finish() {

        WindowInstance previous = windowManager.enterGLContext(context.getWindow());

        if (pendingCount > 0 && pbo.tryRetrieve(readback)) {
            windowManager.exitGLContext(previous);
            consume();
        } else
            windowManager.exitGLContext(previous);

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
        this.audioFrames = null;
        recorderManager.releaseExportMix();
    }
}
