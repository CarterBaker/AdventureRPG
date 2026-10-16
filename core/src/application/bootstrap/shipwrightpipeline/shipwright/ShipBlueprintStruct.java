package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipBlueprintStruct extends StructPackage {

    /*
     * A ship as it is drawn at the drafting table, everything a ship file
     * keeps. The hull is drawn in octants, the half-block partial blocks the
     * engine builds with: one layer at a time from the keel up, each layer the
     * hull's half-breadth out from the centre line along the length, mirrored
     * to the other side, one bit per octant. The design waterline is a layer;
     * interior decks are the layers a floor is laid at. Walls, doors, stairs,
     * furniture, the helm and the capstan stand on a level, a layer some floor
     * lies on, at an octant along the length and across the full breadth,
     * each packed through ShipCellUtility; walls are kept sorted and every
     * place holds at most one of each. Masts stand on the centre line, kept
     * from the stern forward. Rails run round every open deck edge at their
     * height, and a porch may hang off the stern at one level. Copied whole
     * for undo; every list writes in a fixed order, so saving an unchanged
     * blueprint writes the same bytes.
     */

    // Identity
    private String shipName;
    private String displayName;

    // Size — octants along the length, out from the centre line, and up from the keel
    private int length;
    private int halfBreadth;
    private int height;

    // Hull — one bit per octant, x fastest, then half-breadth, then layer
    private long[] hull;
    private int waterline;

    // Decks — interior floor layers, sorted
    private final IntArrayList decks;

    // Options
    private int railHeight;
    private int porchLevel;

    // Placements — packed cells
    private final IntArrayList walls;
    private final IntArrayList doors;
    private final IntArrayList stairs;
    private int helm;
    private int capstan;

    // Furniture
    private final ObjectArrayList<ShipFurnitureStruct> furniture;

    // Masts — from the stern forward
    private final ObjectArrayList<ShipMastStruct> masts;

    // Constructor \\

    public ShipBlueprintStruct(int length, int halfBreadth, int height) {

        // Identity
        this.shipName = EngineSetting.SHIP_NAME_NONE;
        this.displayName = EngineSetting.SHIP_NAME_NONE;

        // Size
        this.length = length;
        this.halfBreadth = halfBreadth;
        this.height = height;

        // Hull
        this.hull = new long[resolveWordCount(length, halfBreadth, height)];
        this.waterline = 0;

        // Decks
        this.decks = new IntArrayList();

        // Options
        this.railHeight = 0;
        this.porchLevel = EngineSetting.SHIP_LEVEL_NONE;

        // Placements
        this.walls = new IntArrayList();
        this.doors = new IntArrayList();
        this.stairs = new IntArrayList();
        this.helm = EngineSetting.SHIP_CELL_NONE;
        this.capstan = EngineSetting.SHIP_CELL_NONE;

        // Furniture
        this.furniture = new ObjectArrayList<>();

        // Masts
        this.masts = new ObjectArrayList<>();
    }

    public ShipBlueprintStruct copy() {

        ShipBlueprintStruct copy = new ShipBlueprintStruct(length, halfBreadth, height);
        copy.set(this);

        return copy;
    }

    // Takes on everything another blueprint holds
    public void set(ShipBlueprintStruct other) {

        // Identity
        this.shipName = other.shipName;
        this.displayName = other.displayName;

        // Size
        this.length = other.length;
        this.halfBreadth = other.halfBreadth;
        this.height = other.height;

        // Hull
        this.hull = other.hull.clone();
        this.waterline = other.waterline;

        // Decks
        decks.clear();
        decks.addAll(other.decks);

        // Options
        this.railHeight = other.railHeight;
        this.porchLevel = other.porchLevel;

        // Placements
        walls.clear();
        walls.addAll(other.walls);
        doors.clear();
        doors.addAll(other.doors);
        stairs.clear();
        stairs.addAll(other.stairs);
        this.helm = other.helm;
        this.capstan = other.capstan;

        // Furniture
        furniture.clear();
        furniture.addAll(other.furniture);

        // Masts
        masts.clear();

        for (int i = 0; i < other.masts.size(); i++)
            masts.add(other.masts.get(i).copy());
    }

    private static int resolveWordCount(int length, int halfBreadth, int height) {
        return (length * halfBreadth * height + Long.SIZE - 1) / Long.SIZE;
    }

    // Hull \\

    public boolean isHull(int x, int y, int halfZ) {

        if (x < 0 || y < 0 || halfZ < 0 || x >= length || y >= height || halfZ >= halfBreadth)
            return false;

        int bit = toBitIndex(x, y, halfZ);

        return (hull[bit >>> 6] & (1L << bit)) != 0;
    }

    // True when an octant across the full breadth lies inside the hull's drawn volume
    public boolean isVolume(int x, int y, int z) {
        return z >= 0 && z < halfBreadth * 2 && isHull(x, y, ShipCellUtility.toHalfBreadth(z, halfBreadth));
    }

    public void setHull(int x, int y, int halfZ, boolean filled) {

        if (x < 0 || y < 0 || halfZ < 0 || x >= length || y >= height || halfZ >= halfBreadth)
            return;

        int bit = toBitIndex(x, y, halfZ);

        if (filled)
            hull[bit >>> 6] |= 1L << bit;
        else
            hull[bit >>> 6] &= ~(1L << bit);
    }

    public void clearLayer(int y) {

        for (int halfZ = 0; halfZ < halfBreadth; halfZ++)
            for (int x = 0; x < length; x++)
                setHull(x, y, halfZ, false);
    }

    public void copyLayer(int fromY, int toY) {

        for (int halfZ = 0; halfZ < halfBreadth; halfZ++)
            for (int x = 0; x < length; x++)
                setHull(x, toY, halfZ, isHull(x, fromY, halfZ));
    }

    public boolean hasHull() {

        for (int i = 0; i < hull.length; i++)
            if (hull[i] != 0L)
                return true;

        return false;
    }

    private int toBitIndex(int x, int y, int halfZ) {
        return (y * halfBreadth + halfZ) * length + x;
    }

    // Size \\

    // Redraws the blueprint on a grid of another size, keeping everything that still fits on it
    public void resize(int newLength, int newHalfBreadth, int newHeight) {

        long[] resized = new long[resolveWordCount(newLength, newHalfBreadth, newHeight)];

        for (int y = 0; y < Math.min(height, newHeight); y++)
            for (int halfZ = 0; halfZ < Math.min(halfBreadth, newHalfBreadth); halfZ++)
                for (int x = 0; x < Math.min(length, newLength); x++) {

                    if (!isHull(x, y, halfZ))
                        continue;

                    int bit = (y * newHalfBreadth + halfZ) * newLength + x;
                    resized[bit >>> 6] |= 1L << bit;
                }

        int oldHalfBreadth = halfBreadth;

        this.hull = resized;
        this.length = newLength;
        this.halfBreadth = newHalfBreadth;
        this.height = newHeight;
        this.waterline = Math.min(waterline, newHeight - 1);

        for (int i = decks.size() - 1; i >= 0; i--)
            if (decks.getInt(i) >= newHeight)
                decks.removeInt(i);

        if (porchLevel >= newHeight)
            this.porchLevel = EngineSetting.SHIP_LEVEL_NONE;

        shiftCells(walls, oldHalfBreadth);
        shiftCells(doors, oldHalfBreadth);
        shiftCells(stairs, oldHalfBreadth);
        this.helm = shiftCell(helm, oldHalfBreadth);
        this.capstan = shiftCell(capstan, oldHalfBreadth);

        for (int i = furniture.size() - 1; i >= 0; i--) {

            ShipFurnitureStruct piece = furniture.get(i);
            int cell = shiftCell(piece.getCell(), oldHalfBreadth);

            if (cell == EngineSetting.SHIP_CELL_NONE)
                furniture.remove(i);
            else
                furniture.set(i, new ShipFurnitureStruct(piece.getFurnishingName(), cell));
        }

        for (int i = masts.size() - 1; i >= 0; i--)
            if (masts.get(i).getX() + EngineSetting.SHIP_MAST_OCTANTS > newLength)
                masts.remove(i);
    }

    // Every cell moved out with the centre line as the breadth changes, those no longer on the grid dropped
    private void shiftCells(IntArrayList cells, int oldHalfBreadth) {

        for (int i = cells.size() - 1; i >= 0; i--) {

            int cell = shiftCell(cells.getInt(i), oldHalfBreadth);

            if (cell == EngineSetting.SHIP_CELL_NONE)
                cells.removeInt(i);
            else
                cells.set(i, cell);
        }

        IntArrays.quickSort(cells.elements(), 0, cells.size());
    }

    private int shiftCell(int cell, int oldHalfBreadth) {

        if (cell == EngineSetting.SHIP_CELL_NONE)
            return cell;

        int level = ShipCellUtility.unpackLevel(cell);
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell) + halfBreadth - oldHalfBreadth;

        if (level >= height || x >= length || z < 0 || z >= halfBreadth * 2)
            return EngineSetting.SHIP_CELL_NONE;

        return ShipCellUtility.pack(level, x, z, ShipCellUtility.unpackSpin(cell));
    }

    // Decks \\

    public boolean hasDeck(int y) {
        return decks.indexOf(y) != EngineSetting.INDEX_NOT_FOUND;
    }

    public void toggleDeck(int y) {

        int index = decks.indexOf(y);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            decks.removeInt(index);
            return;
        }

        decks.add(y);
        IntArrays.quickSort(decks.elements(), 0, decks.size());
    }

    // Walls \\

    public boolean hasWall(int level, int x, int z) {
        return findSorted(walls, ShipCellUtility.pack(level, x, z, 0)) >= 0;
    }

    public void setWall(int level, int x, int z, boolean standing) {

        int cell = ShipCellUtility.pack(level, x, z, 0);
        int index = findSorted(walls, cell);

        if (standing && index < 0)
            walls.add(-index - 1, cell);
        else if (!standing && index >= 0)
            walls.removeInt(index);
    }

    private static int findSorted(IntArrayList cells, int cell) {

        int low = 0;
        int high = cells.size() - 1;

        while (low <= high) {

            int middle = (low + high) >>> 1;
            int value = cells.getInt(middle);

            if (value < cell)
                low = middle + 1;
            else if (value > cell)
                high = middle - 1;
            else
                return middle;
        }

        return -(low + 1);
    }

    // Placements \\

    // The index of whatever stands at a place in a list of placements, whichever way it faces
    public static int findPlacement(IntArrayList cells, int cell) {

        int place = ShipCellUtility.toPlace(cell);

        for (int i = 0; i < cells.size(); i++)
            if (ShipCellUtility.toPlace(cells.getInt(i)) == place)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Sets a placement down, or takes up the one already standing at that place
    public static void togglePlacement(IntArrayList cells, int cell) {

        int index = findPlacement(cells, cell);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            cells.removeInt(index);
            return;
        }

        cells.add(cell);
        IntArrays.quickSort(cells.elements(), 0, cells.size());
    }

    public int findFurniture(int cell) {

        int place = ShipCellUtility.toPlace(cell);

        for (int i = 0; i < furniture.size(); i++)
            if (ShipCellUtility.toPlace(furniture.get(i).getCell()) == place)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Masts \\

    // The mast whose foot covers an octant along the length, INDEX_NOT_FOUND when none does
    public int findMast(int x) {

        for (int i = 0; i < masts.size(); i++) {

            int mastX = masts.get(i).getX();

            if (x >= mastX && x < mastX + EngineSetting.SHIP_MAST_OCTANTS)
                return i;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Steps a mast's foot clear of every other, false when there is no room for it
    public boolean canStepMast(int x) {

        if (x < 0 || x + EngineSetting.SHIP_MAST_OCTANTS > length)
            return false;

        for (int i = 0; i < masts.size(); i++)
            if (Math.abs(masts.get(i).getX() - x) < EngineSetting.SHIP_MAST_SPACING_OCTANTS)
                return false;

        return true;
    }

    public int addMast(ShipMastStruct mast) {

        int index = 0;

        while (index < masts.size() && masts.get(index).getX() < mast.getX())
            index++;

        masts.add(index, mast);

        return index;
    }

    // Management \\

    public void setShipName(String shipName) {
        this.shipName = shipName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setWaterline(int waterline) {
        this.waterline = Math.max(0, Math.min(height - 1, waterline));
    }

    public void setRailHeight(int railHeight) {
        this.railHeight = railHeight;
    }

    public void setPorchLevel(int porchLevel) {
        this.porchLevel = porchLevel;
    }

    public void setHelm(int helm) {
        this.helm = helm;
    }

    public void setCapstan(int capstan) {
        this.capstan = capstan;
    }

    // Accessible \\

    public String getShipName() {
        return shipName;
    }

    public boolean isNamed() {
        return !shipName.isEmpty();
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLength() {
        return length;
    }

    public int getHalfBreadth() {
        return halfBreadth;
    }

    public int getBreadth() {
        return halfBreadth * 2;
    }

    public int getHeight() {
        return height;
    }

    public int getWaterline() {
        return waterline;
    }

    public IntArrayList getDecks() {
        return decks;
    }

    public int getRailHeight() {
        return railHeight;
    }

    public int getPorchLevel() {
        return porchLevel;
    }

    public boolean hasPorch() {
        return porchLevel != EngineSetting.SHIP_LEVEL_NONE;
    }

    public IntArrayList getWalls() {
        return walls;
    }

    public IntArrayList getDoors() {
        return doors;
    }

    public IntArrayList getStairs() {
        return stairs;
    }

    public int getHelm() {
        return helm;
    }

    public boolean hasHelm() {
        return helm != EngineSetting.SHIP_CELL_NONE;
    }

    public int getCapstan() {
        return capstan;
    }

    public boolean hasCapstan() {
        return capstan != EngineSetting.SHIP_CELL_NONE;
    }

    public ObjectArrayList<ShipFurnitureStruct> getFurniture() {
        return furniture;
    }

    public ObjectArrayList<ShipMastStruct> getMasts() {
        return masts;
    }
}
