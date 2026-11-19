package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import engine.root.BranchPackage;

public class MaterialsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Materials tab. Pressing a row chooses its
     * material for every window that lays one and picks it up, to be dropped
     * on whichever window takes materials it is let go over. Rows hand back
     * the name of the material they show.
     */

    // Internal
    private MaterialsManager materialsManager;

    // Base \\

    @Override
    protected void get() {
        this.materialsManager = get(MaterialsManager.class);
    }

    // Materials \\

    // The material is chosen on the press and carried over the windows until let go
    public void dragMaterial(String materialName) {
        materialsManager.dragMaterial(materialName);
    }
}
