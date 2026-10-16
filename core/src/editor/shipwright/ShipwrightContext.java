package editor.shipwright;

import application.runtime.menueventsmanager.menus.shipwright.ShipwrightBranch;
import application.runtime.shipwright.ShipwrightRenderSystem;
import application.runtime.shipwright.ShipwrightSystem;
import engine.root.ContextPackage;

public class ShipwrightContext extends ContextPackage {

    /*
     * Editor tab holding the drafting table, the very table a player stands
     * at in the game — the same systems, menus and branch — except that the
     * ships drawn at it are written into the game's assets, and the table
     * stays open for as long as the tab does.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightRenderSystem shipwrightRenderSystem;
    private ShipwrightBranch shipwrightBranch;
    private ShipwrightTabSystem shipwrightTabSystem;

    // Internal \\

    @Override
    protected void create() {
        this.shipwrightSystem = create(ShipwrightSystem.class);
        this.shipwrightRenderSystem = create(ShipwrightRenderSystem.class);
        this.shipwrightBranch = create(ShipwrightBranch.class);
        this.shipwrightTabSystem = create(ShipwrightTabSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
