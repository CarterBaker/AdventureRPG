package application.bootstrap.itempipeline.container;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ContainerInstance extends InstancePackage {

    /*
     * The inside of a backpack or chest: a box measured in sub-voxels, and the
     * items packed into it by the exact cells their shapes fill, so one item
     * can sit in the hollow of another. Items settle under gravity — a drop
     * enters from the top of the box and falls until it rests on the floor or
     * on something below. place() and remove() are the only paths that change
     * which cells are taken. A container never holds itself, directly or
     * through a container packed inside it. The revision counts every change
     * to the contents, so a view of the container knows when to redraw.
     */

    // Size
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    // Cells — the slot filling each sub-voxel, null where empty
    private ContainerSlotStruct[] cells;

    // Slots
    private ObjectArrayList<ContainerSlotStruct> slots;

    // Revision
    private int revision;

    // Constructor \\

    public void constructor(Vector3Int size) {

        // Size
        this.sizeX = size.x;
        this.sizeY = size.y;
        this.sizeZ = size.z;

        // Cells
        this.cells = new ContainerSlotStruct[sizeX * sizeY * sizeZ];

        // Slots
        this.slots = new ObjectArrayList<>();
    }

    // Placement \\

    public boolean accepts(ItemInstance itemInstance) {
        return !itemInstance.hasContainer()
                || (itemInstance.getContainerInstance() != this
                        && !itemInstance.getContainerInstance().holdsContainer(this));
    }

    public boolean fits(ItemInstance itemInstance, int x, int y, int z, int rotation) {

        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();

        if (x < 0 || y < 0 || z < 0
                || x + shape.getRotatedSizeX(rotation) > sizeX
                || y + shape.getSizeY() > sizeY
                || z + shape.getRotatedSizeZ(rotation) > sizeZ)
            return false;

        for (int i = 0; i < shape.getCellCount(); i++)
            if (cells[toCellIndex(
                    x + shape.getRotatedCellX(i, rotation),
                    y + shape.getCellY(i),
                    z + shape.getRotatedCellZ(i, rotation))] != null)
                return false;

        return true;
    }

    public ContainerSlotStruct place(ItemInstance itemInstance, int x, int y, int z, int rotation) {

        if (!accepts(itemInstance))
            throwException("A container cannot hold itself or a container that holds it.");

        if (!fits(itemInstance, x, y, z, rotation))
            throwException("Item '" + itemInstance.getItemDefinitionHandle().getItemName()
                    + "' does not fit at (" + x + ", " + y + ", " + z + ") in its container.");

        ContainerSlotStruct slot = new ContainerSlotStruct(
                itemInstance, x, y, z, ItemShapeStruct.normalizeRotation(rotation));

        writeCells(slot, slot);
        slots.add(slot);
        revision++;

        return slot;
    }

    public void remove(ContainerSlotStruct slot) {

        if (!slots.remove(slot))
            return;

        writeCells(slot, null);
        revision++;
    }

    // The height a drop at this footprint comes to rest at, or INDEX_NOT_FOUND when it cannot enter from the top
    public int findRestingY(ItemInstance itemInstance, int x, int z, int rotation) {

        int y = sizeY - itemInstance.getItemDefinitionHandle().getShape().getSizeY();

        if (y < 0 || !fits(itemInstance, x, y, z, rotation))
            return EngineSetting.INDEX_NOT_FOUND;

        while (y > 0 && fits(itemInstance, x, y - 1, z, rotation))
            y--;

        return y;
    }

    // Packs an item into the lowest resting place it fits, trying every rotation — null when it fits nowhere
    public ContainerSlotStruct autoPlace(ItemInstance itemInstance) {

        if (!accepts(itemInstance))
            return null;

        ItemShapeStruct shape = itemInstance.getItemDefinitionHandle().getShape();
        int bestY = EngineSetting.INDEX_NOT_FOUND;
        int bestX = 0;
        int bestZ = 0;
        int bestRotation = 0;

        for (int rotation = 0; rotation < EngineSetting.ITEM_ROTATION_COUNT; rotation++)
            for (int z = 0; z + shape.getRotatedSizeZ(rotation) <= sizeZ; z++)
                for (int x = 0; x + shape.getRotatedSizeX(rotation) <= sizeX; x++) {

                    int y = findRestingY(itemInstance, x, z, rotation);

                    if (y == EngineSetting.INDEX_NOT_FOUND)
                        continue;

                    if (bestY != EngineSetting.INDEX_NOT_FOUND && y >= bestY)
                        continue;

                    bestY = y;
                    bestX = x;
                    bestZ = z;
                    bestRotation = rotation;

                    if (bestY == 0)
                        return place(itemInstance, bestX, bestY, bestZ, bestRotation);
                }

        if (bestY == EngineSetting.INDEX_NOT_FOUND)
            return null;

        return place(itemInstance, bestX, bestY, bestZ, bestRotation);
    }

    // Raycast \\

    // Walks the cells a ray crosses in container space and returns the first item it meets
    public ContainerSlotStruct raycast(Vector3 origin, Vector3 direction) {

        float entry = resolveEntryDistance(origin, direction);

        if (entry == Float.MAX_VALUE)
            return null;

        float start = entry + EngineSetting.CONTAINER_RAY_EPSILON;
        int x = clampCell(origin.x + direction.x * start, sizeX);
        int y = clampCell(origin.y + direction.y * start, sizeY);
        int z = clampCell(origin.z + direction.z * start, sizeZ);

        int stepX = direction.x > 0 ? 1 : -1;
        int stepY = direction.y > 0 ? 1 : -1;
        int stepZ = direction.z > 0 ? 1 : -1;

        float nextX = resolveNextBoundary(origin.x, direction.x, x, start);
        float nextY = resolveNextBoundary(origin.y, direction.y, y, start);
        float nextZ = resolveNextBoundary(origin.z, direction.z, z, start);

        float deltaX = direction.x != 0f ? Math.abs(1f / direction.x) : Float.MAX_VALUE;
        float deltaY = direction.y != 0f ? Math.abs(1f / direction.y) : Float.MAX_VALUE;
        float deltaZ = direction.z != 0f ? Math.abs(1f / direction.z) : Float.MAX_VALUE;

        while (isInside(x, y, z)) {

            ContainerSlotStruct slot = cells[toCellIndex(x, y, z)];

            if (slot != null)
                return slot;

            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                y += stepY;
                nextY += deltaY;
            } else {
                z += stepZ;
                nextZ += deltaZ;
            }
        }

        return null;
    }

    // Distance along the ray to where it enters the box, zero from inside, Float.MAX_VALUE on a miss
    private float resolveEntryDistance(Vector3 origin, Vector3 direction) {

        float near = 0f;
        float far = Float.MAX_VALUE;

        float[] origins = { origin.x, origin.y, origin.z };
        float[] directions = { direction.x, direction.y, direction.z };
        int[] sizes = { sizeX, sizeY, sizeZ };

        for (int axis = 0; axis < 3; axis++) {

            if (directions[axis] == 0f) {

                if (origins[axis] < 0f || origins[axis] > sizes[axis])
                    return Float.MAX_VALUE;

                continue;
            }

            float first = (0f - origins[axis]) / directions[axis];
            float second = (sizes[axis] - origins[axis]) / directions[axis];

            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }

        return near <= far ? near : Float.MAX_VALUE;
    }

    private float resolveNextBoundary(float origin, float direction, int cell, float start) {

        if (direction == 0f)
            return Float.MAX_VALUE;

        float boundary = direction > 0f ? cell + 1 : cell;

        return Math.max(start, (boundary - origin) / direction);
    }

    private int clampCell(float position, int size) {
        return Math.max(0, Math.min(size - 1, (int) Math.floor(position)));
    }

    // Contents \\

    public ContainerSlotStruct findSlot(ItemInstance itemInstance) {

        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i).getItemInstance() == itemInstance)
                return slots.get(i);

        return null;
    }

    public boolean holdsContainer(ContainerInstance containerInstance) {

        for (int i = 0; i < slots.size(); i++) {

            ItemInstance itemInstance = slots.get(i).getItemInstance();

            if (!itemInstance.hasContainer())
                continue;

            if (itemInstance.getContainerInstance() == containerInstance
                    || itemInstance.getContainerInstance().holdsContainer(containerInstance))
                return true;
        }

        return false;
    }

    public float getContentWeight() {

        float weight = 0f;

        for (int i = 0; i < slots.size(); i++)
            weight += slots.get(i).getItemInstance().getTotalWeight();

        return weight;
    }

    public void clear() {

        for (int i = 0; i < cells.length; i++)
            cells[i] = null;

        slots.clear();
        revision++;
    }

    // Utility \\

    private void writeCells(ContainerSlotStruct slot, ContainerSlotStruct value) {

        ItemShapeStruct shape = slot.getItemInstance().getItemDefinitionHandle().getShape();

        for (int i = 0; i < shape.getCellCount(); i++)
            cells[toCellIndex(
                    slot.getX() + shape.getRotatedCellX(i, slot.getRotation()),
                    slot.getY() + shape.getCellY(i),
                    slot.getZ() + shape.getRotatedCellZ(i, slot.getRotation()))] = value;
    }

    private boolean isInside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    private int toCellIndex(int x, int y, int z) {
        return x + sizeX * (y + sizeY * z);
    }

    // Accessible \\

    public int getRevision() {
        return revision;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public ObjectArrayList<ContainerSlotStruct> getSlots() {
        return slots;
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public int size() {
        return slots.size();
    }
}
