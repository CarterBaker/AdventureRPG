package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import engine.root.BranchPackage;

public class ArchitectureBuilderBranch extends BranchPackage {

    /*
     * Menu event handlers for the Architecture Builder. Palette rows carry
     * an architecture's name or what the slot picked is filled with; New and
     * Duplicate first open the architecture worked on in the Hierarchy, so
     * the Info Panel's own New and Duplicate make the architecture, named
     * through the same dialogs; Save and Revert act on its file.
     */

    // Internal
    private ArchitectureBuilderManager architectureBuilderManager;
    private InfoPanelBranch infoPanelBranch;

    // Base \\

    @Override
    protected void get() {
        this.architectureBuilderManager = get(ArchitectureBuilderManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
    }

    // Selection \\

    public void selectRow(String key) {
        architectureBuilderManager.choosePaletteRow(key);
    }

    // File \\

    public void save() {
        architectureBuilderManager.save();
    }

    public void revert() {
        architectureBuilderManager.revert();
    }

    public void newArchitecture(WindowInstance window) {
        if (architectureBuilderManager.focusArchitecture())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateArchitecture(WindowInstance window) {
        if (architectureBuilderManager.focusArchitecture())
            infoPanelBranch.copySelection(window);
    }
}
