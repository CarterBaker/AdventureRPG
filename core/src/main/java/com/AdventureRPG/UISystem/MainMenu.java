package com.AdventureRPG.UISystem;

public class MainMenu extends MenuType {
    public MainMenu(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    @Override
    public void Open() {
        // Build or display UI components
        System.out.println("Main Menu opened.");
    }

    @Override
    public void Close() {
        // Tear down or hide UI components
        System.out.println("Main Menu closed.");
    }
}
