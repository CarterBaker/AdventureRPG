package editor.bootstrap.dock.tab;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;

public class TabInstance extends InstancePackage {

    // Data
    private TabData tabData;

    // Runtime References
    private ContextPackage context;
    private FboInstance fbo;

    // Constructor \\

    public void constructor(TabData tabData) {
        this.tabData = tabData;
        this.context = null;
        this.fbo = null;
    }

    // Mutators \\

    public void setContext(ContextPackage context) {
        this.context = context;
    }

    public void setFbo(FboInstance fbo) {
        this.fbo = fbo;
    }

    // Accessible \\

    public TabData getTabData() {
        return tabData;
    }

    public ContextPackage getContext() {
        return context;
    }

    public FboInstance getFbo() {
        return fbo;
    }

    public boolean hasContext() {
        return context != null;
    }

    public boolean hasFbo() {
        return fbo != null;
    }
}