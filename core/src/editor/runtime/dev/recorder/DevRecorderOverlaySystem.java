package editor.runtime.dev.recorder;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.zip.CRC32C;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderOverlay;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayStreamStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.util.RecordingOverlayUtility;
import editor.runtime.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class DevRecorderOverlaySystem extends SystemPackage {

    /*
     * The interface channel in this Dev window: everything the window draws
     * over the world — the vitality effects, every menu and the HUD, the
     * inventory's scene — captured as the window drew it and played back the
     * same way. While a take records the interface, each overlay the window
     * pushed last frame has the pixels it drew read back a frame later
     * without stalling, and a frame that changed is kept, packed on the
     * encoding thread as its difference from the frame before, standing alone
     * every so often so the playhead can land anywhere; an overlay the window
     * stopped drawing is kept as hidden from then on. Nothing is read while
     * the session is suspended. While the interface plays, each overlay's
     * frame at the playhead is unpacked on the encoding thread, written into
     * a target made from the overlay's own definition, and pushed on the
     * overlay's own layer, so it composites exactly as it was drawn; the
     * frame last written stays up until the next is ready, and an export
     * waits for it through prepare(). Nothing of the window's own interface
     * shows while it plays: RecorderManager withholds its menus, and every
     * target the window pushed over the world this frame is taken back
     * before the recorded overlays are pushed. Renders after every other system of
     * the window, so every overlay it pushes this frame is known.
     */

    // Internal
    private RecorderManager recorderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private PBOManager pboManager;
    private WindowManager windowManager;
    private ThreadHandle encodingThread;

    // Capture
    private DevOverlayCaptureStruct[] captures;
    private RecordingTakeStruct take;
    private float lastFrameTime;
    private float lastSampleTime;
    private CRC32C checksum;
    private byte[] hiddenFrame;

    // Playback
    private DevOverlayPlaybackStruct[] playbacks;
    private boolean playing;

    // Base \\

    @Override
    protected void create() {

        // Capture
        this.captures = new DevOverlayCaptureStruct[RecorderOverlay.VALUES.length];
        this.checksum = new CRC32C();
        this.hiddenFrame = new byte[0];

        // Playback
        this.playbacks = new DevOverlayPlaybackStruct[RecorderOverlay.VALUES.length];

        for (int i = 0; i < RecorderOverlay.VALUES.length; i++) {
            captures[i] = new DevOverlayCaptureStruct(RecorderOverlay.VALUES[i]);
            playbacks[i] = new DevOverlayPlaybackStruct(RecorderOverlay.VALUES[i]);
        }
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.pboManager = get(PBOManager.class);
        this.windowManager = get(WindowManager.class);
        this.encodingThread = getThreadHandleFromThreadName(EditorSetting.RECORDER_ENCODING_THREAD_NAME);
    }

    @Override
    protected void dispose() {

        finishTake();
        releasePlayback();

        for (int i = 0; i < RecorderOverlay.VALUES.length; i++) {
            captures[i].getDeflater().end();
            playbacks[i].getInflater().end();
        }
    }

    // Take \\

    // A take that records the interface is captured into from now on
    public void beginTake(RecordingTakeStruct recorded) {

        finishTake();

        if (!recorded.holds(RecorderChannel.INTERFACE))
            return;

        this.take = recorded;
        this.lastFrameTime = recorderManager.getTakeTime();
        this.lastSampleTime = EditorSetting.RECORDER_TIME_UNSET;
    }

    // Hands every frame still being packed to the take, keeps the overlays hidden from its end, and lets go of it
    public void finishTake() {

        if (take == null)
            return;

        for (int i = 0; i < captures.length; i++) {

            DevOverlayCaptureStruct capture = captures[i];

            awaitJob(capture);
            drainRead(capture, false);
            capture.release();
        }

        this.take = null;
    }

    // Render \\

    @Override
    protected void render() {

        boolean session = recorderManager.isSessionFor((DevContext) context);

        if (session && take != null && recorderManager.getState() == RecorderState.RECORDING)
            capture();

        if (isPlayed()) {
            prepare();
            push();
            return;
        }

        releasePlayback();
    }

    // Capture \\

    private void capture() {

        WindowInstance window = context.getWindow();
        boolean suspended = recorderManager.isSuspended();
        float time = recorderManager.getTakeTime();
        boolean due = lastSampleTime < 0f || time - lastSampleTime
                >= EditorSetting.RECORDER_OVERLAY_SAMPLE_INTERVAL - EditorSetting.RECORDER_SAMPLE_TOLERANCE;

        for (int i = 0; i < captures.length; i++) {

            DevOverlayCaptureStruct capture = captures[i];
            FBOInstance drawn = capture.getPushed();

            collectJob(capture);
            capture.setPushed(fboRenderSystem.findPushedFbo(window, capture.getOverlay().getLayer()));

            if (suspended) {
                drainRead(capture, false);
                continue;
            }

            if (drawn == null) {
                drainRead(capture, true);
                keepHidden(capture, lastFrameTime);
                continue;
            }

            if (due)
                read(capture, drawn, lastFrameTime);
        }

        if (due && !suspended)
            this.lastSampleTime = time;

        this.lastFrameTime = time;
    }

    // Queues a read of what the target drew last frame, and keeps the read queued before it once it arrives
    private void read(DevOverlayCaptureStruct capture, FBOInstance drawn, float time) {

        WindowInstance window = context.getWindow();
        int width = drawn.getWidth();
        int height = drawn.getHeight();

        if (capture.getPbo() == null)
            capture.setPbo(pboManager.createPbo(window.getGLWindow(), width, height));

        capture.ensureReadback(Math.max(width, capture.getReadWidth()), Math.max(height, capture.getReadHeight()));

        boolean pending = capture.isReadPending();
        float pendingTime = capture.getReadTime();
        int pendingWidth = capture.getReadWidth();
        int pendingHeight = capture.getReadHeight();
        WindowInstance previous = windowManager.enterGLContext(window);
        boolean retrieved = capture.getPbo().captureFbo(window.getGLWindow(), drawn, capture.getReadback());

        windowManager.exitGLContext(previous);
        capture.queueRead(time, width, height);

        if (pending && retrieved)
            keep(capture, pendingTime, pendingWidth, pendingHeight);
    }

    // Takes in a read still queued, kept when asked, so nothing read before a change of course is lost or misplaced
    private void drainRead(DevOverlayCaptureStruct capture, boolean kept) {

        if (!capture.isReadPending())
            return;

        WindowInstance previous = windowManager.enterGLContext(context.getWindow());
        boolean retrieved = capture.getPbo().tryRetrieve(capture.getReadback());

        windowManager.exitGLContext(previous);
        capture.clearRead();

        if (kept && retrieved)
            keep(capture, capture.getReadTime(), capture.getReadWidth(), capture.getReadHeight());
    }

    // A frame read back is kept when it shows something new, packed on the encoding thread; one arriving while
    // another is still packed is let go, and the next that differs is kept in its place
    private void keep(DevOverlayCaptureStruct capture, float time, int width, int height) {

        int length = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;
        ByteBuffer readback = capture.getReadback();

        readback.position(0).limit(length);
        checksum.reset();
        checksum.update(readback);
        readback.position(0);

        long frameChecksum = checksum.getValue();

        if (capture.isShown() && frameChecksum == capture.getLastChecksum()
                && width == capture.getWidth() && height == capture.getHeight())
            return;

        if (capture.hasJob())
            return;

        boolean resized = capture.ensurePacking(width, height);
        boolean keyframe = resized || !capture.isShown()
                || capture.getFramesSinceKeyframe() >= EditorSetting.RECORDER_OVERLAY_KEYFRAME_INTERVAL;
        byte[] pixels = capture.getPixels();
        byte[] previous = capture.getPrevious();
        byte[] difference = capture.getDifference();
        byte[] packed = capture.getPacked();
        Deflater deflater = capture.getDeflater();

        readback.get(pixels, 0, length);
        readback.position(0);
        capture.keep(frameChecksum, keyframe);
        capture.beginJob(executeAsync(encodingThread, () -> {
            capture.setJobResult(RecordingOverlayUtility.encode(
                    pixels, length, keyframe ? null : previous, difference, packed, deflater));
            System.arraycopy(pixels, 0, previous, 0, length);
        }), time, width, height, keyframe);
    }

    // The overlay kept as hidden from the moment, once, after the frame still being packed
    private void keepHidden(DevOverlayCaptureStruct capture, float time) {

        if (!capture.isShown())
            return;

        awaitJob(capture);
        resolveStream(capture.getOverlay()).addFrame(time, 0, 0, false, hiddenFrame);
        capture.hide();
    }

    // A packed frame that is done joins the take
    private void collectJob(DevOverlayCaptureStruct capture) {
        if (capture.hasJob() && capture.getJob().isDone())
            awaitJob(capture);
    }

    // Waits for the frame being packed and adds it to the take; a frame lost makes the next one stand alone
    private void awaitJob(DevOverlayCaptureStruct capture) {

        if (!capture.hasJob())
            return;

        try {
            capture.getJob().get();
            resolveStream(capture.getOverlay()).addFrame(
                    capture.getJobTime(),
                    capture.getJobWidth(),
                    capture.getJobHeight(),
                    capture.isJobKeyframe(),
                    capture.getJobResult());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            capture.hide();
        } catch (ExecutionException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_FLUSH_FAILED + e.getCause().getMessage());
            capture.hide();
        }

        capture.clearJob();
    }

    private RecordingOverlayStreamStruct resolveStream(RecorderOverlay overlay) {
        return take.getOverlayTrack().getStream(overlay);
    }

    // Playback \\

    private boolean isPlayed() {
        return recorderManager.isSessionFor((DevContext) context)
                && RecorderChannel.INTERFACE.isIn(recorderManager.getPlayedChannels());
    }

    // Every overlay the timeline shows at the playhead is wanted, and unpacked when it is not yet written; true
    // once every overlay shows the frame the playhead calls for, and always while the interface does not play
    public boolean prepare() {

        if (!isPlayed())
            return true;

        float time = recorderManager.getPlayhead();
        RecordingClipStruct clip = recorderManager.getRecording().findPlayingClip(RecorderChannel.INTERFACE, time);
        RecordingOverlayTrackStruct track = clip == null ? null : clip.getTake().getOverlayTrack();
        boolean settled = true;

        this.playing = true;

        for (int i = 0; i < playbacks.length; i++) {

            DevOverlayPlaybackStruct playback = playbacks[i];

            collectDecode(playback);
            want(playback, track, clip, time);

            if (playback.isWanting() && !playback.hasJob()
                    && !playback.isDecoded(playback.getWantedStream(), playback.getWantedFrame())
                    && !playback.isRefused(playback.getWantedStream(), playback.getWantedFrame()))
                decode(playback, playback.getWantedStream(), playback.getWantedFrame());

            settled &= playback.isSettled();
        }

        return settled;
    }

    private void want(
            DevOverlayPlaybackStruct playback,
            RecordingOverlayTrackStruct track,
            RecordingClipStruct clip,
            float time) {

        if (track == null) {
            playback.clearWanted();
            return;
        }

        RecordingOverlayStreamStruct stream = track.getStream(playback.getOverlay());
        int frame = stream.findFrame(clip.toTakeTime(time));

        if (frame < 0 || stream.isHidden(frame)) {
            playback.clearWanted();
            return;
        }

        playback.want(stream, frame);
    }

    // Unpacks the frame on the encoding thread, onward from the frame last unpacked where that is its own run,
    // else from the last frame that stands alone
    private void decode(DevOverlayPlaybackStruct playback, RecordingOverlayStreamStruct stream, int frame) {

        int width = stream.getWidth(frame);
        int height = stream.getHeight(frame);
        int keyframe = stream.findKeyframe(frame);

        if (keyframe < 0) {
            refuse(playback, stream, frame);
            return;
        }

        boolean onward = playback.getDecodedStream() == stream && playback.getDecodedFrame() >= keyframe
                && playback.getDecodedFrame() < frame
                && playback.getDecodedWidth() == width && playback.getDecodedHeight() == height;

        playback.ensureUnpacking(width, height);

        int from = onward ? playback.getDecodedFrame() + 1 : keyframe;
        int length = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;
        byte[] pixels = playback.getPixels();
        byte[] difference = playback.getDifference();
        ByteBuffer upload = playback.getUpload();
        Inflater inflater = playback.getInflater();

        playback.beginJob(executeAsync(encodingThread, () -> {

            for (int i = from; i <= frame; i++)
                if (!stream.isHidden(i) && !RecordingOverlayUtility.decode(
                        stream.getFrame(i), stream.isKeyframe(i), pixels, difference, length, inflater)) {
                    playback.failJob();
                    return;
                }

            upload.clear();
            upload.put(pixels, 0, length);
            upload.flip();
        }), stream, frame);
    }

    // An unpacked frame that is done is written into the overlay's target
    private void collectDecode(DevOverlayPlaybackStruct playback) {

        if (!playback.hasJob() || !playback.getJob().isDone())
            return;

        RecordingOverlayStreamStruct stream = playback.getJobStream();
        int frame = playback.getJobFrame();
        boolean failed = playback.isJobFailed();

        try {
            playback.getJob().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failed = true;
        } catch (ExecutionException e) {
            failed = true;
        }

        playback.clearJob();

        if (failed) {
            refuse(playback, stream, frame);
            return;
        }

        playback.setDecoded(stream, frame, stream.getWidth(frame), stream.getHeight(frame));
        writeFrame(playback);
    }

    private void refuse(DevOverlayPlaybackStruct playback, RecordingOverlayStreamStruct stream, int frame) {
        errorLog(EditorSetting.RECORDER_MESSAGE_OVERLAY_UNREADABLE + playback.getOverlay().name());
        playback.refuse(stream, frame);
        playback.clearDecoded();
    }

    private void writeFrame(DevOverlayPlaybackStruct playback) {

        WindowInstance window = context.getWindow();

        if (playback.getFbo() == null)
            playback.setFbo(fboManager.cloneFbo(playback.getOverlay().getFboName(), window));

        fboManager.writeColor(
                playback.getFbo(),
                playback.getDecodedWidth(),
                playback.getDecodedHeight(),
                playback.getUpload(),
                window);
    }

    // The window's own interface is taken back, then each overlay showing a frame is pushed on its own layer; one
    // its window resized since is written again first
    private void push() {

        WindowInstance window = context.getWindow();

        fboRenderSystem.withdrawPushedFbos(window, EditorSetting.RECORDER_INTERFACE_LIVE_LAYER);

        for (int i = 0; i < playbacks.length; i++) {

            DevOverlayPlaybackStruct playback = playbacks[i];

            if (!playback.isWanting() || !playback.hasDecoded())
                continue;

            FBOInstance fbo = playback.getFbo();

            if (fbo.getWidth() != playback.getDecodedWidth() || fbo.getHeight() != playback.getDecodedHeight()) {

                if (playback.hasJob())
                    continue;

                writeFrame(playback);
            }

            fboRenderSystem.pushFbo(fbo, playback.getOverlay().getLayer(), window);
        }
    }

    // Once nothing plays, every frame being unpacked is let finish and every buffer let go
    private void releasePlayback() {

        if (!playing)
            return;

        for (int i = 0; i < playbacks.length; i++) {

            DevOverlayPlaybackStruct playback = playbacks[i];

            if (playback.hasJob()) {
                try {
                    playback.getJob().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    errorLog(EditorSetting.RECORDER_MESSAGE_OVERLAY_UNREADABLE + playback.getOverlay().name());
                }
            }

            playback.release();
        }

        this.playing = false;
    }
}
