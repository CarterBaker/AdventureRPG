package editor.bootstrap.dockpipeline.tab;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;

public class TabInstance extends InstancePackage {

    /*
     * A tab is a windowed context inside a dock zone.
     * The OS WindowInstance owns the render queue — the context drives what runs.
     * TabInstance just tracks identity, rect, context, and lifecycle state.
     * The context itself owns its FBO and render logic.
     */

    // Data
    private TabData tabData;

    // Context
    private ContextPackage context;
    private FboInstance fbo;

    // State
    private boolean active;
    private boolean suspended;

    // Constructor \\

    public void constructor(TabData tabData) {
        this.tabData = tabData;
        this.active = false;
        this.suspended = false;
    }

    // Lifecycle \\

    public void activate() {
        this.active = true;
        this.suspended = false;
    }

    public void suspend() {
        this.active = false;
        this.suspended = true;
    }

    public void resume() {
        this.suspended = false;
        this.active = true;
    }

    public void resize(int x, int y, int width, int height) {
        tabData.setX(x);
        tabData.setY(y);
        tabData.setWidth(width);
        tabData.setHeight(height);
    }

    // Mutators \\

    public void setContext(ContextPackage context) {
        this.context = context;
    }

    // Queries \\

    public boolean isActive() {
        return active;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean hasContext() {
        return context != null;
    }

    public boolean hasFbo() {
        return fbo != null;
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

    public void setFbo(FboInstance fbo) {
        this.fbo = fbo;
    }

    public String getTitle() {
        return tabData.getTitle();
    }

    public int getX() {
        return tabData.getX();
    }

    public int getY() {
        return tabData.getY();
    }

    public int getWidth() {
        return tabData.getWidth();
    }

    public int getHeight() {
        return tabData.getHeight();
    }
}
