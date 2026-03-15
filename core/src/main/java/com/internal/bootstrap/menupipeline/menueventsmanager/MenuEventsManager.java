package com.internal.bootstrap.menupipeline.menueventsmanager;

import com.internal.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch;
import com.internal.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch;
import com.internal.bootstrap.menupipeline.menueventsmanager.util.GenericButtonBranch;
import com.internal.core.engine.ManagerPackage;

public class MenuEventsManager extends ManagerPackage {

    /*
     * Groups all button action handlers into branches by domain. Generic
     * utility actions live in GenericButtonBranch. Menu-specific actions
     * are grouped per menu in their own branch.
     */

    @Override
    protected void create() {

        // Util
        create(GenericButtonBranch.class);

        // Menus
        create(MainMenuBranch.class);
        create(InventoryBranch.class);
    }
}