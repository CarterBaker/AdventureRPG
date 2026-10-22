package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class RecorderBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recorder tab. Every action works on the one
     * recorder all tabs share. New and renamed recordings are named through
     * the name dialog, takes and markers through the text dialog, and deleting
     * a recording or a take asks for its name to be typed back. Dragging
     * along the timeline moves the playhead to the moment under the cursor.
     * Rows hand back the index of the take or marker they show, and setting
     * rows the key of the setting they change.
     */

    // Internal
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;
    private InputManager inputManager;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.inputManager = get(InputManager.class);
    }

    // Recordings \\

    public void newRecording(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::createRecording);
    }

    public void openRecording(String recordingName) {
        recorderManager.openRecording(recordingName);
    }

    public void saveRecording() {
        recorderManager.saveRecording();
    }

    public void renameRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::renameRecording);
    }

    public void deleteRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        String recordingName = recorderManager.getRecording().getRecordingName();

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + recordingName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                recordingName::equals,
                confirmedName -> recorderManager.deleteRecording());
    }

    // Transport \\

    public void record() {
        recorderManager.record();
    }

    public void play() {
        recorderManager.play();
    }

    public void pause() {
        recorderManager.pause();
    }

    public void stop() {
        recorderManager.stop();
    }

    public void toStart() {
        recorderManager.toStart();
    }

    public void toEnd() {
        recorderManager.toEnd();
    }

    public void previousMarker() {
        recorderManager.previousMarker();
    }

    public void nextMarker() {
        recorderManager.nextMarker();
    }

    public void addMarker() {
        recorderManager.addMarker();
    }

    public void scrubTimeline(WindowInstance window, ElementInstance track) {

        if (track.getComputedW() > 0f)
            recorderManager.scrub(Math.clamp(
                    (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW(), 0f, 1f));
    }

    // Takes \\

    public void selectLayer(String layerKey) {
        recorderManager.selectLayer(Integer.parseInt(layerKey));
    }

    public void toggleLayerMute(String layerKey) {
        recorderManager.toggleLayerMute(Integer.parseInt(layerKey));
    }

    public void cycleLayerTimeRamp(String layerKey) {
        recorderManager.cycleLayerTimeRamp(Integer.parseInt(layerKey));
    }

    public void renameLayer(String layerKey, WindowInstance window) {

        int layerIndex = Integer.parseInt(layerKey);

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_TAKE,
                recorderManager.getRecording().getLayers().get(layerIndex).getName(),
                recorderManager::isLabelValid,
                layerName -> recorderManager.renameLayer(layerIndex, layerName));
    }

    public void deleteLayer(String layerKey, WindowInstance window) {

        int layerIndex = Integer.parseInt(layerKey);
        String layerName = recorderManager.getRecording().getLayers().get(layerIndex).getName();

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + layerName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                "",
                layerName::equals,
                confirmedName -> recorderManager.deleteLayer(layerIndex));
    }

    // Markers \\

    public void selectMarker(String markerKey) {
        recorderManager.selectMarker(Integer.parseInt(markerKey));
    }

    public void renameMarker(String markerKey, WindowInstance window) {

        int markerIndex = Integer.parseInt(markerKey);

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_MARKER,
                recorderManager.getRecording().getMarkers().get(markerIndex).getName(),
                recorderManager::isLabelValid,
                markerName -> recorderManager.renameMarker(markerIndex, markerName));
    }

    public void deleteMarker(String markerKey) {
        recorderManager.deleteMarker(Integer.parseInt(markerKey));
    }

    // Settings \\

    public void actSetting(String settingKey) {
        switch (settingKey) {
            case EditorSetting.RECORDER_KEY_ARM_CAMERA -> recorderManager.toggleArm(RecorderChannel.CAMERA);
            case EditorSetting.RECORDER_KEY_ARM_PLAYER -> recorderManager.toggleArm(RecorderChannel.PLAYER);
            case EditorSetting.RECORDER_KEY_ARM_WORLD -> recorderManager.toggleArm(RecorderChannel.WORLD);
            case EditorSetting.RECORDER_KEY_ARM_TIME -> recorderManager.toggleArm(RecorderChannel.TIME);
            case EditorSetting.RECORDER_KEY_RESOLUTION -> recorderManager.cycleResolution();
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> recorderManager.cycleFrameRate();
            case EditorSetting.RECORDER_KEY_FORMAT -> recorderManager.cycleFormat();
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> recorderManager.cycleMotionBlur();
            case EditorSetting.RECORDER_KEY_SUPERSAMPLE -> recorderManager.toggleSupersample();
            case EditorSetting.RECORDER_KEY_HUD -> recorderManager.toggleHud();
            case EditorSetting.RECORDER_KEY_RANGE_START -> recorderManager.setRangeStart();
            case EditorSetting.RECORDER_KEY_RANGE_END -> recorderManager.setRangeEnd();
            case EditorSetting.RECORDER_KEY_RANGE_CLEAR -> recorderManager.clearRange();
            case EditorSetting.RECORDER_KEY_EXPORT -> recorderManager.export();
            case EditorSetting.RECORDER_KEY_CANCEL_EXPORT -> recorderManager.stop();
            default -> throwException("Unknown recorder setting: " + settingKey);
        }
    }
}
