package com.AdventureRPG.UISystem;

public abstract class MenuType {
    protected final UISystem UISystem;
    protected final Menu Menu;

    public MenuType(UISystem UISystem, Menu Menu) {
        this.UISystem = UISystem;
        this.Menu = Menu;
    }

    public Menu GetMenu() {
        return Menu;
    }

    public abstract void Open();

    public abstract void Close();

    public boolean BlockInput() {
        return true;
    }
}
