package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;

public class CreatorProgressionBranch extends BranchPackage {

    /*
     * Holds the creator's places for character progression that is still to
     * come. Class, Profession, and Skills tabs show a note naming what they
     * will offer, and the attribute panel lists every attribute at its
     * placeholder value — the rows those systems will fill once they exist.
     */

    // Internal
    private MenuManager menuManager;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    // Populate \\

    void populatePlaceholder(CreatorSessionStruct session) {

        CreatorTab tab = session.getActiveTab();

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_PLACEHOLDER,
                note -> {
                    note.findChildById(RuntimeSetting.ELEMENT_CREATOR_PLACEHOLDER_TITLE).setFontText(tab.getTitle());
                    note.findChildById(RuntimeSetting.ELEMENT_CREATOR_PLACEHOLDER_TEXT)
                            .setFontText(tab.getPlaceholderText());
                });
    }

    void populateStats(CreatorSessionStruct session) {

        for (String statName : RuntimeSetting.CREATOR_STAT_NAMES)
            menuManager.inject(
                    session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_STATS, RuntimeSetting.MENU_CREATOR_STAT_ROW,
                    row -> {
                        row.findChildById(RuntimeSetting.ELEMENT_CREATOR_STAT_NAME).setFontText(statName);
                        row.findChildById(RuntimeSetting.ELEMENT_CREATOR_STAT_VALUE)
                                .setFontText(RuntimeSetting.CREATOR_STAT_PLACEHOLDER_VALUE);
                    });
    }
}
