package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.shipwrightpipeline.util.ShipRopeUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightRigBranch extends BranchPackage {

    /*
     * Works out a blueprint's spars, canvas and rope. The bowsprit is worked
     * out from the bow alone on every hull long enough to carry one: it runs
     * forward from just inboard of the stem over the deck there, rising an
     * octant every few, as long as the hull's length sets. The player only
     * steps a mast at its place on the centre line; everything else about it
     * follows from the hull. It is stepped on the lowest deck drawn under
     * both its octants, never below a deck drawn shorter than the rest, and
     * its head stands over the lowest deck any mast rises from as high as the
     * hull's length and beam let it, the middle mast tallest and each a share
     * lower the farther along the line of masts it stands, so a mast stepped
     * through a raised deck is no taller for it. Its
     * sails follow from that height: the course clears the deck by a man's
     * height and each sail above stands a fixed pitch over the last under its
     * own yard, as many as fit under the mast's head, each narrower than the
     * one below, the course as wide as the hull is there. A mast tall enough
     * to carry its course, topsail and topgallant gets a crow's nest, a
     * railed platform over the topgallant that lifts every sail above it
     * clear. Every mast is rigged so each sail and the nest can be reached:
     * its shrouds step down and out either side as ratlines the crew climb,
     * close in under the nest's rim, level with its floor, where its rail is
     * left open, or under the mast's head where it has none, and spreading
     * wider toward the foot, which lands on a channel standing out from the
     * hull at the deck's edge; over a nest a
     * ladder runs up the after face of the mast from the nest to the head,
     * past every yard above it; a stay runs
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
    private ShipwrightFittingBranch shipwrightFittingBranch;

    // Scratch — main thread only
    private IntArrayList ladderScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.ladderScratch = new IntArrayList();
    }

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
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
        VehiclePartStruct channels = createPart(
                EngineSetting.SHIP_PART_CHANNELS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_HULL);
        ObjectArrayList<VehiclePartStruct> canvas = new ObjectArrayList<>();

        int base = resolveHeadBase(layout, masts);

        for (int i = 0; i < masts.size(); i++) {

            ShipMastStruct mast = masts.get(i);
            int deck = resolveDeckLayer(layout, mast);

            if (deck == EngineSetting.SHIP_LEVEL_NONE) {
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_MAST_NO_HULL);
                layout.addMast(0, 0, false);
                continue;
            }

            int foot = resolveFootLayer(blueprint, mast);

            if (isBlocked(layout, mast, foot, deck))
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_MAST_BLOCKED);

            String mastName = resolveMastName(masts.size(), i);
            int height = resolveMastHeight(layout, i, masts.size(), deck, base);
            boolean nest = hasCrowsNest(height);
            int tiers = resolveTierCount(height, nest);

            layout.addMast(height, tiers, nest);
            layout.addPart(buildMast(layout, mast, mastName, foot, deck, height));
            addCanvas(layout, mast, mastName, deck, tiers, nest, canvas);
            canvas.add(buildShrouds(layout, mast, mastName, deck, height, tiers, nest, channels));

            if (nest) {
                addTop(layout, mast, deck, height, tiers, tops);
                addMastLadder(layout, mast, mastName, deck, height, tiers, canvas);
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

        if (!channels.isEmpty())
            layout.addPart(channels);
    }

    // A mast from its step on the floor of its foot to its head, pivoting about its centre at its step
    private VehiclePartStruct buildMast(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int foot,
            int deck,
            int height) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int x = layout.getOriginX() + mast.getX();
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        VehiclePartStruct part = new VehiclePartStruct(
                mastName,
                VehiclePartRole.MAST,
                EngineSetting.SHIP_TEXTURE_SPAR,
                EngineSetting.SHIP_LINK_NONE,
                new Vector3(
                        (x + EngineSetting.SHIP_MAST_OCTANTS * 0.5f) * octant * scale,
                        (foot + 1) * octant * scale,
                        center * octant * scale),
                EngineSetting.AXIS_Y,
                0f);

        part.addBox(
                x * octant,
                (foot + 1) * octant,
                (center - 1) * octant,
                (x + EngineSetting.SHIP_MAST_OCTANTS) * octant,
                resolveMastTop(height, deck),
                (center + 1) * octant);

        return part;
    }

    // True when a stair or a wall stands anywhere a mast rises through, from its foot to its deck
    private boolean isBlocked(ShipLayoutStruct layout, ShipMastStruct mast, int foot, int deck) {

        ShipOctantGridStruct grid = layout.getGrid();
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int startX = layout.getOriginX() + mast.getX();

        for (int y = foot + 1; y <= deck + EngineSetting.SHIP_DECKHOUSE_HEIGHT; y++)
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

    // A railed platform round the mast over the topsail, its rail left open either side where the shrouds come up to
    // it
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

    // Either side, netting stepping down and out from the crow's nest's rim, level with its floor so a climber walks
    // onto it, or from the mast's head where it has none, within reach of every yard below it: close in under its
    // head and spreading wider toward its foot, which lands on the outer edge of a channel standing out from the hull
    // at the deck's edge on its own side, however far either side reaches
    private VehiclePartStruct buildShrouds(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int height,
            int tiers,
            boolean nest,
            VehiclePartStruct channels) {

        VehiclePartStruct shrouds = createPart(
                mastName + EngineSetting.SHIP_PART_SHROUDS_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_RATLINES);
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int top = nest
                ? resolveNestFloor(height, deck, tiers) + octant
                : resolveMastTop(height, deck) - EngineSetting.SHIP_MAST_HEAD * octant;
        int middleX = resolveShroudMiddle(layout, mast);

        for (int side = 0; side < EngineSetting.SHIP_SIDE_COUNT; side++)
            addShroudSide(layout, mast, deck, nest, top, middleX, side, shrouds, channels);

        return shrouds;
    }

    // One side's netting from its head down and out to the deck's edge on that side, and the channel under its foot
    private void addShroudSide(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            int deck,
            boolean nest,
            int top,
            int middleX,
            int side,
            VehiclePartStruct shrouds,
            VehiclePartStruct channels) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        boolean starboard = side == EngineSetting.SHIP_SIDE_STARBOARD;
        int deckHalf = layout.getSection(mast.getX(), deck, side);
        int edgeHalf = Math.max(1, deckHalf) - 1;
        int edge = shipwrightHullBranch.findColumnTop(layout, mast.getX(),
                starboard ? ShipCellUtility.toStarboard(edgeHalf) : ShipCellUtility.toPort(edgeHalf));
        int bottom = (edge + 1 + EngineSetting.SHIP_BULWARK_HEIGHT) * octant;

        if (deckHalf == 0 || top <= bottom)
            return;

        int inner = nest ? 1 + EngineSetting.SHIP_CROWS_NEST_REACH : 1;
        int steps = Math.max(1, deckHalf - inner);
        int minX = middleX - EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int maxX = middleX + EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int innerReach = inner * octant;
        int outerReach = deckHalf * octant + EngineSetting.SHIP_CHANNEL_REACH_SUB_VOXELS;
        int sheet = EngineSetting.SHIP_SHEET_BREADTH * octant;
        int middleZ = EngineSetting.SHIP_SHEET_HALF_BREADTH * octant;
        int outward = starboard ? 1 : -1;

        for (int step = 0; step < steps; step++) {

            int high = top - (top - bottom) * step / steps;
            int low = top - (top - bottom) * (step + 1) / steps;
            int near = resolveShroudReach(innerReach, outerReach, step, steps);
            int far = resolveShroudReach(innerReach, outerReach, step + 1, steps);

            addRatline(shrouds, minX, maxX, low, high, middleZ + near * outward,
                    Math.clamp(middleZ + far * outward, 0, sheet));
        }

        if (starboard)
            addChannel(channels, minX, maxX, bottom, middleZ + deckHalf * octant,
                    Math.min(sheet, middleZ + outerReach));
        else
            addChannel(channels, minX, maxX, bottom, Math.max(0, middleZ - outerReach), middleZ - deckHalf * octant);
    }

    // How far from the centre line a shroud reaches a step down from its head, in sub-voxels: little at first and
    // more with every step, so the netting hangs close in under its head and spreads toward its foot
    private int resolveShroudReach(int innerReach, int outerReach, int step, int steps) {

        float share = (float) Math.pow((float) step / steps, EngineSetting.SHIP_SHROUD_CURVE);

        return innerReach + Math.round((outerReach - innerReach) * share);
    }

    // A plank standing out from the hull's side under a shroud's foot, its top level with the foot
    private void addChannel(VehiclePartStruct channels, int minX, int maxX, int top, int minZ, int maxZ) {

        if (maxZ > minZ)
            channels.addBox(minX, top - EngineSetting.SHIP_CHANNEL_THICKNESS_SUB_VOXELS, minZ, maxX, top, maxZ);
    }

    // A ladder up the after face of the mast from the crow's nest's floor to the mast's head, facing the mast so a
    // climber steps onto it from the nest and reaches every yard over it
    private void addMastLadder(
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int height,
            int tiers,
            ObjectArrayList<VehiclePartStruct> canvas) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int x = layout.getOriginX() + mast.getX();
        int bottom = resolveNestFloor(height, deck, tiers) / octant + 1;
        int top = resolveMastTop(height, deck) / octant - EngineSetting.SHIP_MAST_HEAD;

        if (top <= bottom)
            return;

        VehiclePartStruct ladder = createPart(
                mastName + EngineSetting.SHIP_PART_MAST_LADDER_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_RUNGS);
        VehiclePartStruct rails = createPart(
                mastName + EngineSetting.SHIP_PART_MAST_LADDER_RAILS_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_SPAR);

        ladderScratch.clear();
        ladderScratch.add(x - 1);
        ladderScratch.add(bottom);
        ladderScratch.add(center - 1);
        ladderScratch.add(x);
        ladderScratch.add(top);
        ladderScratch.add(center + 1);
        ladderScratch.add(0);
        shipwrightFittingBranch.addLadder(ladderScratch, 0, ladder, rails);

        canvas.add(rails);
        canvas.add(ladder);
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

    // The layer of the floor a mast is stepped on: the lowest deck drawn under both its octants
    private int resolveFootLayer(ShipBlueprintStruct blueprint, ShipMastStruct mast) {

        int deck = Math.max(blueprint.findLowestDeck(mast.getX()),
                blueprint.findLowestDeck(mast.getX() + EngineSetting.SHIP_MAST_OCTANTS - 1));

        return Math.max(0, deck) * EngineSetting.SHIP_DECK_HEIGHT;
    }

    // The layer every mast's head is measured up from: the lowest deck any mast rises from
    private int resolveHeadBase(ShipLayoutStruct layout, ObjectArrayList<ShipMastStruct> masts) {

        int base = Integer.MAX_VALUE;

        for (int i = 0; i < masts.size(); i++) {

            int deck = resolveDeckLayer(layout, masts.get(i));

            if (deck != EngineSetting.SHIP_LEVEL_NONE)
                base = Math.min(base, deck);
        }

        return base == Integer.MAX_VALUE ? 0 : base;
    }

    // How many octants a mast rises above its deck: its head as high over the base as the hull's length and its beam
    // let a mast stand, the middle of the masts from the stern forward the tallest and each a share lower the
    // farther along the line from it, never shorter than a mast may be over its own deck
    private int resolveMastHeight(ShipLayoutStruct layout, int mastIndex, int mastCount, int deck, int base) {

        int length = resolveHullLength(layout);
        int beam = 0;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            beam = Math.max(beam, shipwrightHullBranch.findWidestSection(layout, x) * 2);

        float tallest = Math.min(length * EngineSetting.SHIP_MAST_HEIGHT_SHARE,
                beam * EngineSetting.SHIP_MAST_BEAM_SHARE);
        float middle = (mastCount - 1) * 0.5f;
        float offset = Math.abs(mastIndex - middle) / Math.max(1f, mastCount * 0.5f);
        float share = 1f - EngineSetting.SHIP_MAST_END_TAPER * Math.min(1f, offset);

        return Math.max(EngineSetting.SHIP_MAST_MIN_HEIGHT,
                Math.min(EngineSetting.SHIP_MAST_MAX_HEIGHT, base + Math.round(tallest * share) - deck));
    }

    // True when a mast stands tall enough to carry the SHIP_CROWS_NEST_TIERS sails under a crow's nest, the nest the
    // crew climbs to for every sail above it
    private boolean hasCrowsNest(int height) {
        return resolveTierCount(height, true) >= EngineSetting.SHIP_CROWS_NEST_TIERS;
    }

    // How many sails a mast carries: every tier that fits between the deck clearance and its head, a crow's nest
    // lifting every tier above it
    private int resolveTierCount(int height, boolean nest) {

        int room = height - EngineSetting.SHIP_SAIL_CLEARANCE - EngineSetting.SHIP_MAST_HEAD
                - (nest ? EngineSetting.SHIP_CROWS_NEST_GAP : 0);

        return Math.min(EngineSetting.SHIP_SAIL_TIER_NAMES.length,
                Math.max(0, room / EngineSetting.SHIP_SAIL_TIER_PITCH));
    }

    // The sub-voxel height a tier's sail hangs down to, a crow's nest lifting every tier above it
    private int resolveTierBase(boolean nest, int deck, int tier) {

        int gap = nest && tier >= EngineSetting.SHIP_CROWS_NEST_TIERS ? EngineSetting.SHIP_CROWS_NEST_GAP : 0;

        return (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE + tier * EngineSetting.SHIP_SAIL_TIER_PITCH + gap)
                * EngineSetting.SHIP_OCTANT_SUB_VOXELS;
    }

    // The sub-voxel height of the crow's nest floor, over the topgallant's yard, below the mast's head however short
    private int resolveNestFloor(int height, int deck, int tiers) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int floor = (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE
                + Math.min(tiers, EngineSetting.SHIP_CROWS_NEST_TIERS) * EngineSetting.SHIP_SAIL_TIER_PITCH) * octant;

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
