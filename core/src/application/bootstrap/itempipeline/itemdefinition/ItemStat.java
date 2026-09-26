package application.bootstrap.itempipeline.itemdefinition;

import engine.root.EngineSetting;

public enum ItemStat {

    /*
     * Statistics an item adds to whoever wears or wields it. An item's JSON
     * "stats" block names each by its lower-case constant name; anything left
     * out adds nothing.
     */

    ARMOR(EngineSetting.ITEM_STAT_TITLE_ARMOR),
    DAMAGE(EngineSetting.ITEM_STAT_TITLE_DAMAGE),
    HEALTH(EngineSetting.ITEM_STAT_TITLE_HEALTH),
    STAMINA(EngineSetting.ITEM_STAT_TITLE_STAMINA),
    STRENGTH(EngineSetting.ITEM_STAT_TITLE_STRENGTH),
    DEXTERITY(EngineSetting.ITEM_STAT_TITLE_DEXTERITY),
    CONSTITUTION(EngineSetting.ITEM_STAT_TITLE_CONSTITUTION),
    INTELLIGENCE(EngineSetting.ITEM_STAT_TITLE_INTELLIGENCE),
    WISDOM(EngineSetting.ITEM_STAT_TITLE_WISDOM),
    CHARISMA(EngineSetting.ITEM_STAT_TITLE_CHARISMA);

    // Internal
    private final String title;

    // Constructor \\

    ItemStat(String title) {
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
