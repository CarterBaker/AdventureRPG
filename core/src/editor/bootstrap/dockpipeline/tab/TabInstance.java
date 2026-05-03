package editor.bootstrap.dockpipeline.tab;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;

public class TabInstance extends InstancePackage {

    // Data
    private TabData tabData;

    // Context
    private ContextPackage context;
    private WindowInstance logicalWindow;

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

    public void setLogicalWindow(WindowInstance logicalWindow) {
        this.logicalWindow = logicalWindow;
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

    public boolean hasLogicalWindow() {
        return logicalWindow != null;
    }

    // Accessible \\

    public TabData getTabData() {
        return tabData;
    }

    public ContextPackage getContext() {
        return context;
    }

    public WindowInstance getLogicalWindow() {
        return logicalWindow;
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