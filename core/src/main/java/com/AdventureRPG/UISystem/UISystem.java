package com.AdventureRPG.UISystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.AdventureRPG.Core.Exceptions.UIException;
import com.AdventureRPG.Core.PhysicsPipeline.InputSystem.InputSystem;
import com.AdventureRPG.Core.Root.SystemFrame;

public class UISystem extends SystemFrame {

    // Root
    private InputSystem inputSystem;

    // UI System
    private final List<MenuInstance> openMenus = new ArrayList<>();

    // Base \\

    @Override
    protected void init() {
        inputSystem = engineManager.get(InputSystem.class);
    }

    // UI System \\

    public MenuInstance open(Menu Menu) {

        MenuInstance menuInstance = switch (Menu) {

            case LoadScreen -> new LoadScreen(this, Menu);
            case Main -> new MainMenu(this, Menu);
            default -> throw new UIException.UnknownMenuException(Menu.name());

        };

        openMenus.add(menuInstance);
        menuInstance.open();

        if (menuInstance.blockInput())
            inputSystem.block(true);

        return menuInstance;
    }

    public void close(Menu Menu) {

        Iterator<MenuInstance> iterator = openMenus.iterator();

        while (iterator.hasNext()) {

            MenuInstance openMenu = iterator.next();

            if (openMenu.getMenu() == Menu) {

                close(openMenu); // Call the close behavior
                iterator.remove(); // Safely remove from list
            }
        }

        updateInputBlockState();
    }

    public void close(MenuInstance Menu) {

        Menu.close();
        openMenus.remove(Menu);

        updateInputBlockState();
    }

    private void updateInputBlockState() {

        for (MenuInstance menu : openMenus)
            if (menu.blockInput()) {

                inputSystem.block(true);
                return;
            }

        inputSystem.block(false);
    }
}
