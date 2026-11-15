package editor.runtime.texturemaps;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureMapsContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * Editor tab listing the maps of the texture being painted — albedo,
     * normal, height, ambient occlusion, metallic, roughness and tint, each
     * its own image beside the texture, as a material's channels are listed
     * in a texturing tool — the one painted selected. Picking a map makes it
     * the one Paint mode's brush lays colour on; the choice is the Character
     * Editor's, so every tab follows at once.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
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
        return EditorSetting.TAB_TITLE_TEXTURE_MAPS;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildMapRows(rows);
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
