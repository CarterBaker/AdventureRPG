package application.bootstrap.renderpipeline.panel;

import engine.root.InstancePackage;

public abstract class PanelViewInstance extends InstancePackage {
    public abstract void render(int x, int y, int w, int h);
}
