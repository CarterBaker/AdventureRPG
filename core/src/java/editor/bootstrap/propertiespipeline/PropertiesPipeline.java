package editor.bootstrap.propertiespipeline;

import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import engine.root.PipelinePackage;

public class PropertiesPipeline extends PipelinePackage {

    /*
     * Registers the editor's one Properties panel's target, shared by every
     * window that shows its values there, so whichever window was worked in
     * last fills the Properties tab.
     */

    // Internal \\

    @Override
    protected void create() {
        create(PropertiesManager.class);
    }
}
