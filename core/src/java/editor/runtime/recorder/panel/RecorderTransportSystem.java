package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RecorderTransportSystem extends SystemPackage {

    /*
     * Shows beside the Recorder tab's buttons what the transport is doing:
     * the status line, redrawn when the recorder's revision moves, so a
     * Record or Play that cannot start says why where it was pressed; the
     * loop button's icon drawn in the accent color while playing loops, and
     * the record button's in the danger color while a take or an animation's
     * keys are recording; and
     * the clock, the playhead against the media file's length, or the frames
     * an export has written, refreshed a few times a second.
     */

    // Internal
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private ElementInstance statusLabel;
    private ElementInstance clockLabel;
    private ElementInstance loopIcon;
    private ElementInstance recordIcon;

    // Colors
    private MenuColorStruct loopingColor;
    private MenuColorStruct recordingColor;

    // State
    private int shownRevision;
    private float refreshElapsed;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.loopingColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.COLOR_CHANNEL_MAX);
        this.recordingColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.recorderRowSystem = get(RecorderRowSystem.class);
    }

    @Override
    protected void awake() {
        this.statusLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_STATUS);
        this.clockLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_CLOCK);
        this.loopIcon = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_LOOP_ICON);
        this.recordIcon = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_RECORD_ICON);
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() != shownRevision) {
            this.shownRevision = recorderManager.getRevision();
            statusLabel.setFontText(recorderManager.getStatusText());
            loopIcon.setColorOverride(recorderManager.isLooping() ? loopingColor : null);
            recordIcon.setColorOverride(recorderManager.isRecordLit() ? recordingColor : null);
            refreshClock();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed < RecorderSetting.REFRESH_SECONDS)
            return;

        this.refreshElapsed = 0f;
        refreshClock();
    }

    // The playhead against the media file's length, the frames written while it exports, nothing while none is open
    private void refreshClock() {

        if (!recorderManager.hasRecording()) {
            clockLabel.setFontText(RecorderSetting.TEXT_EMPTY);
            return;
        }

        if (recorderManager.getState() == RecorderState.EXPORTING) {
            clockLabel.setFontText(String.format(
                    RecorderSetting.FORMAT_PROGRESS,
                    recorderManager.getExportedFrames(),
                    recorderManager.getExportFrameCount()));
            return;
        }

        clockLabel.setFontText(String.format(
                RecorderSetting.FORMAT_CLOCK,
                recorderRowSystem.formatTime(recorderManager.getPlayhead()),
                recorderRowSystem.formatTime(recorderManager.resolveLength())));
    }
}
