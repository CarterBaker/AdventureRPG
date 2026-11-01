package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ShipPickStruct extends StructPackage {

    /*
     * Whatever stands at one octant of a deck at the drafting table, found
     * over its whole footprint: the tool and the option of that tool that set
     * it down, the packed cell it was set at, its index in the blueprint list
     * that keeps it where it has one, and the octants it covers on the sheet
     * from its first to past its last. A pick of nothing has no tool. Refilled
     * in place each time it is asked for.
     */

    // Kind — the tool that sets it down and the option of that tool it is
    private ShipwrightTool tool;
    private int option;

    // Placement
    private int cell;
    private int index;

    // Footprint — sheet octants, the maximum exclusive
    private int minX;
    private int minZ;
    private int maxX;
    private int maxZ;

    // Constructor \\

    public ShipPickStruct() {
        clear();
    }

    // Management \\

    public void clear() {
        this.tool = null;
        this.option = EngineSetting.INDEX_NOT_FOUND;
        this.cell = EngineSetting.SHIP_CELL_NONE;
        this.index = EngineSetting.INDEX_NOT_FOUND;
    }

    public void set(ShipwrightTool tool, int option, int cell, int index) {
        this.tool = tool;
        this.option = option;
        this.cell = cell;
        this.index = index;
    }

    public void setFootprint(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    // Takes on everything another pick holds
    public void set(ShipPickStruct other) {
        set(other.tool, other.option, other.cell, other.index);
        setFootprint(other.minX, other.minZ, other.maxX, other.maxZ);
    }

    // Accessible \\

    public boolean isEmpty() {
        return tool == null;
    }

    // True when the footprint covers an octant of the sheet
    public boolean covers(int x, int z) {
        return x >= minX && x < maxX && z >= minZ && z < maxZ;
    }

    // True when the footprint shares an octant with a rectangle of the sheet
    public boolean overlaps(int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ) {
        return minX < otherMaxX && otherMinX < maxX && minZ < otherMaxZ && otherMinZ < maxZ;
    }

    public ShipwrightTool getTool() {
        return tool;
    }

    public int getOption() {
        return option;
    }

    public int getCell() {
        return cell;
    }

    public int getIndex() {
        return index;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }
}
