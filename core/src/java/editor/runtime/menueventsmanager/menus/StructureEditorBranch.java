package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import engine.root.BranchPackage;

public class StructureEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Structure Editor. Palette rows carry a
     * structure's or an architecture's name, a slot, an entry of the
     * structure's own, a block, a covering or a content; New and Duplicate
     * first open the structure painted in the Hierarchy, so the Info Panel's
     * own New and Duplicate make the structure, named through the same
     * dialogs; Save and Revert act on its file, Undo and Redo on the editor's
     * own history of strokes.
     */

    // Internal
    private StructureEditorManager structureEditorManager;
    private InfoPanelBranch infoPanelBranch;

    // Base \\

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
    }

    // Selection \\

    public void selectRow(String key) {
        structureEditorManager.choosePaletteRow(key);
    }

    // File \\

    public void save() {
        structureEditorManager.save();
    }

    public void revert() {
        structureEditorManager.revert();
    }

    public void undo() {
        structureEditorManager.undo();
    }

    public void redo() {
        structureEditorManager.redo();
    }

    public void newStructure(WindowInstance window) {
        if (structureEditorManager.focusStructure())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateStructure(WindowInstance window) {
        if (structureEditorManager.focusStructure())
            infoPanelBranch.copySelection(window);
    }
}
