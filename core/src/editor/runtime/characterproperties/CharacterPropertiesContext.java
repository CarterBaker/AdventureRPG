package editor.runtime.characterproperties;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterPropertiesContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * Docker window holding the values of what the character editor's mode
     * and selection work on — colors and palettes, the build, an anchor, the
     * mesh, sculpt and weight tools, or the selected bone of the rig — each
     * number on a slider and typed into its box, for whichever of the
     * Character Editor and Character Mesh Editor was worked in last. Every
     * edit goes through CharacterEditorManager, so every such tab shows it
     * at once. The shared tool keys work while this window is focused.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
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
        return EditorSetting.CHARACTER_PROPERTIES_TITLE + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + characterEditorManager.getMode().getLabel();
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        characterEditorManager.step(key, steps);
    }
}
