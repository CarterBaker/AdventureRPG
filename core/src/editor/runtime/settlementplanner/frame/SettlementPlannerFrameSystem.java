package editor.runtime.settlementplanner.frame;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.settlementplanner.SettlementPlannerSetting;
import editor.runtime.worldmap.view.WorldMapViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SettlementPlannerFrameSystem extends SystemPackage {

    /*
     * Keeps the planner's map on its design. Whenever the site moves or
     * another entry is chosen, and once when the tab first has a size, the
     * view is set on the site's column and zoomed so the design's radius
     * fits a few times over across the window's narrower side; between
     * moves the view pans and zooms freely. Preview Here moves the site to the column at the view's centre.
     */

    // Internal
    private SettlementPlannerManager settlementPlannerManager;
    private WorldMapViewSystem worldMapViewSystem;

    // Frame
    private int framedRevision;

    // Base \\

    @Override
    protected void create() {
        this.framedRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.worldMapViewSystem = get(WorldMapViewSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        int frameRevision = settlementPlannerManager.getFrameRevision();

        if (frameRevision == framedRevision || window.getWidth() <= 0 || window.getHeight() <= 0
                || settlementPlannerManager.getSiteWorldHandle() == null)
            return;

        this.framedRevision = frameRevision;

        double radius = Math.max(
                settlementPlannerManager.getFootprintRadiusBlocks(), SettlementPlannerSetting.FRAME_MIN_RADIUS_BLOCKS);

        worldMapViewSystem.focus(
                settlementPlannerManager.getSiteX() + EngineSetting.BLOCK_CENTER_OFFSET,
                settlementPlannerManager.getSiteZ() + EngineSetting.BLOCK_CENTER_OFFSET,
                radius * SettlementPlannerSetting.FRAME_SPAN_RADII / Math.min(window.getWidth(), window.getHeight()));
    }

    // Management \\

    public void previewHere() {
        settlementPlannerManager.previewAt(worldMapViewSystem.getCenterX(), worldMapViewSystem.getCenterZ());
    }
}
