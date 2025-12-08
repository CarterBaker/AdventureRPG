package com.AdventureRPG.uisystem;

public abstract class MenuInstance {
    protected final UISystem UISystem;
    protected final Menu menu;

    public MenuInstance(UISystem UISystem, Menu menu) {
        this.UISystem = UISystem;
        this.menu = menu;
    }

    public Menu getMenu() {
        return menu;
    }

    public abstract void open();

    public abstract void close();

    public boolean blockInput() {
        return true;
    }
}
