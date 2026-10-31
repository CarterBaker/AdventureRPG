package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.util.ShipRopeUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightRigBranch extends BranchPackage {

    /*
     * Works out a blueprint's spars, canvas and rope. The bowsprit is worked
     * out from the bow alone on every hull long enough to carry one: it runs
     * forward from just inboard of the stem over the deck there, rising an
     * octant every few, as long as the hull's length sets. The player only
     * steps a mast at its place on the centre line; everything else about it
     * follows from the hull. It stands on the keel and rises above the
     * highest deck it passes through as tall as the hull's length and beam
     * let it, shorter the farther from the middle of the hull it stands. Its
     * sails follow from that height: the course clears the deck by a man's
     * height and each sail above stands a fixed pitch over the last under its
     * own yard, as many as fit under the mast's head, each narrower than the
     * one below, the course as wide as the hull is there. A mast tall enough
     * to carry two sails over one gets a crow's nest, a railed platform at
     * the head of the lower mast that lifts every sail above it clear. Every
     * mast is rigged so each sail and the nest can be reached: its shrouds
     * step down to the deck edge either side as ratlines the crew climb, from
     * the nest's rim level with its floor, where its rail is left open, or
     * from the mast's head where it has none, and over a nest the topmast
     * shrouds climb on from its rim in to the mast's head; a stay runs
     * forward and down from its head to the foot of the next mast forward, or
     * to the bowsprit's tip. Masts are named from the bow aft. Spars and
     * canvas are laid as boxes of their own in sub-voxels, canvas a single
     * sub-voxel thick; rope is never built of sub-voxels but laid as flat
     * quads its texture is drawn on, the ratlines a sheet of netting a step
     * at a time and every stay two crossed ribbons, so the rig stays light
     * however tall it stands.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;

    // Base \\

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
    }

    // Bowsprit \\

    // How many octants the bowsprit reaches forward of the stem, as the hull's length sets
    private int resolveBowspritLength(ShipLayoutStruct layout) {
        return Math.max(EngineSetting.SHIP_BOWSPRIT_MIN_LENGTH, Math.min(EngineSetting.SHIP_BOWSPRIT_MAX_LENGTH,
                Math.round(resolveHullLength(layout) * EngineSetting.SHIP_BOWSPRIT_LENGTH_SHARE)));
    }

    // Steps forward and up from inboard of the stem, a square spar an octant pair across, its tip kept for the stays;
    // a hull too short for one carries none
    void addBowsprit(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        int stem = findStem(layout);

        if (stem == EngineSetting.INDEX_NOT_FOUND
                || resolveHullLength(layout) < EngineSetting.SHIP_BOWSPRIT_MIN_HULL_LENGTH)
            return;

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int deck = shipwrightHullBranch.findColumnTop(layout, stem, center);
        int startX = layout.getOriginX() + stem + 1 - EngineSetting.SHIP_BOWSPRIT_INBOARD;
        int endX = layout.getOriginX() + stem + 1 + resolveBowspritLength(layout);
        VehiclePartStruct bowsprit = createPart(
                EngineSetting.SHIP_PART_BOWSPRIT, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_SPAR);
        int rise = 0;

        for (int x = startX; x < endX; x += EngineSetting.SHIP_BOWSPRIT_RUN) {

            int y = deck + 1 + rise;

            bowsprit.addBox(
                    x * octant,
                    y * octant,
                    (center - 1) * octant,
                    Math.min(x + EngineSetting.SHIP_BOWSPRIT_RUN, endX) * octant,
                    (y + EngineSetting.SHIP_BOWSPRIT_DEPTH) * octant,
                    (center + 1) * octant);
            rise++;
        }

        layout.addPart(bowsprit);
        layout.setBowspritTip(endX * octant, (deck + rise + EngineSetting.SHIP_BOWSPRIT_DEPTH) * octant);
    }

    // The foremost octant along the sheet the hull reaches on the centre line, INDEX_NOT_FOUND when none
    private int findStem(ShipLayoutStruct layout) {

        for (int x = EngineSetting.SHIP_SHEET_LENGTH - 1; x >= 0; x--)
            if (shipwrightHullBranch.findColumnTop(layout, x, EngineSetting.SHIP_SHEET_HALF_BREADTH)
                    != EngineSetting.SHIP_LEVEL_NONE)
                return x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The first octant along the sheet the hull reaches on the centre line, INDEX_NOT_FOUND when none
    private int findStern(ShipLayoutStruct layout) {

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            if (shipwrightHullBranch.findColumnTop(layout, x, EngineSetting.SHIP_SHEET_HALF_BREADTH)
                    != EngineSetting.SHIP_LEVEL_NONE)
                return x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // How many octants the hull runs from its stern to its stem
    private int resolveHullLength(ShipLayoutStruct layout) {

        int stern = findStern(layout);

        return stern == EngineSetting.INDEX_NOT_FOUND ? 0 : findStem(layout) - stern + 1;
    }

    // Rig \\

    void addRig(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        ObjectArrayList<ShipMastStruct> masts = blueprint.getMasts();
        VehiclePartStruct tops = createPart(
                EngineSetting.SHIP_PART_TOPS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK);
        VehiclePartStruct stays = createPart(
                EngineSetting.SHIP_PART_STAYS, VehiclePartRole.RIGGING, EngineSetting.SHIP_TEXTURE_ROPE);
        ObjectArrayList<VehiclePartStruct> canvas = new ObjectArrayList<>();

        for (int i = 0; i < masts.size(); i++) {

            ShipMastStruct mast = masts.get(i);
            int deck = resolveDeckLayer(layout, mast);

            if (deck == EngineSetting.SHIP_LEVEL_NONE) {
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_MAST_NO_HULL);
                layout.addMast(0, 0, false);
                continue;
            }

            if (isBlocked(layout, mast, deck))
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_MAST_BLOCKED);

            String mastName = resolveMastName(masts.size(), i);
            int height = resolveMastHeight(layout, mast);
            boolean nest = hasCrowsNest(height);
            int tiers = resolveTierCount(height, nest);

            layout.addMast(height, tiers, nest);
            layout.addPart(buildMast(layout, mast, mastName, deck, height));
            addCanvas(layout, mast, mastName, deck, tiers, nest, canvas);
            canvas.add(buildShrouds(layout, mast, mastName, deck, height, tiers, nest));

            if (nest) {
                addTop(layout, mast, deck, height, tiers, tops);
                canvas.add(buildTopmastShrouds(layout, mast, mastName, deck, height, tiers));
            }

            addStay(blueprint, layout, i, deck, height, stays);
        }

        if (!tops.isEmpty())
            layout.addPart(tops);

        for (int i = 0; i < canvas.size(); i++)
            if (!canvas.get(i).isEmpty())
                layout.addPart(canvas.get(i));

        if (!stays.isEmpty())
            layout.addPart(stays);
    }

    // A mast from its step on the keel to its head, pivoting about its centre on the deck it rises from
    private VehiclePartStruct buildMast(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int height) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int keel = shipwrightHullBranch.findKeel(layout, mast.getX(), center);
        int x = layout.getOriginX() + mast.getX();
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        VehiclePartStruct part = new VehiclePartStruct(
                mastName,
                VehiclePartRole.MAST,
                EngineSetting.SHIP_TEXTURE_SPAR,
                EngineSetting.SHIP_LINK_NONE,
                new Vector3(
                        (x + EngineSetting.SHIP_MAST_OCTANTS * 0.5f) * octant * scale,
                        (keel + 1) * octant * scale,
                        center * octant * scale),
                EngineSetting.AXIS_Y,
                0f);

        part.addBox(
                x * octant,
                (keel + 1) * octant,
                (center - 1) * octant,
                (x + EngineSetting.SHIP_MAST_OCTANTS) * octant,
                resolveMastTop(height, deck),
                (center + 1) * octant);

        return part;
    }

    // True when a stair or a wall stands anywhere a mast rises through, from its keel to its deck
    private boolean isBlocked(ShipLayoutStruct layout, ShipMastStruct mast, int deck) {

        ShipOctantGridStruct grid = layout.getGrid();
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int startX = layout.getOriginX() + mast.getX();

        for (int y = 0; y <= deck + EngineSetting.SHIP_DECKHOUSE_HEIGHT; y++)
            for (int z = center - 1; z <= center; z++)
                for (int x = startX; x < startX + EngineSetting.SHIP_MAST_OCTANTS; x++) {

                    ShipPiece piece = grid.get(x, y, z);

                    if (piece == ShipPiece.STAIRS || piece == ShipPiece.WALLS)
                        return true;
                }

        return false;
    }

    // Each tier's yard on the mast's forward face and its sail hanging beneath it, a sub-voxel thick
    private void addCanvas(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int tiers,
            boolean nest,
            ObjectArrayList<VehiclePartStruct> canvas) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH * octant;
        int fore = (layout.getOriginX() + mast.getX() + EngineSetting.SHIP_MAST_OCTANTS) * octant;
        float span = shipwrightHullBranch.findWidestSection(layout, mast.getX()) * octant
                * EngineSetting.SHIP_SAIL_SPAN_SHARE;

        for (int tier = 0; tier < tiers; tier++) {

            String tierName = mastName + EngineSetting.SHIP_SAIL_TIER_NAMES[tier];
            String yardName = tierName + EngineSetting.SHIP_PART_YARD_SUFFIX;
            int base = resolveTierBase(nest, deck, tier);
            int head = base + (EngineSetting.SHIP_SAIL_TIER_PITCH - 1) * octant;
            int sailHalf = Math.max(EngineSetting.SHIP_SAIL_MIN_HALF_SPAN, Math.min(center,
                    Math.round(span * (float) Math.pow(EngineSetting.SHIP_SAIL_TAPER, tier))));
            int yardHalf = Math.min(center, sailHalf + EngineSetting.SHIP_YARD_OVERHANG);

            VehiclePartStruct yard = new VehiclePartStruct(
                    yardName, VehiclePartRole.YARD, EngineSetting.SHIP_TEXTURE_SPAR, mastName,
                    new Vector3(), EngineSetting.AXIS_Y, 0f);
            yard.addBox(fore, head, center - yardHalf, fore + octant, head + octant, center + yardHalf);

            VehiclePartStruct sail = new VehiclePartStruct(
                    tierName, VehiclePartRole.SAIL, EngineSetting.SHIP_TEXTURE_SAIL, yardName,
                    new Vector3(), EngineSetting.AXIS_Y, 0f);
            int sailX = fore + EngineSetting.SHIP_SAIL_OFFSET;
            sail.addBox(sailX, base, center - sailHalf, sailX + 1, head, center + sailHalf);

            canvas.add(yard);
            canvas.add(sail);
        }
    }

    // A railed platform round the mast at the head of the lower mast, its rail left open either side where the
    // shrouds come up to it
    private void addTop(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            int deck,
            int height,
            int tiers,
            VehiclePartStruct tops) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int reach = EngineSetting.SHIP_CROWS_NEST_REACH;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int x = layout.getOriginX() + mast.getX();
        int floor = resolveNestFloor(height, deck, tiers);
        int minX = (x - reach) * octant;
        int maxX = (x + EngineSetting.SHIP_MAST_OCTANTS + reach) * octant;
        int minZ = (center - 1 - reach) * octant;
        int maxZ = (center + 1 + reach) * octant;
        int railBottom = floor + octant;
        int railTop = railBottom + EngineSetting.SHIP_CROWS_NEST_RAIL;
        int gapMinX = resolveShroudMiddle(layout, mast) - EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int gapMaxX = resolveShroudMiddle(layout, mast) + EngineSetting.SHIP_SHROUD_HALF_SPAN;

        tops.addBox(minX, floor, minZ, maxX, railBottom, maxZ);
        tops.addBox(minX, railBottom, minZ, gapMinX, railTop, minZ + 1);
        tops.addBox(gapMaxX, railBottom, minZ, maxX, railTop, minZ + 1);
        tops.addBox(minX, railBottom, maxZ - 1, gapMinX, railTop, maxZ);
        tops.addBox(gapMaxX, railBottom, maxZ - 1, maxX, railTop, maxZ);
        tops.addBox(minX, railBottom, minZ + 1, minX + 1, railTop, maxZ - 1);
        tops.addBox(maxX - 1, railBottom, minZ + 1, maxX, railTop, maxZ - 1);
    }

    // Shrouds \\

    // Either side, netting stepping down and out to the deck edge from the crow's nest's rim, level with its floor so
    // a climber walks onto it, or from the mast's head where it has none, within reach of every yard below it
    private VehiclePartStruct buildShrouds(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int height,
            int tiers,
            boolean nest) {

        VehiclePartStruct shrouds = createPart(
                mastName + EngineSetting.SHIP_PART_SHROUDS_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_RATLINES);
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int deckHalf = layout.getSection(mast.getX(), deck);
        int inner = nest ? 1 + EngineSetting.SHIP_CROWS_NEST_REACH : 1;
        int edge = shipwrightHullBranch.findColumnTop(layout, mast.getX(), center + Math.max(1, deckHalf) - 1);
        int top = nest
                ? resolveNestFloor(height, deck, tiers) + octant
                : resolveMastTop(height, deck) - EngineSetting.SHIP_MAST_HEAD * octant;
        int bottom = (edge + 1 + EngineSetting.SHIP_BULWARK_HEIGHT) * octant;
        int run = Math.max(0, deckHalf - inner);
        int steps = Math.max(1, run);

        if (top <= bottom)
            return shrouds;

        int middleX = resolveShroudMiddle(layout, mast);
        int minX = middleX - EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int maxX = middleX + EngineSetting.SHIP_SHROUD_HALF_SPAN;

        for (int step = 0; step < steps; step++) {

            int high = top - (top - bottom) * step / steps;
            int low = top - (top - bottom) * (step + 1) / steps;
            int near = inner + run * step / steps;
            int far = inner + run * (step + 1) / steps;

            addRatline(shrouds, minX, maxX, low, high, (center + near) * octant, (center + far) * octant);
            addRatline(shrouds, minX, maxX, low, high, (center - near) * octant, (center - far) * octant);
        }

        return shrouds;
    }

    // Either side, netting rising from the crow's nest's rim in to the mast's head, within reach of every yard over
    // the nest
    private VehiclePartStruct buildTopmastShrouds(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int height,
            int tiers) {

        VehiclePartStruct shrouds = createPart(
                mastName + EngineSetting.SHIP_PART_TOPMAST_SHROUDS_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_RATLINES);
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int reach = EngineSetting.SHIP_CROWS_NEST_REACH;
        int bottom = resolveNestFloor(height, deck, tiers) + octant;
        int top = resolveMastTop(height, deck) - EngineSetting.SHIP_MAST_HEAD * octant;

        if (top <= bottom)
            return shrouds;

        int middleX = resolveShroudMiddle(layout, mast);
        int minX = middleX - EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int maxX = middleX + EngineSetting.SHIP_SHROUD_HALF_SPAN;

        for (int step = 0; step < reach; step++) {

            int low = bottom + (top - bottom) * step / reach;
            int high = bottom + (top - bottom) * (step + 1) / reach;
            int far = 1 + reach - step;
            int near = far - 1;

            addRatline(shrouds, minX, maxX, low, high, (center + near) * octant, (center + far) * octant);
            addRatline(shrouds, minX, maxX, low, high, (center - near) * octant, (center - far) * octant);
        }

        return shrouds;
    }

    // One step of a shroud's netting: a sheet falling from the inboard edge at its head to the outboard edge at its
    // foot, the ratlines drawn on it, its own climb zone keeping the climber close to it
    private void addRatline(VehiclePartStruct shrouds, int minX, int maxX, int low, int high, int innerZ, int outerZ) {
        shrouds.addQuad(EngineSetting.SUB_VOXEL_FACE_POSITIVE_Z,
                minX, low, outerZ,
                maxX, low, outerZ,
                maxX, high, innerZ,
                minX, high, innerZ);
    }

    // The sub-voxel along the length a mast's shrouds are centred on
    private int resolveShroudMiddle(ShipLayoutStruct layout, ShipMastStruct mast) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;

        return (layout.getOriginX() + mast.getX()) * octant + EngineSetting.SHIP_MAST_OCTANTS * octant / 2;
    }

    // Stays \\

    // From a mast's head forward and down to the foot of the next mast forward, or to the bowsprit's tip
    private void addStay(
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            int mastIndex,
            int deck,
            int height,
            VehiclePartStruct stays) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        ObjectArrayList<ShipMastStruct> masts = blueprint.getMasts();
        ShipMastStruct mast = masts.get(mastIndex);
        int startX = (layout.getOriginX() + mast.getX() + EngineSetting.SHIP_MAST_OCTANTS) * octant;
        int top = resolveMastTop(height, deck) - EngineSetting.SHIP_MAST_HEAD * octant;
        int endX;
        int bottom;

        if (mastIndex + 1 < masts.size()) {

            ShipMastStruct forward = masts.get(mastIndex + 1);
            int forwardDeck = resolveDeckLayer(layout, forward);

            if (forwardDeck == EngineSetting.SHIP_LEVEL_NONE)
                return;

            endX = (layout.getOriginX() + forward.getX()) * octant;
            bottom = (forwardDeck + 1 + EngineSetting.SHIP_STAY_FOOT) * octant;
        } else if (layout.hasBowsprit()) {
            endX = layout.getBowspritTipX();
            bottom = layout.getBowspritTipY();
        } else {
            return;
        }

        int z = EngineSetting.SHIP_SHEET_HALF_BREADTH * octant;

        if (endX <= startX || top <= bottom)
            return;

        ShipRopeUtility.addRope(stays, startX, top, z, endX, bottom, z);
    }

    // Mast \\

    // The layer of the highest deck a mast rises through, SHIP_LEVEL_NONE when no hull stands under it
    private int resolveDeckLayer(ShipLayoutStruct layout, ShipMastStruct mast) {
        return shipwrightHullBranch.findColumnTop(layout, mast.getX(), EngineSetting.SHIP_SHEET_HALF_BREADTH);
    }

    // How many octants a mast rises above its deck: as tall as the hull's length and its beam let it stand, and
    // shorter the farther from the middle of the hull it is stepped
    private int resolveMastHeight(ShipLayoutStruct layout, ShipMastStruct mast) {

        int stern = findStern(layout);
        int length = resolveHullLength(layout);
        int beam = 0;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            beam = Math.max(beam, shipwrightHullBranch.findWidestSection(layout, x) * 2);

        float tallest = Math.min(length * EngineSetting.SHIP_MAST_HEIGHT_SHARE,
                beam * EngineSetting.SHIP_MAST_BEAM_SHARE);
        float middle = stern + length * 0.5f;
        float offset = Math.abs(mast.getX() + EngineSetting.SHIP_MAST_OCTANTS * 0.5f - middle)
                / Math.max(1f, length * 0.5f);
        float share = 1f - EngineSetting.SHIP_MAST_END_TAPER * Math.min(1f, offset);

        return Math.max(EngineSetting.SHIP_MAST_MIN_HEIGHT,
                Math.min(EngineSetting.SHIP_MAST_MAX_HEIGHT, Math.round(tallest * share)));
    }

    // True when a mast stands tall enough to carry SHIP_CROWS_NEST_MIN_SAILS sails over a crow's nest, the nest the
    // crew climbs to for every sail above the course
    private boolean hasCrowsNest(int height) {
        return resolveTierCount(height, true) >= EngineSetting.SHIP_CROWS_NEST_MIN_SAILS;
    }

    // How many sails a mast carries: every tier that fits between the deck clearance and its head, a crow's nest
    // lifting every tier above the course
    private int resolveTierCount(int height, boolean nest) {

        int room = height - EngineSetting.SHIP_SAIL_CLEARANCE - EngineSetting.SHIP_MAST_HEAD
                - (nest ? EngineSetting.SHIP_CROWS_NEST_GAP : 0);

        return Math.min(EngineSetting.SHIP_SAIL_TIER_NAMES.length,
                Math.max(0, room / EngineSetting.SHIP_SAIL_TIER_PITCH));
    }

    // The sub-voxel height a tier's sail hangs down to, a crow's nest lifting every tier above the course
    private int resolveTierBase(boolean nest, int deck, int tier) {

        int gap = nest && tier > 0 ? EngineSetting.SHIP_CROWS_NEST_GAP : 0;

        return (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE + tier * EngineSetting.SHIP_SAIL_TIER_PITCH + gap)
                * EngineSetting.SHIP_OCTANT_SUB_VOXELS;
    }

    // The sub-voxel height of the crow's nest floor, over the course yard, below the mast's head however short
    private int resolveNestFloor(int height, int deck, int tiers) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int floor = (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE
                + Math.min(tiers, 1) * EngineSetting.SHIP_SAIL_TIER_PITCH) * octant;

        return Math.min(floor, resolveMastTop(height, deck) - (EngineSetting.SHIP_MAST_HEAD + 1) * octant);
    }

    private int resolveMastTop(int height, int deck) {
        return (deck + 1 + height) * EngineSetting.SHIP_OCTANT_SUB_VOXELS;
    }

    // Masts are named from the bow aft; a lone mast is the mainmast
    String resolveMastName(int mastCount, int mastIndex) {

        int fromBow = mastCount - 1 - mastIndex;

        if (mastCount == 1)
            return EngineSetting.SHIP_MAST_NAME_SINGLE;

        if (fromBow < EngineSetting.SHIP_MAST_NAMES.length)
            return EngineSetting.SHIP_MAST_NAMES[fromBow];

        return EngineSetting.SHIP_MAST_NAME_PREFIX + (fromBow + 1);
    }

    // Utility \\

    private VehiclePartStruct createPart(String partName, VehiclePartRole role, String textureName) {
        return new VehiclePartStruct(
                partName, role, textureName, EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
    }
}
