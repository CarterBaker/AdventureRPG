package com.AdventureRPG.UISystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.AdventureRPG.GameManager;

public class UISystem {

    // Game Manager
    public final GameManager GameManager;

    private final List<MenuType> openMenus = new ArrayList<>();

    public UISystem(GameManager GameManager) {
        this.GameManager = GameManager;
    }

    public void Update() {

    }

    public MenuType Open(Menu Menu) {
        MenuType menuInstance = switch (Menu) {
            case LoadScreen -> new LoadScreen(this, Menu);
            case Main -> new MainMenu(this, Menu);
            default -> throw new IllegalArgumentException("Unknown menu type: " + Menu);
        };

        openMenus.add(menuInstance);
        menuInstance.Open();

        return menuInstance;
    }

    public void Close(Menu Menu) {
        Iterator<MenuType> iterator = openMenus.iterator();
        while (iterator.hasNext()) {
            MenuType openMenu = iterator.next();
            if (openMenu.GetMenu() == Menu) {
                Close(openMenu); // Call the close behavior
                iterator.remove(); // Safely remove from list
            }
        }
    }

    public void Close(MenuType Menu) {
        Menu.Close();
    }
}
