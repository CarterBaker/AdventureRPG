package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightRigBranch extends BranchPackage {

    /*
     * Works out a blueprint's spars, canvas and rope. The bowsprit is worked
     * out from the bow alone: it runs forward from just inboard of the stem
     * over the deck there, rising an octant every few, as long as the hull's
     * length sets. Every mast steps on the keel at its place on the centre
     * line and rises its height above the deck it passes through. Its sails
     * follow from that height: the course clears the deck by a man's height
     * and each sail above stands a fixed pitch over the last under its own
     * yard, as many as fit under the mast's head, each narrower than the one
     * below, the course as wide as the hull is there. A crow's nest is a railed
     * platform at the head of the lower mast, and lifts every sail above it
     * clear. A rigged mast's shrouds step down from its nest, or its head, to
     * the deck edge either side as ratlines the crew climb, two rope rails
     * with a rung at every step, and a stay runs
     * forward and down from its head to the foot of the next mast forward, or
     * to the bowsprit's tip. Masts are named from the bow aft. Spars, canvas
     * and rope are laid as boxes of their own in sub-voxels, rope and canvas a
     * single sub-voxel thick, so the rig stays light however tall it stands.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;

    // Base \\

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
    }

    // Bowsprit \\

    // How many octants the bowsprit reaches forward of the stem, none for a ship with no hull at its bow
    int resolveBowspritLength(ShipBlueprintStruct blueprint) {

        if (findStem(blueprint) == EngineSetting.INDEX_NOT_FOUND)
            return 0;

        return Math.max(EngineSetting.SHIP_BOWSPRIT_MIN_LENGTH, Math.min(EngineSetting.SHIP_BOWSPRIT_MAX_LENGTH,
                Math.round(blueprint.getLength() * EngineSetting.SHIP_BOWSPRIT_LENGTH_SHARE)));
    }

    // Steps forward and up from inboard of the stem, a square spar an octant pair across, its tip kept for the stays
    void addBowsprit(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        int stem = findStem(blueprint);

        if (stem == EngineSetting.INDEX_NOT_FOUND)
            return;

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = blueprint.getHalfBreadth();
        int deck = shipwrightHullBranch.findColumnTop(blueprint, stem, center);
        int startX = layout.getOriginX() + stem + 1 - EngineSetting.SHIP_BOWSPRIT_INBOARD;
        int endX = layout.getOriginX() + stem + 1 + resolveBowspritLength(blueprint);
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

    // The foremost octant along the length the hull reaches on the centre line, INDEX_NOT_FOUND when none
    private int findStem(ShipBlueprintStruct blueprint) {

        for (int x = blueprint.getLength() - 1; x >= 0; x--)
            if (shipwrightHullBranch.findColumnTop(blueprint, x, blueprint.getHalfBreadth())
                    != EngineSetting.SHIP_LEVEL_NONE)
                return x;

        return EngineSetting.INDEX_NOT_FOUND;
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
            int deck = resolveDeckLayer(blueprint, mast);

            if (deck == EngineSetting.SHIP_LEVEL_NONE) {
                layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_MAST_NO_HULL);
                layout.addMastSailCount(0);
                continue;
            }

            String mastName = resolveMastName(masts.size(), i);
            int tiers = resolveTierCount(mast);

            layout.addPart(buildMast(blueprint, layout, mast, mastName, deck));
            layout.addMastSailCount(tiers);
            addCanvas(blueprint, layout, mast, mastName, deck, tiers, canvas);

            if (mast.hasCrowsNest())
                addTop(blueprint, layout, mast, deck, tiers, tops);

            if (!mast.isRigged())
                continue;

            canvas.add(buildShrouds(blueprint, layout, mast, mastName, deck, tiers));
            addStay(blueprint, layout, i, deck, stays);
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
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = blueprint.getHalfBreadth();
        int keel = shipwrightHullBranch.findKeel(blueprint, mast.getX(), center);
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
                resolveMastTop(mast, deck),
                (center + 1) * octant);

        return part;
    }

    // Each tier's yard on the mast's forward face and its sail hanging beneath it, a sub-voxel thick
    private void addCanvas(
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int tiers,
            ObjectArrayList<VehiclePartStruct> canvas) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = blueprint.getHalfBreadth() * octant;
        int fore = (layout.getOriginX() + mast.getX() + EngineSetting.SHIP_MAST_OCTANTS) * octant;
        float span = resolveHalfBreadth(blueprint, mast.getX()) * octant * EngineSetting.SHIP_SAIL_SPAN_SHARE;

        for (int tier = 0; tier < tiers; tier++) {

            String tierName = mastName + EngineSetting.SHIP_SAIL_TIER_NAMES[tier];
            String yardName = tierName + EngineSetting.SHIP_PART_YARD_SUFFIX;
            int base = resolveTierBase(mast, deck, tier);
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

    // A railed platform round the mast at the head of the lower mast
    private void addTop(
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            int deck,
            int tiers,
            VehiclePartStruct tops) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int reach = EngineSetting.SHIP_CROWS_NEST_REACH;
        int center = blueprint.getHalfBreadth();
        int x = layout.getOriginX() + mast.getX();
        int floor = resolveNestFloor(mast, deck, tiers);
        int minX = (x - reach) * octant;
        int maxX = (x + EngineSetting.SHIP_MAST_OCTANTS + reach) * octant;
        int minZ = (center - 1 - reach) * octant;
        int maxZ = (center + 1 + reach) * octant;
        int railTop = floor + octant + EngineSetting.SHIP_CROWS_NEST_RAIL;

        tops.addBox(minX, floor, minZ, maxX, floor + octant, maxZ);
        tops.addBox(minX, floor + octant, minZ, maxX, railTop, minZ + 1);
        tops.addBox(minX, floor + octant, maxZ - 1, maxX, railTop, maxZ);
        tops.addBox(minX, floor + octant, minZ + 1, minX + 1, railTop, maxZ - 1);
        tops.addBox(maxX - 1, floor + octant, minZ + 1, maxX, railTop, maxZ - 1);
    }

    // Shrouds \\

    // Either side, a ladder stepping down and out from the nest or the mast's head to the deck edge
    private VehiclePartStruct buildShrouds(
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            ShipMastStruct mast,
            String mastName,
            int deck,
            int tiers) {

        VehiclePartStruct shrouds = createPart(
                mastName + EngineSetting.SHIP_PART_SHROUDS_SUFFIX, VehiclePartRole.LADDER,
                EngineSetting.SHIP_TEXTURE_ROPE);
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = blueprint.getHalfBreadth();
        int deckHalf = resolveDeckHalfBreadth(blueprint, mast.getX(), deck);
        int steps = deckHalf - 1;

        if (steps < 1)
            return shrouds;

        int edge = shipwrightHullBranch.findColumnTop(blueprint, mast.getX(), center + deckHalf - 1);
        int top = mast.hasCrowsNest()
                ? resolveNestFloor(mast, deck, tiers)
                : resolveMastTop(mast, deck) - EngineSetting.SHIP_MAST_HEAD * octant;
        int bottom = (edge + 1 + blueprint.getRailHeight()) * octant;
        int fall = top - bottom;

        if (fall <= 0)
            return shrouds;

        int middleX = (layout.getOriginX() + mast.getX()) * octant + EngineSetting.SHIP_MAST_OCTANTS * octant / 2;
        int minX = middleX - EngineSetting.SHIP_SHROUD_HALF_SPAN;
        int maxX = middleX + EngineSetting.SHIP_SHROUD_HALF_SPAN;

        for (int step = 0; step < steps; step++) {

            int high = top - fall * step / steps;
            int low = top - fall * (step + 1) / steps;
            int starboard = (center + 1 + step) * octant;
            int port = (center - 1 - step) * octant;

            addRatline(shrouds, minX, maxX, low, high, starboard, starboard + octant, starboard);
            addRatline(shrouds, minX, maxX, low, high, port - octant, port, port - 1);
        }

        return shrouds;
    }

    // One step of a ratline: its two rope rails falling at the shroud's face and the rung it is stepped down at
    private void addRatline(
            VehiclePartStruct shrouds,
            int minX,
            int maxX,
            int low,
            int high,
            int rungMinZ,
            int rungMaxZ,
            int railZ) {

        shrouds.addBox(minX, low, railZ, minX + 1, high, railZ + 1);
        shrouds.addBox(maxX - 1, low, railZ, maxX, high, railZ + 1);
        shrouds.addBox(minX, low, rungMinZ, maxX, low + 1, rungMaxZ);
    }

    // Stays \\

    // From a mast's head forward and down to the foot of the next mast forward, or to the bowsprit's tip
    private void addStay(
            ShipBlueprintStruct blueprint,
            ShipLayoutStruct layout,
            int mastIndex,
            int deck,
            VehiclePartStruct stays) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        ObjectArrayList<ShipMastStruct> masts = blueprint.getMasts();
        ShipMastStruct mast = masts.get(mastIndex);
        int startX = (layout.getOriginX() + mast.getX() + EngineSetting.SHIP_MAST_OCTANTS) * octant;
        int top = resolveMastTop(mast, deck) - EngineSetting.SHIP_MAST_HEAD * octant;
        int endX;
        int bottom;

        if (mastIndex + 1 < masts.size()) {

            ShipMastStruct forward = masts.get(mastIndex + 1);
            int forwardDeck = resolveDeckLayer(blueprint, forward);

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

        int steps = (endX - startX) / octant;
        int fall = top - bottom;
        int z = blueprint.getHalfBreadth() * octant;

        if (steps < 1 || fall <= 0)
            return;

        for (int step = 0; step < steps; step++) {

            int x = startX + step * octant;
            int high = top - fall * step / steps;
            int low = top - fall * (step + 1) / steps;

            stays.addBox(x, high - 1, z, x + octant, high, z + 1);
            stays.addBox(x + octant - 1, low, z, x + octant, high, z + 1);
        }
    }

    // Mast \\

    // The layer of the deck a mast rises from, SHIP_LEVEL_NONE when no hull stands under it
    int resolveDeckLayer(ShipBlueprintStruct blueprint, ShipMastStruct mast) {
        return shipwrightHullBranch.findColumnTop(blueprint, mast.getX(), blueprint.getHalfBreadth());
    }

    // How many sails a mast carries: every tier that fits between the deck clearance and its head
    int resolveTierCount(ShipMastStruct mast) {

        int room = mast.getHeight() - EngineSetting.SHIP_SAIL_CLEARANCE - EngineSetting.SHIP_MAST_HEAD;
        int tiers = Math.max(0, room / EngineSetting.SHIP_SAIL_TIER_PITCH);

        if (mast.hasCrowsNest() && tiers > 1)
            tiers = Math.max(1, (room - EngineSetting.SHIP_CROWS_NEST_GAP) / EngineSetting.SHIP_SAIL_TIER_PITCH);

        return Math.min(EngineSetting.SHIP_SAIL_TIER_NAMES.length, tiers);
    }

    // The sub-voxel height a tier's sail hangs down to, the crow's nest lifting every tier above the course
    private int resolveTierBase(ShipMastStruct mast, int deck, int tier) {

        int gap = mast.hasCrowsNest() && tier > 0 ? EngineSetting.SHIP_CROWS_NEST_GAP : 0;

        return (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE + tier * EngineSetting.SHIP_SAIL_TIER_PITCH + gap)
                * EngineSetting.SHIP_OCTANT_SUB_VOXELS;
    }

    // The sub-voxel height of the crow's nest floor, over the course yard, below the mast's head however short
    private int resolveNestFloor(ShipMastStruct mast, int deck, int tiers) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int floor = (deck + 1 + EngineSetting.SHIP_SAIL_CLEARANCE
                + Math.min(tiers, 1) * EngineSetting.SHIP_SAIL_TIER_PITCH) * octant;

        return Math.min(floor, resolveMastTop(mast, deck) - (EngineSetting.SHIP_MAST_HEAD + 1) * octant);
    }

    private int resolveMastTop(ShipMastStruct mast, int deck) {
        return (deck + 1 + mast.getHeight()) * EngineSetting.SHIP_OCTANT_SUB_VOXELS;
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

    // Breadth \\

    // The most octants the hull reaches out from the centre line at an octant along the length, at any layer
    private int resolveHalfBreadth(ShipBlueprintStruct blueprint, int x) {

        int reach = 0;

        for (int y = 0; y < blueprint.getHeight(); y++)
            for (int halfZ = reach; halfZ < blueprint.getHalfBreadth(); halfZ++)
                if (blueprint.isHull(x, y, halfZ))
                    reach = halfZ + 1;

        return reach;
    }

    // How far out the deck reaches from the centre line at a mast
    private int resolveDeckHalfBreadth(ShipBlueprintStruct blueprint, int x, int deck) {

        int reach = 0;

        for (int halfZ = 0; halfZ < blueprint.getHalfBreadth(); halfZ++)
            if (blueprint.isHull(x, deck, halfZ))
                reach = halfZ + 1;

        return reach;
    }

    // Utility \\

    private VehiclePartStruct createPart(String partName, VehiclePartRole role, String textureName) {
        return new VehiclePartStruct(
                partName, role, textureName, EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
    }
}
