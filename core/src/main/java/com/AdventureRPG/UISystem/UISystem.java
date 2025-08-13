package com.AdventureRPG.UISystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.AdventureRPG.GameManager;

public class UISystem {

    // Game Manager
    public final GameManager gameManager;

    // UI System
    private final List<MenuType> openMenus = new ArrayList<>();

    // Base \\

    public UISystem(GameManager gameManager) {

        // Game Manager
        this.gameManager = gameManager;
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

    }

    public void render() {

    }

    // UI System \\

    public MenuType open(Menu Menu) {
        MenuType menuInstance = switch (Menu) {
            case LoadScreen -> new LoadScreen(this, Menu);
            case Main -> new MainMenu(this, Menu);
            default -> throw new IllegalArgumentException("Unknown menu type: " + Menu);
        };

        openMenus.add(menuInstance);
        menuInstance.open();

        if (menuInstance.blockInput())
            gameManager.inputSystem.block(true);

        return menuInstance;
    }

    public void close(Menu Menu) {
        Iterator<MenuType> iterator = openMenus.iterator();
        while (iterator.hasNext()) {
            MenuType openMenu = iterator.next();
            if (openMenu.getMenu() == Menu) {
                close(openMenu); // Call the close behavior
                iterator.remove(); // Safely remove from list
            }
        }

        updateInputBlockState();
    }

    public void close(MenuType Menu) {
        Menu.close();
        openMenus.remove(Menu);
        updateInputBlockState();
    }

    private void updateInputBlockState() {

        for (MenuType menu : openMenus) {
            if (menu.blockInput()) {
                gameManager.inputSystem.block(true);
                return;
            }
        }

        gameManager.inputSystem.block(false);
    }
}
