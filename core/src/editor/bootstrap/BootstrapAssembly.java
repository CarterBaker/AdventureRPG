package editor.bootstrap;

import editor.bootstrap.dockpipeline.DockPipeline;
import engine.root.AssemblyPackage;

public class BootstrapAssembly extends AssemblyPackage {

    @Override
    protected void create() {
        create(DockPipeline.class);
    }
}