package application.runtime.menueventsmanager;

import application.runtime.menueventsmanager.menus.DefeatBranch;
import application.runtime.menueventsmanager.menus.HUDBranch;
import application.runtime.menueventsmanager.menus.LoadMenuBranch;
import application.runtime.menueventsmanager.menus.MainMenuBranch;
import application.runtime.menueventsmanager.menus.PauseMenuBranch;
import application.runtime.menueventsmanager.menus.character.CharacterAttributeBranch;
import application.runtime.menueventsmanager.menus.character.CharacterBranch;
import application.runtime.menueventsmanager.menus.character.CharacterProfessionBranch;
import application.runtime.menueventsmanager.menus.character.CharacterProficiencyBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CharacterCreatorBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorAppearanceBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorBodyBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorNameBranch;
import application.runtime.menueventsmanager.menus.charactercreator.CreatorProgressionBranch;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import application.runtime.menueventsmanager.menus.inventory.InventoryContainerBranch;
import application.runtime.menueventsmanager.menus.inventory.InventoryDragBranch;
import application.runtime.menueventsmanager.menus.inventory.InventoryEquipmentBranch;
import application.runtime.menueventsmanager.menus.settings.SettingsBindingBranch;
import application.runtime.menueventsmanager.menus.settings.SettingsMenuBranch;
import application.runtime.menueventsmanager.menus.settings.SettingsOptionBranch;
import application.runtime.menueventsmanager.util.GenericButtonBranch;
import application.runtime.menueventsmanager.util.ProgressionDisplayBranch;
import engine.root.ManagerPackage;

public class MenuEventsManager extends ManagerPackage {

    /*
     * Owns this context's menu callback branches. Branches live in the
     * context's registry, so menu actions from this window always reach this
     * context's own instances.
     */

    @Override
    protected void create() {
        create(MainMenuBranch.class);
        create(LoadMenuBranch.class);
        create(CharacterCreatorBranch.class);
        create(CreatorNameBranch.class);
        create(CreatorAppearanceBranch.class);
        create(CreatorBodyBranch.class);
        create(CreatorProgressionBranch.class);
        create(SettingsMenuBranch.class);
        create(SettingsOptionBranch.class);
        create(SettingsBindingBranch.class);
        create(PauseMenuBranch.class);
        create(InventoryBranch.class);
        create(InventoryEquipmentBranch.class);
        create(InventoryContainerBranch.class);
        create(InventoryDragBranch.class);
        create(CharacterBranch.class);
        create(CharacterAttributeBranch.class);
        create(CharacterProficiencyBranch.class);
        create(CharacterProfessionBranch.class);
        create(HUDBranch.class);
        create(DefeatBranch.class);
        create(GenericButtonBranch.class);
        create(ProgressionDisplayBranch.class);
    }
}
