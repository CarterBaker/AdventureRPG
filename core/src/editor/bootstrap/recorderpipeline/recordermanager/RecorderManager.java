package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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
     * them from the bin, and clips are moved by dragging, split, trimmed,
     * stamped, muted, given or stripped of channels, and removed, each by one
     * method here. The player cannot be recorded without the camera that
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

    // Drag
    private RecordingDragStruct drag;

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

        // Directory
        this.recordingDirectory = new File(internal.path, EditorSetting.RECORDER_DIRECTORY);
        this.videoDirectory = new File(recordingDirectory, EditorSetting.RECORDER_VIDEO_DIRECTORY);

        // Transport
        this.state = RecorderState.IDLE;
        this.armedChannels = RecorderChannel.ALL;

        // Selection
        clearSelection();

        // Drag
        this.drag = new RecordingDragStruct();

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

        if (state == RecorderState.IDLE)
            preview();
    }

    public void scrub(float fraction) {
        if (recording != null)
            seek(fraction * resolveTimelineSpan());
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

    // The timeline drawn runs the recording's length, and on while a take records past it
    public float resolveTimelineSpan() {

        float span = EditorSetting.RECORDER_MIN_TIMELINE_SECONDS;

        if (recording != null)
            span = Math.max(span, recording.getDuration());

        return Math.max(span, playhead);
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

        this.selectedClip = clipIndex;
        this.selectedTake = recording.getTakes().indexOf(recording.getClips().get(clipIndex).getTake());
        revision++;
    }

    public int findClipAt(float time, int lane) {
        return recording == null ? EngineSetting.INDEX_NOT_FOUND : recording.findClipAt(time, lane);
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

    public void splitClip() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        int split = recordingTimelineBranch.splitClip(recording, clip, playhead);

        if (split == EngineSetting.INDEX_NOT_FOUND) {
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
            return;
        }

        this.selectedClip = split;
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_SPLIT + clip.getTake().getName());
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

        drag.begin(RecorderDragKind.TAKE, takeIndex, window, pressX, pressY, 0f);
        revision++;
    }

    // A clip taken hold of at a moment within it, to be moved so that moment stays under the cursor
    public void beginClipDrag(int clipIndex, float grabTime, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !canEditTimeline() || !isClipIndex(clipIndex))
            return;

        drag.begin(RecorderDragKind.CLIP, clipIndex, window, pressX, pressY,
                grabTime - recording.getClips().get(clipIndex).getStart());
        revision++;
    }

    // Where the carried take or clip would land with the cursor where it is now
    public void dragTo(float x, float y, boolean overLanes, float time, int lane) {

        if (!drag.isActive())
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);
        drag.land(overLanes, Math.max(0f, time - drag.getGrabOffset()), lane);
    }

    // Letting go lays or moves what was carried where it landed; a press that never travelled selects it instead
    public void endDrag() {

        if (!drag.isActive())
            return;

        RecorderDragKind kind = drag.getKind();
        int index = drag.getIndex();
        boolean dropped = drag.isMoved() && drag.isOverLanes();
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
                else
                    selectClip(index);
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

    // How long what is carried runs, so the tab can draw where it would land
    public float resolveDragLength() {
        return switch (drag.getKind()) {
            case TAKE -> recording.getTakes().get(drag.getIndex()).getDuration();
            case CLIP -> recording.getClips().get(drag.getIndex()).getLength();
            case NONE -> 0f;
        };
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

    private void clearSelection() {
        this.selectedTake = EngineSetting.INDEX_NOT_FOUND;
        this.selectedClip = EngineSetting.INDEX_NOT_FOUND;
        this.selectedMarker = EngineSetting.INDEX_NOT_FOUND;
    }

    private void markUnsaved() {
        this.unsaved = true;
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

    public RecordingDragStruct getDrag() {
        return drag;
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
