package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InventoryViewStruct extends StructPackage {

    /*
     * One open container: the container item, the world item it stands as
     * when it was opened where it lies, whether it is shown this frame, its
     * panel menu, and, while its list is toggled on, the menu listing its
     * contents with the rows listed and the item behind each. The container
     * matrix stands the space on its own floor and the view projection is
     * the panel's own camera looking down into it; both are recomputed every
     * frame, and the inverse of their product turns a window point back into
     * a container-space ray. The view turns by its yaw, tilts by its pitch
     * and closes in by its zoom. A container in the world also keeps its
     * item's world matrix, so it can be drawn open where it stands. The
     * listed revision says which state of the container the list and its
     * weight show.
     */

    // Identity
    private final InventoryContainer inventoryContainer;

    // Container
    private ItemInstance containerItem;
    private WorldItemInstance worldItem;
    private boolean shown;
    private int listedRevision;

    // Menus
    private MenuInstance panelMenu;
    private MenuInstance listMenu;

    // List
    private final ObjectArrayList<ItemInstance> rowItems;
    private final ObjectArrayList<ElementInstance> rowElements;

    // View
    private float yaw;
    private float pitch;
    private float zoom;
    private final Matrix4 containerMatrix;
    private final Matrix4 viewProjection;
    private final Matrix4 inversePickMatrix;

    // World
    private boolean worldPlaced;
    private final Matrix4 worldItemMatrix;

    // Constructor \\

    public InventoryViewStruct(InventoryContainer inventoryContainer) {

        // Identity
        this.inventoryContainer = inventoryContainer;

        // List
        this.rowItems = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();

        // View
        this.yaw = RuntimeSetting.INVENTORY_VIEW_DEFAULT_YAW_DEGREES;
        this.pitch = RuntimeSetting.INVENTORY_VIEW_PITCH_DEFAULT_DEGREES;
        this.zoom = RuntimeSetting.INVENTORY_VIEW_ZOOM_MIN;
        this.containerMatrix = new Matrix4();
        this.viewProjection = new Matrix4();
        this.inversePickMatrix = new Matrix4();

        // World
        this.worldItemMatrix = new Matrix4();
    }

    // Management \\

    public void open(ItemInstance containerItem, WorldItemInstance worldItem, MenuInstance panelMenu) {
        this.containerItem = containerItem;
        this.worldItem = worldItem;
        this.panelMenu = panelMenu;
        this.shown = false;
        this.worldPlaced = false;
        invalidateList();
    }

    public void close() {
        this.containerItem = null;
        this.worldItem = null;
        this.panelMenu = null;
        this.listMenu = null;
        this.shown = false;
        this.worldPlaced = false;
        clearRows();
    }

    public void setListMenu(MenuInstance listMenu) {
        this.listMenu = listMenu;
        invalidateList();
    }

    public void invalidateList() {
        this.listedRevision = EngineSetting.INDEX_NOT_FOUND;
        clearRows();
    }

    public void addRow(ItemInstance itemInstance, ElementInstance rowElement) {
        rowItems.add(itemInstance);
        rowElements.add(rowElement);
    }

    public void clearRows() {
        rowItems.clear();
        rowElements.clear();
    }

    public void turn(float yawDegrees, float pitchDegrees) {
        this.yaw += yawDegrees;
        this.pitch = Math.max(
                RuntimeSetting.INVENTORY_VIEW_PITCH_MIN_DEGREES,
                Math.min(RuntimeSetting.INVENTORY_VIEW_PITCH_MAX_DEGREES, pitch + pitchDegrees));
    }

    public void zoom(float factor) {
        this.zoom = Math.max(
                RuntimeSetting.INVENTORY_VIEW_ZOOM_MIN,
                Math.min(RuntimeSetting.INVENTORY_VIEW_ZOOM_MAX, zoom * factor));
    }

    // The window point to container ray transform, rebuilt once both matrices are placed
    public void resolvePickMatrix() {
        inversePickMatrix.set(viewProjection).multiply(containerMatrix).inverse();
    }

    // Accessible \\

    public InventoryContainer getInventoryContainer() {
        return inventoryContainer;
    }

    public boolean isOpen() {
        return containerItem != null;
    }

    public ItemInstance getContainerItem() {
        return containerItem;
    }

    public ContainerInstance getContainerInstance() {
        return containerItem.getContainerInstance();
    }

    public boolean isInWorld() {
        return worldItem != null;
    }

    public WorldItemInstance getWorldItem() {
        return worldItem;
    }

    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    public int getListedRevision() {
        return listedRevision;
    }

    public void setListedRevision(int listedRevision) {
        this.listedRevision = listedRevision;
    }

    public MenuInstance getPanelMenu() {
        return panelMenu;
    }

    public ElementInstance getAreaElement() {
        return panelMenu.getEntryPoint(RuntimeSetting.ENTRY_CONTAINER_AREA);
    }

    public boolean hasList() {
        return listMenu != null;
    }

    public MenuInstance getListMenu() {
        return listMenu;
    }

    public ElementInstance getListElement() {
        return listMenu.getEntryPoint(RuntimeSetting.ENTRY_LIST_ROWS);
    }

    public ObjectArrayList<ItemInstance> getRowItems() {
        return rowItems;
    }

    public ObjectArrayList<ElementInstance> getRowElements() {
        return rowElements;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getZoom() {
        return zoom;
    }

    public Matrix4 getContainerMatrix() {
        return containerMatrix;
    }

    public Matrix4 getViewProjection() {
        return viewProjection;
    }

    public Matrix4 getInversePickMatrix() {
        return inversePickMatrix;
    }

    public boolean isWorldPlaced() {
        return worldPlaced;
    }

    public void setWorldPlaced(boolean worldPlaced) {
        this.worldPlaced = worldPlaced;
    }

    public Matrix4 getWorldItemMatrix() {
        return worldItemMatrix;
    }
}
