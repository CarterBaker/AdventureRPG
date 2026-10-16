package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipLayoutStruct extends StructPackage {

    /*
     * A ship blueprint compiled into what it is built of. Its octant grid
     * holds every piece laid out in model octants, the blueprint's stern
     * standing originX octants in from the grid's aft edge so a rudder and a
     * porch fit behind it; its vehicle parts are those pieces merged into
     * boxes and every spar, sail, rope and fitting, in the order they fill the
     * model grid; its cargo is every door and piece of furniture, each with
     * the sub-voxel box it stands in. It knows every level a floor lies on,
     * how many sails each mast carries, where its bowsprit's tip reaches, the
     * design draft and the height of its centre of mass, both in blocks, and the first problem that keeps it
     * from being built, if any. Owned by whoever compiled it and refilled in
     * place each time.
     */

    // Grid
    private final ShipOctantGridStruct grid;
    private int originX;

    // Parts
    private final ObjectArrayList<VehiclePartStruct> parts;

    // Cargo — one sub-voxel box of six ints per slot
    private final ObjectArrayList<FurnishingSlotStruct> cargoSlots;
    private final IntArrayList cargoBoxes;

    // Levels — floor layers, sorted
    private final IntArrayList levels;

    // Rig — sails per mast, from the stern forward
    private final IntArrayList mastSailCounts;

    // Bowsprit — its tip in model sub-voxels, SHIP_LEVEL_NONE while there is none
    private int bowspritTipX;
    private int bowspritTipY;

    // Hull — model blocks
    private float draft;
    private float centerOfMassHeight;

    // Problem
    private String problem;

    // Constructor \\

    public ShipLayoutStruct(ShipOctantGridStruct grid) {

        // Grid
        this.grid = grid;

        // Parts
        this.parts = new ObjectArrayList<>();

        // Cargo
        this.cargoSlots = new ObjectArrayList<>();
        this.cargoBoxes = new IntArrayList();

        // Levels
        this.levels = new IntArrayList();

        // Rig
        this.mastSailCounts = new IntArrayList();

        // Problem
        this.problem = EngineSetting.SHIP_PROBLEM_NONE;
    }

    // Management \\

    public void clear(int originX) {

        this.originX = originX;
        parts.clear();
        cargoSlots.clear();
        cargoBoxes.clear();
        levels.clear();
        mastSailCounts.clear();
        this.bowspritTipX = EngineSetting.SHIP_LEVEL_NONE;
        this.bowspritTipY = EngineSetting.SHIP_LEVEL_NONE;
        this.draft = 0f;
        this.centerOfMassHeight = 0f;
        this.problem = EngineSetting.SHIP_PROBLEM_NONE;
    }

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

    public void addLevel(int level) {

        if (levels.indexOf(level) == EngineSetting.INDEX_NOT_FOUND)
            levels.add(level);
    }

    public void addMastSailCount(int sailCount) {
        mastSailCounts.add(sailCount);
    }

    public void setBowspritTip(int bowspritTipX, int bowspritTipY) {
        this.bowspritTipX = bowspritTipX;
        this.bowspritTipY = bowspritTipY;
    }

    public void setHull(float draft, float centerOfMassHeight) {
        this.draft = draft;
        this.centerOfMassHeight = centerOfMassHeight;
    }

    // Keeps the first problem met; later ones follow from it
    public void reportProblem(String problem) {

        if (this.problem.isEmpty())
            this.problem = problem;
    }

    // Accessible \\

    public ShipOctantGridStruct getGrid() {
        return grid;
    }

    public int getOriginX() {
        return originX;
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

    public IntArrayList getLevels() {
        return levels;
    }

    public IntArrayList getMastSailCounts() {
        return mastSailCounts;
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

    public float getDraft() {
        return draft;
    }

    public float getCenterOfMassHeight() {
        return centerOfMassHeight;
    }

    public boolean hasProblem() {
        return !problem.isEmpty();
    }

    public String getProblem() {
        return problem;
    }
}
