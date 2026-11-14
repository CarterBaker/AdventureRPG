package editor.runtime.shipwright;

import application.runtime.menueventsmanager.menus.shipwright.ShipwrightBranch;
import application.runtime.shipwright.ShipwrightCatalogueSystem;
import application.runtime.shipwright.ShipwrightDialogSystem;
import application.runtime.shipwright.ShipwrightRenderSystem;
import application.runtime.shipwright.ShipwrightSystem;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import engine.root.ContextPackage;

public class ShipwrightContext extends ContextPackage implements FileMenuProvider {

    /*
     * Editor tab holding the drafting table, the very table a player stands
     * at in the game — the same systems, menus and branch — except that the
     * ships drawn at it are written into the game's assets, and the table
     * stays open for as long as the tab does; closing the tab over unsaved
     * work asks first.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightCatalogueSystem shipwrightCatalogueSystem;
    private ShipwrightDialogSystem shipwrightDialogSystem;
    private ShipwrightRenderSystem shipwrightRenderSystem;
    private ShipwrightBranch shipwrightBranch;
    private ShipwrightTabSystem shipwrightTabSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.shipwrightSystem = create(ShipwrightSystem.class);
        this.shipwrightCatalogueSystem = create(ShipwrightCatalogueSystem.class);
        this.shipwrightDialogSystem = create(ShipwrightDialogSystem.class);
        this.shipwrightRenderSystem = create(ShipwrightRenderSystem.class);
        this.shipwrightBranch = create(ShipwrightBranch.class);
        this.shipwrightTabSystem = create(ShipwrightTabSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_SHIPWRIGHT;
    }
}
