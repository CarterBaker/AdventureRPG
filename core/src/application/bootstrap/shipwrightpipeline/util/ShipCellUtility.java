package application.bootstrap.shipwrightpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ShipCellUtility extends EngineUtility {

    /*
     * The one packing of a drafting-table cell into an int, shared by every
     * placement a ship blueprint keeps: the level it stands on, its octant
     * along the length and across the full breadth, and its quarter turn.
     * Packed cells sort by level, then length, then breadth, so a sorted list
     * of them writes the same bytes every time it is saved. Also the one
     * mapping between the breadth across the whole ship and the half-breadth
     * a mirrored hull is drawn on.
     */

    // Pack \\

    public static int pack(int level, int x, int z, int spin) {
        return (level << EngineSetting.SHIP_CELL_LEVEL_SHIFT)
                | (x << EngineSetting.SHIP_CELL_X_SHIFT)
                | (z << EngineSetting.SHIP_CELL_Z_SHIFT)
                | (spin & EngineSetting.SHIP_CELL_SPIN_MASK);
    }

    public static int unpackLevel(int cell) {
        return (cell >>> EngineSetting.SHIP_CELL_LEVEL_SHIFT) & EngineSetting.SHIP_CELL_LEVEL_MASK;
    }

    public static int unpackX(int cell) {
        return (cell >>> EngineSetting.SHIP_CELL_X_SHIFT) & EngineSetting.SHIP_CELL_X_MASK;
    }

    public static int unpackZ(int cell) {
        return (cell >>> EngineSetting.SHIP_CELL_Z_SHIFT) & EngineSetting.SHIP_CELL_Z_MASK;
    }

    public static int unpackSpin(int cell) {
        return cell & EngineSetting.SHIP_CELL_SPIN_MASK;
    }

    // The same cell with its quarter turn cleared, so a place is found whichever way it faces
    public static int toPlace(int cell) {
        return cell & ~EngineSetting.SHIP_CELL_SPIN_MASK;
    }

    // Mirror \\

    // The octant out from the centre line a cell across the full breadth lies on, either side alike
    public static int toHalfBreadth(int z, int halfBreadth) {
        return z >= halfBreadth ? z - halfBreadth : halfBreadth - 1 - z;
    }

    public static int toStarboard(int halfZ, int halfBreadth) {
        return halfBreadth + halfZ;
    }

    public static int toPort(int halfZ, int halfBreadth) {
        return halfBreadth - 1 - halfZ;
    }

    // Spin \\

    // One step along the length a quarter turn faces: forward, across to starboard, aft, across to port
    public static int stepX(int spin) {
        return spin == 0 ? 1 : spin == 2 ? -1 : 0;
    }

    public static int stepZ(int spin) {
        return spin == 1 ? 1 : spin == 3 ? -1 : 0;
    }
}
