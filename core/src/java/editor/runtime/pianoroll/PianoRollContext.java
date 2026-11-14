package editor.runtime.pianoroll;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.pianorollmanager.PianoRollManager;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.pianoroll.panel.PianoRollSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PianoRollContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, PropertiesProvider, CharacterViewRowSource,
        FileMenuProvider {

    /*
     * Editor tab for the notes of the notes clip selected on the Timeline:
     * pitch rows down the side beside a keyboard whose keys sound when
     * clicked, time across on the song's bars and beats, and the clip's notes
     * as bars, drawn, erased, cut, selected and moved with the Tools tab's
     * tools on the Snap grid, changed by its Notes actions, and played in
     * with Musical Typing. The notes selected are the Properties tab's while
     * this tab is worked in, and Undo and Redo step the clip's edits. All
     * state lives in PianoRollManager and RecorderManager; this tab only
     * shows it and hands presses, drags and the wheel to PianoRollBranch. The
     * tab is a tool canvas, the piano roll, for as long as it is open.
     */

    // Internal
    private PianoRollSystem pianoRollSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PianoRollManager pianoRollManager;

    // Internal \\

    @Override
    protected void create() {
        this.pianoRollSystem = create(PianoRollSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.pianoRollManager = get(PianoRollManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.PIANO_ROLL;
    }

    // The Notes actions run exactly as their keys run them
    @Override
    public void runToolAction(String actionKey) {
        pianoRollManager.act(actionKey);
    }

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return pianoRollManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.PIANO_ROLL_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        pianoRollManager.buildRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.PIANO_ROLL_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        pianoRollManager.stepRow(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_PIANO_ROLL;
    }
}
