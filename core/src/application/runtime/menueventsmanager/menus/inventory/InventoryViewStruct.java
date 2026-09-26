package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.runtime.RuntimeSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryViewStruct extends StructPackage {

    /*
     * One container shown in the inventory: the menu holding its item list
     * and 3D view, the container item on show, the rows listed and the item
     * behind each, and the view's turn. The view matrix carries container
     * sub-voxel space onto the window's pixels and is recomputed every frame
     * from the view element; its inverse turns the cursor back into a ray.
     * The listed revision says which state of the container the list shows.
     */

    // Identity
    private final InventoryContainer inventoryContainer;

    // Menu
    private MenuInstance menu;
    private String menuName;

    // Container
    private ItemInstance containerItem;
    private int listedRevision;

    // Rows
    private final ObjectArrayList<ItemInstance> rowItems;
    private final ObjectArrayList<ElementInstance> rowElements;

    // View
    private float yaw;
    private final Matrix4 viewMatrix;
    private final Matrix4 inverseViewMatrix;

    // Constructor \\

    public InventoryViewStruct(InventoryContainer inventoryContainer) {

        // Identity
        this.inventoryContainer = inventoryContainer;

        // Rows
        this.rowItems = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();

        // View
        this.yaw = RuntimeSetting.INVENTORY_VIEW_DEFAULT_YAW_DEGREES;
        this.viewMatrix = new Matrix4();
        this.inverseViewMatrix = new Matrix4();
    }

    // Management \\

    public void open(MenuInstance menu, String menuName, ItemInstance containerItem) {
        this.menu = menu;
        this.menuName = menuName;
        this.containerItem = containerItem;
        this.listedRevision = -1;
        this.rowItems.clear();
        this.rowElements.clear();
    }

    public void close() {
        this.menu = null;
        this.menuName = null;
        this.containerItem = null;
        this.rowItems.clear();
        this.rowElements.clear();
    }

    public void addRow(ItemInstance itemInstance, ElementInstance rowElement) {
        rowItems.add(itemInstance);
        rowElements.add(rowElement);
    }

    public void clearRows() {
        rowItems.clear();
        rowElements.clear();
    }

    public void turn(float degrees) {
        this.yaw += degrees;
    }

    // Accessible \\

    public InventoryContainer getInventoryContainer() {
        return inventoryContainer;
    }

    public boolean isOpen() {
        return menu != null;
    }

    public MenuInstance getMenu() {
        return menu;
    }

    public String getMenuName() {
        return menuName;
    }

    public ItemInstance getContainerItem() {
        return containerItem;
    }

    public ContainerInstance getContainerInstance() {
        return containerItem.getContainerInstance();
    }

    public int getListedRevision() {
        return listedRevision;
    }

    public void setListedRevision(int listedRevision) {
        this.listedRevision = listedRevision;
    }

    public ObjectArrayList<ItemInstance> getRowItems() {
        return rowItems;
    }

    public ObjectArrayList<ElementInstance> getRowElements() {
        return rowElements;
    }

    public ElementInstance getViewElement() {
        return menu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_VIEW);
    }

    public ElementInstance getListElement() {
        return menu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_LIST);
    }

    public float getYaw() {
        return yaw;
    }

    public Matrix4 getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4 getInverseViewMatrix() {
        return inverseViewMatrix;
    }
}
