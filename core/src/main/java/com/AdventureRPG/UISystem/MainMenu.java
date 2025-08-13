package com.AdventureRPG.UISystem;

public class MainMenu extends MenuType {

    public MainMenu(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    @Override
    public void open() {
        // Build or display UI components
        System.out.println("Main Menu opened."); // TODO: Remove debug line
    }

    @Override
    public void close() {
        // Tear down or hide UI components
        System.out.println("Main Menu closed."); // TODO: Remove debug line
    }
}
