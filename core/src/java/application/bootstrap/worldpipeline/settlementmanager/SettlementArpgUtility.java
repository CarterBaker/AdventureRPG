package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.layout.LayoutRangeStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.settlement.SettlementData;
import application.bootstrap.worldpipeline.settlement.SettlementRoleStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SettlementArpgUtility extends EngineUtility {

    /*
     * The single definition of the settlement format. A settlement names its
     * "weight" among free sites, its "radius_blocks" and the "ground" it may
     * stand on, the "center" role it grows around, its "streets", its "lots"
     * with their setback, lot limit, fill role and leading "roles", how many
     * "overpasses" bridge its streets, an optional "wall" radius and tower
     * spacing, the "outposts" its trails lead to, and the "link" road it is
     * joined to its neighbours by. Every range is two whole numbers, least
     * and most, and its reach is held within what one settlement cell's
     * neighbours look across. A malformed field throws a catchable
     * InternalException naming the settlement, so SettlementBuilder fails the
     * boot on it while a live rebuild from the editor reports it and keeps
     * the settlement it already had.
     */

    // Parse \\

    static SettlementData parse(String settlementName, short settlementID, ArpgObjectStruct arpg) {

        float weight = ArpgUtility.getFloat(arpg, "weight", 1f);
        int radiusBlocks = ArpgUtility.validateInt(arpg, "radius_blocks");

        ArpgObjectStruct groundArpg = resolveGroup(arpg, "ground");
        int minGroundHeightBlocks = ArpgUtility.getInt(
                groundArpg, "min_height_blocks", EngineSetting.DEFAULT_SETTLEMENT_MIN_GROUND_HEIGHT_BLOCKS);
        int maxGroundHeightBlocks = ArpgUtility.getInt(
                groundArpg, "max_height_blocks", EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS);
        int maxSlopeBlocks = ArpgUtility.getInt(
                groundArpg, "max_slope_blocks", EngineSetting.DEFAULT_SETTLEMENT_MAX_SLOPE_BLOCKS);

        ArpgObjectStruct streetsArpg = ArpgUtility.validateObject(arpg, "streets");
        LayoutRangeStruct spokes = parseRange(streetsArpg, "spokes", settlementName);
        LayoutRangeStruct spokeLengthBlocks = parseRange(streetsArpg, "spoke_length_blocks", settlementName);
        LayoutRangeStruct branches = parseOptionalRange(streetsArpg, "branches", settlementName);
        LayoutRangeStruct branchLengthBlocks = parseOptionalRange(streetsArpg, "branch_length_blocks", settlementName);
        float bend = ArpgUtility.getFloat(streetsArpg, "bend", EngineSetting.DEFAULT_SETTLEMENT_STREET_BEND);

        ArpgObjectStruct lotsArpg = ArpgUtility.validateObject(arpg, "lots");
        int setbackBlocks = ArpgUtility.getInt(
                lotsArpg, "setback_blocks", EngineSetting.DEFAULT_SETTLEMENT_SETBACK_BLOCKS);
        int maxLots = ArpgUtility.validateInt(lotsArpg, "max_lots");
        String fillRole = ArpgUtility.validateString(lotsArpg, "fill");
        ObjectArrayList<SettlementRoleStruct> roles = parseRoles(lotsArpg, settlementName);

        ArpgObjectStruct wallArpg = resolveGroup(arpg, "wall");
        int wallRadiusBlocks = ArpgUtility.getInt(wallArpg, "radius_blocks", 0);
        int towerSpacingBlocks = ArpgUtility.getInt(
                wallArpg, "tower_spacing_blocks", EngineSetting.DEFAULT_SETTLEMENT_TOWER_SPACING_BLOCKS);

        ArpgObjectStruct outpostsArpg = resolveGroup(arpg, "outposts");
        LayoutRangeStruct outposts = parseOptionalRange(outpostsArpg, "count", settlementName);
        LayoutRangeStruct trailLengthBlocks = parseOptionalRange(outpostsArpg, "trail_length_blocks", settlementName);
        float outpostChance = ArpgUtility.getFloat(
                outpostsArpg, "chance", EngineSetting.DEFAULT_SETTLEMENT_OUTPOST_CHANCE);

        int reachBlocks = Math.max(radiusBlocks, EngineSetting.SETTLEMENT_RING_MAX_RADIUS_BLOCKS
                + spokeLengthBlocks.getMax() + branchLengthBlocks.getMax() + trailLengthBlocks.getMax()
                + EngineSetting.SETTLEMENT_REACH_MARGIN_BLOCKS);

        validate(settlementName, weight, radiusBlocks, spokes, maxLots, wallRadiusBlocks, towerSpacingBlocks,
                spokeLengthBlocks, outpostChance, reachBlocks);

        return new SettlementData(
                settlementName,
                settlementID,
                RegistryUtility.toNameSeed(settlementName),
                ArpgUtility.getString(arpg, "display_name", settlementName),
                weight,
                radiusBlocks,
                minGroundHeightBlocks,
                maxGroundHeightBlocks,
                maxSlopeBlocks,
                ArpgUtility.getString(arpg, "center", ""),
                spokes,
                spokeLengthBlocks,
                branches,
                branchLengthBlocks,
                bend,
                setbackBlocks,
                maxLots,
                fillRole,
                roles,
                parseOptionalRange(arpg, "overpasses", settlementName),
                wallRadiusBlocks,
                towerSpacingBlocks,
                outposts,
                trailLengthBlocks,
                outpostChance,
                ArpgUtility.getEnum(arpg, "link", LayoutRoadKind.class, LayoutRoadKind.PATH),
                reachBlocks);
    }

    // Roles \\

    private static ObjectArrayList<SettlementRoleStruct> parseRoles(ArpgObjectStruct lotsArpg, String settlementName) {

        ObjectArrayList<SettlementRoleStruct> roles = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(lotsArpg, "roles"))
            return roles;

        ArpgArrayStruct rolesArpg = lotsArpg.getAsArray("roles");

        for (int i = 0; i < rolesArpg.size(); i++) {

            ArpgObjectStruct roleArpg = rolesArpg.get(i).getAsObject();

            roles.add(new SettlementRoleStruct(
                    ArpgUtility.validateString(roleArpg, "role"),
                    parseRange(roleArpg, "count", settlementName)));
        }

        return roles;
    }

    // Ranges \\

    private static LayoutRangeStruct parseRange(ArpgObjectStruct arpg, String key, String settlementName) {

        ArpgArrayStruct rangeArpg = ArpgUtility.validateArray(arpg, key, 2);
        int min = rangeArpg.get(0).getAsInt();
        int max = rangeArpg.get(1).getAsInt();

        if (min < 0 || max < min)
            throw fail(settlementName, "\"" + key + "\" [" + min + ", " + max
                    + "] — a range runs from at least 0 up to no less than its least.");

        return new LayoutRangeStruct(min, max);
    }

    // A range that is none at all when it is not given
    private static LayoutRangeStruct parseOptionalRange(ArpgObjectStruct arpg, String key, String settlementName) {
        return ArpgUtility.hasArray(arpg, key) ? parseRange(arpg, key, settlementName) : new LayoutRangeStruct(0, 0);
    }

    // An optional group, read as empty so every field in it falls back to its default
    private static ArpgObjectStruct resolveGroup(ArpgObjectStruct arpg, String key) {
        return ArpgUtility.hasObject(arpg, key) ? arpg.getAsObject(key) : new ArpgObjectStruct();
    }

    // Validation \\

    private static void validate(
            String settlementName,
            float weight,
            int radiusBlocks,
            LayoutRangeStruct spokes,
            int maxLots,
            int wallRadiusBlocks,
            int towerSpacingBlocks,
            LayoutRangeStruct spokeLengthBlocks,
            float outpostChance,
            int reachBlocks) {

        if (weight < 0f || radiusBlocks <= 0 || maxLots < 0)
            throw fail(settlementName, "needs a weight and lot limit of at least 0 and a radius greater than 0.");

        if (spokes.getMin() < 1)
            throw fail(settlementName, "must lay at least one street from its ring.");

        if (wallRadiusBlocks > 0 && (wallRadiusBlocks >= spokeLengthBlocks.getMin() || towerSpacingBlocks < 1))
            throw fail(settlementName, "wall radius " + wallRadiusBlocks
                    + " must stay inside its shortest street (" + spokeLengthBlocks.getMin()
                    + ") so every street passes out through a gate, with towers at least 1 block apart.");

        if (outpostChance < 0f || outpostChance > 1f)
            throw fail(settlementName, "outpost \"chance\" must be from 0 to 1.");

        if (reachBlocks > EngineSetting.SETTLEMENT_MAX_REACH_BLOCKS)
            throw fail(settlementName, "reaches " + reachBlocks
                    + " blocks from its centre through its ring, streets, lanes and trails — no settlement may "
                    + "reach past " + EngineSetting.SETTLEMENT_MAX_REACH_BLOCKS + ".");
    }

    private static InternalException fail(String settlementName, String message) {
        return new InternalException("Settlement \"" + settlementName + "\" " + message);
    }
}
