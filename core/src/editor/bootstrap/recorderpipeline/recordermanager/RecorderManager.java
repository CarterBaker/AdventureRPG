package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLayerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderManager extends ManagerPackage {

    /*
     * Owns the recorder: the recordings saved in the game's own directory, the
     * one open, and its transport. A session — recording, playing, paused or
     * exporting — drives the first Dev window that has not crashed, found
     * among TabManager's open tabs as console commands find it, and that
     * window's DevRecorderSystem carries the session out inside its own crash
     * boundary from what this holds: which channels play from the recording,
     * which the take records live, and the playhead. A take records the armed
     * channels as a new layer from the playhead until stopped, the layers
     * beneath playing every other channel; the player cannot be recorded
     * without the camera that follows it. While a session plays the world,
     * residents and thrown items are held. Stopping ends every session and
     * the Dev window goes back to just where it was. Each structural change —
     * a take, a layer or marker edit, an export setting — has one method here,
     * marks the recording unsaved, and moves the revision the recorder tab
     * redraws on. Takes save the recording at once.
     */

    // Internal
    private TabManager tabManager;
    private WorldManager worldManager;
    private NpcManager npcManager;
    private ProjectileManager projectileManager;
    private RecordingFileBranch recordingFileBranch;

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
    private RecordingLayerStruct takeLayer;
    private int takeCount;
    private DevContext devContext;

    // Selection
    private int selectedLayer;
    private int selectedMarker;

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

        // Directory
        this.recordingDirectory = new File(internal.path, EditorSetting.RECORDER_DIRECTORY);
        this.videoDirectory = new File(recordingDirectory, EditorSetting.RECORDER_VIDEO_DIRECTORY);

        // Transport
        this.state = RecorderState.IDLE;
        this.armedChannels = RecorderChannel.ALL;

        // Selection
        this.selectedLayer = EngineSetting.INDEX_NOT_FOUND;
        this.selectedMarker = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.statusText = EditorSetting.RECORDER_STATUS_READY;
    }

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.worldManager = get(WorldManager.class);
        this.npcManager = get(NpcManager.class);
        this.projectileManager = get(ProjectileManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        releaseCrashedDevContext();
        advancePlayhead();
        holdWorld();
    }

    private void releaseCrashedDevContext() {
        if (devContext != null && devContext.isCrashed())
            stop();
    }

    private void advancePlayhead() {

        if (state != RecorderState.RECORDING && state != RecorderState.PLAYING)
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

        boolean held = devContext != null && RecorderChannel.WORLD.isIn(getPlayedChannels());

        npcManager.setHeld(held);
        projectileManager.setHeld(held);
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

        this.takeLayer = new RecordingLayerStruct(resolveLayerName(), armedChannels, playhead);
        this.takeCount++;
        this.state = RecorderState.RECORDING;
        setStatus(EditorSetting.RECORDER_STATUS_RECORDING + takeLayer.getName());
    }

    public void play() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING || !hasTakes())
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

    // Holds the Dev window at the playhead as the recording stands there
    private void preview() {

        if (!hasTakes() || !beginSession())
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

        this.state = RecorderState.IDLE;
        this.devContext = null;
        holdWorld();
    }

    private void finishTake() {

        takeLayer.setEndTime(playhead);

        if (takeLayer.getDuration() < EditorSetting.RECORDER_MIN_TAKE_SECONDS) {
            setStatus(EditorSetting.RECORDER_STATUS_TAKE_TOO_SHORT);
            this.takeLayer = null;
            return;
        }

        recording.addLayer(takeLayer);
        this.selectedLayer = recording.getLayers().size() - 1;
        this.takeLayer = null;
        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_SAVED + recording.getLayers().get(selectedLayer).getName());
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
            seek(fraction * recording.getDuration());
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
        setStatus(EditorSetting.RECORDER_STATUS_OPENED + recordingName);
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
        this.selectedLayer = EngineSetting.INDEX_NOT_FOUND;
        this.selectedMarker = EngineSetting.INDEX_NOT_FOUND;
        revision++;
    }

    // Layers \\

    public void selectLayer(int layerIndex) {

        if (!isLayerIndex(layerIndex))
            return;

        this.selectedLayer = layerIndex;
        revision++;
    }

    public void toggleLayerMute(int layerIndex) {

        if (!isLayerIndex(layerIndex) || state == RecorderState.EXPORTING)
            return;

        RecordingLayerStruct layer = recording.getLayers().get(layerIndex);
        layer.setMuted(!layer.isMuted());
        markUnsaved();
    }

    public void renameLayer(int layerIndex, String layerName) {

        if (!isLayerIndex(layerIndex) || !isLabelValid(layerName))
            return;

        recording.getLayers().get(layerIndex).setName(layerName);
        markUnsaved();
    }

    // Steps the layer's clock jumps through each ramp in turn, back to a cut after the longest
    public void cycleLayerTimeRamp(int layerIndex) {

        if (!isLayerIndex(layerIndex) || state == RecorderState.EXPORTING)
            return;

        RecordingLayerStruct layer = recording.getLayers().get(layerIndex);
        float[] ramps = EditorSetting.RECORDER_TIME_RAMP_SECONDS;
        int index = 0;

        while (index < ramps.length && ramps[index] != layer.getTimeRampSeconds())
            index++;

        layer.setTimeRampSeconds(ramps[(index + 1) % ramps.length]);
        markUnsaved();
    }

    public void deleteLayer(int layerIndex) {

        if (!isLayerIndex(layerIndex) || state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        recording.removeLayer(layerIndex);
        this.selectedLayer = EngineSetting.INDEX_NOT_FOUND;
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
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

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING || !hasTakes())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();

        if (exportStruct.getFormat().needsFfmpeg() && FfmpegUtility.findExecutable(internal.path) == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_FFMPEG);
            return;
        }

        if (!beginSession())
            return;

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

        this.state = RecorderState.IDLE;
        this.devContext = null;
        holdWorld();

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

    private String resolveLayerName() {

        int number = recording.getLayers().size() + 1;

        while (hasLayerName(EditorSetting.RECORDER_LAYER_NAME_PREFIX + number))
            number++;

        return EditorSetting.RECORDER_LAYER_NAME_PREFIX + number;
    }

    private boolean hasLayerName(String layerName) {

        for (int i = 0; i < recording.getLayers().size(); i++)
            if (recording.getLayers().get(i).getName().equals(layerName))
                return true;

        return false;
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

    private boolean hasTakes() {

        if (recording != null && !recording.getLayers().isEmpty())
            return true;

        setStatus(EditorSetting.RECORDER_STATUS_NOTHING_RECORDED);
        return false;
    }

    private boolean isLayerIndex(int layerIndex) {
        return recording != null && layerIndex >= 0 && layerIndex < recording.getLayers().size();
    }

    private boolean isMarkerIndex(int markerIndex) {
        return recording != null && markerIndex >= 0 && markerIndex < recording.getMarkers().size();
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

    // The channels a session plays from the recording — everything but what a take records live
    public int getPlayedChannels() {
        return state.isSession() ? RecorderChannel.ALL & ~getLiveChannels() : RecorderChannel.NONE;
    }

    public int getLiveChannels() {
        return state == RecorderState.RECORDING ? takeLayer.getChannelMask() : RecorderChannel.NONE;
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

    public RecordingLayerStruct getTakeLayer() {
        return takeLayer;
    }

    public int getTakeCount() {
        return takeCount;
    }

    public int getSelectedLayer() {
        return selectedLayer;
    }

    public int getSelectedMarker() {
        return selectedMarker;
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
