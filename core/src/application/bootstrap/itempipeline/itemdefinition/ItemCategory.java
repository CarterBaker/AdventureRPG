package application.bootstrap.itempipeline.itemdefinition;

import engine.root.EngineSetting;

public enum ItemCategory {

    /*
     * The kind of thing an item is. A container's contents are listed grouped
     * under these headers, in this order, and an item's JSON names its
     * category by the lower-case constant name.
     */

    WEAPON(EngineSetting.ITEM_CATEGORY_TITLE_WEAPON),
    ARMOR(EngineSetting.ITEM_CATEGORY_TITLE_ARMOR),
    CLOTHING(EngineSetting.ITEM_CATEGORY_TITLE_CLOTHING),
    JEWELRY(EngineSetting.ITEM_CATEGORY_TITLE_JEWELRY),
    CONTAINER(EngineSetting.ITEM_CATEGORY_TITLE_CONTAINER),
    CONSUMABLE(EngineSetting.ITEM_CATEGORY_TITLE_CONSUMABLE),
    MATERIAL(EngineSetting.ITEM_CATEGORY_TITLE_MATERIAL),
    MISC(EngineSetting.ITEM_CATEGORY_TITLE_MISC);

    // Internal
    private final String title;

    // Constructor \\

    ItemCategory(String title) {
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
