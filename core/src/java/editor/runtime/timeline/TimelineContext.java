package editor.runtime.timeline;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.recorder.panel.RecorderTimelineSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TimelineContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, RecorderTabProvider, FileMenuProvider, PropertiesProvider,
        CharacterViewRowSource {

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
     * for as long as it is open: a Video's clips, or a Music file's, where
     * Draw draws a notes clip, Export MIDI and Render export it, and the
     * Properties tab shows the mix of the lane selected and the clip
     * selected's instrument, volume and transpose.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTimelineSystem recorderTimelineSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private RecorderBranch recorderBranch;
    private RecorderManager recorderManager;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTimelineSystem = create(RecorderTimelineSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void get() {
        this.recorderBranch = get(RecorderBranch.class);
        this.recorderManager = get(RecorderManager.class);
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
        return recorderManager.getToolCanvas();
    }

    // The Nesting and Export actions run exactly as their keys run them, on this tab's window
    @Override
    public void runToolAction(String actionKey) {
        recorderBranch.actSetting(actionKey, getWindow());
    }

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return recorderManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.RECORDER_TIMELINE_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildRows(rows, true);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.RECORDER_MUSIC_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        recorderManager.stepRow(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
