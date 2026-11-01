package editor.runtime.animationoutliner;

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

public class AnimationOutlinerContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * Docker window listing the open clip: the character it plays on, whether
     * it loops, and every bone of its rig with how many keys it holds, the
     * selected one highlighted. Picking a bone picks it for every Animation
     * Editor tab; the state lives in AnimationEditorManager. The shared tool
     * keys work while this window is focused.
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
        return EditorSetting.ANIMATION_OUTLINER_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        animationEditorManager.buildListRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.ANIMATION_EDITOR_ROW_TEMPLATES;
    }
}
