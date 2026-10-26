package editor.runtime.settlementplanner;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.settlementplanner.frame.SettlementPlannerFrameSystem;
import editor.runtime.settlementplanner.menu.SettlementPlannerMenuSystem;
import editor.runtime.worldmap.render.WorldMapRenderSystem;
import editor.runtime.worldmap.view.WorldMapViewSystem;
import engine.root.ContextPackage;

public class SettlementPlannerContext extends ContextPackage {

    /*
     * Editor tab for designing a settlement or structure before, and while,
     * it stands in the world. It shows the world map, the same map the World
     * Map tab draws, framed on the design's site, where the design is drawn
     * exactly as it would stand, every hand placement marked with its icon,
     * beside the catalogue of settlement types and structures and the
     * architectures the chosen settlement can be built in. The design, its
     * preview and its live edits live in the editor; the World Editor places
     * it and brings placements back to edit.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private SettlementPlannerFrameSystem settlementPlannerFrameSystem;
    private WorldMapViewSystem worldMapViewSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldMapRenderSystem worldMapRenderSystem;
    private SettlementPlannerMenuSystem settlementPlannerMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.settlementPlannerFrameSystem = create(SettlementPlannerFrameSystem.class);
        this.worldMapViewSystem = create(WorldMapViewSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
        this.worldMapRenderSystem = create(WorldMapRenderSystem.class);
        this.settlementPlannerMenuSystem = create(SettlementPlannerMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void previewHere() {
        settlementPlannerFrameSystem.previewHere();
    }
}
