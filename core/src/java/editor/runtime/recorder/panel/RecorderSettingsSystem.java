package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RecorderSettingsSystem extends SystemPackage {

    /*
     * Fills the Recorder Settings tab: the channels armed for the next take,
     * each pressed to arm or disarm it, and the open recording's export
     * settings — its size, frame rate, format, motion blur, supersampling and
     * range — with the export itself, or, while it runs, its progress and
     * its cancel. The rows are drawn again when the recorder's revision
     * moves, and the progress a few times a second.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Rows
    private ElementInstance progressLabel;

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
        this.menuManager = get(MenuManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderRowSystem = get(RecorderRowSystem.class);
    }

    @Override
    protected void awake() {
        recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_SETTINGS);
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() != shownRevision) {
            this.shownRevision = recorderManager.getRevision();
            refreshSettings();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= RecorderSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;
            refreshProgress();
        }
    }

    // Settings \\

    private void refreshSettings() {

        int entryPoint = RecorderSetting.ENTRY_SETTINGS;

        menuManager.ejectAll(recorderRowSystem.getMenu(), entryPoint);
        this.progressLabel = null;

        int armedChannels = recorderManager.getArmedChannels();

        recorderRowSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_RECORD);

        for (RecorderChannel channel : RecorderChannel.VALUES)
            recorderRowSystem.injectSetting(
                    entryPoint,
                    channel.getLabel(),
                    channel.isIn(armedChannels) ? RecorderSetting.TEXT_ARMED : RecorderSetting.TEXT_OFF,
                    EditorSetting.RECORDER_KEY_ARM_PREFIX + channel.getKey());

        recorderRowSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_EXPORT);

        if (!recorderManager.hasRecording()) {
            recorderRowSystem.injectText(entryPoint, RecorderSetting.TEXT_NO_RECORDING);
            return;
        }

        RecordingExportStruct exportStruct = recorderManager.getRecording().getExportStruct();

        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_RESOLUTION,
                String.format(RecorderSetting.FORMAT_RESOLUTION, exportStruct.getWidth(), exportStruct.getHeight()),
                EditorSetting.RECORDER_KEY_RESOLUTION);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_FRAME_RATE,
                String.format(RecorderSetting.FORMAT_FRAME_RATE, exportStruct.getFrameRate()),
                EditorSetting.RECORDER_KEY_FRAME_RATE);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_FORMAT,
                exportStruct.getFormat().getLabel(), EditorSetting.RECORDER_KEY_FORMAT);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_MOTION_BLUR,
                exportStruct.getMotionBlurSamples() > 1
                        ? String.format(RecorderSetting.FORMAT_MOTION_BLUR, exportStruct.getMotionBlurSamples())
                        : RecorderSetting.TEXT_OFF,
                EditorSetting.RECORDER_KEY_MOTION_BLUR);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_SUPERSAMPLE,
                recorderRowSystem.toOnOff(exportStruct.isSupersampled()), EditorSetting.RECORDER_KEY_SUPERSAMPLE);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_RANGE_START,
                exportStruct.hasRangeStart()
                        ? recorderRowSystem.formatTime(exportStruct.getRangeStart())
                        : RecorderSetting.TEXT_RANGE_FROM_START,
                EditorSetting.RECORDER_KEY_RANGE_START);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_RANGE_END,
                exportStruct.hasRangeEnd()
                        ? recorderRowSystem.formatTime(exportStruct.getRangeEnd())
                        : RecorderSetting.TEXT_RANGE_TO_END,
                EditorSetting.RECORDER_KEY_RANGE_END);
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_CLEAR_RANGE,
                EditorSetting.RECORDER_KEY_RANGE_CLEAR);

        if (recorderManager.getState() == RecorderState.EXPORTING) {
            recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_CANCEL_EXPORT,
                    EditorSetting.RECORDER_KEY_CANCEL_EXPORT);
            this.progressLabel = recorderRowSystem.injectText(entryPoint, RecorderSetting.TEXT_EMPTY);
            refreshProgress();
            return;
        }

        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_EXPORT, EditorSetting.RECORDER_KEY_EXPORT);
    }

    private void refreshProgress() {
        if (progressLabel != null && recorderManager.getState() == RecorderState.EXPORTING)
            progressLabel.setFontText(String.format(
                    RecorderSetting.FORMAT_PROGRESS,
                    recorderManager.getExportedFrames(),
                    recorderManager.getExportFrameCount()));
    }
}
