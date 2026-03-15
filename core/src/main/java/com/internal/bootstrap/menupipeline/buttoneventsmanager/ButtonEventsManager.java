package com.internal.bootstrap.menupipeline.buttoneventsmanager;

import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.InventoryBranch;
import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.MainMenuBranch;
import com.internal.bootstrap.menupipeline.buttoneventsmanager.util.GenericButtonBranch;
import com.internal.core.engine.ManagerPackage;

public class ButtonEventsManager extends ManagerPackage {

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