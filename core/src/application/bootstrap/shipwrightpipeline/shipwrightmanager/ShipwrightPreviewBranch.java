package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipClimb;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPreviewStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipWall;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightPreviewBranch extends BranchPackage {

    /*
     * Builds the picture of every option of the drafting table no item
     * stands for, so each catalogue tile shows what it sets down: a little
     * model laid in an octant grid of its own and built by the very builders
     * a ship is, then meshed at rest as a vehicle meshes it. A deck is a
     * square of floor, a porch a strip of floor reaching out to one side; the
     * waist a floor cut open and railed round, in its middle or to one side;
     * a roof stepped shingles over railed floor, whole or to one side; each
     * wall stands on a strip of floor, the lip a sill between two posts of
     * planking; a window panes in a wall; a hatch, grate and glass floor lie
     * in a floor; a gun platform faces its lidded port in the side; a
     * staircase climbs a deck; a ladder and a boarding ladder stand at their
     * height; a mast carries a yard and a sail; the helm and the capstan are
     * built whole. Made once when the table's manager wakes and kept.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;
    private ShipwrightFittingBranch shipwrightFittingBranch;
    private ShipwrightCompileBranch shipwrightCompileBranch;
    private VehicleManager vehicleManager;

    // Previews — by the option of each tool they picture, null for one an item pictures; the deck, waist and roof
    // mirrored then one side
    private ShipPreviewStruct[] deckPreviews;
    private ShipPreviewStruct[] waistPreviews;
    private ShipPreviewStruct[] roofPreviews;
    private ShipPreviewStruct mastPreview;
    private ShipPreviewStruct helmPreview;
    private ShipPreviewStruct capstanPreview;
    private ShipPreviewStruct[] wallPreviews;
    private ShipPreviewStruct[] openingPreviews;
    private ShipPreviewStruct[] climbPreviews;

    // Scratch — main thread only
    private ShipLayoutStruct layout;

    // Base \\

    @Override
    protected void create() {

        // Previews
        this.deckPreviews = new ShipPreviewStruct[EngineSetting.SHIP_SIDE_COUNT];
        this.waistPreviews = new ShipPreviewStruct[EngineSetting.SHIP_SIDE_COUNT];
        this.roofPreviews = new ShipPreviewStruct[EngineSetting.SHIP_SIDE_COUNT];
        this.wallPreviews = new ShipPreviewStruct[ShipWall.VALUES.length];
        this.openingPreviews = new ShipPreviewStruct[ShipOpening.VALUES.length];
        this.climbPreviews = new ShipPreviewStruct[ShipClimb.VALUES.length];

        // Scratch
        this.layout = new ShipLayoutStruct(new ShipOctantGridStruct());
    }

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
        this.shipwrightCompileBranch = get(ShipwrightCompileBranch.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Build \\

    // Every picture built and meshed, apart from any ship
    void buildPreviews() {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;
        int mirrored = EngineSetting.SHIPWRIGHT_OPTION_MIRRORED;
        int oneSide = EngineSetting.SHIPWRIGHT_OPTION_ONE_SIDE;

        deckPreviews[mirrored] = buildDeck();
        deckPreviews[oneSide] = buildPorch();
        waistPreviews[mirrored] = buildWaist(span);
        waistPreviews[oneSide] = buildWaist(span / 2);
        roofPreviews[mirrored] = buildRoof(span);
        roofPreviews[oneSide] = buildRoof(span / 2);
        this.mastPreview = buildMast();
        this.helmPreview = buildHelm();
        this.capstanPreview = buildCapstan();

        for (int i = 0; i < ShipWall.VALUES.length; i++)
            wallPreviews[i] = buildWall(ShipWall.VALUES[i]);

        openingPreviews[ShipOpening.WINDOW.ordinal()] = buildWindow();
        openingPreviews[ShipOpening.HATCH.ordinal()] = buildFloorOpening(ShipOpening.HATCH);
        openingPreviews[ShipOpening.GRATE.ordinal()] = buildFloorOpening(ShipOpening.GRATE);
        openingPreviews[ShipOpening.GLASS.ordinal()] = buildFloorOpening(ShipOpening.GLASS);
        openingPreviews[ShipOpening.PLATFORM.ordinal()] = buildPlatform();
        climbPreviews[ShipClimb.STAIR.ordinal()] = buildStair();
        climbPreviews[ShipClimb.LADDER.ordinal()] = buildLadder();
        climbPreviews[ShipClimb.BOARDING.ordinal()] = buildBoardingLadder();
    }

    // A square of floor
    private ShipPreviewStruct buildDeck() {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, span);

        return finish();
    }

    // A strip of floor along the tile with a square of it reaching out to one side in the middle
    private ShipPreviewStruct buildPorch() {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, span / 2);
        fill(ShipPiece.DECKS, 1, 0, span / 2, span - 1, 1, span);

        return finish();
    }

    // A square of floor cut open, a rail round the hole, across the breadth given: the whole square, or one side of it
    private ShipPreviewStruct buildWaist(int breadth) {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, span);
        fill(ShipPiece.RAILS, 0, 1, 0, span, 1 + EngineSetting.SHIP_WAIST_RAIL_HEIGHT, breadth);
        clear(1, 0, 1, span - 1, 1 + EngineSetting.SHIP_WAIST_RAIL_HEIGHT, breadth - 1);

        return finish();
    }

    // A wall of a kind standing its height on a strip of floor; a lip a sill between two posts of planking
    private ShipPreviewStruct buildWall(ShipWall wall) {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;
        int height = wall.getHeight();

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, 1);
        fill(wall.getPiece(), 0, 1, 0, span, 1 + height, 1);

        if (wall == ShipWall.LIP) {
            fill(ShipPiece.HULL, 0, 1, 0, 1, EngineSetting.SHIP_DECK_HEIGHT, 1);
            fill(ShipPiece.HULL, span - 1, 1, 0, span, EngineSetting.SHIP_DECK_HEIGHT, 1);
        }

        return finish();
    }

    // Panes glazed across the middle of a wall, reaching out to the wall either side
    private ShipPreviewStruct buildWindow() {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;
        int sill = EngineSetting.SHIP_WINDOW_SILL;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, 1);
        fill(ShipPiece.WALLS, 0, 1, 0, span, EngineSetting.SHIP_DECK_HEIGHT, 1);
        fill(ShipPiece.PANES, 1, sill, 0, span - 1, sill + EngineSetting.SHIP_WINDOW_HEIGHT, 1);

        return finish();
    }

    // A square of floor railed round, a roof laid over it across the breadth given: the whole square, or one side of
    // it
    private ShipPreviewStruct buildRoof(int breadth) {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;
        int top = 1 + EngineSetting.SHIP_RAILING_HEIGHT;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, span);
        fill(ShipPiece.RAILS, 0, 1, 0, span, top, span);
        clear(1, 1, 1, span - 1, top, span - 1);

        for (int z = 0; z < breadth; z++)
            for (int x = 0; x < span; x++)
                layout.addRoof(x, 0, z);

        shipwrightFittingBranch.addRoofs(layout);

        return finish();
    }

    // A square of floor with a hatch, a grate or a glass floor laid over the hole in its middle
    private ShipPreviewStruct buildFloorOpening(ShipOpening opening) {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span, 1, span);
        clear(1, 0, 1, span - 1, 1, span - 1);

        if (opening.isCover())
            layout.addCover(opening, 1, 0, 1, span - 1, span - 1, 0);
        else
            layout.addHatch(1, 0, 1, span - 1, span - 1, 0);

        shipwrightFittingBranch.addHatchLids(layout);
        shipwrightFittingBranch.addGrates(layout);
        shipwrightFittingBranch.addGlassFloors(layout);

        return finish();
    }

    // A platform's bed facing its lidded port in a strip of the side
    private ShipPreviewStruct buildPlatform() {

        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;
        int sill = EngineSetting.SHIP_GUN_PORT_SILL;
        int top = sill + EngineSetting.SHIP_GUN_PORT_HEIGHT;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, span + 1, 1, span - 1);
        fill(ShipPiece.PLATFORMS, 0, 1, 0, span, 2, span - 1);
        fill(ShipPiece.HULL, span, 1, 0, span + 1, EngineSetting.SHIP_DECK_HEIGHT, span - 1);
        clear(span, sill, 0, span + 1, top, span - 1);
        layout.addPort(span, sill, 0, span + 1, top, span - 1, 0);
        shipwrightFittingBranch.addPortLids(layout);

        return finish();
    }

    // A staircase climbing a deck, a step an octant up per octant along
    private ShipPreviewStruct buildStair() {

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, EngineSetting.SHIP_DECK_HEIGHT, 1, EngineSetting.SHIP_STAIR_WIDTH);

        for (int step = 0; step < EngineSetting.SHIP_DECK_HEIGHT; step++)
            fill(ShipPiece.STAIRS, step, 1, 0, step + 1, step + 2, EngineSetting.SHIP_STAIR_WIDTH);

        return finish();
    }

    // A ladder standing on a square of floor, climbing a deck
    private ShipPreviewStruct buildLadder() {

        int size = EngineSetting.SHIP_HATCH_OCTANTS;

        begin();
        fill(ShipPiece.DECKS, 0, 0, 0, size, 1, size);
        layout.addLadder(0, 1, 0, size, 1 + EngineSetting.SHIP_DECK_HEIGHT, size, 0);
        shipwrightFittingBranch.addLadders(layout);

        return finish();
    }

    // A rope ladder hung from a strip of deck, its step bridging it to the deck
    private ShipPreviewStruct buildBoardingLadder() {

        int width = EngineSetting.SHIP_STAIR_WIDTH;
        int deck = EngineSetting.SHIP_DECK_HEIGHT - EngineSetting.SHIP_LADDER_HANDHOLD - 1;

        begin();
        fill(ShipPiece.DECKS, 0, deck, 0, width, deck + 1, 1);
        layout.addBoardingLadder(0, 0, 0, width, EngineSetting.SHIP_DECK_HEIGHT, 1, 1);
        shipwrightFittingBranch.addBoardingLadders(layout);

        return finish();
    }

    // A mast with a yard across its head and a sail under it
    private ShipPreviewStruct buildMast() {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int height = EngineSetting.SHIPWRIGHT_PREVIEW_MAST_OCTANTS * octant;
        int span = EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS * octant;
        int fore = EngineSetting.SHIP_MAST_OCTANTS * octant;
        int yard = height - EngineSetting.SHIP_MAST_HEAD * octant;

        begin();

        VehiclePartStruct mast = createPart(EngineSetting.SHIP_MAST_NAME_SINGLE, VehiclePartRole.MAST,
                EngineSetting.SHIP_TEXTURE_SPAR);
        VehiclePartStruct yardPart = createPart(
                EngineSetting.SHIP_MAST_NAME_SINGLE + EngineSetting.SHIP_PART_YARD_SUFFIX, VehiclePartRole.STRUCTURE,
                EngineSetting.SHIP_TEXTURE_SPAR);
        VehiclePartStruct sail = createPart(EngineSetting.SHIP_SAIL_TIER_NAMES[0], VehiclePartRole.STRUCTURE,
                EngineSetting.SHIP_TEXTURE_SAIL);

        mast.addBox(0, 0, span - octant, fore, height, span + octant);
        yardPart.addBox(fore, yard, 0, fore + octant, yard + octant, span * 2);
        sail.addBox(fore + EngineSetting.SHIP_SAIL_OFFSET, EngineSetting.SHIP_SAIL_CLEARANCE * octant, octant,
                fore + EngineSetting.SHIP_SAIL_OFFSET + 1, yard, span * 2 - octant);

        layout.addPart(mast);
        layout.addPart(yardPart);
        layout.addPart(sail);

        return finish();
    }

    private ShipPreviewStruct buildHelm() {

        int origin = EngineSetting.SHIPWRIGHT_PREVIEW_ORIGIN_SUB_VOXELS;

        begin();
        shipwrightFittingBranch.buildHelm(layout.getParts(), origin, 0, origin);

        return finish();
    }

    private ShipPreviewStruct buildCapstan() {

        int origin = EngineSetting.SHIPWRIGHT_PREVIEW_ORIGIN_SUB_VOXELS;

        begin();
        shipwrightFittingBranch.buildCapstan(layout.getParts(), origin, 0, origin);

        return finish();
    }

    // Utility \\

    // The scratch layout and its grid cleared for the next picture
    private void begin() {

        int size = EngineSetting.SHIPWRIGHT_PREVIEW_MAST_OCTANTS + EngineSetting.SHIPWRIGHT_PREVIEW_SPAN_OCTANTS;

        layout.clear(0);
        layout.getGrid().reset(size, size, size);
    }

    // The grid's pieces merged into parts as a ship's are, then every part the picture holds meshed at rest
    private ShipPreviewStruct finish() {

        shipwrightCompileBranch.mergePieces(layout);

        ObjectArrayList<VehiclePartStruct> parts = new ObjectArrayList<>(layout.getParts());

        return new ShipPreviewStruct(parts, vehicleManager.buildRestingMeshes(parts));
    }

    private void fill(ShipPiece piece, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = minY; y < maxY; y++)
            for (int z = minZ; z < maxZ; z++)
                for (int x = minX; x < maxX; x++)
                    grid.set(x, y, z, piece);
    }

    private void clear(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = minY; y < maxY; y++)
            for (int z = minZ; z < maxZ; z++)
                for (int x = minX; x < maxX; x++)
                    grid.clear(x, y, z);
    }

    private VehiclePartStruct createPart(String partName, VehiclePartRole role, String textureName) {
        return new VehiclePartStruct(
                partName, role, textureName, EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
    }

    // Accessible \\

    // The picture of a tool's option, null for one an item pictures
    ShipPreviewStruct getPreview(ShipwrightTool tool, int option) {
        return switch (tool) {
            case DECK -> deckPreviews[option];
            case WAIST -> waistPreviews[option];
            case ROOF -> roofPreviews[option];
            case WALLS -> wallPreviews[option];
            case OPENINGS -> openingPreviews[option];
            case STAIRS -> climbPreviews[option];
            case MASTS -> mastPreview;
            case FURNITURE -> option == EngineSetting.SHIPWRIGHT_ITEM_HELM ? helmPreview
                    : option == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN ? capstanPreview : null;
        };
    }
}
