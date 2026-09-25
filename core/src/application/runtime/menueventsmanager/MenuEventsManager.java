package application.runtime.menueventsmanager;

import application.runtime.menueventsmanager.menus.InventoryBranch;
import application.runtime.menueventsmanager.menus.LoadMenuBranch;
import application.runtime.menueventsmanager.menus.MainMenuBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CharacterCreatorBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorAppearanceBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorBodyBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorNameBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorProgressionBranch;
import application.runtime.menueventsmanager.util.GenericButtonBranch;
import engine.root.ManagerPackage;

public class MenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(MainMenuBranch.class);
        create(LoadMenuBranch.class);
        create(CharacterCreatorBranch.class);
        create(CreatorNameBranch.class);
        create(CreatorAppearanceBranch.class);
        create(CreatorBodyBranch.class);
        create(CreatorProgressionBranch.class);
        create(InventoryBranch.class);
        create(GenericButtonBranch.class);
    }
}
