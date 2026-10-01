package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.container.ContainerSlotStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import engine.input.Input;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryContainerBranch extends BranchPackage {

    /*
     * Opens containers, each in its own panel: the worn bag on the left and,
     * on the right, a chest or bag opened where it lies. A panel shows its
     * container from above through its own camera, so the whole inside is in
     * reach — tilted and turned by dragging beside it, brought closer with
     * the wheel — and a list of its contents, grouped by category, toggles
     * over it. A container opened in the world is drawn open where it stands:
     * its lid is left off, and only a space inside its own model is ever
     * shown there, never a pocket. Every frame each open view is framed anew,
     * and lists rebuild only when their container changes. Pressing an item
     * in any panel picks it up.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private InventoryBranch inventoryBranch;
    private InventoryDragBranch inventoryDragBranch;

    // State
    private boolean[] container2ListShown;

    // Scratch
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private ObjectArrayList<ContainerSlotStruct> categorySlots;

    // Base \\

    @Override
    protected void create() {

        // State
        this.container2ListShown = new boolean[InventoryContainer.VALUES.length];

        // Scratch
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.categorySlots = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.inventoryDragBranch = get(InventoryDragBranch.class);
    }

    // Update \\

    void update(InventorySessionStruct session) {

        for (InventoryViewStruct view : session.getViews()) {

            syncView(session, view);

            if (!view.isOpen())
                continue;

            syncList(session, view);
            placeView(session, view);
            refreshView(view);
            zoomView(session, view);
        }
    }

    // Views \\

    // Each view follows the container it should show: the worn bag, or the container opened in the world
    private void syncView(InventorySessionStruct session, InventoryViewStruct view) {

        ItemInstance containerItem = resolveContainerItem(session, view.getInventoryContainer());

        if (view.getContainerItem() == containerItem)
            return;

        closeView(view);

        if (containerItem != null)
            openView(session, view, containerItem);
    }

    private ItemInstance resolveContainerItem(InventorySessionStruct session, InventoryContainer inventoryContainer) {

        if (inventoryContainer == InventoryContainer.BACKPACK)
            return session.getInventory().getItem(EquipmentSlot.BACKPACK);

        return session.hasChest()
                ? worldItemPlacementSystem.resolveItemInstance(session.getChestWorldItem())
                : null;
    }

    private void openView(InventorySessionStruct session, InventoryViewStruct view, ItemInstance containerItem) {

        InventoryContainer inventoryContainer = view.getInventoryContainer();
        WorldItemInstance worldItem = inventoryContainer == InventoryContainer.CHEST
                ? session.getChestWorldItem()
                : null;
        MenuInstance panelMenu = menuManager.openMenu(inventoryContainer.getPanelMenuName(), session.getWindow());

        view.open(containerItem, worldItem, panelMenu);

        panelMenu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_TITLE)
                .setFontText(containerItem.getItemDefinitionHandle().getDisplayName());
        panelMenu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_TOGGLE_LABEL)
                .setFontText(RuntimeSetting.INVENTORY_TEXT_SHOW_LIST);
        panelMenu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_HINT).setFontText(resolveHint(inventoryContainer));

        if (worldItem != null)
            worldItemPlacementSystem.setItemOpen(worldItem, true);
    }

    private String resolveHint(InventoryContainer inventoryContainer) {

        if (inventoryContainer == InventoryContainer.BACKPACK)
            return String.format(
                    RuntimeSetting.INVENTORY_FORMAT_VIEW_HINT, InputNameUtility.getName(KeyBindings.ROTATE_ITEM));

        return String.format(
                RuntimeSetting.INVENTORY_FORMAT_CHEST_HINT,
                InputNameUtility.getName(KeyBindings.ACTIVATE),
                InputNameUtility.getName(KeyBindings.INVENTORY),
                InputNameUtility.getName(KeyBindings.PAUSE));
    }

    private void closeView(InventoryViewStruct view) {

        if (!view.isOpen())
            return;

        closeList(view);
        menuManager.closeMenu(view.getPanelMenu());

        if (view.isInWorld())
            worldItemPlacementSystem.setItemOpen(view.getWorldItem(), false);

        view.close();
    }

    void closeAll(InventorySessionStruct session) {

        for (InventoryViewStruct view : session.getViews())
            closeView(view);
    }

    // Placement \\

    // The space stands on its own floor under its panel's camera; a container in the world is placed there too
    private void placeView(InventorySessionStruct session, InventoryViewStruct view) {

        WindowInstance window = session.getWindow();
        ElementInstance area = view.getAreaElement();

        if (view.isInWorld())
            view.setWorldPlaced(placeInWorld(session, view));

        view.setShown(!view.hasList() && area.getComputedW() > 0f && area.getComputedH() > 0f);

        if (!view.isShown())
            return;

        InventoryViewUtility.composeContainerMatrix(
                view.getYaw(), view.getContainerInstance(), view.getContainerMatrix());
        InventoryViewUtility.composePanelViewProjection(
                area,
                view.getContainerInstance(),
                view.getPitch(),
                view.getZoom(),
                window.getWidth(),
                window.getHeight(),
                view.getViewProjection());
        view.resolvePickMatrix();
    }

    private boolean placeInWorld(InventorySessionStruct session, InventoryViewStruct view) {

        EntityInstance player = session.getPlayer();

        return worldItemPlacementSystem.composeTransform(
                view.getWorldItem(),
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                view.getWorldItemMatrix());
    }

    // The wheel over a panel brings its container closer
    private void zoomView(InventorySessionStruct session, InventoryViewStruct view) {

        WindowInstance window = session.getWindow();
        float wheel = inputManager.getRawInput(window).getScrollY();

        if (wheel == 0f || !view.isShown() || !InventoryViewUtility.isInside(
                view.getAreaElement(),
                inputManager.getHoverMouseX(window),
                inputManager.getHoverMouseY(window)))
            return;

        view.zoom((float) Math.pow(RuntimeSetting.INVENTORY_VIEW_ZOOM_STEP, wheel));
    }

    // List \\

    private void syncList(InventorySessionStruct session, InventoryViewStruct view) {

        boolean listShown = container2ListShown[view.getInventoryContainer().ordinal()];

        if (listShown && !view.hasList())
            view.setListMenu(openList(session, view));

        if (!listShown)
            closeList(view);
    }

    private MenuInstance openList(InventorySessionStruct session, InventoryViewStruct view) {

        MenuInstance menu = menuManager.openMenu(
                view.getInventoryContainer().getListMenuName(), session.getWindow());

        menu.getEntryPoint(RuntimeSetting.ENTRY_LIST_TITLE)
                .setFontText(view.getContainerItem().getItemDefinitionHandle().getDisplayName());
        menu.getEntryPoint(RuntimeSetting.ENTRY_LIST_TOGGLE_LABEL).setFontText(RuntimeSetting.INVENTORY_TEXT_HIDE_LIST);

        return menu;
    }

    private void closeList(InventoryViewStruct view) {

        if (!view.hasList())
            return;

        menuManager.closeMenu(view.getListMenu());
        view.setListMenu(null);
    }

    private void refreshView(InventoryViewStruct view) {

        ContainerInstance containerInstance = view.getContainerInstance();

        if (containerInstance.getRevision() == view.getListedRevision())
            return;

        String weight = String.format(RuntimeSetting.INVENTORY_FORMAT_HOLDING, containerInstance.getContentWeight());

        view.setListedRevision(containerInstance.getRevision());
        view.getPanelMenu().getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_WEIGHT).setFontText(weight);

        if (!view.hasList())
            return;

        MenuInstance menu = view.getListMenu();

        menu.getEntryPoint(RuntimeSetting.ENTRY_LIST_WEIGHT).setFontText(weight);
        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_LIST_ROWS);
        view.clearRows();

        if (containerInstance.isEmpty()) {
            menuManager.inject(menu, RuntimeSetting.ENTRY_LIST_ROWS, RuntimeSetting.MENU_INVENTORY_LIST_EMPTY);
            return;
        }

        for (ItemCategory itemCategory : ItemCategory.VALUES)
            injectCategory(view, itemCategory);
    }

    private void injectCategory(InventoryViewStruct view, ItemCategory itemCategory) {

        ObjectArrayList<ContainerSlotStruct> slots = view.getContainerInstance().getSlots();

        categorySlots.clear();

        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i).getItemInstance().getItemDefinitionHandle().getCategory() == itemCategory)
                categorySlots.add(slots.get(i));

        if (categorySlots.isEmpty())
            return;

        categorySlots.sort((first, second) -> first.getItemInstance().getItemDefinitionHandle().getDisplayName()
                .compareTo(second.getItemInstance().getItemDefinitionHandle().getDisplayName()));

        menuManager.inject(
                view.getListMenu(), RuntimeSetting.ENTRY_LIST_ROWS, RuntimeSetting.MENU_INVENTORY_LIST_HEADER,
                header -> header.setFontText(itemCategory.getTitle()));

        for (int i = 0; i < categorySlots.size(); i++)
            injectRow(view, categorySlots.get(i).getItemInstance());
    }

    private void injectRow(InventoryViewStruct view, ItemInstance itemInstance) {

        ItemDefinitionHandle item = itemInstance.getItemDefinitionHandle();
        String argument = view.getInventoryContainer().name()
                + RuntimeSetting.INVENTORY_ARGUMENT_SEPARATOR
                + view.getRowItems().size();

        ElementInstance row = menuManager.inject(
                view.getListMenu(), RuntimeSetting.ENTRY_LIST_ROWS, RuntimeSetting.MENU_INVENTORY_LIST_ROW,
                element -> {
                    element.setOnDragArgOverride(argument);
                    element.findChildById(RuntimeSetting.ELEMENT_INVENTORY_ROW_NAME)
                            .setFontText(item.getDisplayName());
                    element.findChildById(RuntimeSetting.ELEMENT_INVENTORY_ROW_WEIGHT).setFontText(
                            String.format(RuntimeSetting.INVENTORY_FORMAT_WEIGHT, itemInstance.getTotalWeight()));
                });

        view.addRow(itemInstance, row);
    }

    // Toggle \\

    public void toggleList(String inventoryContainerName, WindowInstance window) {

        if (inventoryBranch.getSession(window) == null)
            return;

        int index = InventoryContainer.valueOf(inventoryContainerName).ordinal();

        container2ListShown[index] = !container2ListShown[index];
    }

    // Drag \\

    // Pressing an item in any panel picks it up; pressing beside this panel's container turns it
    public void dragView(String inventoryContainerName, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null)
            return;

        InventoryViewStruct view = session.getView(InventoryContainer.valueOf(inventoryContainerName));

        switch (session.getDragMode()) {

            case NONE -> {

                if (pickUpAt(session, window))
                    return;

                session.setDragMode(view.isShown() ? InventoryDragMode.TURN_VIEW : InventoryDragMode.SPENT);
            }

            case TURN_VIEW -> {

                Input rawInput = inputManager.getRawInput(window);

                view.turn(
                        rawInput.getDeltaX() * RuntimeSetting.INVENTORY_VIEW_TURN_DEGREES_PER_PIXEL,
                        rawInput.getDeltaY() * RuntimeSetting.INVENTORY_VIEW_TURN_DEGREES_PER_PIXEL);
            }

            default -> {
            }
        }
    }

    // Pressing outside every panel still picks up an item a zoomed container shows there
    public void dragScene(WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null || session.getDragMode() != InventoryDragMode.NONE)
            return;

        if (!pickUpAt(session, window))
            session.setDragMode(InventoryDragMode.SPENT);
    }

    private boolean pickUpAt(InventorySessionStruct session, WindowInstance window) {

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        for (InventoryViewStruct view : session.getViews()) {

            ContainerSlotStruct slot = pickSlot(session, view, x, y);

            if (slot == null)
                continue;

            inventoryDragBranch.pickUpFromView(session, view, slot, x, y);
            return true;
        }

        return false;
    }

    public void dragRow(String argument, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null || session.getDragMode() != InventoryDragMode.NONE)
            return;

        String[] parts = argument.split(RuntimeSetting.INVENTORY_ARGUMENT_SEPARATOR);
        InventoryViewStruct view = session.getView(InventoryContainer.valueOf(parts[0]));
        int rowIndex = Integer.parseInt(parts[1]);

        if (!view.isOpen() || rowIndex >= view.getRowItems().size()) {
            session.setDragMode(InventoryDragMode.SPENT);
            return;
        }

        ContainerSlotStruct slot = view.getContainerInstance().findSlot(view.getRowItems().get(rowIndex));

        if (slot == null) {
            session.setDragMode(InventoryDragMode.SPENT);
            return;
        }

        inventoryDragBranch.pickUpFromList(session, view, slot);
    }

    // Picking \\

    // True when the cursor's ray passes through this shown view's space
    boolean pointsInto(InventorySessionStruct session, InventoryViewStruct view, float x, float y) {

        if (!view.isShown())
            return false;

        castRay(session, view, x, y);

        return view.getContainerInstance().intersects(rayOrigin, rayDirection);
    }

    // The item the cursor points at in this view, null over empty space
    ContainerSlotStruct pickSlot(InventorySessionStruct session, InventoryViewStruct view, float x, float y) {

        if (!pointsInto(session, view, x, y))
            return null;

        return view.getContainerInstance().raycast(rayOrigin, rayDirection);
    }

    // Where the cursor aims in this view, in sub-voxels: the first item surface its ray touches, else the floor
    boolean pickPoint(InventorySessionStruct session, InventoryViewStruct view, float x, float y, Vector3 out) {

        castRay(session, view, x, y);

        return view.getContainerInstance().raycastPoint(rayOrigin, rayDirection, out)
                || InventoryViewUtility.intersectFloor(rayOrigin, rayDirection, out);
    }

    private void castRay(InventorySessionStruct session, InventoryViewStruct view, float x, float y) {

        WindowInstance window = session.getWindow();

        InventoryViewUtility.castRay(
                view.getInversePickMatrix(),
                window.getWidth(),
                window.getHeight(),
                x,
                y,
                rayOrigin,
                rayDirection);
    }
}
