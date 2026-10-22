package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recorder tab. Every action works on the one
     * recorder all tabs share. New and renamed recordings are named through
     * the name dialog, takes and markers through the text dialog, and deleting
     * a recording or a take asks for its name to be typed back. Dragging along
     * the ruler moves the playhead to the moment under the cursor. Pressing a
     * take in the bin picks it up, and letting go over the lanes lays it there;
     * pressing a clip on the lanes takes hold of it, and letting go moves it.
     * A press that never travels selects instead, and a press on an empty lane
     * moves the playhead there. The drag is read from the window the press
     * began in until the button is let go. Rows hand back the index of the
     * take or marker they show, and setting rows the key of what they change.
     */

    // Internal
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;
    private InputManager inputManager;
    private WindowManager windowManager;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // A drag ends when its button is let go, and is dropped when its window leaves
    @Override
    protected void update() {

        RecordingDragStruct drag = recorderManager.getDrag();

        if (!drag.isActive())
            return;

        if (!windowManager.getWindows().contains(drag.getWindow())) {
            recorderManager.cancelDrag();
            return;
        }

        if (!inputManager.getRawInput(drag.getWindow()).isMouseDown(Buttons.LEFT))
            recorderManager.endDrag();
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
            recorderManager.scrub(resolveFraction(window, track));
    }

    // Takes \\

    // The take is picked up on the press and carried over the lanes until let go
    public void dragTake(String takeKey, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginTakeDrag(
                    Integer.parseInt(takeKey),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        dragOver(window, findLanes(window));
    }

    public void placeTake(String takeKey) {
        recorderManager.placeTakeAtPlayhead(Integer.parseInt(takeKey));
    }

    public void renameTake(String takeKey, WindowInstance window) {

        int takeIndex = Integer.parseInt(takeKey);

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_TAKE,
                recorderManager.getRecording().getTakes().get(takeIndex).getName(),
                recorderManager::isLabelValid,
                takeName -> recorderManager.renameTake(takeIndex, takeName));
    }

    public void deleteTake(String takeKey, WindowInstance window) {

        int takeIndex = Integer.parseInt(takeKey);
        String takeName = recorderManager.getRecording().getTakes().get(takeIndex).getName();

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + takeName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                "",
                takeName::equals,
                confirmedName -> recorderManager.deleteTake(takeIndex));
    }

    // Timeline \\

    // A press on a clip takes hold of it where it was pressed; a press on an empty lane moves the playhead there
    public void dragLanes(WindowInstance window, ElementInstance lanes) {

        if (lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f)
            return;

        float time = resolveFraction(window, lanes) * recorderManager.resolveTimelineSpan();

        if (!recorderManager.getDrag().isActive()) {

            int clipIndex = recorderManager.findClipAt(time, resolveLane(window, lanes));

            if (clipIndex == EngineSetting.INDEX_NOT_FOUND) {
                recorderManager.seek(time);
                return;
            }

            recorderManager.beginClipDrag(
                    clipIndex,
                    time,
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));
        }

        dragOver(window, lanes);
    }

    // Tells the recorder where what is carried would land with the cursor where it is now
    private void dragOver(WindowInstance window, ElementInstance lanes) {

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        if (lanes == null || lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f) {
            recorderManager.dragTo(x, y, false, 0f, 0);
            return;
        }

        boolean over = x >= lanes.getComputedLeft() && x <= lanes.getComputedLeft() + lanes.getComputedW()
                && y >= lanes.getComputedTop() && y <= lanes.getComputedTop() + lanes.getComputedH();

        recorderManager.dragTo(
                x,
                y,
                over,
                resolveFraction(window, lanes) * recorderManager.resolveTimelineSpan(),
                resolveLane(window, lanes));
    }

    // How far along the element the cursor stands, 0 to 1
    private float resolveFraction(WindowInstance window, ElementInstance element) {
        return Math.clamp(
                (inputManager.getHoverMouseX(window) - element.getComputedLeft()) / element.getComputedW(), 0f, 1f);
    }

    // The lane under the cursor, the highest drawn at the top
    private int resolveLane(WindowInstance window, ElementInstance lanes) {

        float laneHeight = lanes.getComputedH() / EditorSetting.RECORDER_LANE_COUNT;
        int row = (int) Math.floor((inputManager.getHoverMouseY(window) - lanes.getComputedTop()) / laneHeight);

        return EditorSetting.RECORDER_LANE_COUNT - 1 - Math.clamp(row, 0, EditorSetting.RECORDER_LANE_COUNT - 1);
    }

    // The lanes of the Recorder tab open in the window, null when it shows none
    private ElementInstance findLanes(WindowInstance window) {

        ObjectArrayList<MenuInstance> menus = window.getMenuListHandle().getMenus();

        for (int i = 0; i < menus.size(); i++)
            if (menus.get(i).getMenuData().getName().equals(EditorSetting.RECORDER_MENU))
                return menus.get(i).getEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES);

        return null;
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

        if (settingKey.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX)) {
            recorderManager.toggleArm(resolveChannel(settingKey, EditorSetting.RECORDER_KEY_ARM_PREFIX));
            return;
        }

        if (settingKey.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)) {
            recorderManager.toggleClipChannel(
                    resolveChannel(settingKey, EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX));
            return;
        }

        switch (settingKey) {
            case EditorSetting.RECORDER_KEY_CLIP_MUTE -> recorderManager.toggleClipMute();
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> recorderManager.cycleClipTimeRamp();
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_START -> recorderManager.trimClipStart();
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_END -> recorderManager.trimClipEnd();
            case EditorSetting.RECORDER_KEY_CLIP_SPLIT -> recorderManager.splitClip();
            case EditorSetting.RECORDER_KEY_CLIP_STAMP -> recorderManager.stampClip();
            case EditorSetting.RECORDER_KEY_CLIP_REMOVE -> recorderManager.removeClip();
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

    private RecorderChannel resolveChannel(String settingKey, String prefix) {

        RecorderChannel channel = RecorderChannel.findByKey(settingKey.substring(prefix.length()));

        if (channel == null)
            throwException("Unknown recorder channel: " + settingKey);

        return channel;
    }
}
