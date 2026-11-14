package editor.runtime.faceoutliner;

import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FaceOutlinerContext extends ContextPackage implements CharacterViewRowSource, FileMenuProvider {

    /*
     * Docker window listing what the face editor works on: the gender the
     * face is previewed as and whether edits mirror across it, every face
     * control under its group, and every face bone. Picking a row picks it
     * for every Face Editor tab; the state lives in FaceEditorManager. The
     * shared tool keys work while this window is focused.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public int getRowRevision() {
        return faceEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.FACE_OUTLINER_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        faceEditorManager.buildListRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.FACE_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        faceEditorManager.step(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_FACE;
    }
}
