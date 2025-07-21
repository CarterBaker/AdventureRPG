package com.AdventureRPG.UISystem;

public class LoadScreen extends MenuType {
    public LoadScreen(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    @Override
    public void Open() {
        // Build or display UI components
        System.out.println("Load Screen opened.");
    }

    @Override
    public void Close() {
        // Tear down or hide UI components
        System.out.println("Load Screen closed.");
    }
}
