package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RecorderTransportSystem extends SystemPackage {

    /*
     * Shows the Recorder tab's transport: when the recorder's revision moves
     * it redraws the status line, what is open — the recording and the
     * recordings it was opened inside, outermost first — and the loop, on or
     * off, with the range it plays round; the clock, the playhead against the
     * recording's length, refreshes a few times a second.
     */

    // Internal
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private ElementInstance statusLabel;
    private ElementInstance clockLabel;
    private ElementInstance openLabel;
    private ElementInstance loopLabel;

    // State
    private int shownRevision;
    private float refreshElapsed;

    // Base \\

    @Override
    protected void create() {

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
        this.statusLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_STATUS);
        this.clockLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_CLOCK);
        this.openLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_OPEN);
        this.loopLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_LOOP);
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() != shownRevision) {
            this.shownRevision = recorderManager.getRevision();
            statusLabel.setFontText(recorderManager.getStatusText());
            openLabel.setFontText(recorderRowSystem.describeOpen());
            refreshLoop();
            refreshClock();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed < RecorderSetting.REFRESH_SECONDS)
            return;

        this.refreshElapsed = 0f;
        refreshClock();
    }

    // The playhead against the recording's length, nothing while none is open
    private void refreshClock() {

        if (!recorderManager.hasRecording()) {
            clockLabel.setFontText(RecorderSetting.TEXT_EMPTY);
            return;
        }

        clockLabel.setFontText(String.format(
                RecorderSetting.FORMAT_CLOCK,
                recorderRowSystem.formatTime(recorderManager.getPlayhead()),
                recorderRowSystem.formatTime(recorderManager.resolveLength())));
    }

    // Whether playing loops, and the range it loops round: the export's range, the whole recording while none is set
    private void refreshLoop() {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null) {
            loopLabel.setFontText(RecorderSetting.TEXT_EMPTY);
            return;
        }

        RecordingExportStruct exportStruct = recording.getExportStruct();
        float duration = recording.getDuration();

        loopLabel.setFontText(String.format(
                RecorderSetting.FORMAT_LOOP,
                recorderManager.isLooping() ? RecorderSetting.TEXT_LOOP_ON : RecorderSetting.TEXT_LOOP_OFF,
                recorderRowSystem.formatTime(exportStruct.resolveStart(duration)),
                recorderRowSystem.formatTime(exportStruct.resolveEnd(duration))));
    }
}
