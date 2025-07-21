package com.AdventureRPG.UISystem;

public abstract class MenuType {
    protected final UISystem UISystem;

    public MenuType(UISystem UISystem) {
        this.UISystem = UISystem;
    }

    public abstract void Open();
    public abstract void Close();
}
