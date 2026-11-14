package editor.bootstrap.shipviewerpipeline;

import editor.bootstrap.shipviewerpipeline.shipviewermanager.ShipViewerManager;
import engine.root.PipelinePackage;

public class ShipViewerPipeline extends PipelinePackage {

    /*
     * Registers the ship the Ship Viewer shows, chosen in the hierarchy or
     * at the Shipwright, so every viewer draws the ship being worked on.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ShipViewerManager.class);
    }
}
