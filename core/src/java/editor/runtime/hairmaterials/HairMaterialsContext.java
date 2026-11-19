package editor.runtime.hairmaterials;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class HairMaterialsContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * The docker window listing the materials a strand of hair may be painted
     * with, every texture of the atlas the open character is painted from, the
     * one picked marked. Picking one makes it the material the Hair tab's new
     * strands grow in and its Brush paints, a choice every Hair tab shares,
     * as the Brushes tab holds the brush every canvas strokes with. The shared
     * tool keys work while this window is focused.
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
        return EditorSetting.TAB_TITLE_HAIR_MATERIALS;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildHairMaterialRows(rows);
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
