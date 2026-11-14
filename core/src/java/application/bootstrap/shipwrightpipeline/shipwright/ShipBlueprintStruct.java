package application.bootstrap.shipwrightpipeline.shipwright;

import java.util.Arrays;

import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipBlueprintStruct extends StructPackage {

    /*
     * A vehicle as it is drawn at the drafting table, everything its file
     * keeps, and the category it is drawn and built as. A ship is a stack of
     * decks from the bottom up, each drawn as its outline: how many octants it
     * reaches out from the centre line at every octant along the sheet, on
     * each side, drawn mirrored to both sides or, as a porch, to one. The
     * waist is cut through a deck's floor octant by octant and a roof laid
     * over its open floor the same way, each kept sorted, every octant with
     * its mirror or alone. Walls, half walls, railings, lips, doors, windows,
     * hatches, grates, glass floors, gun platforms, stairs, ladders, boarding
     * ladders and furniture stand on a deck at an octant along the length and
     * across the full breadth, never mirrored, each packed through
     * ShipCellUtility with its deck as its level; walls, half walls, railings
     * and lips are kept sorted and a place holds at most one of them. The helm
     * and the capstan are one placement each, and masts stand on the centre
     * line kept from the stern forward. Copied whole for undo; every list
     * writes in a fixed order, so saving an unchanged blueprint writes the
     * same bytes.
     */

    // Identity
    private String shipName;
    private String displayName;
    private VehicleCategory category;

    // Decks — the half-beam of every deck at every octant along the sheet, deck after deck, starboard then port
    private final byte[] outlines;
    private int deckCount;

    // Waist — packed cells cut out of a deck's floor, their level the deck they are cut through
    private final IntArrayList waists;

    // Roofs — packed cells roofed over a deck's open floor, their level the deck they roof
    private final IntArrayList roofs;

    // Rooms — packed cells, their level the deck they stand on
    private final IntArrayList walls;
    private final IntArrayList halfWalls;
    private final IntArrayList railings;
    private final IntArrayList lips;
    private final IntArrayList doors;
    private final IntArrayList windows;
    private final IntArrayList hatches;
    private final IntArrayList grates;
    private final IntArrayList glassFloors;
    private final IntArrayList gunPlatforms;
    private final IntArrayList stairs;
    private final IntArrayList ladders;
    private final IntArrayList boardingLadders;

    // Fittings — packed cells
    private int helm;
    private int capstan;

    // Furniture
    private final ObjectArrayList<ShipFurnitureStruct> furniture;

    // Masts — from the stern forward
    private final ObjectArrayList<ShipMastStruct> masts;

    // Constructor \\

    public ShipBlueprintStruct() {

        // Identity
        this.shipName = EngineSetting.SHIP_NAME_NONE;
        this.displayName = EngineSetting.SHIP_NAME_NONE;
        this.category = VehicleCategory.SHIP;

        // Decks
        this.outlines = new byte[EngineSetting.SHIP_SIDE_COUNT * EngineSetting.SHIP_MAX_DECKS
                * EngineSetting.SHIP_SHEET_LENGTH];
        this.deckCount = 1;

        // Waist
        this.waists = new IntArrayList();

        // Roofs
        this.roofs = new IntArrayList();

        // Rooms
        this.walls = new IntArrayList();
        this.halfWalls = new IntArrayList();
        this.railings = new IntArrayList();
        this.lips = new IntArrayList();
        this.doors = new IntArrayList();
        this.windows = new IntArrayList();
        this.hatches = new IntArrayList();
        this.grates = new IntArrayList();
        this.glassFloors = new IntArrayList();
        this.gunPlatforms = new IntArrayList();
        this.stairs = new IntArrayList();
        this.ladders = new IntArrayList();
        this.boardingLadders = new IntArrayList();

        // Fittings
        this.helm = EngineSetting.SHIP_CELL_NONE;
        this.capstan = EngineSetting.SHIP_CELL_NONE;

        // Furniture
        this.furniture = new ObjectArrayList<>();

        // Masts
        this.masts = new ObjectArrayList<>();
    }

    public ShipBlueprintStruct copy() {

        ShipBlueprintStruct copy = new ShipBlueprintStruct();
        copy.set(this);

        return copy;
    }

    // Takes on everything another blueprint holds
    public void set(ShipBlueprintStruct other) {

        // Identity
        this.shipName = other.shipName;
        this.displayName = other.displayName;
        this.category = other.category;

        // Decks
        System.arraycopy(other.outlines, 0, outlines, 0, outlines.length);
        this.deckCount = other.deckCount;

        // Waist
        copyCells(other.waists, waists);

        // Roofs
        copyCells(other.roofs, roofs);

        // Rooms
        copyCells(other.walls, walls);
        copyCells(other.halfWalls, halfWalls);
        copyCells(other.railings, railings);
        copyCells(other.lips, lips);
        copyCells(other.doors, doors);
        copyCells(other.windows, windows);
        copyCells(other.hatches, hatches);
        copyCells(other.grates, grates);
        copyCells(other.glassFloors, glassFloors);
        copyCells(other.gunPlatforms, gunPlatforms);
        copyCells(other.stairs, stairs);
        copyCells(other.ladders, ladders);
        copyCells(other.boardingLadders, boardingLadders);

        // Fittings
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

    private static void copyCells(IntArrayList from, IntArrayList to) {
        to.clear();
        to.addAll(from);
    }

    // Decks \\

    // How many octants a deck reaches out from the centre line at an octant along the sheet on its wider side, 0
    // where it is not drawn
    public int getOutline(int deck, int x) {
        return Math.max(getOutline(deck, x, EngineSetting.SHIP_SIDE_STARBOARD),
                getOutline(deck, x, EngineSetting.SHIP_SIDE_PORT));
    }

    // How many octants a deck reaches out from the centre line on one side at an octant along the sheet
    public int getOutline(int deck, int x, int side) {

        if (deck < 0 || deck >= deckCount || x < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH)
            return 0;

        return outlines[toOutlineIndex(deck, x, side)];
    }

    // A deck drawn out to a half-beam at an octant along the sheet on both sides, mirrored
    public void setOutline(int deck, int x, int halfBeam) {
        setOutline(deck, x, EngineSetting.SHIP_SIDE_STARBOARD, halfBeam);
        setOutline(deck, x, EngineSetting.SHIP_SIDE_PORT, halfBeam);
    }

    // A deck drawn out to a half-beam at an octant along the sheet on one side alone
    public void setOutline(int deck, int x, int side, int halfBeam) {

        if (deck < 0 || deck >= deckCount || x < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH)
            return;

        outlines[toOutlineIndex(deck, x, side)] = (byte) Math.max(0,
                Math.min(EngineSetting.SHIP_SHEET_HALF_BREADTH, halfBeam));
    }

    private static int toOutlineIndex(int deck, int x, int side) {
        return (side * EngineSetting.SHIP_MAX_DECKS + deck) * EngineSetting.SHIP_SHEET_LENGTH + x;
    }

    // True when a deck's floor covers an octant across the full breadth, on the side of the centre line it lies
    public boolean isOnDeck(int deck, int x, int z) {
        return z >= 0 && z < EngineSetting.SHIP_SHEET_BREADTH
                && ShipCellUtility.toHalfBreadth(z) < getOutline(deck, x, ShipCellUtility.toSide(z));
    }

    public boolean isDeckDrawn(int deck) {

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            if (getOutline(deck, x) > 0)
                return true;

        return false;
    }

    // A new deck on top of the stack, drawn as the deck under it to start from; false when the stack is full
    public boolean addDeck() {

        if (deckCount >= EngineSetting.SHIP_MAX_DECKS)
            return false;

        for (int side = 0; side < EngineSetting.SHIP_SIDE_COUNT; side++)
            System.arraycopy(outlines, toOutlineIndex(deckCount - 1, 0, side),
                    outlines, toOutlineIndex(deckCount, 0, side), EngineSetting.SHIP_SHEET_LENGTH);

        deckCount++;

        return true;
    }

    // Takes a deck out with everything standing on it, every deck above it lowered into its place; false for the last
    public boolean removeDeck(int deck) {

        if (deckCount <= 1 || deck < 0 || deck >= deckCount)
            return false;

        int length = EngineSetting.SHIP_SHEET_LENGTH;

        for (int side = 0; side < EngineSetting.SHIP_SIDE_COUNT; side++) {
            System.arraycopy(outlines, toOutlineIndex(deck + 1, 0, side), outlines, toOutlineIndex(deck, 0, side),
                    (deckCount - deck - 1) * length);
            Arrays.fill(outlines, toOutlineIndex(deckCount - 1, 0, side), toOutlineIndex(deckCount, 0, side),
                    (byte) 0);
        }

        deckCount--;

        removeDeckCells(waists, deck);
        removeDeckCells(roofs, deck);
        removeDeckCells(walls, deck);
        removeDeckCells(halfWalls, deck);
        removeDeckCells(railings, deck);
        removeDeckCells(lips, deck);
        removeDeckCells(doors, deck);
        removeDeckCells(windows, deck);
        removeDeckCells(hatches, deck);
        removeDeckCells(grates, deck);
        removeDeckCells(glassFloors, deck);
        removeDeckCells(gunPlatforms, deck);
        removeDeckCells(stairs, deck);
        removeDeckCells(ladders, deck);
        removeDeckCells(boardingLadders, deck);
        this.helm = removeDeckCell(helm, deck);
        this.capstan = removeDeckCell(capstan, deck);

        for (int i = furniture.size() - 1; i >= 0; i--) {

            ShipFurnitureStruct piece = furniture.get(i);
            int cell = removeDeckCell(piece.getCell(), deck);

            if (cell == EngineSetting.SHIP_CELL_NONE)
                furniture.remove(i);
            else
                furniture.set(i, new ShipFurnitureStruct(piece.getFurnishingName(), cell));
        }

        return true;
    }

    // Every cell on a deck dropped, every cell above it lowered a deck
    private static void removeDeckCells(IntArrayList cells, int deck) {

        for (int i = cells.size() - 1; i >= 0; i--) {

            int cell = removeDeckCell(cells.getInt(i), deck);

            if (cell == EngineSetting.SHIP_CELL_NONE)
                cells.removeInt(i);
            else
                cells.set(i, cell);
        }
    }

    private static int removeDeckCell(int cell, int deck) {

        if (cell == EngineSetting.SHIP_CELL_NONE)
            return cell;

        int level = ShipCellUtility.unpackLevel(cell);

        if (level == deck)
            return EngineSetting.SHIP_CELL_NONE;

        if (level < deck)
            return cell;

        return ShipCellUtility.pack(level - 1, ShipCellUtility.unpackX(cell), ShipCellUtility.unpackZ(cell),
                ShipCellUtility.unpackSpin(cell));
    }

    // Everything standing off the deck it was set down on taken up, and every mast with no deck under it
    public void prunePlacements() {

        pruneCells(waists);
        pruneCells(roofs);
        pruneCells(walls);
        pruneCells(halfWalls);
        pruneCells(railings);
        pruneCells(lips);
        pruneCells(doors);
        pruneCells(windows);
        pruneCells(hatches);
        pruneCells(grates);
        pruneCells(glassFloors);
        pruneCells(gunPlatforms);
        pruneCells(stairs);
        pruneCells(ladders);
        pruneCells(boardingLadders);

        if (!isCellOnDeck(helm))
            this.helm = EngineSetting.SHIP_CELL_NONE;

        if (!isCellOnDeck(capstan))
            this.capstan = EngineSetting.SHIP_CELL_NONE;

        for (int i = furniture.size() - 1; i >= 0; i--)
            if (!isCellOnDeck(furniture.get(i).getCell()))
                furniture.remove(i);

        for (int i = masts.size() - 1; i >= 0; i--)
            if (findLowestDeck(masts.get(i).getX()) == EngineSetting.SHIP_DECK_NONE)
                masts.remove(i);
    }

    private void pruneCells(IntArrayList cells) {

        for (int i = cells.size() - 1; i >= 0; i--)
            if (!isCellOnDeck(cells.getInt(i)))
                cells.removeInt(i);
    }

    private boolean isCellOnDeck(int cell) {
        return cell == EngineSetting.SHIP_CELL_NONE || isOnDeck(
                ShipCellUtility.unpackLevel(cell), ShipCellUtility.unpackX(cell), ShipCellUtility.unpackZ(cell));
    }

    // The lowest deck drawn across the centre line at an octant along the sheet, SHIP_DECK_NONE when none is
    public int findLowestDeck(int x) {

        for (int deck = 0; deck < deckCount; deck++)
            if (getOutline(deck, x) > 0)
                return deck;

        return EngineSetting.SHIP_DECK_NONE;
    }

    // The highest deck drawn across the centre line at an octant along the sheet, SHIP_DECK_NONE when none is
    public int findHighestDeck(int x) {

        for (int deck = deckCount - 1; deck >= 0; deck--)
            if (getOutline(deck, x) > 0)
                return deck;

        return EngineSetting.SHIP_DECK_NONE;
    }

    // The first and last octant along the sheet any deck is drawn at, written to the bounds given; false when none
    public boolean resolveExtent(int[] bounds) {

        bounds[0] = Integer.MAX_VALUE;
        bounds[1] = Integer.MIN_VALUE;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            if (findLowestDeck(x) != EngineSetting.SHIP_DECK_NONE) {
                bounds[0] = Math.min(bounds[0], x);
                bounds[1] = Math.max(bounds[1], x);
            }

        return bounds[0] <= bounds[1];
    }

    // Waist \\

    // True when the waist is cut through a deck's floor at a place
    public boolean hasWaist(int level, int x, int z) {
        return findSorted(waists, ShipCellUtility.pack(level, x, z, 0)) >= 0;
    }

    // Cuts the waist through a deck's floor at a place and, mirrored, its mirror across the centre line, or fills
    // them back in
    public void setWaist(int level, int x, int z, boolean cut, boolean mirrored) {
        setMirroredCell(waists, level, x, z, cut, mirrored);
    }

    // Roofs \\

    // True when a roof is laid over a deck's floor at a place
    public boolean hasRoof(int level, int x, int z) {
        return findSorted(roofs, ShipCellUtility.pack(level, x, z, 0)) >= 0;
    }

    // Lays a roof over a deck's floor at a place and, mirrored, its mirror across the centre line, or takes them up
    public void setRoof(int level, int x, int z, boolean laid, boolean mirrored) {
        setMirroredCell(roofs, level, x, z, laid, mirrored);
    }

    // Keeps a place in a sorted list, with its mirror across the centre line when mirrored, or takes them out
    private static void setMirroredCell(IntArrayList cells, int level, int x, int z, boolean kept, boolean mirrored) {

        int halfZ = ShipCellUtility.toHalfBreadth(z);

        if (!mirrored) {
            setSorted(cells, ShipCellUtility.pack(level, x, z, 0), kept);
            return;
        }

        setSorted(cells, ShipCellUtility.pack(level, x, ShipCellUtility.toStarboard(halfZ), 0), kept);
        setSorted(cells, ShipCellUtility.pack(level, x, ShipCellUtility.toPort(halfZ), 0), kept);
    }

    // Walls \\

    // The wall standing at a place, null when none does
    public ShipWall getWall(int level, int x, int z) {

        int place = ShipCellUtility.pack(level, x, z, 0);

        if (findSorted(walls, place) >= 0)
            return ShipWall.FULL;

        if (findSorted(halfWalls, place) >= 0)
            return ShipWall.HALF;

        if (findSorted(railings, place) >= 0)
            return ShipWall.RAILING;

        return findSorted(lips, place) >= 0 ? ShipWall.LIP : null;
    }

    public boolean hasWall(int level, int x, int z) {
        return getWall(level, x, z) != null;
    }

    // Stands a full wall, half wall, railing or lip at a place, replacing whichever stood there, or with null takes it
    // down
    public void setWall(int level, int x, int z, ShipWall wall) {

        int place = ShipCellUtility.pack(level, x, z, 0);

        removeSorted(walls, place);
        removeSorted(halfWalls, place);
        removeSorted(railings, place);
        removeSorted(lips, place);

        if (wall != null)
            setSorted(getWalls(wall), place, true);
    }

    private static void removeSorted(IntArrayList cells, int cell) {
        setSorted(cells, cell, false);
    }

    // Keeps a cell in a sorted list, or takes it out
    private static void setSorted(IntArrayList cells, int cell, boolean kept) {

        int index = findSorted(cells, cell);

        if (kept && index < 0)
            cells.add(-index - 1, cell);
        else if (!kept && index >= 0)
            cells.removeInt(index);
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

    // True when a mast's foot fits on the sheet clear of every other
    public boolean canStepMast(int x) {

        if (x < 0 || x + EngineSetting.SHIP_MAST_OCTANTS > EngineSetting.SHIP_SHEET_LENGTH
                || masts.size() >= EngineSetting.SHIP_MAX_MASTS)
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

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    // Sets how many decks the stack holds, as a ship file is read
    public void setDeckCount(int deckCount) {
        this.deckCount = Math.max(1, Math.min(EngineSetting.SHIP_MAX_DECKS, deckCount));
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

    public VehicleCategory getCategory() {
        return category;
    }

    public int getDeckCount() {
        return deckCount;
    }

    public boolean hasHull() {
        return isDeckDrawn(0);
    }

    public IntArrayList getWaists() {
        return waists;
    }

    public IntArrayList getRoofs() {
        return roofs;
    }

    public IntArrayList getWalls() {
        return walls;
    }

    public IntArrayList getHalfWalls() {
        return halfWalls;
    }

    public IntArrayList getRailings() {
        return railings;
    }

    public IntArrayList getLips() {
        return lips;
    }

    // The placements of one kind of standing wall
    public IntArrayList getWalls(ShipWall wall) {
        return switch (wall) {
            case HALF -> halfWalls;
            case RAILING -> railings;
            case LIP -> lips;
            default -> walls;
        };
    }

    public IntArrayList getDoors() {
        return doors;
    }

    public IntArrayList getWindows() {
        return windows;
    }

    public IntArrayList getHatches() {
        return hatches;
    }

    public IntArrayList getGrates() {
        return grates;
    }

    public IntArrayList getGlassFloors() {
        return glassFloors;
    }

    public IntArrayList getGunPlatforms() {
        return gunPlatforms;
    }

    public IntArrayList getStairs() {
        return stairs;
    }

    public IntArrayList getLadders() {
        return ladders;
    }

    public IntArrayList getBoardingLadders() {
        return boardingLadders;
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

    // The placements of one kind of opening
    public IntArrayList getOpenings(ShipOpening opening) {
        return switch (opening) {
            case DOOR -> doors;
            case WINDOW -> windows;
            case HATCH -> hatches;
            case GRATE -> grates;
            case GLASS -> glassFloors;
            case PLATFORM -> gunPlatforms;
        };
    }

    // The placements of one kind of climb
    public IntArrayList getClimbs(ShipClimb climb) {
        return switch (climb) {
            case STAIR -> stairs;
            case LADDER -> ladders;
            case BOARDING -> boardingLadders;
        };
    }

    public ObjectArrayList<ShipFurnitureStruct> getFurniture() {
        return furniture;
    }

    public ObjectArrayList<ShipMastStruct> getMasts() {
        return masts;
    }
}
