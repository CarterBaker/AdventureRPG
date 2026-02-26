package com.internal.bootstrap.menupipeline.menu;

import com.internal.core.engine.InstancePackage;

public class MenuInstance extends InstancePackage {

    private MenuHandle handle;
    private boolean visible;

    public void constructor(MenuHandle handle) {
        this.handle = handle;
        this.visible = true;
    }

    public void show() {
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    public MenuHandle getHandle() {
        return handle;
    }

    public boolean isVisible() {
        return visible;
    }
}