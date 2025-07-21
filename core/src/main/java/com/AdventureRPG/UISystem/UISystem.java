package com.AdventureRPG.UISystem;

import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.GameManager;

public class UISystem {

    // Game Manager
    public final GameManager GameManager;

    private final List<MenuType> openMenus = new ArrayList<>();

    public UISystem(GameManager GameManager) {
        // Setup Game Systems
        this.GameManager = GameManager;
    }

    public void Open(Menu menu) {
        MenuType menuInstance = switch (menu) {
            case Main -> new MainMenu(this);
            default -> throw new IllegalArgumentException("Unknown menu type: " + menu);
        };

        openMenus.add(menuInstance);
        menuInstance.Open();
    }

    public List<MenuType> getOpenMenus() {
        return openMenus;
    }
}
