package editor.runtime.recorder;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderTimelineSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage
        implements ToolCanvasProvider, RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the recorder's timeline: the transport that records,
     * plays and stops in the Dev window, and the timeline's lanes of clips to
     * move, split, trim, stamp, mute and strip of channels, zoomed and
     * scrolled, sounds dropped onto them from the sound library, takes from
     * the Recorder Bin and transitions on their edges from the transitions
     * tool, each clip showing the nodes of what the player does along it.
     * Its other work is in the tabs beside it: the Recorder Keys, where every
     * value a take recorded is a curve of keys; the Recorder Bin of takes
     * beside the selected clip's edits; the Recorder Assets the recording
     * uses; and the Recorder Settings, the channels armed and the export.
     * The recordings themselves are made and opened in the Recordings tool.
     * All state lives in RecorderManager; this tab only shows it and hands
     * clicks, drags and the wheel to RecorderBranch. The tab is a tool
     * canvas, the timeline, for as long as it is open.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTimelineSystem recorderTimelineSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTimelineSystem = create(RecorderTimelineSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return EditorSetting.RECORDER_MENU;
    }

    // The timeline is no pane, so working in it leaves the pane the keys act on as it is
    @Override
    public RecorderPane getRecorderPane() {
        return null;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.RECORDER;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
