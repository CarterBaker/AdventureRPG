package com.AdventureRPG.uisystem;

public class MainMenu extends MenuInstance {

    public MainMenu(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    @Override
    public void open() {
        // Build or display UI components
        System.out.println("Main Menu opened."); // TODO: Debug line
    }

    @Override
    public void close() {
        // Tear down or hide UI components
        System.out.println("Main Menu closed."); // TODO: Debug line
    }
}
