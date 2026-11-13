package editor.runtime.timeline;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderTimelineSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class TimelineContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the recorder's timeline: its lanes of clips to move,
     * split, trim, stamp, mute and strip of channels, zoomed and scrolled,
     * sounds dropped onto them from the sound library, takes from the
     * Recorder Bin, saved recordings from the Recordings tab to nest, and
     * transitions on their edges from the transitions tool, each clip showing
     * the nodes of what the player does along it; its title names the
     * recording open and the ones it was opened inside. A nested clip clicked
     * twice opens its recording, and the Tools panel's Nesting actions nest
     * the clips selected into a new recording or open the nested clip
     * selected. The transport is the Recorder tab beside it. All state lives
     * in RecorderManager; this tab only shows it and hands clicks, drags and
     * the wheel to RecorderBranch. The tab is a tool canvas, the timeline,
     * for as long as it is open.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTimelineSystem recorderTimelineSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private RecorderBranch recorderBranch;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTimelineSystem = create(RecorderTimelineSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.recorderBranch = get(RecorderBranch.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return EditorSetting.RECORDER_TIMELINE_MENU;
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

    // The Nesting actions run exactly as their keys run them, on this tab's window
    @Override
    public void runToolAction(String actionKey) {
        recorderBranch.actSetting(actionKey, getWindow());
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
