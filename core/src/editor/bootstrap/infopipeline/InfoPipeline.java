package editor.bootstrap.infopipeline;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import engine.root.PipelinePackage;

public class InfoPipeline extends PipelinePackage {

    /*
     * Registers the editor's shared JSON content state, so every hierarchy
     * tab, info panel, and tool that reads content works on the same files.
     */

    // Internal \\

    @Override
    protected void create() {
        create(InfoManager.class);
    }
}
