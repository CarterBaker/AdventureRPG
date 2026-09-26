package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.container.ContainerSlotStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.inventory.InventoryViewUtility;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryContainerBranch extends BranchPackage {

    /*
     * Shows the worn backpack and the opened chest beside the equipment panel.
     * Each takes the full height alone; together the chest's panels sit
     * directly above the backpack's. A container's panels are its contents
     * listed under a header for each category, and a 3D view of the
     * container with every item standing where it rests. Panels open, move,
     * and close with what is worn, and a list is rebuilt only when its
     * container changes. Pressing on an item in a view or a list picks it up;
     * pressing on empty space in a view turns the view instead. The view
     * matrices are recomputed every frame from their elements, so the cursor
     * always picks what is drawn.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private InventoryBranch inventoryBranch;
    private InventoryDragBranch inventoryDragBranch;

    // Scratch
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private ObjectArrayList<ContainerSlotStruct> categorySlots;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.categorySlots = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.inventoryDragBranch = get(InventoryDragBranch.class);
    }

    // Update \\

    void update(InventorySessionStruct session) {

        ItemInstance backpackItem = session.getInventory().getItem(EquipmentSlot.BACKPACK);
        ItemInstance chestItem = session.getChestItem();
        boolean stacked = backpackItem != null && chestItem != null;

        syncView(session, session.getView(InventoryContainer.CHEST), chestItem,
                stacked ? RuntimeSetting.MENU_INVENTORY_CONTAINER_UPPER : RuntimeSetting.MENU_INVENTORY_CONTAINER);
        syncView(session, session.getView(InventoryContainer.BACKPACK), backpackItem,
                stacked ? RuntimeSetting.MENU_INVENTORY_CONTAINER_LOWER : RuntimeSetting.MENU_INVENTORY_CONTAINER);

        for (InventoryViewStruct view : session.getViews()) {

            if (!view.isOpen())
                continue;

            refreshList(view);
            updateViewMatrix(view);
        }
    }

    // Views \\

    private void syncView(
            InventorySessionStruct session,
            InventoryViewStruct view,
            ItemInstance containerItem,
            String menuName) {

        if (containerItem == null) {
            closeView(view);
            return;
        }

        if (view.isOpen() && view.getContainerItem() == containerItem && view.getMenuName().equals(menuName))
            return;

        closeView(view);

        MenuInstance menu = menuManager.openMenu(menuName, session.getWindow());
        view.open(menu, menuName, containerItem);

        menu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_TITLE)
                .setFontText(containerItem.getItemDefinitionHandle().getDisplayName());
        menu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_HINT).setFontText(String.format(
                RuntimeSetting.INVENTORY_FORMAT_VIEW_HINT, InputNameUtility.getName(KeyBindings.ROTATE_ITEM)));
        view.getViewElement().setOnDragArgOverride(view.getInventoryContainer().name());
    }

    private void closeView(InventoryViewStruct view) {

        if (!view.isOpen())
            return;

        menuManager.closeMenu(view.getMenu());
        view.close();
    }

    void closeAll(InventorySessionStruct session) {

        for (InventoryViewStruct view : session.getViews())
            closeView(view);
    }

    private void updateViewMatrix(InventoryViewStruct view) {

        ElementInstance viewElement = view.getViewElement();

        if (viewElement.getComputedW() <= 0f || viewElement.getComputedH() <= 0f)
            return;

        InventoryViewUtility.composeViewMatrix(
                viewElement, view.getContainerInstance(), view.getYaw(), view.getViewMatrix());
        view.getInverseViewMatrix().set(view.getViewMatrix()).inverse();
    }

    // List \\

    private void refreshList(InventoryViewStruct view) {

        ContainerInstance containerInstance = view.getContainerInstance();

        if (containerInstance.getRevision() == view.getListedRevision())
            return;

        MenuInstance menu = view.getMenu();

        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_CONTAINER_LIST);
        view.clearRows();
        view.setListedRevision(containerInstance.getRevision());

        menu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_WEIGHT).setFontText(
                String.format(RuntimeSetting.INVENTORY_FORMAT_HOLDING, containerInstance.getContentWeight()));

        if (containerInstance.isEmpty()) {
            menuManager.inject(menu, RuntimeSetting.ENTRY_CONTAINER_LIST, RuntimeSetting.MENU_INVENTORY_LIST_EMPTY);
            return;
        }

        for (ItemCategory itemCategory : ItemCategory.values())
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
                view.getMenu(), RuntimeSetting.ENTRY_CONTAINER_LIST, RuntimeSetting.MENU_INVENTORY_LIST_HEADER,
                header -> header.setFontText(itemCategory.getTitle()));

        for (int i = 0; i < categorySlots.size(); i++)
            injectRow(view, categorySlots.get(i).getItemInstance());
    }

    private void injectRow(InventoryViewStruct view, ItemInstance itemInstance) {

        String argument = view.getInventoryContainer().name()
                + RuntimeSetting.INVENTORY_ARGUMENT_SEPARATOR
                + view.getRowItems().size();

        ElementInstance row = menuManager.inject(
                view.getMenu(), RuntimeSetting.ENTRY_CONTAINER_LIST, RuntimeSetting.MENU_INVENTORY_LIST_ROW,
                element -> {
                    element.setOnDragArgOverride(argument);
                    element.findChildById(RuntimeSetting.ELEMENT_INVENTORY_ROW_NAME)
                            .setFontText(itemInstance.getItemDefinitionHandle().getDisplayName());
                    element.findChildById(RuntimeSetting.ELEMENT_INVENTORY_ROW_WEIGHT).setFontText(
                            String.format(RuntimeSetting.INVENTORY_FORMAT_WEIGHT, itemInstance.getTotalWeight()));
                });

        view.addRow(itemInstance, row);
    }

    // Drag \\

    public void dragView(String inventoryContainerName, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null)
            return;

        InventoryViewStruct view = session.getView(InventoryContainer.valueOf(inventoryContainerName));

        if (!view.isOpen())
            return;

        switch (session.getDragMode()) {

            case NONE -> {

                float x = inputManager.getHoverMouseX(window);
                float y = inputManager.getHoverMouseY(window);
                ContainerSlotStruct slot = pickSlot(view, x, y);

                if (slot != null)
                    inventoryDragBranch.pickUpFromView(session, view, slot, x, y);
                else
                    session.setDragMode(InventoryDragMode.TURN_VIEW);
            }

            case TURN_VIEW -> view.turn(
                    inputManager.getRawInput(window).getDeltaX()
                            * RuntimeSetting.INVENTORY_VIEW_TURN_DEGREES_PER_PIXEL);

            default -> {
            }
        }
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

    // The item drawn under a window point in this view, null over empty space
    ContainerSlotStruct pickSlot(InventoryViewStruct view, float x, float y) {

        if (!InventoryViewUtility.isInside(view.getViewElement(), x, y))
            return null;

        InventoryViewUtility.castRay(view.getInverseViewMatrix(), x, y, rayOrigin, rayDirection);

        return view.getContainerInstance().raycast(rayOrigin, rayDirection);
    }

    // Where a window point meets this view's floor, in sub-voxels — false when it misses
    boolean pickFloor(InventoryViewStruct view, float x, float y, Vector3 out) {
        InventoryViewUtility.castRay(view.getInverseViewMatrix(), x, y, rayOrigin, rayDirection);
        return InventoryViewUtility.intersectFloor(rayOrigin, rayDirection, out);
    }
}
