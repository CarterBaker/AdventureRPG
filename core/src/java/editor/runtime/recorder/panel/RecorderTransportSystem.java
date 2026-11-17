package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RecorderTransportSystem extends SystemPackage {

    /*
     * Shows beside the Recorder tab's buttons what the transport is doing:
     * the status line, redrawn when the recorder's revision moves, so a
     * Record or Play that cannot start says why where it was pressed, and
     * the clock, the playhead against the media file's length, refreshed a
     * few times a second.
     */

    // Internal
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private ElementInstance statusLabel;
    private ElementInstance clockLabel;

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
        this.statusLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_STATUS);
        this.clockLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TRANSPORT_CLOCK);
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() != shownRevision) {
            this.shownRevision = recorderManager.getRevision();
            statusLabel.setFontText(recorderManager.getStatusText());
            refreshClock();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed < RecorderSetting.REFRESH_SECONDS)
            return;

        this.refreshElapsed = 0f;
        refreshClock();
    }

    // The playhead against the media file's length, nothing while none is open
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
}
