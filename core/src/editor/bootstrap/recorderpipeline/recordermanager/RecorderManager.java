package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveKind;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

public class RecorderManager extends ManagerPackage {

    /*
     * Owns the recorder: the recordings saved in the game's own directory, the
     * one open, its transport, and the editing of its bin and timeline. A
     * session — recording, playing, paused or exporting — drives the first
     * Dev window that has not crashed, found among TabManager's open tabs as
     * console commands find it, and that window's DevRecorderSystem carries
     * the session out inside its own crash boundary from what this holds:
     * which channels play from the timeline, which the take records live,
     * and the playhead. A take records the armed channels from the playhead
     * until stopped, the clips beneath playing every other channel; it then
     * joins the bin and is laid on the timeline where it was recorded, over
     * every clip it was recorded against. Takes are laid again by dragging
     * them from the bin, and clips are moved by dragging, trimmed by dragging
     * an edge or at the playhead, cut in two at the playhead or with the cut
     * tool wherever it is pressed, stamped, muted, given or stripped of
     * channels, and removed, each by one method here; what is dragged snaps
     * to the timeline's edges, and the cut tool shows where it would cut
     * under the pointer. A press on the lanes is read once, where it
     * began, for what it takes hold of. The nodes of every clip playing the
     * player — what it does, run by run — are rebuilt after every edit, and
     * are retimed, moved, changed and removed here too. The view of the
     * timeline zooms and scrolls, and pages along after a playing playhead.
     * The Keys tab shows the selected clip's take track by track — the
     * camera, the player, every resident, item and vehicle, the weather and
     * the clock — and a track opened there has every value it holds laid on
     * a curve from what it recorded, which plays instead from then on. Keys
     * are added, deleted, retimed on their rows, moved and bent by their
     * handles on a graph, given a mode or a typed value, and a curve is laid
     * again from the recording or let go to play as recorded, each by one
     * method here; a key or handle dragged changes its curve as it moves.
     * The player cannot be recorded without the camera that
     * follows it. While a session plays the world, residents, thrown items
     * and vehicles are held. While the game is paused the session is
     * suspended — the playhead waits and a take records nothing — and it
     * carries on a few frames after the pause ends, so the pause menu never
     * reaches a take. Stopping ends every session and the Dev window goes
     * back to just where it was. Each structural change marks the recording
     * unsaved and moves the revision the recorder tab redraws on. Takes save
     * the recording at once.
     */

    // Internal
    private TabManager tabManager;
    private WorldManager worldManager;
    private ClockManager clockManager;
    private NpcManager npcManager;
    private ProjectileManager projectileManager;
    private VehicleManager vehicleManager;
    private RecordingFileBranch recordingFileBranch;
    private RecordingTimelineBranch recordingTimelineBranch;
    private RecordingNodeBranch recordingNodeBranch;
    private RecordingCurveBranch recordingCurveBranch;

    // Directory
    private File recordingDirectory;
    private File videoDirectory;

    // Recording
    private RecordingInstance recording;
    private boolean unsaved;

    // Transport
    private RecorderState state;
    private float playhead;
    private int armedChannels;
    private RecordingTakeStruct take;
    private float takeStart;
    private int takeCount;
    private DevContext devContext;

    // Suspension
    private boolean suspended;
    private int resumeFrames;

    // Selection
    private int selectedTake;
    private int selectedClip;
    private int selectedMarker;
    private int selectedNode;
    private int selectedNodeClip;
    private RecordingBodyTrackStruct selectedNodeTrack;
    private RecorderNodeKind selectedNodeKind;
    private float selectedNodeTime;

    // Nodes
    private ObjectArrayList<RecordingNodeStruct> nodes;

    // Drag
    private RecordingDragStruct drag;

    // Cut
    private boolean cutting;
    private int cutHoverClip;
    private float cutHoverTime;

    // View
    private RecordingViewStruct view;
    private int viewRevision;

    // Keys
    private boolean keysShown;
    private boolean graphShown;
    private RecordingTakeStruct keyTake;
    private ObjectArrayList<RecordingCurveTrackStruct> keyTracks;
    private ReferenceOpenHashSet<RecordingCurveTrackStruct> openedTracks;
    private RecordingCurveTrackStruct selectedTrack;
    private int selectedSlot;
    private int selectedKey;
    private double graphMinimum;
    private double graphMaximum;
    private int keysRevision;
    private int curveRevision;

    // Export
    private File exportFile;
    private int exportedFrames;
    private int exportFrameCount;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.recordingFileBranch = create(RecordingFileBranch.class);
        this.recordingTimelineBranch = create(RecordingTimelineBranch.class);
        this.recordingNodeBranch = create(RecordingNodeBranch.class);
        this.recordingCurveBranch = create(RecordingCurveBranch.class);

        // Directory
        this.recordingDirectory = new File(internal.path, EditorSetting.RECORDER_DIRECTORY);
        this.videoDirectory = new File(recordingDirectory, EditorSetting.RECORDER_VIDEO_DIRECTORY);

        // Transport
        this.state = RecorderState.IDLE;
        this.armedChannels = RecorderChannel.ALL;

        // Selection
        clearSelection();

        // Nodes
        this.nodes = new ObjectArrayList<>();

        // Drag
        this.drag = new RecordingDragStruct();

        // Cut
        this.cutHoverClip = EngineSetting.INDEX_NOT_FOUND;

        // View
        this.view = new RecordingViewStruct();

        // Keys
        this.keyTracks = new ObjectArrayList<>();
        this.openedTracks = new ReferenceOpenHashSet<>();
        clearCurveSelection();

        // Status
        this.statusText = EditorSetting.RECORDER_STATUS_READY;
    }

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
        this.npcManager = get(NpcManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        releaseCrashedDevContext();
        updateSuspension();
        advancePlayhead();
        updateView();
        updateKeyTracks();
        holdWorld();
    }

    private void releaseCrashedDevContext() {
        if (devContext != null && devContext.isCrashed())
            stop();
    }

    // A running session waits while the game is paused, and for a few frames after, so nothing of the pause is kept
    private void updateSuspension() {

        boolean wanted = state.isSession() && clockManager.isPaused();

        if (wanted)
            this.resumeFrames = EditorSetting.RECORDER_RESUME_FRAMES;
        else if (resumeFrames > 0)
            this.resumeFrames--;

        boolean nowSuspended = state.isSession() && (wanted || resumeFrames > 0);

        if (nowSuspended == suspended)
            return;

        this.suspended = nowSuspended;
        setStatus(suspended ? EditorSetting.RECORDER_STATUS_SUSPENDED : resolveRunningStatus());
    }

    private String resolveRunningStatus() {
        return switch (state) {
            case RECORDING -> EditorSetting.RECORDER_STATUS_RECORDING + take.getName();
            case PLAYING -> EditorSetting.RECORDER_STATUS_PLAYING;
            case PAUSED -> EditorSetting.RECORDER_STATUS_PAUSED;
            case EXPORTING -> EditorSetting.RECORDER_STATUS_EXPORTING + exportFile.getName();
            case IDLE -> EditorSetting.RECORDER_STATUS_READY;
        };
    }

    private void advancePlayhead() {

        if (suspended || state != RecorderState.RECORDING && state != RecorderState.PLAYING)
            return;

        playhead += internal.getDeltaTime();

        if (state == RecorderState.PLAYING && playhead >= recording.getDuration()) {
            playhead = recording.getDuration();
            state = RecorderState.PAUSED;
            revision++;
        }
    }

    // The view keeps to the timeline as it grows and shrinks, and pages along after a running playhead
    private void updateView() {

        view.resolve(resolveTimelineSpan());

        if (state == RecorderState.RECORDING || state == RecorderState.PLAYING || state == RecorderState.EXPORTING)
            revealPlayhead();
    }

    private void revealPlayhead() {
        if (view.reveal(playhead, resolveTimelineSpan()))
            viewRevision++;
    }

    // The world is held whenever a session plays it rather than recording it live
    private void holdWorld() {

        int played = devContext != null ? getPlayedChannels() : RecorderChannel.NONE;

        npcManager.setHeld(RecorderChannel.CHARACTERS.isIn(played));
        projectileManager.setHeld(RecorderChannel.ITEMS.isIn(played));
        vehicleManager.setHeld(RecorderChannel.VEHICLES.isIn(played));
    }

    // Session \\

    // Binds the session to the first Dev window still running, in the world the recording was made in
    private boolean bindDevContext() {

        if (!recording.getWorldName().equals(worldManager.getActiveWorld().getWorldName())) {
            setStatus(EditorSetting.RECORDER_STATUS_WRONG_WORLD + recording.getWorldName());
            return false;
        }

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();

        for (int i = 0; i < openTabs.size(); i++)
            if (openTabs.get(i).getTabContext().getContentContext() instanceof DevContext candidate
                    && !candidate.isCrashed()) {
                this.devContext = candidate;
                return true;
            }

        setStatus(EditorSetting.RECORDER_STATUS_NO_DEV_WINDOW);
        return false;
    }

    // Starts a session if none runs, true once one does
    private boolean beginSession() {
        return state.isSession() || bindDevContext();
    }

    // The Dev window leaving ends any session it held
    public void releaseDevContext(DevContext context) {
        if (devContext == context)
            stop();
    }

    // Transport \\

    public void record() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        if (armedChannels == RecorderChannel.NONE) {
            setStatus(EditorSetting.RECORDER_STATUS_NOTHING_ARMED);
            return;
        }

        if (recording == null && !createRecording(resolveDefaultRecordingName()))
            return;

        if (!beginSession())
            return;

        cancelDrag();
        this.take = new RecordingTakeStruct(resolveTakeName(), armedChannels);
        this.takeStart = playhead;
        this.takeCount++;
        this.state = RecorderState.RECORDING;
        setStatus(EditorSetting.RECORDER_STATUS_RECORDING + take.getName());
    }

    public void play() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING || !hasClips())
            return;

        if (!beginSession())
            return;

        if (playhead >= recording.getDuration())
            playhead = 0f;

        this.state = RecorderState.PLAYING;
        setStatus(EditorSetting.RECORDER_STATUS_PLAYING);
    }

    public void pause() {

        switch (state) {
            case PLAYING -> {
                this.state = RecorderState.PAUSED;
                setStatus(EditorSetting.RECORDER_STATUS_PAUSED);
            }
            case PAUSED -> play();
            case IDLE -> preview();
            default -> {
            }
        }
    }

    // Holds the Dev window at the playhead as the timeline stands there
    private void preview() {

        if (!hasClips() || !beginSession())
            return;

        this.state = RecorderState.PAUSED;
        setStatus(EditorSetting.RECORDER_STATUS_PAUSED);
    }

    // The one way every session ends; the Dev window returns to just where it was before the session began
    public void stop() {

        if (!state.isSession())
            return;

        if (state == RecorderState.RECORDING)
            finishTake();

        if (state == RecorderState.EXPORTING)
            setStatus(EditorSetting.RECORDER_STATUS_EXPORT_CANCELLED);
        else if (state != RecorderState.RECORDING)
            setStatus(EditorSetting.RECORDER_STATUS_READY);

        endSession();
    }

    private void endSession() {
        this.state = RecorderState.IDLE;
        this.devContext = null;
        this.suspended = false;
        this.resumeFrames = 0;
        holdWorld();
    }

    // The take joins the bin and is laid where it was recorded, once the Dev window has handed over all it holds
    private void finishTake() {

        take.setDuration(playhead - takeStart);
        devContext.finishTake();

        if (take.getDuration() < EditorSetting.RECORDER_MIN_TAKE_SECONDS) {
            setStatus(EditorSetting.RECORDER_STATUS_TAKE_TOO_SHORT);
            this.take = null;
            return;
        }

        recording.addTake(take);
        this.selectedTake = recording.getTakes().size() - 1;
        this.selectedClip = recordingTimelineBranch.placeRecordedTake(recording, take, takeStart);
        this.take = null;
        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_SAVED + recording.getTakes().get(selectedTake).getName());
    }

    // Seek \\

    public void seek(float time) {

        if (recording == null || state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        this.playhead = Math.clamp(time, 0f, recording.getDuration());
        revealPlayhead();

        if (state == RecorderState.IDLE)
            preview();
    }

    // The moment a share of the way across the view
    public void scrub(float fraction) {
        if (recording != null)
            seek(view.toTime(fraction));
    }

    public void toStart() {
        seek(0f);
    }

    public void toEnd() {
        if (recording != null)
            seek(recording.getDuration());
    }

    public void previousMarker() {

        if (recording == null)
            return;

        ObjectArrayList<RecordingMarkerStruct> markers = recording.getMarkers();

        for (int i = markers.size() - 1; i >= 0; i--)
            if (markers.get(i).getTime() < playhead - EditorSetting.RECORDER_MARKER_STEP_TOLERANCE) {
                selectMarker(i);
                return;
            }

        seek(0f);
    }

    public void nextMarker() {

        if (recording == null)
            return;

        ObjectArrayList<RecordingMarkerStruct> markers = recording.getMarkers();

        for (int i = 0; i < markers.size(); i++)
            if (markers.get(i).getTime() > playhead + EditorSetting.RECORDER_MARKER_STEP_TOLERANCE) {
                selectMarker(i);
                return;
            }

        toEnd();
    }

    // How long the recording runs, and on while a take records past it
    public float resolveLength() {
        return recording != null ? Math.max(recording.getDuration(), playhead) : playhead;
    }

    // The timeline drawn runs the recording's length and a little past it, so a clip can be laid after the last
    public float resolveTimelineSpan() {

        float length = resolveLength();

        return Math.max(
                EditorSetting.RECORDER_MIN_TIMELINE_SECONDS,
                length + Math.max(
                        EditorSetting.RECORDER_TIMELINE_TAIL_SECONDS,
                        length * EditorSetting.RECORDER_TIMELINE_TAIL_SHARE));
    }

    // View \\

    // The view zoomed by the factor about the moment, which stays where it is drawn
    public void zoomView(float time, float factor) {
        if (view.zoom(time, factor, resolveTimelineSpan()))
            viewRevision++;
    }

    // The buttons zoom about the playhead while it is in view, and about the middle of the view while it is not
    public void zoomViewIn() {
        zoomView(resolveZoomAnchor(), 1f / EditorSetting.RECORDER_ZOOM_BUTTON_STEP);
    }

    public void zoomViewOut() {
        zoomView(resolveZoomAnchor(), EditorSetting.RECORDER_ZOOM_BUTTON_STEP);
    }

    private float resolveZoomAnchor() {
        return view.contains(playhead) ? playhead : view.getMiddle();
    }

    public void fitView() {
        view.fit();
        view.resolve(resolveTimelineSpan());
        viewRevision++;
    }

    // The view moved along by a share of its own length, back for a share below zero
    public void scrollView(float share) {
        if (view.scrollTo(view.getStart() + share * view.getSpan(), resolveTimelineSpan()))
            viewRevision++;
    }

    // Arming \\

    // The player follows the camera, so arming the player arms the camera and disarming the camera the player
    public void toggleArm(RecorderChannel channel) {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        armedChannels ^= channel.getBit();

        if (channel == RecorderChannel.PLAYER && RecorderChannel.PLAYER.isIn(armedChannels))
            armedChannels |= RecorderChannel.CAMERA.getBit();

        if (channel == RecorderChannel.CAMERA && !RecorderChannel.CAMERA.isIn(armedChannels))
            armedChannels &= ~RecorderChannel.PLAYER.getBit();

        revision++;
    }

    // Recordings \\

    public boolean createRecording(String recordingName) {

        if (!isRecordingNameAvailable(recordingName)) {
            setStatus(EditorSetting.RECORDER_STATUS_NAME_TAKEN + recordingName);
            return false;
        }

        closeRecording();

        RecordingInstance created = create(RecordingInstance.class);
        created.constructor(
                recordingName,
                worldManager.getActiveWorld().getWorldName(),
                new RecordingExportStruct());

        activateRecording(created);
        markUnsaved();
        saveRecording();
        return true;
    }

    public void openRecording(String recordingName) {

        if (recording != null && recording.getRecordingName().equals(recordingName))
            return;

        File file = getRecordingFile(recordingName);

        if (!file.isFile()) {
            setStatus(EditorSetting.RECORDER_STATUS_MISSING + recordingName);
            return;
        }

        RecordingInstance opened = recordingFileBranch.read(file, recordingName);

        if (opened == null) {
            setStatus(EditorSetting.RECORDER_STATUS_UNREADABLE + recordingName);
            return;
        }

        closeRecording();
        activateRecording(opened);

        if (!recordingFileBranch.isUpgraded()) {
            setStatus(EditorSetting.RECORDER_STATUS_OPENED + recordingName);
            return;
        }

        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_UPGRADED + recordingName);
    }

    public void saveRecording() {

        if (recording == null || !unsaved)
            return;

        recordingDirectory.mkdirs();

        if (!recordingFileBranch.write(getRecordingFile(recording.getRecordingName()), recording)) {
            setStatus(EditorSetting.RECORDER_STATUS_SAVE_FAILED + recording.getRecordingName());
            return;
        }

        this.unsaved = false;
        revision++;
    }

    public void renameRecording(String recordingName) {

        if (recording == null || state.isSession() || !isRecordingNameAvailable(recordingName))
            return;

        saveRecording();

        File previous = getRecordingFile(recording.getRecordingName());

        if (!previous.renameTo(getRecordingFile(recordingName))) {
            setStatus(EditorSetting.RECORDER_STATUS_RENAME_FAILED + recording.getRecordingName());
            return;
        }

        recording.setRecordingName(recordingName);
        setStatus(EditorSetting.RECORDER_STATUS_RENAMED + recordingName);
    }

    public void deleteRecording() {

        if (recording == null)
            return;

        stop();

        File file = getRecordingFile(recording.getRecordingName());
        String recordingName = recording.getRecordingName();

        if (file.exists() && !file.delete()) {
            setStatus(EditorSetting.RECORDER_STATUS_DELETE_FAILED + recordingName);
            return;
        }

        this.recording = null;
        this.unsaved = false;
        cancelDrag();
        clearSelection();
        rebuildNodes();
        setStatus(EditorSetting.RECORDER_STATUS_DELETED + recordingName);
    }

    // Saves and lets go of the open recording, ending any session on it
    private void closeRecording() {

        if (recording == null)
            return;

        stop();
        saveRecording();
        this.recording = null;
    }

    private void activateRecording(RecordingInstance activated) {
        this.recording = activated;
        this.unsaved = false;
        this.playhead = 0f;
        cancelDrag();
        clearSelection();
        openedTracks.clear();
        fitView();
        rebuildNodes();
        revision++;
    }

    // Takes \\

    public void selectTake(int takeIndex) {

        if (!isTakeIndex(takeIndex))
            return;

        this.selectedTake = takeIndex;
        revision++;
    }

    public void renameTake(int takeIndex, String takeName) {

        if (!isTakeIndex(takeIndex) || !isLabelValid(takeName))
            return;

        recording.getTakes().get(takeIndex).setName(takeName);
        markUnsaved();
    }

    // The take leaves the bin and every clip of it the timeline
    public void deleteTake(int takeIndex) {

        if (!isTakeIndex(takeIndex) || !canEditTimeline())
            return;

        String takeName = recording.getTakes().get(takeIndex).getName();

        recording.removeTake(takeIndex);
        clearSelection();
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_DELETED + takeName);
    }

    // The whole take laid from the playhead, on the lowest clear lane
    public void placeTakeAtPlayhead(int takeIndex) {
        placeTake(takeIndex, playhead, 0);
    }

    public void placeTake(int takeIndex, float start, int lane) {

        if (!isTakeIndex(takeIndex) || !canEditTimeline())
            return;

        RecordingTakeStruct placed = recording.getTakes().get(takeIndex);

        this.selectedTake = takeIndex;
        this.selectedClip = recordingTimelineBranch.placeTake(recording, placed, start, lane);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_PLACED + placed.getName());
    }

    // Clips \\

    public void selectClip(int clipIndex) {

        if (!isClipIndex(clipIndex))
            return;

        if (clipIndex != selectedNodeClip)
            clearNodeSelection();

        this.selectedClip = clipIndex;
        this.selectedTake = recording.getTakes().indexOf(recording.getClips().get(clipIndex).getTake());
        revision++;
    }

    // The clip selected with none of its nodes, as a click on it away from them selects it
    private void selectClipAlone(int clipIndex) {
        clearNodeSelection();
        selectClip(clipIndex);
    }

    // The clip's start or end dragged to the moment, as far as its take and the clips beside it allow
    public void trimClipTo(int clipIndex, float time, boolean start) {

        if (!isClipIndex(clipIndex) || !canEditTimeline())
            return;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);

        if (start)
            recordingTimelineBranch.trimStartTo(clip, recordingTimelineBranch.resolveTrimStart(recording, clip, time));
        else
            recordingTimelineBranch.trimEndTo(clip, recordingTimelineBranch.resolveTrimEnd(recording, clip, time));

        this.selectedClip = clipIndex;
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRIMMED + clip.getTake().getName());
    }

    public void moveClip(int clipIndex, float start, int lane) {

        if (!isClipIndex(clipIndex) || !canEditTimeline())
            return;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);

        recordingTimelineBranch.moveClip(recording, clip, start, lane);
        this.selectedClip = clipIndex;
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_MOVED + clip.getTake().getName());
    }

    // The selected clip plays the channel or stops playing it; only a channel its take recorded can be given
    public void toggleClipChannel(RecorderChannel channel) {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null || !clip.getTake().holds(channel))
            return;

        clip.setChannelMask(clip.getChannelMask() ^ channel.getBit());
        markUnsaved();
    }

    public void toggleClipMute() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        clip.setMuted(!clip.isMuted());
        markUnsaved();
    }

    // Steps the clip's clock jumps through each ramp in turn, back to a cut after the longest
    public void cycleClipTimeRamp() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        float[] ramps = EditorSetting.RECORDER_TIME_RAMP_SECONDS;
        int index = 0;

        while (index < ramps.length && ramps[index] != clip.getTimeRampSeconds())
            index++;

        clip.setTimeRampSeconds(ramps[(index + 1) % ramps.length]);
        markUnsaved();
    }

    // The selected clip cut in two at the playhead
    public void splitClip() {
        if (requireSelectedClip() != null && !cutClip(selectedClip, playhead))
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
    }

    // The one way a clip is cut in two; the piece after the cut is selected, and false when the moment is too near
    // either end of the clip to cut there
    private boolean cutClip(int clipIndex, float time) {

        if (!isClipIndex(clipIndex) || !canEditTimeline())
            return false;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);
        int cut = recordingTimelineBranch.splitClip(recording, clip, time);

        if (cut == EngineSetting.INDEX_NOT_FOUND)
            return false;

        clearNodeSelection();
        this.selectedClip = cut;
        this.selectedTake = recording.getTakes().indexOf(clip.getTake());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_CUT + clip.getTake().getName());
        return true;
    }

    // Cut \\

    // The cut tool turned on or off; while it is on a press on a clip cuts it instead of taking hold of it
    public void toggleCutTool() {

        this.cutting = !cutting;
        clearCutHover();
        setStatus(cutting ? EditorSetting.RECORDER_STATUS_CUT_TOOL_ON : EditorSetting.RECORDER_STATUS_CUT_TOOL_OFF);
    }

    // Where the cut tool would cut with the pointer where it is now, nowhere when it is over no clip
    public void hoverCut(float time, int lane, float secondsPerPixel, boolean snapping) {

        int clipIndex = cutting && canEditTimeline() ? recording.findClipAt(time, lane) : EngineSetting.INDEX_NOT_FOUND;

        this.cutHoverClip = clipIndex;

        if (clipIndex != EngineSetting.INDEX_NOT_FOUND)
            this.cutHoverTime = recordingTimelineBranch.resolveCut(recording, recording.getClips().get(clipIndex),
                    time, playhead, snapping ? EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel : 0f);
    }

    public void clearCutHover() {
        this.cutHoverClip = EngineSetting.INDEX_NOT_FOUND;
    }

    public void trimClipStart() {
        trimClip(true);
    }

    public void trimClipEnd() {
        trimClip(false);
    }

    private void trimClip(boolean start) {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        boolean trimmed = start
                ? recordingTimelineBranch.trimStart(clip, playhead)
                : recordingTimelineBranch.trimEnd(clip, playhead);

        if (!trimmed) {
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
            return;
        }

        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRIMMED + clip.getTake().getName());
    }

    // A copy of the selected clip laid from the playhead
    public void stampClip() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        this.selectedClip = recordingTimelineBranch.stampClip(recording, clip, playhead);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_STAMPED + clip.getTake().getName());
    }

    // The selected clip leaves the timeline; its take stays in the bin
    public void removeClip() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        recording.removeClip(selectedClip);
        this.selectedClip = EngineSetting.INDEX_NOT_FOUND;
        clearNodeSelection();
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_CLIP_REMOVED + clip.getTake().getName());
    }

    // The selected clip when the timeline can be edited, null after saying why not
    private RecordingClipStruct requireSelectedClip() {

        if (!canEditTimeline())
            return null;

        if (!isClipIndex(selectedClip)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CLIP);
            return null;
        }

        return recording.getClips().get(selectedClip);
    }

    // Nothing on the timeline changes under a take or an export
    private boolean canEditTimeline() {
        return recording != null && state != RecorderState.RECORDING && state != RecorderState.EXPORTING;
    }

    // Drag \\

    // A take picked up from the bin, to be laid where it is let go
    public void beginTakeDrag(int takeIndex, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !canEditTimeline() || !isTakeIndex(takeIndex))
            return;

        beginDrag(RecorderDragKind.TAKE, takeIndex, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, 0f, 0f);
    }

    // What a press on the lanes takes hold of, read once where it began: a clip's edge to trim it, a node's notch or a
    // gesture to retime or move it, a clip to move it, or the empty lanes to scroll the view; the timeline can only
    // be scrolled under a take or an export
    public void pressLanes(
            float time,
            int lane,
            boolean nameRow,
            float secondsPerPixel,
            WindowInstance window,
            float pressX,
            float pressY) {

        if (drag.isActive() || recording == null)
            return;

        int clipIndex = canEditTimeline() ? recording.findClipAt(time, lane) : EngineSetting.INDEX_NOT_FOUND;

        if (cutting && clipIndex != EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.CUT, clipIndex, EngineSetting.INDEX_NOT_FOUND,
                    window, pressX, pressY, time, 0f);
            landCut(time, EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel);
            return;
        }

        int startEdge = canEditTimeline() ? recordingTimelineBranch.findStartEdge(
                recording, time, lane, EditorSetting.RECORDER_EDGE_GRAB_PIXELS * secondsPerPixel)
                : EngineSetting.INDEX_NOT_FOUND;
        int endEdge = canEditTimeline() ? recordingTimelineBranch.findEndEdge(
                recording, time, lane, EditorSetting.RECORDER_EDGE_GRAB_PIXELS * secondsPerPixel)
                : EngineSetting.INDEX_NOT_FOUND;

        if (startEdge != EngineSetting.INDEX_NOT_FOUND || endEdge != EngineSetting.INDEX_NOT_FOUND) {
            beginTrimDrag(time, startEdge, endEdge, clipIndex, window, pressX, pressY);
            return;
        }

        if (clipIndex == EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.PAN, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                    window, pressX, pressY, time, 0f);
            return;
        }

        float notchTolerance = EditorSetting.RECORDER_NOTCH_GRAB_PIXELS * secondsPerPixel;
        int nodeIndex = recordingNodeBranch.findNodeAt(nodes, clipIndex,
                recording.getClips().get(clipIndex).getTake().getPlayerTrack(), time, nameRow, notchTolerance);

        if (nodeIndex != EngineSetting.INDEX_NOT_FOUND && isNodeGrabbed(nodes.get(nodeIndex), time, notchTolerance)) {
            RecordingNodeStruct node = nodes.get(nodeIndex);
            beginDrag(RecorderDragKind.NODE, clipIndex, nodeIndex, window, pressX, pressY, time,
                    node.getKind() == RecorderNodeKind.GESTURE ? time - node.getStart() : 0f);
            return;
        }

        beginDrag(RecorderDragKind.CLIP, clipIndex, nodeIndex, window, pressX, pressY, time,
                time - recording.getClips().get(clipIndex).getStart());
    }

    // The edge of the clip under the press when it has one there, else the nearer of the two edges found
    private void beginTrimDrag(
            float time,
            int startEdge,
            int endEdge,
            int clipIndex,
            WindowInstance window,
            float pressX,
            float pressY) {

        boolean start;

        if (clipIndex != EngineSetting.INDEX_NOT_FOUND && (startEdge == clipIndex) != (endEdge == clipIndex))
            start = startEdge == clipIndex;
        else if (startEdge == EngineSetting.INDEX_NOT_FOUND || endEdge == EngineSetting.INDEX_NOT_FOUND)
            start = startEdge != EngineSetting.INDEX_NOT_FOUND;
        else
            start = Math.abs(recording.getClips().get(startEdge).getStart() - time)
                    <= Math.abs(recording.getClips().get(endEdge).getEnd() - time);

        beginDrag(start ? RecorderDragKind.TRIM_START : RecorderDragKind.TRIM_END, start ? startEdge : endEdge,
                EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, time, 0f);
    }

    // A gesture is taken hold of anywhere along it, a movement only at the notch it opens with
    private boolean isNodeGrabbed(RecordingNodeStruct node, float time, float notchTolerance) {
        return node.isMovable() && (node.getKind() == RecorderNodeKind.GESTURE
                || recordingNodeBranch.isOnNotch(node, time, notchTolerance));
    }

    private void beginDrag(
            RecorderDragKind kind,
            int index,
            int nodeIndex,
            WindowInstance window,
            float pressX,
            float pressY,
            float pressTime,
            float grabOffset) {

        drag.begin(kind, index, nodeIndex, window, pressX, pressY, pressTime, grabOffset, view.getStart());
        drag.land(false, pressTime, EngineSetting.INDEX_NOT_FOUND);
        revision++;
    }

    // Where what is carried would land with the cursor where it is now, snapped to the timeline's edges when asked;
    // a scroll moves the view so the moment pressed stays under the cursor
    public void dragTo(
            float x,
            float y,
            boolean overLanes,
            float time,
            int lane,
            float secondsPerPixel,
            boolean snapping) {

        if (!drag.isActive())
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);

        float tolerance = snapping ? EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel : 0f;

        switch (drag.getKind()) {
            case TAKE, CLIP -> landSpan(overLanes, time, lane, tolerance);
            case TRIM_START, TRIM_END -> landEdge(time, tolerance);
            case CUT -> landCut(time, tolerance);
            case NODE -> landNode(time);
            case PAN -> panView(x, secondsPerPixel);
            case KEY, GRAPH_KEY, HANDLE_IN, HANDLE_OUT, KEYS_PRESS, NONE -> {
            }
        }
    }

    // The span lands on the lane it would really be laid on, which the tab shows before it is let go
    private void landSpan(boolean overLanes, float time, int lane, float tolerance) {

        RecordingClipStruct ignored = drag.getKind() == RecorderDragKind.CLIP
                ? recording.getClips().get(drag.getIndex())
                : null;
        float length = resolveDragLength();
        float start = Math.max(0f, recordingTimelineBranch.snapSpan(
                recording, Math.max(0f, time - drag.getGrabOffset()), length, playhead, tolerance, ignored));

        drag.land(overLanes, start,
                recordingTimelineBranch.resolveLane(recording, start, start + length, lane, ignored));
    }

    private void landEdge(float time, float tolerance) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());
        float snapped = recordingTimelineBranch.snapTime(recording, time, playhead, tolerance, clip);

        drag.land(true, drag.getKind() == RecorderDragKind.TRIM_START
                ? recordingTimelineBranch.resolveTrimStart(recording, clip, snapped)
                : recordingTimelineBranch.resolveTrimEnd(recording, clip, snapped),
                clip.getLane());
    }

    private void landCut(float time, float tolerance) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());

        drag.land(true, recordingTimelineBranch.resolveCut(recording, clip, time, playhead, tolerance), clip.getLane());
    }

    private void landNode(float time) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());

        drag.land(true,
                recordingNodeBranch.resolveLanding(nodes.get(drag.getNode()), clip, time - drag.getGrabOffset()),
                clip.getLane());
    }

    private void panView(float x, float secondsPerPixel) {
        if (drag.isMoved() && view.scrollTo(
                drag.getViewStart() - (x - drag.getPressX()) * secondsPerPixel, resolveTimelineSpan()))
            viewRevision++;
    }

    // Letting go lays, moves, trims or retimes what was carried where it landed; a press that never travelled
    // selects what it was on instead, or moves the playhead to an empty moment
    public void endDrag() {

        if (!drag.isActive())
            return;

        RecorderDragKind kind = drag.getKind();
        int index = drag.getIndex();
        int nodeIndex = drag.getNode();
        boolean moved = drag.isMoved();
        boolean dropped = moved && drag.isOverLanes();
        float pressTime = drag.getPressTime();
        float landingStart = drag.getLandingStart();
        int landingLane = drag.getLandingLane();

        cancelDrag();

        switch (kind) {
            case TAKE -> {
                if (dropped)
                    placeTake(index, landingStart, landingLane);
                else
                    selectTake(index);
            }
            case CLIP -> {
                if (dropped)
                    moveClip(index, landingStart, landingLane);
                else if (nodeIndex != EngineSetting.INDEX_NOT_FOUND)
                    selectNode(nodeIndex);
                else
                    selectClipAlone(index);
            }
            case CUT -> {
                if (!cutClip(index, landingStart))
                    setStatus(EditorSetting.RECORDER_STATUS_CUT_TOO_NEAR);
            }
            case TRIM_START, TRIM_END -> {
                if (moved)
                    trimClipTo(index, landingStart, kind == RecorderDragKind.TRIM_START);
                else
                    selectClipAlone(index);
            }
            case NODE -> {
                if (moved)
                    moveNode(nodeIndex, landingStart);
                else
                    selectNode(nodeIndex);
            }
            case PAN -> {
                if (!moved)
                    seek(pressTime);
            }
            case KEY, GRAPH_KEY, HANDLE_IN, HANDLE_OUT -> {
                if (moved)
                    finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_MOVED);
                else
                    selectKey(index);
            }
            case KEYS_PRESS -> {
                if (!moved && nodeIndex != EngineSetting.INDEX_NOT_FOUND)
                    selectNode(nodeIndex);
                else if (!moved)
                    releaseKeyAt(pressTime);
            }
            case NONE -> {
            }
        }
    }

    public void cancelDrag() {

        if (!drag.isActive())
            return;

        drag.clear();
        revision++;
    }

    // How long what is carried runs, so the tab can draw where it would land; nothing for what lands at a moment
    public float resolveDragLength() {
        return switch (drag.getKind()) {
            case TAKE -> recording.getTakes().get(drag.getIndex()).getDuration();
            case CLIP -> recording.getClips().get(drag.getIndex()).getLength();
            case TRIM_START, TRIM_END, CUT, NODE, PAN, KEY, GRAPH_KEY, HANDLE_IN, HANDLE_OUT, KEYS_PRESS, NONE -> 0f;
        };
    }

    // Nodes \\

    // Selecting a node selects the clip it lies on
    public void selectNode(int nodeIndex) {

        if (!isNodeIndex(nodeIndex))
            return;

        RecordingNodeStruct node = nodes.get(nodeIndex);

        selectClip(node.getClipIndex());
        anchorNode(node, node.getTakeStart());
        this.selectedNode = nodeIndex;
    }

    public void nextNodeState() {
        stepNodeState(1);
    }

    public void previousNodeState() {
        stepNodeState(-1);
    }

    // The selected movement does the next or the previous movement state over its run instead
    private void stepNodeState(int step) {

        RecordingNodeStruct node = requireSelectedNode();

        if (node == null || node.getKind() != RecorderNodeKind.MOVEMENT)
            return;

        recordingNodeBranch.stepState(resolveNodeTrack(node), node, step);
        markUnsaved();

        if (isNodeIndex(selectedNode))
            setStatus(EditorSetting.RECORDER_STATUS_NODE_CHANGED + nodes.get(selectedNode).getLabel());
    }

    // A movement removed carries on what came before it; a gesture removed leaves the hands free
    public void removeNode() {

        RecordingNodeStruct node = requireSelectedNode();

        if (node == null)
            return;

        String label = node.getLabel();

        if (node.getKind() == RecorderNodeKind.GESTURE) {
            recordingNodeBranch.removeGesture(resolveNodeTrack(node), node);
            clearNodeSelection();
        } else if (!recordingNodeBranch.removeMovement(resolveNodeTrack(node), node)) {
            setStatus(EditorSetting.RECORDER_STATUS_ONLY_MOVEMENT);
            return;
        }

        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_NODE_REMOVED + label);
    }

    // A movement dragged opens at the moment instead; a gesture dragged plays from it instead
    public void moveNode(int nodeIndex, float time) {

        if (!isNodeIndex(nodeIndex) || !canEditTimeline())
            return;

        RecordingNodeStruct node = nodes.get(nodeIndex);

        if (!node.isMovable())
            return;

        RecordingClipStruct clip = recording.getClips().get(node.getClipIndex());
        float takeTime = clip.toTakeTime(recordingNodeBranch.resolveLanding(node, clip, time));
        String label = node.getLabel();

        if (node.getKind() == RecorderNodeKind.MOVEMENT)
            recordingNodeBranch.retimeMovement(resolveNodeTrack(node), node, takeTime);
        else
            recordingNodeBranch.moveGesture(resolveNodeTrack(node), node, takeTime);

        this.selectedClip = node.getClipIndex();
        anchorNode(node, takeTime);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_RETIMED + label);
    }

    // The selected node when the timeline can be edited, null after saying why not
    private RecordingNodeStruct requireSelectedNode() {

        if (!canEditTimeline())
            return null;

        if (!isNodeIndex(selectedNode)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_NODE);
            return null;
        }

        return nodes.get(selectedNode);
    }

    private RecordingBodyTrackStruct resolveNodeTrack(RecordingNodeStruct node) {
        return node.getTrack();
    }

    // The selection follows a node through rebuilds by its clip, whose it is, its kind and a moment of its run
    private void anchorNode(RecordingNodeStruct node, float takeTime) {
        this.selectedNodeClip = node.getClipIndex();
        this.selectedNodeTrack = node.getTrack();
        this.selectedNodeKind = node.getKind();
        this.selectedNodeTime = takeTime;
    }

    private void clearNodeSelection() {
        this.selectedNode = EngineSetting.INDEX_NOT_FOUND;
        this.selectedNodeClip = EngineSetting.INDEX_NOT_FOUND;
        this.selectedNodeTrack = null;
        this.selectedNodeKind = null;
    }

    // Every clip's nodes read again from the takes, the selected node found again among them
    private void rebuildNodes() {

        recordingNodeBranch.collectNodes(recording, nodes);

        this.selectedNode = selectedNodeKind != null
                ? recordingNodeBranch.findNode(
                        nodes, selectedNodeClip, selectedNodeTrack, selectedNodeKind, selectedNodeTime)
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // Keys \\

    public void showKeys(boolean shown) {
        this.keysShown = shown;
        keysRevision++;
    }

    public void showGraph(boolean shown) {

        this.graphShown = shown;

        if (shown)
            frameGraph();

        keysRevision++;
    }

    // The tracks of the selected clip's take, listed again whenever another take's clip is selected: the camera,
    // the player, every resident, item and vehicle, the weather and the clock, whichever it holds
    private void updateKeyTracks() {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingTakeStruct take = clip != null ? clip.getTake() : null;

        if (take == keyTake)
            return;

        this.keyTake = take;
        keyTracks.clear();
        clearCurveSelection();
        keysRevision++;

        if (take == null)
            return;

        addKeyTrack(take.getCameraTrack());
        addKeyTrack(take.getPlayerTrack());

        for (int i = 0; i < take.getResidentTracks().size(); i++)
            addKeyTrack(take.getResidentTracks().get(i));

        for (int i = 0; i < take.getProjectileTracks().size(); i++)
            addKeyTrack(take.getProjectileTracks().get(i));

        for (int i = 0; i < take.getVehicleTracks().size(); i++)
            addKeyTrack(take.getVehicleTracks().get(i));

        addKeyTrack(take.getWeatherTrack());
        addKeyTrack(take.getClockTrack());
    }

    private void addKeyTrack(RecordingCurveTrackStruct track) {
        if (track != null && !track.isEmpty())
            keyTracks.add(track);
    }

    // A track opened shows every value it holds, each laid on a curve from what it recorded the first time
    public void toggleTrack(int trackIndex) {

        if (!isKeyTrackIndex(trackIndex))
            return;

        RecordingCurveTrackStruct track = keyTracks.get(trackIndex);

        if (openedTracks.remove(track)) {
            keysRevision++;
            return;
        }

        openedTracks.add(track);

        if (ensureCurves(track))
            markUnsaved();

        keysRevision++;
    }

    private boolean ensureCurves(RecordingCurveTrackStruct track) {

        if (!canEditTimeline())
            return false;

        boolean laid = false;

        for (int slot = 0; slot < track.getCurveCount(); slot++)
            if (!track.hasCurve(slot) && recordingCurveBranch.ensureCurve(track, slot, resolveFitWorld()))
                laid = true;

        return laid;
    }

    // A curve selected for its keys to be edited, laid from what it recorded first if it has none
    public void selectCurve(int trackIndex, int slot) {

        if (!isKeyTrackIndex(trackIndex))
            return;

        RecordingCurveTrackStruct track = keyTracks.get(trackIndex);

        if (slot < 0 || slot >= track.getCurveCount())
            return;

        if (!track.hasCurve(slot) && canEditTimeline()
                && recordingCurveBranch.ensureCurve(track, slot, resolveFitWorld()))
            markUnsaved();

        this.selectedTrack = track;
        this.selectedSlot = slot;
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        frameGraph();
        keysRevision++;
    }

    public void selectKey(int keyIndex) {

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || keyIndex < 0 || keyIndex >= curve.size())
            return;

        this.selectedKey = keyIndex;
        keysRevision++;
    }

    // What a press on a row of the Keys tab takes hold of: a key on a curve's row to retime it, or a node on a
    // character's movement or gesture row to retime or move it; anywhere else it selects what it lies on, or with
    // nothing there moves the playhead
    public void pressKeyRow(
            int trackIndex,
            int row,
            float time,
            float secondsPerPixel,
            WindowInstance window,
            float pressX,
            float pressY) {

        RecordingClipStruct clip = getSelectedClipStruct();

        if (drag.isActive() || clip == null || !isKeyTrackIndex(trackIndex))
            return;

        RecordingCurveTrackStruct track = keyTracks.get(trackIndex);

        if (row >= 0) {

            if (track != selectedTrack || row != selectedSlot)
                selectCurve(trackIndex, row);

            RecordingCurveStruct curve = getSelectedCurve();
            int key = curve != null && canEditTimeline()
                    ? curve.findKey(clip.toTakeTime(time), EditorSetting.RECORDER_KEY_GRAB_PIXELS * secondsPerPixel)
                    : EngineSetting.INDEX_NOT_FOUND;

            if (key != EngineSetting.INDEX_NOT_FOUND) {
                beginKeyDrag(RecorderDragKind.KEY, curve, key, 0.0, window, pressX, pressY, time);
                return;
            }
        }

        int nodeIndex = EngineSetting.INDEX_NOT_FOUND;

        if ((row == EditorSetting.RECORDER_KEY_ROW_MOVEMENT || row == EditorSetting.RECORDER_KEY_ROW_GESTURES)
                && track instanceof RecordingBodyTrackStruct body) {

            float notchTolerance = EditorSetting.RECORDER_NOTCH_GRAB_PIXELS * secondsPerPixel;

            nodeIndex = recordingNodeBranch.findNodeAt(nodes, selectedClip, body, time,
                    row == EditorSetting.RECORDER_KEY_ROW_GESTURES, notchTolerance);

            if (nodeIndex != EngineSetting.INDEX_NOT_FOUND && canEditTimeline()
                    && isNodeGrabbed(nodes.get(nodeIndex), time, notchTolerance)) {
                RecordingNodeStruct node = nodes.get(nodeIndex);
                beginDrag(RecorderDragKind.NODE, selectedClip, nodeIndex, window, pressX, pressY, time,
                        node.getKind() == RecorderNodeKind.GESTURE ? time - node.getStart() : 0f);
                return;
            }
        }

        beginDrag(RecorderDragKind.KEYS_PRESS, EngineSetting.INDEX_NOT_FOUND, nodeIndex,
                window, pressX, pressY, time, 0f);
    }

    // What a press on the graph takes hold of: a handle of the selected key, else the key nearest it, each within
    // reach in pixels; anywhere else it lets the key go, and never travelling moves the playhead
    public void pressGraph(
            float time,
            double value,
            float secondsPerPixel,
            double valuePerPixel,
            WindowInstance window,
            float pressX,
            float pressY) {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (drag.isActive() || clip == null || curve == null)
            return;

        float takeTime = clip.toTakeTime(time);

        if (canEditTimeline() && selectedKey >= 0 && selectedKey < curve.size()
                && curve.getMode(selectedKey) != RecorderKeyMode.CONSTANT) {

            float keyTime = curve.getTime(selectedKey);
            double keyValue = curve.getValue(selectedKey);

            if (selectedKey > 0 && isWithinGrab(keyTime + curve.getInTime(selectedKey),
                    keyValue + curve.getInValue(selectedKey), takeTime, value, secondsPerPixel, valuePerPixel)) {
                beginKeyDrag(RecorderDragKind.HANDLE_IN, curve, selectedKey, value, window, pressX, pressY, time);
                return;
            }

            if (selectedKey < curve.size() - 1 && isWithinGrab(keyTime + curve.getOutTime(selectedKey),
                    keyValue + curve.getOutValue(selectedKey), takeTime, value, secondsPerPixel, valuePerPixel)) {
                beginKeyDrag(RecorderDragKind.HANDLE_OUT, curve, selectedKey, value, window, pressX, pressY, time);
                return;
            }
        }

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        double nearestPixels = EditorSetting.RECORDER_GRAPH_GRAB_PIXELS;

        for (int i = 0; canEditTimeline() && i < curve.size(); i++) {

            double pixels = Math.hypot(
                    (curve.getTime(i) - takeTime) / secondsPerPixel,
                    (curve.getValue(i) - value) / valuePerPixel);

            if (pixels <= nearestPixels) {
                nearest = i;
                nearestPixels = pixels;
            }
        }

        if (nearest != EngineSetting.INDEX_NOT_FOUND) {
            beginKeyDrag(RecorderDragKind.GRAPH_KEY, curve, nearest, value, window, pressX, pressY, time);
            return;
        }

        beginDrag(RecorderDragKind.KEYS_PRESS, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, time, 0f);
    }

    private boolean isWithinGrab(
            float pointTime,
            double pointValue,
            float time,
            double value,
            float secondsPerPixel,
            double valuePerPixel) {

        return Math.hypot((pointTime - time) / secondsPerPixel, (pointValue - value) / valuePerPixel)
                <= EditorSetting.RECORDER_GRAPH_GRAB_PIXELS;
    }

    private void beginKeyDrag(
            RecorderDragKind kind,
            RecordingCurveStruct curve,
            int keyIndex,
            double pressValue,
            WindowInstance window,
            float pressX,
            float pressY,
            float time) {

        this.selectedKey = keyIndex;
        beginDrag(kind, keyIndex, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, time, 0f);
        drag.anchorKey(curve.getTime(keyIndex), curve.getValue(keyIndex), pressValue);
        keysRevision++;
    }

    // A key or handle carried on the Keys tab follows the cursor, its curve changing as it moves; a node carried
    // there lands as it does on the lanes
    public void dragKeysTo(float x, float y, float time, double value, float secondsPerPixel, boolean snapping) {

        if (!drag.isActive())
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (drag.getKind() == RecorderDragKind.NODE) {
            landNode(time);
            return;
        }

        if (!drag.getKind().isKey() || !drag.isMoved() || clip == null || curve == null
                || drag.getIndex() >= curve.size())
            return;

        int keyIndex = drag.getIndex();

        if (drag.getKind() == RecorderDragKind.HANDLE_IN || drag.getKind() == RecorderDragKind.HANDLE_OUT) {
            curve.setHandle(keyIndex, drag.getKind() == RecorderDragKind.HANDLE_OUT,
                    clip.toTakeTime(time) - curve.getTime(keyIndex), value - curve.getValue(keyIndex));
            curveRevision++;
            return;
        }

        float landing = clip.toTimelineTime(drag.getKeyTime()) + time - drag.getPressTime();

        if (snapping)
            landing = recordingTimelineBranch.snapTime(recording, landing, playhead,
                    EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel, null);

        curve.moveKey(keyIndex, clip.toTakeTime(landing), drag.getKind() == RecorderDragKind.GRAPH_KEY
                ? drag.getKeyValue() + value - drag.getPressValue()
                : curve.getValue(keyIndex));
        curveRevision++;
    }

    public void addKeyAtPlayhead() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return;

        RecordingClipStruct clip = getSelectedClipStruct();
        float takeTime = Math.clamp(clip.toTakeTime(playhead), 0f, clip.getTake().getDuration());

        this.selectedKey = curve.addKey(takeTime, curve.evaluate(takeTime), RecorderKeyMode.AUTO);
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_ADDED);
    }

    // The selected key leaves its curve; the last key leaving lets the curve go, so the value plays as recorded
    public void deleteKey() {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        if (curve.size() == 1)
            selectedTrack.setCurve(selectedSlot, null);
        else
            curve.removeKey(selectedKey);

        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_DELETED);
    }

    // The selected key's handles stand in the next mode in turn
    public void cycleKeyMode() {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        curve.setMode(selectedKey, RecorderKeyMode.VALUES[
                (curve.getMode(selectedKey).ordinal() + 1) % RecorderKeyMode.VALUES.length]);
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_CHANGED);
    }

    public boolean isKeyValueValid(String text) {

        RecordingCurveStruct curve = getSelectedCurve();

        return curve != null && selectedKey >= 0 && selectedKey < curve.size()
                && !Double.isNaN(parseKeyValue(curve, text));
    }

    public void setKeyValue(String text) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null || !isKeyValueValid(text))
            return;

        curve.setValue(selectedKey, parseKeyValue(curve, text));
        frameGraph();
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_CHANGED);
    }

    private double parseKeyValue(RecordingCurveStruct curve, String text) {
        return RecordingCurveUtility.parseValue(curve.getProperty().getKind(), text, curve.getValue(selectedKey),
                clockManager.getCalendarHandle());
    }

    // The selected key's value written so the value dialog can start from it
    public String formatKeyValueInput() {

        RecordingCurveStruct curve = getSelectedCurve();

        return curve != null && selectedKey >= 0 && selectedKey < curve.size()
                ? RecordingCurveUtility.formatInput(curve.getProperty().getKind(), curve.getValue(selectedKey),
                        clockManager.getCalendarHandle())
                : "";
    }

    public String formatValue(RecorderCurveKind kind, double value) {
        return RecordingCurveUtility.formatValue(kind, value, clockManager.getCalendarHandle());
    }

    // The selected curve laid again from what it recorded, every edit to it undone
    public void refitCurve() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return;

        recordingCurveBranch.refitCurve(selectedTrack, selectedSlot, resolveFitWorld());
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        frameGraph();
        finishKeyEdit(EditorSetting.RECORDER_STATUS_CURVE_REFIT);
    }

    // The selected curve let go, so its value plays as recorded until it is opened again
    public void removeCurve() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return;

        selectedTrack.setCurve(selectedSlot, null);
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        finishKeyEdit(EditorSetting.RECORDER_STATUS_CURVE_REMOVED);
    }

    // The graph's values framed about everything the selected curve reaches across the part of its clip in view
    public void frameGraph() {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (clip == null || curve == null || curve.isEmpty())
            return;

        float from = Math.max(view.getStart(), clip.getStart());
        float to = Math.min(view.getEnd(), clip.getEnd());

        if (to <= from) {
            from = clip.getStart();
            to = clip.getEnd();
        }

        double minimum = recordingCurveBranch.resolveMinimum(curve, clip.toTakeTime(from), clip.toTakeTime(to));
        double maximum = recordingCurveBranch.resolveMaximum(curve, clip.toTakeTime(from), clip.toTakeTime(to));
        double span = Math.max(maximum - minimum, curve.getProperty().getKind().getMinimumSpan());
        double middle = (minimum + maximum) / 2.0;
        double reach = span / 2.0 * (1.0 + 2.0 * EditorSetting.RECORDER_GRAPH_MARGIN_SHARE);

        this.graphMinimum = middle - reach;
        this.graphMaximum = middle + reach;
        keysRevision++;
    }

    // Every key edit is saved with the recording and named on the status line
    private void finishKeyEdit(String status) {
        markUnsaved();
        keysRevision++;
        setStatus(status + selectedTrack.getTrackLabel() + EditorSetting.RECORDER_KEY_STATUS_SEPARATOR
                + selectedTrack.getCurveLabel(selectedSlot));
    }

    // The selected curve when keys can be edited, null after saying why not
    private RecordingCurveStruct requireSelectedCurve() {

        if (!canEditTimeline())
            return null;

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || getSelectedClipStruct() == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CURVE);
            return null;
        }

        return curve;
    }

    private RecordingCurveStruct requireSelectedKey() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return null;

        if (selectedKey < 0 || selectedKey >= curve.size()) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_KEY);
            return null;
        }

        return curve;
    }

    // A press on nothing lets the selected key go and moves the playhead to where it was
    private void releaseKeyAt(float time) {
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        keysRevision++;
        seek(time);
    }

    private void clearCurveSelection() {
        this.selectedTrack = null;
        this.selectedSlot = EngineSetting.INDEX_NOT_FOUND;
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
    }

    // Positions are read across the world the recording was made in; in another world chunks are counted as they lie
    private WorldHandle resolveFitWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        return recording != null && recording.getWorldName().equals(activeWorld.getWorldName()) ? activeWorld : null;
    }

    private RecordingClipStruct getSelectedClipStruct() {
        return isClipIndex(selectedClip) ? recording.getClips().get(selectedClip) : null;
    }

    private boolean isKeyTrackIndex(int trackIndex) {
        return trackIndex >= 0 && trackIndex < keyTracks.size();
    }

    // Markers \\

    // A marker at the playhead, named in order
    public void addMarker() {

        if (recording == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_RECORDING);
            return;
        }

        addMarker(playhead, EditorSetting.RECORDER_MARKER_NAME_PREFIX + (recording.getMarkers().size() + 1));
    }

    public void addMarker(float time, String markerName) {

        if (recording == null)
            return;

        this.selectedMarker = recording.addMarker(new RecordingMarkerStruct(time, markerName));
        markUnsaved();
    }

    // Selecting a marker moves the playhead onto it
    public void selectMarker(int markerIndex) {

        if (!isMarkerIndex(markerIndex))
            return;

        this.selectedMarker = markerIndex;
        seek(recording.getMarkers().get(markerIndex).getTime());
        revision++;
    }

    public void renameMarker(int markerIndex, String markerName) {

        if (!isMarkerIndex(markerIndex) || !isLabelValid(markerName))
            return;

        recording.getMarkers().get(markerIndex).setName(markerName);
        markUnsaved();
    }

    public void deleteMarker(int markerIndex) {

        if (!isMarkerIndex(markerIndex))
            return;

        recording.removeMarker(markerIndex);
        this.selectedMarker = EngineSetting.INDEX_NOT_FOUND;
        markUnsaved();
    }

    // Export Settings \\

    public void cycleResolution() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_HEIGHTS, exportStruct.getHeight());
        int next = (index + 1) % EditorSetting.RECORDER_EXPORT_HEIGHTS.length;

        exportStruct.setSize(EditorSetting.RECORDER_EXPORT_WIDTHS[next], EditorSetting.RECORDER_EXPORT_HEIGHTS[next]);
        markUnsaved();
    }

    public void cycleFrameRate() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_FRAME_RATES, exportStruct.getFrameRate());

        exportStruct.setFrameRate(EditorSetting.RECORDER_EXPORT_FRAME_RATES[
                (index + 1) % EditorSetting.RECORDER_EXPORT_FRAME_RATES.length]);
        markUnsaved();
    }

    public void cycleFormat() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();

        exportStruct.setFormat(
                VideoFormat.VALUES[(exportStruct.getFormat().ordinal() + 1) % VideoFormat.VALUES.length]);
        markUnsaved();
    }

    public void cycleMotionBlur() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES, exportStruct.getMotionBlurSamples());

        exportStruct.setMotionBlurSamples(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES[
                (index + 1) % EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES.length]);
        markUnsaved();
    }

    public void toggleSupersample() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setSupersampled(!exportStruct.isSupersampled());
        markUnsaved();
    }

    public void toggleHud() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setHud(!exportStruct.isHud());
        markUnsaved();
    }

    public void setRangeStart() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setRangeStart(playhead);

        if (exportStruct.hasRangeEnd() && exportStruct.getRangeEnd() < playhead)
            exportStruct.setRangeEnd(EditorSetting.RECORDER_TIME_UNSET);

        markUnsaved();
    }

    public void setRangeEnd() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setRangeEnd(playhead);

        if (exportStruct.hasRangeStart() && exportStruct.getRangeStart() > playhead)
            exportStruct.setRangeStart(EditorSetting.RECORDER_TIME_UNSET);

        markUnsaved();
    }

    public void clearRange() {

        if (!canEditExport())
            return;

        recording.getExportStruct().clearRange();
        markUnsaved();
    }

    private boolean canEditExport() {
        return recording != null && state != RecorderState.EXPORTING;
    }

    private int findIndex(int[] values, int value) {

        for (int i = 0; i < values.length; i++)
            if (values[i] == value)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Export \\

    // Renders the export range into a new video beside the recordings, frame by frame in the Dev window
    public void export() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING || !hasClips())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();

        if (exportStruct.getFormat().needsFfmpeg() && FfmpegUtility.findExecutable(internal.path) == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_FFMPEG);
            return;
        }

        if (!beginSession())
            return;

        cancelDrag();
        saveRecording();
        videoDirectory.mkdirs();

        float duration = recording.getDuration();
        float span = exportStruct.resolveEnd(duration) - exportStruct.resolveStart(duration);

        this.exportFile = resolveVideoFile(exportStruct.getFormat());
        this.exportedFrames = 0;
        this.exportFrameCount = Math.max(1, Math.round(span * exportStruct.getFrameRate()));
        this.playhead = exportStruct.resolveStart(duration);
        this.state = RecorderState.EXPORTING;
        setStatus(EditorSetting.RECORDER_STATUS_EXPORTING + exportFile.getName());
    }

    public void reportExportProgress(int exportedFrames) {
        this.exportedFrames = exportedFrames;
    }

    // The export's last word — the session ends either way, and a failed video is never left behind
    public void finishExport(boolean succeeded, String message) {

        if (state != RecorderState.EXPORTING)
            return;

        endSession();

        if (!succeeded && exportFile.exists())
            exportFile.delete();

        setStatus(succeeded
                ? EditorSetting.RECORDER_STATUS_EXPORTED + exportFile.getAbsolutePath()
                : EditorSetting.RECORDER_STATUS_EXPORT_FAILED + message);
    }

    // The next free video named after the recording
    private File resolveVideoFile(VideoFormat format) {

        int number = EditorSetting.RECORDER_FIRST_NAME_NUMBER;
        File file;

        do {
            file = new File(videoDirectory, recording.getRecordingName()
                    + EditorSetting.RECORDER_VIDEO_NAME_SEPARATOR + number
                    + EditorSetting.RECORDER_EXTENSION_SEPARATOR + format.getExtension());
            number++;
        } while (file.exists());

        return file;
    }

    // Names \\

    private String resolveDefaultRecordingName() {

        int number = EditorSetting.RECORDER_FIRST_NAME_NUMBER;

        while (!isRecordingNameAvailable(EditorSetting.RECORDER_DEFAULT_NAME_PREFIX + number))
            number++;

        return EditorSetting.RECORDER_DEFAULT_NAME_PREFIX + number;
    }

    private String resolveTakeName() {

        int number = recording.getTakes().size() + 1;

        while (recording.hasTakeName(EditorSetting.RECORDER_TAKE_NAME_PREFIX + number))
            number++;

        return EditorSetting.RECORDER_TAKE_NAME_PREFIX + number;
    }

    public boolean isRecordingNameAvailable(String recordingName) {
        return FileUtility.isValidFileName(recordingName, EditorSetting.RECORDER_NAME_MAX_LENGTH)
                && !getRecordingFile(recordingName).exists();
    }

    public boolean isLabelValid(String label) {
        return !label.isBlank() && label.length() <= EditorSetting.RECORDER_LABEL_MAX_LENGTH;
    }

    // Every recording saved in the game's directory, most recently changed first
    public ObjectArrayList<String> getRecordingNames() {

        ObjectArrayList<File> recordingFiles = new ObjectArrayList<>();
        File[] files = recordingDirectory.listFiles();

        if (files != null)
            for (File file : files)
                if (ArpgUtility.isArpgFile(file))
                    recordingFiles.add(file);

        recordingFiles.sort((first, second) -> Long.compare(second.lastModified(), first.lastModified()));

        ObjectArrayList<String> recordingNames = new ObjectArrayList<>(recordingFiles.size());

        for (int i = 0; i < recordingFiles.size(); i++)
            recordingNames.add(FileUtility.getFileName(recordingFiles.get(i)));

        return recordingNames;
    }

    private File getRecordingFile(String recordingName) {
        return ArpgUtility.resolveFile(recordingDirectory, recordingName);
    }

    // Utility \\

    private boolean hasClips() {

        if (recording != null && !recording.getClips().isEmpty())
            return true;

        setStatus(EditorSetting.RECORDER_STATUS_NOTHING_RECORDED);
        return false;
    }

    private boolean isTakeIndex(int takeIndex) {
        return recording != null && takeIndex >= 0 && takeIndex < recording.getTakes().size();
    }

    private boolean isClipIndex(int clipIndex) {
        return recording != null && clipIndex >= 0 && clipIndex < recording.getClips().size();
    }

    private boolean isMarkerIndex(int markerIndex) {
        return recording != null && markerIndex >= 0 && markerIndex < recording.getMarkers().size();
    }

    private boolean isNodeIndex(int nodeIndex) {
        return recording != null && nodeIndex >= 0 && nodeIndex < nodes.size();
    }

    private void clearSelection() {
        this.selectedTake = EngineSetting.INDEX_NOT_FOUND;
        this.selectedClip = EngineSetting.INDEX_NOT_FOUND;
        this.selectedMarker = EngineSetting.INDEX_NOT_FOUND;
        clearNodeSelection();
    }

    // Every edit moves the nodes with it
    private void markUnsaved() {
        this.unsaved = true;
        rebuildNodes();
        revision++;
    }

    private void setStatus(String statusText) {
        this.statusText = statusText;
        revision++;
    }

    // Accessible \\

    public boolean isSessionFor(DevContext context) {
        return devContext == context && state.isSession();
    }

    // True while the game is paused under a running session, and for the few frames after
    public boolean isSuspended() {
        return suspended;
    }

    // The channels a session plays from the timeline — everything but what a take records live
    public int getPlayedChannels() {
        return state.isSession() ? RecorderChannel.ALL & ~getLiveChannels() : RecorderChannel.NONE;
    }

    public int getLiveChannels() {
        return state == RecorderState.RECORDING ? take.getChannelMask() : RecorderChannel.NONE;
    }

    public RecorderState getState() {
        return state;
    }

    public float getPlayhead() {
        return playhead;
    }

    public void setExportPlayhead(float playhead) {
        if (state == RecorderState.EXPORTING)
            this.playhead = playhead;
    }

    public int getArmedChannels() {
        return armedChannels;
    }

    public boolean hasRecording() {
        return recording != null;
    }

    public RecordingInstance getRecording() {
        return recording;
    }

    public boolean isUnsaved() {
        return unsaved;
    }

    public RecordingTakeStruct getTake() {
        return take;
    }

    // The take's own time at the playhead
    public float getTakeTime() {
        return playhead - takeStart;
    }

    public int getTakeCount() {
        return takeCount;
    }

    public int getSelectedTake() {
        return selectedTake;
    }

    public int getSelectedClip() {
        return selectedClip;
    }

    public int getSelectedMarker() {
        return selectedMarker;
    }

    public int getSelectedNode() {
        return selectedNode;
    }

    public ObjectArrayList<RecordingNodeStruct> getNodes() {
        return nodes;
    }

    public RecordingDragStruct getDrag() {
        return drag;
    }

    public boolean isCutting() {
        return cutting;
    }

    // The clip the cut tool is over, -1 for none
    public int getCutHoverClip() {
        return cutHoverClip;
    }

    public float getCutHoverTime() {
        return cutHoverTime;
    }

    public RecordingViewStruct getView() {
        return view;
    }

    public boolean isKeysShown() {
        return keysShown;
    }

    public boolean isGraphShown() {
        return graphShown;
    }

    public ObjectArrayList<RecordingCurveTrackStruct> getKeyTracks() {
        return keyTracks;
    }

    public boolean isTrackOpened(RecordingCurveTrackStruct track) {
        return openedTracks.contains(track);
    }

    public RecordingCurveTrackStruct getSelectedTrack() {
        return selectedTrack;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public RecordingCurveStruct getSelectedCurve() {
        return selectedTrack != null ? selectedTrack.getCurve(selectedSlot) : null;
    }

    public int getSelectedKey() {
        return selectedKey;
    }

    public RecordingClipStruct getKeyClip() {
        return getSelectedClipStruct();
    }

    public double getGraphMinimum() {
        return graphMinimum;
    }

    public double getGraphMaximum() {
        return graphMaximum;
    }

    public int getKeysRevision() {
        return keysRevision;
    }

    public int getCurveRevision() {
        return curveRevision;
    }

    public int getViewRevision() {
        return viewRevision;
    }

    public File getExportFile() {
        return exportFile;
    }

    public int getExportedFrames() {
        return exportedFrames;
    }

    public int getExportFrameCount() {
        return exportFrameCount;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
