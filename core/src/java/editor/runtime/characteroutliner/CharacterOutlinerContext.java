package editor.runtime.characteroutliner;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
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

public class CharacterOutlinerContext extends ContextPackage implements CharacterViewRowSource, FileMenuProvider {

    /*
     * Docker window listing what the character editor works on in its mode:
     * how the preview stands and the features, bones, anchors or worn parts
     * to pick, the rig's bones in Rig mode, for the layer of the Character
     * Editor worked in last. Picking a row picks it for every Character
     * Editor window; the state lives in
     * CharacterEditorManager. The shared tool keys work while this window is
     * focused.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
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
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public int getRowRevision() {
        return characterEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.CHARACTER_OUTLINER_TITLE + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + characterEditorManager.getMode().getLabel();
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildListRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        characterEditorManager.step(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_CHARACTER;
    }
}
