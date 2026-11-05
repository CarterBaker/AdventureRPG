package application.runtime.shipwright;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPreviewStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTab;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightCatalogueSystem extends SystemPackage {

    /*
     * The catalogue of the tab open under a drafting table's sheet: one
     * scrolling list of every option the tab lists, laid out under a heading
     * for each section — the tab's own, the masts, or furniture by the folder
     * its table is kept in, every furnishing in the game — a row of tiles at
     * a time, each tile the option's name under its picture:
     * the item it stands for, or the fitting built as a ship builds it, which
     * ShipwrightRenderSystem draws; an option with neither is a tile of its
     * name alone. The
     * option picked is lit; picked by the bindings, it is scrolled into view.
     * The list is laid out again only when the table opens, the tab changes
     * or the options it lists do, never while it is merely scrolled or
     * picked from.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightManager shipwrightManager;
    private MenuManager menuManager;

    // Layout — what the list was last laid out for
    private MenuInstance shownMenu;
    private ShipwrightTab shownTab;
    private int shownOptionCount;
    private int shownOption;
    private boolean scrollPending;

    // Tiles — one per option, in order
    private ObjectArrayList<ElementInstance> tileElements;
    private ObjectArrayList<ElementInstance> iconElements;
    private ObjectArrayList<ItemDefinitionHandle> tileItems;
    private ObjectArrayList<ShipPreviewStruct> tilePreviews;
    private IntArrayList tileOptions;

    // Colors
    private MenuColorStruct pickedColor;

    // Base \\

    @Override
    protected void create() {

        // Tiles
        this.tileElements = new ObjectArrayList<>();
        this.iconElements = new ObjectArrayList<>();
        this.tileItems = new ObjectArrayList<>();
        this.tilePreviews = new ObjectArrayList<>();
        this.tileOptions = new IntArrayList();

        // Colors
        this.pickedColor = new MenuColorStruct(EngineSetting.SHIPWRIGHT_PICKED_TOOL_COLOR);
    }

    @Override
    protected void get() {
        this.shipwrightSystem = get(ShipwrightSystem.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.menuManager = get(MenuManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (!shipwrightSystem.isOpen()) {
            this.shownMenu = null;
            return;
        }

        ShipwrightSessionInstance session = shipwrightSystem.getSession();
        ShipwrightTab tab = session.getTab();
        int count = shipwrightManager.getOptionCount(session);
        int option = shipwrightManager.getOptionIndex(session);

        if (shipwrightSystem.getTableMenu() != shownMenu || tab != shownTab || count != shownOptionCount)
            layOut(session, tab, count);

        if (option != shownOption)
            pick(option);

        scrollToPicked();
    }

    // Layout \\

    // Every option of the tab open as a tile, a heading over each section, a row of tiles at a time
    private void layOut(ShipwrightSessionInstance session, ShipwrightTab tab, int count) {

        MenuInstance tableMenu = shipwrightSystem.getTableMenu();

        this.shownMenu = tableMenu;
        this.shownTab = tab;
        this.shownOptionCount = count;
        this.shownOption = EngineSetting.INDEX_NOT_FOUND;

        menuManager.ejectAll(tableMenu, EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE);
        tileElements.clear();
        iconElements.clear();
        tileItems.clear();
        tilePreviews.clear();
        tileOptions.clear();

        ElementInstance list = tableMenu.getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE);
        String section = null;
        ElementInstance row = null;

        if (list != null)
            list.setScrollY(0f);

        for (int entry = 0; entry < count; entry++) {

            String optionSection = shipwrightManager.getEntrySection(session, entry);

            if (!optionSection.equals(section)) {
                section = optionSection;
                row = null;
                injectHeading(tableMenu, optionSection);
            }

            if (row == null || row.getChildren().size() == EngineSetting.SHIPWRIGHT_CATALOGUE_COLUMNS)
                row = menuManager.inject(tableMenu, EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE,
                        EngineSetting.SHIPWRIGHT_TEMPLATE_CATALOGUE_ROW);

            injectTile(row, session, entry);
        }
    }

    private void injectHeading(MenuInstance tableMenu, String section) {
        menuManager.inject(tableMenu, EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE,
                EngineSetting.SHIPWRIGHT_TEMPLATE_CATALOGUE_HEADING,
                element -> element.findChildById(EngineSetting.SHIPWRIGHT_ELEMENT_CATALOGUE_HEADING_LABEL)
                        .setFontText(section));
    }

    private void injectTile(ElementInstance row, ShipwrightSessionInstance session, int entry) {

        ShipwrightTool tool = shipwrightManager.getEntryTool(session, entry);
        int option = shipwrightManager.getEntryOption(session, entry);
        String name = shipwrightManager.getOptionName(session, tool, option);
        ItemDefinitionHandle item = shipwrightManager.getOptionItem(tool, option);
        ShipPreviewStruct preview = shipwrightManager.getOptionPreview(tool, option);
        String template = item == null && preview == null
                ? EngineSetting.SHIPWRIGHT_TEMPLATE_CATALOGUE_TEXT
                : EngineSetting.SHIPWRIGHT_TEMPLATE_CATALOGUE_ITEM;
        ElementInstance tile = menuManager.inject(row, template, element -> {
            element.setActionArgOverride(Integer.toString(entry));
            element.findChildById(EngineSetting.SHIPWRIGHT_ELEMENT_CATALOGUE_LABEL).setFontText(name);
        });

        tileElements.add(tile);
        iconElements.add(tile.findChildById(EngineSetting.SHIPWRIGHT_ELEMENT_CATALOGUE_ICON));
        tileItems.add(item);
        tilePreviews.add(preview);
        tileOptions.add(entry);
    }

    // Pick \\

    // The tile of the option picked lit, every other tile as it was laid out, the picked one due in view
    private void pick(int option) {

        this.shownOption = option;
        this.scrollPending = true;

        for (int i = 0; i < tileElements.size(); i++)
            tileElements.get(i).setColorOverride(tileOptions.getInt(i) == option ? pickedColor : null);
    }

    // Scrolls the list just far enough to show the picked tile, once the tile has been laid out on screen
    private void scrollToPicked() {

        if (!scrollPending)
            return;

        ElementInstance list = shownMenu.getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE);
        int index = tileOptions.indexOf(shownOption);

        if (list == null || index == EngineSetting.INDEX_NOT_FOUND) {
            this.scrollPending = false;
            return;
        }

        ElementInstance tile = tileElements.get(index);

        if (tile.getComputedH() <= 0f || list.getComputedH() <= 0f)
            return;

        float listLow = list.getComputedTop();
        float listHigh = listLow + list.getComputedH();
        float tileLow = tile.getComputedTop();
        float tileHigh = tileLow + tile.getComputedH();

        if (tileHigh > listHigh)
            list.setScrollY(list.getScrollY() - (tileHigh - listHigh));
        else if (tileLow < listLow)
            list.setScrollY(list.getScrollY() + (listLow - tileLow));

        this.scrollPending = false;
    }

    // Accessible \\

    // The list the tiles scroll in, null while no table is open
    public ElementInstance getListElement() {
        return shownMenu != null ? shownMenu.getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_CATALOGUE) : null;
    }

    public int getTileCount() {
        return tileElements.size();
    }

    public ElementInstance getIconElement(int tileIndex) {
        return iconElements.get(tileIndex);
    }

    // The item a tile pictures, null for one that pictures none
    public ItemDefinitionHandle getTileItem(int tileIndex) {
        return tileItems.get(tileIndex);
    }

    // The built fitting a tile pictures, null for one that pictures none
    public ShipPreviewStruct getTilePreview(int tileIndex) {
        return tilePreviews.get(tileIndex);
    }
}
