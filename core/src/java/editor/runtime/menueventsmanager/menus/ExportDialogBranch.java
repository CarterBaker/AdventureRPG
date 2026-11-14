package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderExportResult;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class ExportDialogBranch extends BranchPackage {

    /*
     * The editor's export dialog. Asking for an export opens it over the
     * whole window the request came from, holding every other control there
     * until the video is finished. While the export renders it shows the
     * video being made, a bar filling as frames are encoded, the frame it has
     * reached, and the real time spent and left, estimated from the frames
     * made so far, or that it waits on a pause menu opened under it; its one
     * button cancels. Once the export ends, or if it was refused, it says how
     * — where the video was saved, or why there is none — and the button,
     * Enter or Escape closes it. It follows its window's size for as long as
     * it is open.
     */

    // Internal
    private RecorderManager recorderManager;
    private MenuManager menuManager;
    private InputManager inputManager;

    // Dialog
    private MenuInstance dialog;
    private ElementInstance titleLabel;
    private ElementInstance fileLabel;
    private ElementInstance progressFill;
    private ElementInstance framesLabel;
    private ElementInstance timeLabel;
    private ElementInstance messageLabel;
    private ElementInstance buttonLabel;

    // Progress
    private DimensionVector2Struct[] fillSizes;
    private int shownStep;
    private boolean running;
    private long startNanos;
    private long firstFrameNanos;
    private long refreshedNanos;

    // Base \\

    @Override
    protected void create() {

        // Progress
        this.fillSizes = new DimensionVector2Struct[EditorSetting.EXPORT_DIALOG_FILL_STEPS + 1];

        for (int i = 0; i < fillSizes.length; i++)
            fillSizes[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(
                            EngineSetting.PERCENT_MAX * i / EditorSetting.EXPORT_DIALOG_FILL_STEPS),
                    DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX));
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void update() {

        if (dialog == null)
            return;

        WindowInstance window = dialog.getWindow();

        if (!window.hasCompositeTarget()) {
            close();
            return;
        }

        fitToWindow(window);

        if (running) {
            updateRunning();
            return;
        }

        updateKeys(window);
    }

    // Management \\

    // Asks the recorder for an export and opens the dialog on what came of it
    public void export(WindowInstance window) {

        if (dialog != null)
            return;

        recorderManager.export();
        open(window);
    }

    public boolean isOpen() {
        return dialog != null;
    }

    // Dialog Events \\

    // Cancels a running export, or closes the dialog once it has ended
    public void press() {

        if (dialog == null)
            return;

        if (running) {
            recorderManager.stop();
            return;
        }

        close();
    }

    private void open(WindowInstance window) {

        this.dialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_EXPORT_DIALOG, window.getGLWindow());

        this.titleLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_TITLE);
        this.fileLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_FILE);
        this.progressFill = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_FILL);
        this.framesLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_FRAMES);
        this.timeLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_TIME);
        this.messageLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_MESSAGE);
        this.buttonLabel = requireEntryPoint(EditorSetting.ENTRY_EXPORT_DIALOG_BUTTON);

        this.shownStep = EngineSetting.INDEX_NOT_FOUND;
        this.startNanos = System.nanoTime();
        this.firstFrameNanos = EditorSetting.EXPORT_DIALOG_TIME_UNSET;
        this.refreshedNanos = startNanos;
        this.running = recorderManager.getExportResult() == RecorderExportResult.EXPORTING;

        showFill(0);

        if (!running) {
            showResult();
            return;
        }

        titleLabel.setFontText(EditorSetting.EXPORT_DIALOG_TITLE_EXPORTING);
        fileLabel.setFontText(describeExport());
        buttonLabel.setFontText(EditorSetting.EXPORT_DIALOG_BUTTON_CANCEL);
        refreshProgress(startNanos);
    }

    private void close() {

        if (dialog == null)
            return;

        menuManager.closeMenuWindow(dialog);
        this.dialog = null;
        this.titleLabel = null;
        this.fileLabel = null;
        this.progressFill = null;
        this.framesLabel = null;
        this.timeLabel = null;
        this.messageLabel = null;
        this.buttonLabel = null;
        this.running = false;
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = dialog.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Export dialog '" + EditorSetting.MENU_EDITOR_EXPORT_DIALOG + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    // The dialog covers its OS window whatever size the window is given
    private void fitToWindow(WindowInstance window) {

        WindowInstance osWindow = window.getGLWindow();

        if (!window.isPlacedAt(0f, 0f, osWindow.getWidth(), osWindow.getHeight()))
            window.place(0f, 0f, osWindow.getWidth(), osWindow.getHeight());
    }

    private void updateRunning() {

        if (recorderManager.getExportResult() != RecorderExportResult.EXPORTING) {
            showResult();
            return;
        }

        long now = System.nanoTime();

        if (now - refreshedNanos < EditorSetting.EXPORT_DIALOG_REFRESH_NANOS)
            return;

        this.refreshedNanos = now;
        refreshProgress(now);
    }

    private void updateKeys(WindowInstance window) {

        Input rawInput = inputManager.getRawInput(window);

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD))
            close();
    }

    // Progress \\

    private void refreshProgress(long now) {

        int exported = recorderManager.getExportedFrames();
        int total = recorderManager.getExportFrameCount();

        if (exported > 0 && firstFrameNanos == EditorSetting.EXPORT_DIALOG_TIME_UNSET)
            this.firstFrameNanos = now;

        showFill((int) ((long) exported * EditorSetting.EXPORT_DIALOG_FILL_STEPS / total));
        messageLabel.setFontText(recorderManager.isSuspended()
                ? EditorSetting.RECORDER_STATUS_SUSPENDED
                : EditorSetting.EXPORT_DIALOG_TEXT_WAIT);

        if (exported == 0) {
            framesLabel.setFontText(EditorSetting.EXPORT_DIALOG_TEXT_PREPARING);
            timeLabel.setFontText(String.format(
                    EditorSetting.EXPORT_DIALOG_FORMAT_TIME,
                    formatDuration(now - startNanos),
                    EditorSetting.EXPORT_DIALOG_TEXT_ESTIMATING));
            return;
        }

        framesLabel.setFontText(String.format(
                EditorSetting.EXPORT_DIALOG_FORMAT_FRAMES,
                exported,
                total,
                (int) ((long) exported * (long) EngineSetting.PERCENT_MAX / total)));
        timeLabel.setFontText(String.format(
                EditorSetting.EXPORT_DIALOG_FORMAT_TIME,
                formatDuration(now - startNanos),
                exported >= EditorSetting.EXPORT_DIALOG_ESTIMATE_FRAMES
                        ? formatDuration((now - firstFrameNanos) / (exported - 1) * (total - exported))
                        : EditorSetting.EXPORT_DIALOG_TEXT_ESTIMATING));
    }

    // What the export ended in, or why it never began
    private void showResult() {

        RecorderExportResult result = recorderManager.getExportResult();
        boolean exported = result == RecorderExportResult.EXPORTED;

        this.running = false;

        titleLabel.setFontText(switch (result) {
            case EXPORTED -> EditorSetting.EXPORT_DIALOG_TITLE_EXPORTED;
            case FAILED -> EditorSetting.EXPORT_DIALOG_TITLE_FAILED;
            case CANCELLED -> EditorSetting.EXPORT_DIALOG_TITLE_CANCELLED;
            default -> EditorSetting.EXPORT_DIALOG_TITLE_REFUSED;
        });

        buttonLabel.setFontText(EditorSetting.EXPORT_DIALOG_BUTTON_CLOSE);

        if (!exported) {
            framesLabel.setFontText(EditorSetting.EXPORT_DIALOG_TEXT_EMPTY);
            timeLabel.setFontText(EditorSetting.EXPORT_DIALOG_TEXT_EMPTY);
            messageLabel.setFontText(recorderManager.getStatusText());
            return;
        }

        showFill(EditorSetting.EXPORT_DIALOG_FILL_STEPS);
        fileLabel.setFontText(recorderManager.getExportFile().getName());
        framesLabel.setFontText(String.format(
                EditorSetting.EXPORT_DIALOG_FORMAT_FRAMES_DONE, recorderManager.getExportFrameCount()));
        timeLabel.setFontText(String.format(
                EditorSetting.EXPORT_DIALOG_FORMAT_TOOK, formatDuration(System.nanoTime() - startNanos)));
        messageLabel.setFontText(EditorSetting.EXPORT_DIALOG_TEXT_SAVED_IN
                + recorderManager.getExportFile().getParentFile().getAbsolutePath());
    }

    private void showFill(int step) {

        if (step == shownStep)
            return;

        this.shownStep = step;
        progressFill.setSizeOverride(fillSizes[Math.clamp(step, 0, EditorSetting.EXPORT_DIALOG_FILL_STEPS)]);
    }

    // Utility \\

    private String describeExport() {

        RecordingExportStruct exportStruct = recorderManager.getRecording().getExportStruct();

        return String.format(
                EditorSetting.EXPORT_DIALOG_FORMAT_FILE,
                recorderManager.getExportFile().getName(),
                exportStruct.getWidth(),
                exportStruct.getHeight(),
                exportStruct.getFrameRate(),
                exportStruct.getFormat().getLabel());
    }

    private String formatDuration(long nanos) {

        long seconds = Math.max(0L, nanos / EngineSetting.NANOS_PER_SECOND);
        long hours = seconds / EditorSetting.EXPORT_DIALOG_SECONDS_PER_HOUR;
        long minutes = seconds % EditorSetting.EXPORT_DIALOG_SECONDS_PER_HOUR
                / EditorSetting.EXPORT_DIALOG_SECONDS_PER_MINUTE;
        long remainder = seconds % EditorSetting.EXPORT_DIALOG_SECONDS_PER_MINUTE;

        return hours > 0
                ? String.format(EditorSetting.EXPORT_DIALOG_FORMAT_HOURS, hours, minutes, remainder)
                : String.format(EditorSetting.EXPORT_DIALOG_FORMAT_MINUTES, minutes, remainder);
    }
}
