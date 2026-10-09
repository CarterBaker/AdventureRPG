package application.bootstrap.worldpipeline.treemanager;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import application.bootstrap.worldpipeline.tree.TreeBranchStruct;
import application.bootstrap.worldpipeline.tree.TreeData;
import application.bootstrap.worldpipeline.tree.TreeForm;
import application.bootstrap.worldpipeline.tree.TreeGrowthStruct;
import application.bootstrap.worldpipeline.tree.TreeLeafStruct;
import application.bootstrap.worldpipeline.tree.TreeLogStruct;
import application.bootstrap.worldpipeline.tree.TreeTrunkStruct;
import application.bootstrap.worldpipeline.tree.TreeWoodStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeArpgUtility extends EngineUtility {

    /*
     * The single definition of the tree species format: display name, form,
     * and the optional trunk, branches, leaves, wood and growth objects, every
     * field falling back to its EngineSetting default. Names the species
     * points at — textures, its tool type, its log and seed items — are
     * resolved through the functions the caller hands in, so the parsed
     * species is complete and every lookup happens once, at load. A malformed
     * field throws a catchable InternalException naming the species, so
     * TreeBuilder fails the boot on it while a live rebuild from the editor
     * reports it and keeps the species it already had.
     */

    // Parse \\

    static TreeData parse(
            String treeName,
            short treeID,
            ArpgObjectStruct treeArpg,
            Function<String, float[]> resolveTextureCorner,
            ToIntFunction<String> resolveToolTypeID,
            ToIntFunction<String> resolveItemID) {

        String displayName = parseDisplayName(treeArpg, treeName);
        TreeForm form = parseForm(treeArpg, treeName);
        TreeTrunkStruct trunk = parseTrunk(objectOrEmpty(treeArpg, "trunk"), treeName);
        TreeBranchStruct branches = parseBranches(objectOrEmpty(treeArpg, "branches"), treeName);
        TreeLeafStruct leaves = parseLeaves(objectOrEmpty(treeArpg, "leaves"), treeName);
        TreeWoodStruct wood = parseWood(objectOrEmpty(treeArpg, "wood"), treeName, resolveToolTypeID, resolveItemID);
        TreeGrowthStruct growth = parseGrowth(objectOrEmpty(treeArpg, "growth"), treeName, resolveItemID);

        return new TreeData(
                treeName, displayName, treeID, RegistryUtility.toNameSeed(treeName),
                form, trunk, branches, leaves, wood, growth,
                computeReach(form, trunk, branches, leaves),
                computeHeightReach(trunk, leaves),
                resolvePartCorners(wood, leaves, resolveTextureCorner),
                resolvePartColors(wood, leaves),
                resolvePartOpaque(),
                resolvePartSway());
    }

    // Identity \\

    private static String parseDisplayName(ArpgObjectStruct treeArpg, String treeName) {

        String displayName = ArpgUtility.getString(treeArpg, "display_name", "").trim();

        if (displayName.isEmpty())
            return treeName.substring(treeName.lastIndexOf('/') + 1);

        return displayName;
    }

    private static TreeForm parseForm(ArpgObjectStruct treeArpg, String treeName) {

        String raw = ArpgUtility.getString(treeArpg, "form", EngineSetting.TREE_DEFAULT_FORM);

        for (TreeForm form : TreeForm.values())
            if (form.name().equalsIgnoreCase(raw))
                return form;

        throw fail(treeName, "has unknown \"form\" \"" + raw + "\".");
    }

    // Trunk \\

    private static TreeTrunkStruct parseTrunk(ArpgObjectStruct trunkArpg, String treeName) {

        float[] height = parseRange(
                trunkArpg, "height_blocks",
                EngineSetting.TREE_DEFAULT_MIN_HEIGHT_BLOCKS, EngineSetting.TREE_DEFAULT_MAX_HEIGHT_BLOCKS,
                treeName, "trunk");

        float radiusBlocks = ArpgUtility.getFloat(
                trunkArpg, "radius_blocks", EngineSetting.TREE_DEFAULT_TRUNK_RADIUS_BLOCKS);
        float taper = ArpgUtility.getFloat(trunkArpg, "taper", EngineSetting.TREE_DEFAULT_TRUNK_TAPER);
        float flare = ArpgUtility.getFloat(trunkArpg, "flare", EngineSetting.TREE_DEFAULT_TRUNK_FLARE);
        float leader = ArpgUtility.getFloat(trunkArpg, "leader", EngineSetting.TREE_DEFAULT_TRUNK_LEADER);
        float leanDegrees = ArpgUtility.getFloat(
                trunkArpg, "lean_degrees", EngineSetting.TREE_DEFAULT_TRUNK_LEAN_DEGREES);
        float wobble = ArpgUtility.getFloat(trunkArpg, "wobble", EngineSetting.TREE_DEFAULT_TRUNK_WOBBLE);
        int stems = ArpgUtility.getInt(trunkArpg, "stems", EngineSetting.TREE_DEFAULT_TRUNK_STEMS);
        float stemSpreadBlocks = ArpgUtility.getFloat(
                trunkArpg, "stem_spread_blocks", EngineSetting.TREE_DEFAULT_TRUNK_STEM_SPREAD_BLOCKS);

        if (height[0] <= 0f || height[1] > EngineSetting.TREE_MAX_HEIGHT_BLOCKS)
            throw fail(treeName, "\"trunk\" \"height_blocks\" must lie between 0 and "
                    + EngineSetting.TREE_MAX_HEIGHT_BLOCKS + " blocks.");

        if (radiusBlocks <= 0f || radiusBlocks > EngineSetting.TREE_MAX_TRUNK_RADIUS_BLOCKS)
            throw fail(treeName, "\"trunk\" \"radius_blocks\" must lie between 0 and "
                    + EngineSetting.TREE_MAX_TRUNK_RADIUS_BLOCKS + " blocks.");

        requireUnit(taper, treeName, "trunk", "taper");
        requireUnit(leader, treeName, "trunk", "leader");

        if (flare < 0f)
            throw fail(treeName, "\"trunk\" \"flare\" must not be negative.");

        if (wobble < 0f)
            throw fail(treeName, "\"trunk\" \"wobble\" must not be negative.");

        if (stems < 1 || stems > EngineSetting.TREE_MAX_STEMS)
            throw fail(treeName, "\"trunk\" \"stems\" must lie between 1 and " + EngineSetting.TREE_MAX_STEMS + ".");

        if (stemSpreadBlocks < 0f)
            throw fail(treeName, "\"trunk\" \"stem_spread_blocks\" must not be negative.");

        return new TreeTrunkStruct(
                height[0], height[1], radiusBlocks, taper, flare, leader,
                leanDegrees, wobble, stems, stemSpreadBlocks);
    }

    // Branches \\

    private static TreeBranchStruct parseBranches(ArpgObjectStruct branchArpg, String treeName) {

        float crownStart = ArpgUtility.getFloat(
                branchArpg, "crown_start", EngineSetting.TREE_DEFAULT_CROWN_START);
        float[] count = parseRange(
                branchArpg, "count",
                EngineSetting.TREE_DEFAULT_MIN_BRANCH_COUNT, EngineSetting.TREE_DEFAULT_MAX_BRANCH_COUNT,
                treeName, "branches");
        float angleDegrees = ArpgUtility.getFloat(
                branchArpg, "angle_degrees", EngineSetting.TREE_DEFAULT_BRANCH_ANGLE_DEGREES);
        float length = ArpgUtility.getFloat(branchArpg, "length", EngineSetting.TREE_DEFAULT_BRANCH_LENGTH);
        int levels = ArpgUtility.getInt(branchArpg, "levels", EngineSetting.TREE_DEFAULT_BRANCH_LEVELS);
        int children = ArpgUtility.getInt(branchArpg, "children", EngineSetting.TREE_DEFAULT_BRANCH_CHILDREN);
        float childLength = ArpgUtility.getFloat(
                branchArpg, "child_length", EngineSetting.TREE_DEFAULT_BRANCH_CHILD_LENGTH);
        float childAngleDegrees = ArpgUtility.getFloat(
                branchArpg, "child_angle_degrees", EngineSetting.TREE_DEFAULT_BRANCH_CHILD_ANGLE_DEGREES);
        float radiusRatio = ArpgUtility.getFloat(
                branchArpg, "radius_ratio", EngineSetting.TREE_DEFAULT_BRANCH_RADIUS_RATIO);
        float gravity = ArpgUtility.getFloat(branchArpg, "gravity", EngineSetting.TREE_DEFAULT_BRANCH_GRAVITY);
        float twistDegrees = ArpgUtility.getFloat(
                branchArpg, "twist_degrees", EngineSetting.TREE_DEFAULT_BRANCH_TWIST_DEGREES);
        int segments = ArpgUtility.getInt(branchArpg, "segments", EngineSetting.TREE_DEFAULT_BRANCH_SEGMENTS);

        requireUnit(crownStart, treeName, "branches", "crown_start");
        requireUnit(childLength, treeName, "branches", "child_length");
        requireUnit(radiusRatio, treeName, "branches", "radius_ratio");

        if (count[0] < 0f || count[1] > EngineSetting.TREE_MAX_BRANCH_COUNT)
            throw fail(treeName, "\"branches\" \"count\" must lie between 0 and "
                    + EngineSetting.TREE_MAX_BRANCH_COUNT + ".");

        if (length < 0f)
            throw fail(treeName, "\"branches\" \"length\" must not be negative.");

        if (levels < 0 || levels > EngineSetting.TREE_MAX_BRANCH_LEVELS)
            throw fail(treeName, "\"branches\" \"levels\" must lie between 0 and "
                    + EngineSetting.TREE_MAX_BRANCH_LEVELS + ".");

        if (children < 0 || children > EngineSetting.TREE_MAX_BRANCH_CHILDREN)
            throw fail(treeName, "\"branches\" \"children\" must lie between 0 and "
                    + EngineSetting.TREE_MAX_BRANCH_CHILDREN + ".");

        if (segments < 1 || segments > EngineSetting.TREE_MAX_BRANCH_SEGMENTS)
            throw fail(treeName, "\"branches\" \"segments\" must lie between 1 and "
                    + EngineSetting.TREE_MAX_BRANCH_SEGMENTS + ".");

        return new TreeBranchStruct(
                crownStart, (int) count[0], (int) count[1], angleDegrees, length, levels, children,
                childLength, childAngleDegrees, radiusRatio, gravity, twistDegrees, segments);
    }

    // Leaves \\

    private static TreeLeafStruct parseLeaves(ArpgObjectStruct leafArpg, String treeName) {

        float radiusBlocks = ArpgUtility.getFloat(
                leafArpg, "radius_blocks", EngineSetting.TREE_DEFAULT_LEAF_RADIUS_BLOCKS);
        float squash = ArpgUtility.getFloat(leafArpg, "squash", EngineSetting.TREE_DEFAULT_LEAF_SQUASH);
        float density = ArpgUtility.getFloat(leafArpg, "density", EngineSetting.TREE_DEFAULT_LEAF_DENSITY);
        String textureName = ArpgUtility.getString(
                leafArpg, "texture", EngineSetting.TREE_DEFAULT_LEAF_TEXTURE);
        int color = parseColor(leafArpg, "color", EngineSetting.TREE_DEFAULT_LEAF_COLOR, treeName);
        String accentTextureName = ArpgUtility.getString(leafArpg, "accent_texture", "").trim();

        if (accentTextureName.isEmpty())
            accentTextureName = textureName;
        int accentColor = parseColor(leafArpg, "accent_color", EngineSetting.TREE_DEFAULT_LEAF_COLOR, treeName);
        float accentChance = ArpgUtility.getFloat(leafArpg, "accent_chance", 0f);
        float hangBlocks = ArpgUtility.getFloat(leafArpg, "hang_blocks", 0f);

        if (radiusBlocks < 0f)
            throw fail(treeName, "\"leaves\" \"radius_blocks\" must not be negative.");

        if (squash <= 0f)
            throw fail(treeName, "\"leaves\" \"squash\" must be greater than 0.");

        requireUnit(density, treeName, "leaves", "density");
        requireUnit(accentChance, treeName, "leaves", "accent_chance");

        if (hangBlocks < 0f)
            throw fail(treeName, "\"leaves\" \"hang_blocks\" must not be negative.");

        return new TreeLeafStruct(
                radiusBlocks, squash, density, textureName, color,
                accentTextureName, accentColor, accentChance, hangBlocks);
    }

    // Wood \\

    private static TreeWoodStruct parseWood(
            ArpgObjectStruct woodArpg,
            String treeName,
            ToIntFunction<String> resolveToolTypeID,
            ToIntFunction<String> resolveItemID) {

        String barkTextureName = ArpgUtility.getString(
                woodArpg, "bark_texture", EngineSetting.TREE_DEFAULT_BARK_TEXTURE);
        String woodTextureName = ArpgUtility.getString(
                woodArpg, "wood_texture", EngineSetting.TREE_DEFAULT_WOOD_TEXTURE);
        int barkColor = parseColor(woodArpg, "bark_color", EngineSetting.TREE_DEFAULT_BARK_COLOR, treeName);
        int barkSubVoxels = ArpgUtility.getInt(
                woodArpg, "bark_sub_voxels", EngineSetting.TREE_DEFAULT_BARK_SUB_VOXELS);
        String toolTypeName = ArpgUtility.getString(woodArpg, "tool", EngineSetting.TREE_DEFAULT_TOOL);
        int toolTier = ArpgUtility.getInt(woodArpg, "tool_tier", EngineSetting.DEFAULT_TOOL_TIER);

        if (barkSubVoxels < 1)
            throw fail(treeName, "\"wood\" \"bark_sub_voxels\" must be at least 1.");

        if (toolTier < 0)
            throw fail(treeName, "\"wood\" \"tool_tier\" must not be negative.");

        short toolTypeID = (short) resolveToolTypeID.applyAsInt(toolTypeName);
        ObjectArrayList<TreeLogStruct> logs = parseLogs(woodArpg, treeName, resolveItemID);

        return new TreeWoodStruct(
                barkTextureName, woodTextureName, barkColor, barkSubVoxels,
                toolTypeName, toolTypeID, toolTier, logs);
    }

    // Logs ordered thickest first, so the first a piece of wood is thick enough for is the one it splits into
    private static ObjectArrayList<TreeLogStruct> parseLogs(
            ArpgObjectStruct woodArpg,
            String treeName,
            ToIntFunction<String> resolveItemID) {

        ObjectArrayList<TreeLogStruct> logs = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(woodArpg, "logs"))
            return logs;

        if (woodArpg.getAsArray("logs").size() > EngineSetting.TREE_MAX_LOG_KINDS)
            throw fail(treeName, "\"wood\" \"logs\" may list no more than " + EngineSetting.TREE_MAX_LOG_KINDS
                    + " kinds of log.");

        for (ArpgElementStruct element : woodArpg.getAsArray("logs")) {

            ArpgObjectStruct logArpg = element.getAsObject();
            String itemName = requireString(logArpg, "item", treeName, "wood\" \"logs");
            int minRadiusSubVoxels = ArpgUtility.getInt(logArpg, "min_radius_sub_voxels", 0);
            float lengthBlocks = ArpgUtility.getFloat(
                    logArpg, "length_blocks", EngineSetting.TREE_DEFAULT_LOG_LENGTH_BLOCKS);

            if (minRadiusSubVoxels < 0)
                throw fail(treeName, "log \"" + itemName + "\" has a negative \"min_radius_sub_voxels\".");

            if (lengthBlocks <= 0f)
                throw fail(treeName, "log \"" + itemName + "\" must have a \"length_blocks\" greater than 0.");

            logs.add(new TreeLogStruct(
                    itemName, resolveItemID.applyAsInt(itemName), minRadiusSubVoxels, lengthBlocks));
        }

        logs.sort((first, second) -> Integer.compare(
                second.getMinRadiusSubVoxels(), first.getMinRadiusSubVoxels()));

        return logs;
    }

    // Growth \\

    private static TreeGrowthStruct parseGrowth(
            ArpgObjectStruct growthArpg,
            String treeName,
            ToIntFunction<String> resolveItemID) {

        float days = ArpgUtility.getFloat(growthArpg, "days", EngineSetting.TREE_DEFAULT_GROWTH_DAYS);
        float[] wildAge = parseRange(
                growthArpg, "wild_age",
                EngineSetting.TREE_DEFAULT_WILD_MIN_AGE, EngineSetting.TREE_DEFAULT_WILD_MAX_AGE,
                treeName, "growth");
        String seedItemName = ArpgUtility.getString(growthArpg, "seed", "");
        float seedChance = ArpgUtility.getFloat(growthArpg, "seed_chance", EngineSetting.TREE_DEFAULT_SEED_CHANCE);

        if (days <= 0f)
            throw fail(treeName, "\"growth\" \"days\" must be greater than 0.");

        requireUnit(wildAge[0], treeName, "growth", "wild_age");
        requireUnit(wildAge[1], treeName, "growth", "wild_age");
        requireUnit(seedChance, treeName, "growth", "seed_chance");

        int seedItemID = seedItemName.isEmpty()
                ? EngineSetting.REGISTRY_RESERVED_ID
                : resolveItemID.applyAsInt(seedItemName);

        return new TreeGrowthStruct(days, wildAge[0], wildAge[1], seedItemName, seedItemID, seedChance);
    }

    // Reach \\

    // How far sideways a grown tree can reach from its root — every limb at full length laid out straight
    private static float computeReach(
            TreeForm form,
            TreeTrunkStruct trunk,
            TreeBranchStruct branches,
            TreeLeafStruct leaves) {

        float height = trunk.getMaxHeightBlocks();
        float limb = height * branches.getLength();
        float chain = limb;
        float twig = limb;

        if (form != TreeForm.PALM)
            for (int level = 1; level <= branches.getLevels(); level++) {
                twig *= branches.getChildLength();
                chain += twig;
            }

        float lean = height * (float) Math.sin(Math.toRadians(Math.abs(trunk.getLeanDegrees())))
                + height * trunk.getWobble();

        return lean + trunk.getStemSpreadBlocks() + trunk.getRadiusBlocks() * (1f + trunk.getFlare())
                + chain + leaves.getRadiusBlocks() + EngineSetting.TREE_REACH_MARGIN_BLOCKS;
    }

    // How far above its root a grown tree can reach
    private static float computeHeightReach(TreeTrunkStruct trunk, TreeLeafStruct leaves) {
        return trunk.getMaxHeightBlocks() + leaves.getRadiusBlocks() + EngineSetting.TREE_REACH_MARGIN_BLOCKS;
    }

    // Parts \\

    private static float[] resolvePartCorners(
            TreeWoodStruct wood,
            TreeLeafStruct leaves,
            Function<String, float[]> resolveTextureCorner) {

        float[] corners = new float[EngineSetting.TREE_PART_COUNT * EngineSetting.TREE_PART_CORNER_FLOATS];

        writeCorner(corners, EngineSetting.TREE_PART_BARK, resolveTextureCorner.apply(wood.getBarkTextureName()));
        writeCorner(corners, EngineSetting.TREE_PART_WOOD, resolveTextureCorner.apply(wood.getWoodTextureName()));
        writeCorner(corners, EngineSetting.TREE_PART_LEAF, resolveTextureCorner.apply(leaves.getTextureName()));
        writeCorner(corners, EngineSetting.TREE_PART_ACCENT,
                resolveTextureCorner.apply(leaves.getAccentTextureName()));

        return corners;
    }

    private static void writeCorner(float[] corners, int part, float[] corner) {
        corners[part * EngineSetting.TREE_PART_CORNER_FLOATS] = corner[0];
        corners[part * EngineSetting.TREE_PART_CORNER_FLOATS + 1] = corner[1];
    }

    private static int[] resolvePartColors(TreeWoodStruct wood, TreeLeafStruct leaves) {

        int[] colors = new int[EngineSetting.TREE_PART_COUNT];

        colors[EngineSetting.TREE_PART_BARK] = wood.getBarkColor();
        colors[EngineSetting.TREE_PART_WOOD] = EngineSetting.PACKED_COLOR_WHITE;
        colors[EngineSetting.TREE_PART_LEAF] = leaves.getColor();
        colors[EngineSetting.TREE_PART_ACCENT] = leaves.getAccentColor();

        return colors;
    }

    // Every part hides what lies behind it — leaves are solid foliage, so wood buried in a crown costs nothing
    private static boolean[] resolvePartOpaque() {

        boolean[] opaque = new boolean[EngineSetting.TREE_PART_COUNT];

        Arrays.fill(opaque, true);

        return opaque;
    }

    private static boolean[] resolvePartSway() {

        boolean[] sway = new boolean[EngineSetting.TREE_PART_COUNT];

        sway[EngineSetting.TREE_PART_LEAF] = true;
        sway[EngineSetting.TREE_PART_ACCENT] = true;

        return sway;
    }

    // Values \\

    private static ArpgObjectStruct objectOrEmpty(ArpgObjectStruct arpg, String key) {
        return ArpgUtility.hasObject(arpg, key) ? arpg.getAsObject(key) : new ArpgObjectStruct();
    }

    // A [min, max] pair, or one number standing for both, or the defaults when the field is absent
    private static float[] parseRange(
            ArpgObjectStruct arpg,
            String key,
            float defaultMin,
            float defaultMax,
            String treeName,
            String group) {

        if (!arpg.has(key))
            return new float[] { defaultMin, defaultMax };

        if (ArpgUtility.hasNumber(arpg, key)) {
            float value = arpg.get(key).getAsFloat();
            return new float[] { value, value };
        }

        ArpgArrayStruct range = arpg.getAsArray(key);

        if (range.size() != 2)
            throw fail(treeName, "\"" + group + "\" \"" + key + "\" must be one number or a [min, max] pair.");

        float min = range.get(0).getAsFloat();
        float max = range.get(1).getAsFloat();

        if (min > max)
            throw fail(treeName, "\"" + group + "\" \"" + key + "\" has its minimum above its maximum.");

        return new float[] { min, max };
    }

    // A "#RRGGBB" color packed as 0xRRGGBB
    private static int parseColor(ArpgObjectStruct arpg, String key, String fallback, String treeName) {

        String raw = ArpgUtility.getString(arpg, key, fallback);
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() != EngineSetting.TREE_COLOR_HEX_DIGITS)
            throw fail(treeName, "has invalid \"" + key + "\" \"" + raw + "\" — expected \"#RRGGBB\".");

        try {
            return Integer.parseInt(hex, EngineSetting.ARPG_TEXT_HEX_RADIX);
        } catch (NumberFormatException e) {
            throw new InternalException("Tree \"" + treeName + "\" has invalid \"" + key + "\" \"" + raw
                    + "\" — not valid hex.", e);
        }
    }

    private static String requireString(ArpgObjectStruct arpg, String key, String treeName, String field) {

        if (!ArpgUtility.hasString(arpg, key))
            throw fail(treeName, "has an entry in \"" + field + "\" without a \"" + key + "\".");

        return arpg.get(key).getAsString();
    }

    private static void requireUnit(float value, String treeName, String group, String key) {
        if (value < 0f || value > 1f)
            throw fail(treeName, "\"" + group + "\" \"" + key + "\" must lie between 0 and 1.");
    }

    private static InternalException fail(String treeName, String message) {
        return new InternalException("Tree \"" + treeName + "\" " + message);
    }
}
