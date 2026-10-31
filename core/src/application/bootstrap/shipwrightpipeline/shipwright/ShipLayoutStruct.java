package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipLayoutStruct extends StructPackage {

    /*
     * A ship blueprint compiled into what it is built of. Its sections are the
     * hull's half-beam at every layer and octant along the sheet, every layer
     * between two decks blended from the one below to the one above; its
     * octant grid holds every piece laid out in model octants, the sheet
     * standing originX octants in from the grid's aft edge so a rudder and a
     * porch fit behind it; its vehicle parts are those pieces merged into
     * boxes and every spar, sail, rope and fitting, in the order they fill the
     * model grid; its cargo is every door and piece of furniture, each with
     * the sub-voxel box it stands in. Every hatch, ladder, gun-port lid and
     * window pane is
     * kept as the octant box it fills and the quarter turn it faces, laid by
     * the hull and the openings before the grid is merged and built as a
     * part of its own after. It knows how far its anchors drop, how tall each
     * mast rises, how many sails it carries and whether it has a crow's nest,
     * and where its bowsprit's tip reaches; what the ship weighs, the
     * ballast it is trimmed with, the height of its centre of mass and the
     * draft it floats at; how high the sea may rise before it comes aboard;
     * and how long, how broad and how sharp it is at its waterline. A problem
     * in what was built — a stair or a piece of furniture with no room — is
     * kept apart from one in the hull's shape, so a change can be refused for
     * the first and the second only shown. Owned by whoever compiled it and
     * refilled in place each time.
     */

    // Sections — half-beams in octants, a layer of SHIP_SHEET_LENGTH after another
    private final byte[] sections;
    private int sectionLayers;

    // Grid
    private final ShipOctantGridStruct grid;
    private int originX;

    // Parts
    private final ObjectArrayList<VehiclePartStruct> parts;

    // Cargo — one sub-voxel box of six ints per slot
    private final ObjectArrayList<FurnishingSlotStruct> cargoSlots;
    private final IntArrayList cargoBoxes;

    // Fittings — an octant box of six ints and a quarter turn for each, in model octants
    private final IntArrayList hatchBoxes;
    private final IntArrayList ladderBoxes;
    private final IntArrayList portBoxes;
    private final IntArrayList paneBoxes;

    // Anchors — model blocks
    private float anchorDrop;

    // Rig — every mast's height in octants, its sails and its crow's nest, from the stern forward
    private final IntArrayList mastHeights;
    private final IntArrayList mastSailCounts;
    private final BooleanArrayList mastNests;

    // Bowsprit — its tip in model sub-voxels, SHIP_LEVEL_NONE while there is none
    private int bowspritTipX;
    private int bowspritTipY;

    // Weight — tonnes, the centre of mass height in model blocks
    private float structureMass;
    private float ballastMass;
    private float centerOfMassHeight;

    // Water — model blocks
    private float draft;
    private float sillHeight;
    private float openingSill;

    // Waterline — blocks, the entry angle in degrees
    private float waterlineLength;
    private float waterlineBeam;
    private float entryDegrees;

    // Problems
    private String buildProblem;
    private String shapeProblem;

    // Constructor \\

    public ShipLayoutStruct(ShipOctantGridStruct grid) {

        // Sections
        this.sections = new byte[EngineSetting.SHIP_MAX_DECKS * EngineSetting.SHIP_DECK_HEIGHT
                * EngineSetting.SHIP_SHEET_LENGTH];

        // Grid
        this.grid = grid;

        // Parts
        this.parts = new ObjectArrayList<>();

        // Cargo
        this.cargoSlots = new ObjectArrayList<>();
        this.cargoBoxes = new IntArrayList();

        // Fittings
        this.hatchBoxes = new IntArrayList();
        this.ladderBoxes = new IntArrayList();
        this.portBoxes = new IntArrayList();
        this.paneBoxes = new IntArrayList();

        // Rig
        this.mastHeights = new IntArrayList();
        this.mastSailCounts = new IntArrayList();
        this.mastNests = new BooleanArrayList();

        // Problems
        this.buildProblem = EngineSetting.SHIP_PROBLEM_NONE;
        this.shapeProblem = EngineSetting.SHIP_PROBLEM_NONE;
    }

    // Management \\

    public void clear(int originX) {

        this.originX = originX;
        this.sectionLayers = 0;
        parts.clear();
        cargoSlots.clear();
        cargoBoxes.clear();
        hatchBoxes.clear();
        ladderBoxes.clear();
        portBoxes.clear();
        paneBoxes.clear();
        this.anchorDrop = EngineSetting.DEFAULT_VEHICLE_ANCHOR_DROP;
        mastHeights.clear();
        mastSailCounts.clear();
        mastNests.clear();
        this.bowspritTipX = EngineSetting.SHIP_LEVEL_NONE;
        this.bowspritTipY = EngineSetting.SHIP_LEVEL_NONE;
        this.structureMass = 0f;
        this.ballastMass = 0f;
        this.centerOfMassHeight = 0f;
        this.draft = 0f;
        this.sillHeight = 0f;
        this.openingSill = Float.MAX_VALUE;
        this.waterlineLength = 0f;
        this.waterlineBeam = 0f;
        this.entryDegrees = 0f;
        this.buildProblem = EngineSetting.SHIP_PROBLEM_NONE;
        this.shapeProblem = EngineSetting.SHIP_PROBLEM_NONE;
    }

    // Sections \\

    public void setSectionLayers(int sectionLayers) {
        this.sectionLayers = sectionLayers;
    }

    public void setSection(int x, int y, int halfBeam) {
        sections[y * EngineSetting.SHIP_SHEET_LENGTH + x] = (byte) halfBeam;
    }

    // How many octants the hull reaches out from the centre line at a layer and an octant along the sheet
    public int getSection(int x, int y) {

        if (x < 0 || y < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH || y >= sectionLayers)
            return 0;

        return sections[y * EngineSetting.SHIP_SHEET_LENGTH + x];
    }

    // The hull's half-beam where the water meets it at a layer: the section there, or inside the bulwarks of an open
    // deck just below it
    public int getWaterlineSection(int x, int y) {

        for (int depth = 0; depth <= EngineSetting.SHIP_BULWARK_HEIGHT; depth++) {

            int section = getSection(x, y - depth);

            if (section > 0 && (depth == 0 || (y - depth) % EngineSetting.SHIP_DECK_HEIGHT == 0))
                return section;
        }

        return 0;
    }

    // True when an octant across the sheet's full breadth lies inside the hull, the octant along the sheet given
    public boolean isVolume(int x, int y, int z) {

        int halfBreadth = EngineSetting.SHIP_SHEET_HALF_BREADTH;

        if (z < 0 || z >= halfBreadth * 2)
            return false;

        return (z >= halfBreadth ? z - halfBreadth : halfBreadth - 1 - z) < getSection(x, y);
    }

    // Parts \\

    public void addPart(VehiclePartStruct part) {
        parts.add(part);
    }

    public void addCargo(FurnishingSlotStruct slot, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        cargoSlots.add(slot);
        cargoBoxes.add(minX);
        cargoBoxes.add(minY);
        cargoBoxes.add(minZ);
        cargoBoxes.add(maxX);
        cargoBoxes.add(maxY);
        cargoBoxes.add(maxZ);
    }

    // A hatch lid lying in a floor, the octants it closes
    public void addHatch(int minX, int y, int minZ, int maxX, int maxZ, int spin) {
        addFittingBox(hatchBoxes, minX, y, minZ, maxX, y + 1, maxZ, spin);
    }

    // A ladder's octants from the floor it stands on up past the floor it climbs to
    public void addLadder(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int spin) {
        addFittingBox(ladderBoxes, minX, minY, minZ, maxX, maxY, maxZ, spin);
    }

    // A gun port cut through the planking, its lid filling the octants cut
    public void addPort(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int spin) {
        addFittingBox(portBoxes, minX, minY, minZ, maxX, maxY, maxZ, spin);
    }

    // A window cut through a wall or the side, glazed across the octants cut
    public void addPane(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int spin) {
        addFittingBox(paneBoxes, minX, minY, minZ, maxX, maxY, maxZ, spin);
    }

    private static void addFittingBox(
            IntArrayList boxes,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            int spin) {

        boxes.add(minX);
        boxes.add(minY);
        boxes.add(minZ);
        boxes.add(maxX);
        boxes.add(maxY);
        boxes.add(maxZ);
        boxes.add(spin);
    }

    public void setAnchorDrop(float anchorDrop) {
        this.anchorDrop = anchorDrop;
    }

    // The rig worked out for the next mast from the stern forward
    public void addMast(int height, int sailCount, boolean crowsNest) {
        mastHeights.add(height);
        mastSailCounts.add(sailCount);
        mastNests.add(crowsNest);
    }

    public void setBowspritTip(int bowspritTipX, int bowspritTipY) {
        this.bowspritTipX = bowspritTipX;
        this.bowspritTipY = bowspritTipY;
    }

    // Weight & Water \\

    public void setWeight(float structureMass, float ballastMass, float centerOfMassHeight) {
        this.structureMass = structureMass;
        this.ballastMass = ballastMass;
        this.centerOfMassHeight = centerOfMassHeight;
    }

    public void setDraft(float draft) {
        this.draft = draft;
    }

    public void setSillHeight(float sillHeight) {
        this.sillHeight = sillHeight;
    }

    // Lowers the sill to the bottom of an opening cut through the hull's side, in model blocks
    public void lowerOpeningSill(float height) {
        this.openingSill = Math.min(openingSill, height);
    }

    public void setWaterline(float waterlineLength, float waterlineBeam, float entryDegrees) {
        this.waterlineLength = waterlineLength;
        this.waterlineBeam = waterlineBeam;
        this.entryDegrees = entryDegrees;
    }

    // Problems \\

    // Keeps the first problem met in what was built; later ones follow from it
    public void reportBuildProblem(String problem) {

        if (buildProblem.isEmpty())
            this.buildProblem = problem;
    }

    // Keeps the first problem met in the hull's shape
    public void reportShapeProblem(String problem) {

        if (shapeProblem.isEmpty())
            this.shapeProblem = problem;
    }

    // Accessible \\

    public ShipOctantGridStruct getGrid() {
        return grid;
    }

    public int getOriginX() {
        return originX;
    }

    public int getSectionLayers() {
        return sectionLayers;
    }

    public ObjectArrayList<VehiclePartStruct> getParts() {
        return parts;
    }

    public ObjectArrayList<FurnishingSlotStruct> getCargoSlots() {
        return cargoSlots;
    }

    public int getCargoCount() {
        return cargoSlots.size();
    }

    // One bound of a cargo slot's box: 0 to 2 its minimum on x, y and z, 3 to 5 its maximum
    public int getCargoBound(int slotIndex, int bound) {
        return cargoBoxes.getInt(slotIndex * EngineSetting.BOX_INT_STRIDE + bound);
    }

    public IntArrayList getHatchBoxes() {
        return hatchBoxes;
    }

    public IntArrayList getLadderBoxes() {
        return ladderBoxes;
    }

    public IntArrayList getPortBoxes() {
        return portBoxes;
    }

    public IntArrayList getPaneBoxes() {
        return paneBoxes;
    }

    // How many fittings a list of fitting boxes holds
    public static int countFittings(IntArrayList boxes) {
        return boxes.size() / EngineSetting.SHIP_FITTING_BOX_STRIDE;
    }

    // One bound of a fitting's octant box, 0 to 5 as a box's bounds run, 6 the quarter turn it faces
    public static int getFittingBound(IntArrayList boxes, int fittingIndex, int bound) {
        return boxes.getInt(fittingIndex * EngineSetting.SHIP_FITTING_BOX_STRIDE + bound);
    }

    // How far the anchors fall below their catheads when let go, in model blocks
    public float getAnchorDrop() {
        return anchorDrop;
    }

    public int getMastCount() {
        return mastHeights.size();
    }

    public int getMastHeight(int mastIndex) {
        return mastHeights.getInt(mastIndex);
    }

    public int getMastSailCount(int mastIndex) {
        return mastSailCounts.getInt(mastIndex);
    }

    public boolean hasCrowsNest(int mastIndex) {
        return mastNests.getBoolean(mastIndex);
    }

    public int getSailCount() {

        int sails = 0;

        for (int i = 0; i < mastSailCounts.size(); i++)
            sails += mastSailCounts.getInt(i);

        return sails;
    }

    public boolean hasBowsprit() {
        return bowspritTipX != EngineSetting.SHIP_LEVEL_NONE;
    }

    public int getBowspritTipX() {
        return bowspritTipX;
    }

    public int getBowspritTipY() {
        return bowspritTipY;
    }

    public float getStructureMass() {
        return structureMass;
    }

    public float getBallastMass() {
        return ballastMass;
    }

    public float getMass() {
        return structureMass + ballastMass;
    }

    public float getCenterOfMassHeight() {
        return centerOfMassHeight;
    }

    public float getDraft() {
        return draft;
    }

    // True once the hull has been floated, so its draft and everything worked from it can be shown
    public boolean isAfloat() {
        return draft > 0f;
    }

    // How high the sea may rise, in model blocks, before it comes over the lowest bulwark aboard
    public float getSillHeight() {
        return sillHeight;
    }

    // How high the sea may rise, in model blocks, before it comes in through a door or window in the side
    public float getOpeningSill() {
        return openingSill;
    }

    public float getWaterlineLength() {
        return waterlineLength;
    }

    public float getWaterlineBeam() {
        return waterlineBeam;
    }

    public float getEntryDegrees() {
        return entryDegrees;
    }

    public boolean hasProblem() {
        return !buildProblem.isEmpty() || !shapeProblem.isEmpty();
    }

    public boolean hasBuildProblem() {
        return !buildProblem.isEmpty();
    }

    public String getBuildProblem() {
        return buildProblem;
    }

    // The problem to show first: one in what was built, then one in the hull's shape
    public String getProblem() {
        return buildProblem.isEmpty() ? shapeProblem : buildProblem;
    }
}
