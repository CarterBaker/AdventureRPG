package editor.runtime.animationproperties;

import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationPropertiesContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * Docker window holding the selected bone at the playhead: its key there
     * or the way to make one, its rotation, position and scale, and the tools
     * that key, copy, paste and mirror the whole pose. Every edit goes
     * through AnimationEditorManager, with its undo. The shared tool keys
     * work while this window is focused.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
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
        this.animationEditorManager = get(AnimationEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public int getRowRevision() {
        return animationEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.ANIMATION_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        animationEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.ANIMATION_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        animationEditorManager.step(key, steps);
    }
}
