package application.bootstrap.worldpipeline.structuremanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.structure.StructureData;
import application.bootstrap.worldpipeline.structure.StructureFixedPlacementStruct;
import application.bootstrap.worldpipeline.structure.StructureFrequencyStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureRulesStruct;
import application.bootstrap.worldpipeline.structure.StructureSurfaceType;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class StructureBuilder extends BuilderPackage {

    /*
     * Parses structure JSON into a StructureData and wraps it in a
     * StructureHandle. Block entries are single "position"s or inclusive
     * "from"/"to" boxes applied in order, so a later entry overwrites an
     * earlier one at the same cell. Every block, biome, and value is resolved
     * and validated here, so a malformed structure fails at boot.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    // Block Accumulation
    private IntArrayList offsetX;
    private IntArrayList offsetY;
    private IntArrayList offsetZ;
    private ShortArrayList blockIDs;
    private ShortArrayList blockOrientations;
    private ObjectArrayList<DynamicGeometryType> blockGeometry;
    private Long2IntOpenHashMap position2BlockIndex;

    // Base \\

    @Override
    protected void create() {

        // Block Accumulation
        this.offsetX = new IntArrayList();
        this.offsetY = new IntArrayList();
        this.offsetZ = new IntArrayList();
        this.blockIDs = new ShortArrayList();
        this.blockOrientations = new ShortArrayList();
        this.blockGeometry = new ObjectArrayList<>();
        this.position2BlockIndex = new Long2IntOpenHashMap();
        this.position2BlockIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    // Build \\

    StructureHandle build(File file, String structureName) {

        short structureID = RegistryUtility.toShortID(structureName);
        JsonObject json = JsonUtility.loadJsonObject(file);

        int[] origin = parseOrigin(json);

        clearBlocks();
        parseBlocks(json, structureName, origin);

        int minOffsetX = min(offsetX);
        int maxOffsetX = max(offsetX);
        int minOffsetZ = min(offsetZ);
        int maxOffsetZ = max(offsetZ);
        int horizontalReachBlocks = Math.max(
                Math.max(-minOffsetX, maxOffsetX),
                Math.max(-minOffsetZ, maxOffsetZ));

        int yOffsetBlocks = JsonUtility.getInt(
                json, "y_offset_blocks", EngineSetting.DEFAULT_STRUCTURE_Y_OFFSET_BLOCKS);

        boolean foundation = json.has("foundation_block");
        short foundationBlockID = foundation
                ? parseFoundationBlockID(json, structureName)
                : EngineSetting.REGISTRY_RESERVED_ID;

        StructureRulesStruct rules = parseRules(json, structureName);
        StructureFrequencyStruct frequency = parseFrequency(json, structureName);
        ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements = parseFixedPlacements(json, structureName);

        StructureData structureData = new StructureData(
                structureName, structureID,
                offsetX.toIntArray(), offsetY.toIntArray(), offsetZ.toIntArray(),
                blockIDs.toShortArray(), blockOrientations.toShortArray(),
                blockGeometry.toArray(new DynamicGeometryType[0]),
                minOffsetX, maxOffsetX, minOffsetZ, maxOffsetZ, horizontalReachBlocks,
                yOffsetBlocks, foundationBlockID, foundation,
                rules, frequency, fixedPlacements);

        StructureHandle structureHandle = create(StructureHandle.class);
        structureHandle.constructor(structureData);

        return structureHandle;
    }

    // Origin Parsing \\

    private int[] parseOrigin(JsonObject json) {

        if (!json.has("origin"))
            return new int[3];

        return parseVector(json, "origin");
    }

    // Block Parsing \\

    private void clearBlocks() {

        offsetX.clear();
        offsetY.clear();
        offsetZ.clear();
        blockIDs.clear();
        blockOrientations.clear();
        blockGeometry.clear();
        position2BlockIndex.clear();
    }

    private void parseBlocks(JsonObject json, String structureName, int[] origin) {

        JsonArray blockArray = JsonUtility.validateArray(json, "blocks");

        for (JsonElement element : blockArray)
            parseBlockEntry(element.getAsJsonObject(), structureName, origin);

        if (blockIDs.isEmpty())
            throwException("Structure \"" + structureName + "\" \"blocks\" must declare at least one block.");
    }

    private void parseBlockEntry(JsonObject entry, String structureName, int[] origin) {

        String blockName = JsonUtility.validateString(entry, "block");
        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);
        short orientation = parseOrientation(entry, blockHandle, structureName);

        boolean hasPosition = entry.has("position");
        boolean hasBox = entry.has("from") || entry.has("to");

        if (hasPosition == hasBox)
            throwException("Structure \"" + structureName + "\" block entry \"" + blockName
                    + "\" must declare either \"position\" or both \"from\" and \"to\" — not both, not neither.");

        if (hasPosition) {
            int[] position = parseVector(entry, "position");
            putBlock(position[0] - origin[0], position[1] - origin[1], position[2] - origin[2],
                    blockHandle, orientation, structureName);
            return;
        }

        int[] from = parseVector(entry, "from");
        int[] to = parseVector(entry, "to");

        parseBlockBox(from, to, origin, blockHandle, orientation, structureName);
    }

    private void parseBlockBox(
            int[] from,
            int[] to,
            int[] origin,
            BlockHandle blockHandle,
            short orientation,
            String structureName) {

        int minX = Math.min(from[0], to[0]) - origin[0];
        int maxX = Math.max(from[0], to[0]) - origin[0];
        int minY = Math.min(from[1], to[1]) - origin[1];
        int maxY = Math.max(from[1], to[1]) - origin[1];
        int minZ = Math.min(from[2], to[2]) - origin[2];
        int maxZ = Math.max(from[2], to[2]) - origin[2];

        long volume = ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);

        if (volume > EngineSetting.STRUCTURE_MAX_BLOCK_COUNT)
            throwException("Structure \"" + structureName + "\" box of \"" + blockHandle.getBlockName()
                    + "\" covers " + volume + " blocks, which exceeds the structure limit of "
                    + EngineSetting.STRUCTURE_MAX_BLOCK_COUNT + ".");

        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    putBlock(x, y, z, blockHandle, orientation, structureName);
    }

    private void putBlock(
            int x,
            int y,
            int z,
            BlockHandle blockHandle,
            short orientation,
            String structureName) {

        validateExtent(x, structureName);
        validateExtent(y, structureName);
        validateExtent(z, structureName);

        long position = Coordinate3Long.pack(x, y, z);
        int index = position2BlockIndex.get(position);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            blockIDs.set(index, blockHandle.getBlockID());
            blockOrientations.set(index, orientation);
            blockGeometry.set(index, blockHandle.getGeometry());
            return;
        }

        if (blockIDs.size() >= EngineSetting.STRUCTURE_MAX_BLOCK_COUNT)
            throwException("Structure \"" + structureName + "\" declares more than "
                    + EngineSetting.STRUCTURE_MAX_BLOCK_COUNT + " blocks.");

        position2BlockIndex.put(position, blockIDs.size());

        offsetX.add(x);
        offsetY.add(y);
        offsetZ.add(z);
        blockIDs.add(blockHandle.getBlockID());
        blockOrientations.add(orientation);
        blockGeometry.add(blockHandle.getGeometry());
    }

    private void validateExtent(int offset, String structureName) {

        if (Math.abs(offset) > EngineSetting.STRUCTURE_MAX_EXTENT_BLOCKS)
            throwException("Structure \"" + structureName + "\" places a block " + offset
                    + " blocks from its origin — every block must lie within "
                    + EngineSetting.STRUCTURE_MAX_EXTENT_BLOCKS + " blocks of the origin on each axis.");
    }

    // Orientation Parsing \\

    private short parseOrientation(JsonObject entry, BlockHandle blockHandle, String structureName) {

        int spinCount = EngineSetting.STRUCTURE_ORIENTATION_SPIN_COUNT;

        if (blockHandle.getRotationType() == BlockRotationType.NONE)
            return EngineSetting.DEFAULT_BLOCK_ORIENTATION;

        Direction3Vector facing = entry.has("facing")
                ? parseFacing(entry.get("facing").getAsString(), structureName)
                : Direction3Vector.VALUES[EngineSetting.DEFAULT_BLOCK_ORIENTATION / spinCount];

        int spin = JsonUtility.getInt(entry, "spin", EngineSetting.DEFAULT_BLOCK_ORIENTATION % spinCount);

        if (spin < 0 || spin >= spinCount)
            throwException("Structure \"" + structureName + "\" block \"" + blockHandle.getBlockName()
                    + "\" has spin " + spin + " — spin must be between 0 and " + (spinCount - 1) + ".");

        return StructurePlacementUtility.encodeOrientation(facing, spin);
    }

    private Direction3Vector parseFacing(String raw, String structureName) {

        try {
            return Direction3Vector.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return throwException("Structure \"" + structureName + "\" has invalid facing \"" + raw
                    + "\" — expected NORTH, EAST, SOUTH, WEST, UP, or DOWN.", e);
        }
    }

    // Foundation Parsing \\

    private short parseFoundationBlockID(JsonObject json, String structureName) {

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(
                json.get("foundation_block").getAsString());
        DynamicGeometryType geometry = blockHandle.getGeometry();

        if (geometry == DynamicGeometryType.NONE || geometry == DynamicGeometryType.LIQUID)
            throwException("Structure \"" + structureName + "\" \"foundation_block\" \""
                    + blockHandle.getBlockName() + "\" must be a solid block.");

        return blockHandle.getBlockID();
    }

    // Rules Parsing \\

    private StructureRulesStruct parseRules(JsonObject json, String structureName) {

        JsonObject rulesJson = json.has("rules") ? json.getAsJsonObject("rules") : new JsonObject();

        ShortOpenHashSet biomeIDs = parseBiomes(rulesJson);
        StructureSurfaceType surfaceType = parseSurfaceType(rulesJson, structureName);

        int minGroundHeightBlocks = JsonUtility.getInt(
                rulesJson, "min_ground_height_blocks", EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS);
        int maxGroundHeightBlocks = JsonUtility.getInt(
                rulesJson, "max_ground_height_blocks", EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS);

        if (minGroundHeightBlocks > maxGroundHeightBlocks)
            throwException("Structure \"" + structureName + "\" \"min_ground_height_blocks\" ("
                    + minGroundHeightBlocks + ") exceeds \"max_ground_height_blocks\" ("
                    + maxGroundHeightBlocks + ").");

        boolean slopeLimited = rulesJson.has("max_slope_blocks");
        int maxSlopeBlocks = slopeLimited
                ? rulesJson.get("max_slope_blocks").getAsInt()
                : EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS - EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS;

        if (maxSlopeBlocks < 0)
            throwException("Structure \"" + structureName + "\" \"max_slope_blocks\" must not be negative.");

        return new StructureRulesStruct(
                biomeIDs, surfaceType,
                minGroundHeightBlocks, maxGroundHeightBlocks,
                maxSlopeBlocks, slopeLimited);
    }

    private ShortOpenHashSet parseBiomes(JsonObject rulesJson) {

        ShortOpenHashSet biomeIDs = new ShortOpenHashSet();

        if (!rulesJson.has("biomes"))
            return biomeIDs;

        for (JsonElement element : rulesJson.getAsJsonArray("biomes"))
            biomeIDs.add(biomeManager.getBiomeIDFromBiomeName(element.getAsString()));

        return biomeIDs;
    }

    private StructureSurfaceType parseSurfaceType(JsonObject rulesJson, String structureName) {

        if (!rulesJson.has("surface"))
            return StructureSurfaceType.ANY;

        String raw = rulesJson.get("surface").getAsString();

        try {
            return StructureSurfaceType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return throwException("Structure \"" + structureName + "\" has invalid surface \"" + raw
                    + "\" — expected LAND, UNDERWATER, or ANY.", e);
        }
    }

    // Frequency Parsing \\

    private StructureFrequencyStruct parseFrequency(JsonObject json, String structureName) {

        if (!json.has("frequency"))
            return null;

        JsonObject frequencyJson = json.getAsJsonObject("frequency");

        float chance = JsonUtility.validateFloat(frequencyJson, "chance");
        int spacingBlocks = JsonUtility.validateInt(frequencyJson, "spacing_blocks");
        int separationBlocks = JsonUtility.getInt(
                frequencyJson, "separation_blocks", EngineSetting.DEFAULT_STRUCTURE_SEPARATION_BLOCKS);
        boolean randomRotation = JsonUtility.getBoolean(frequencyJson, "random_rotation", false);

        if (chance <= 0f || chance > 1f)
            throwException("Structure \"" + structureName + "\" frequency \"chance\" " + chance
                    + " — chance must be greater than 0 and no more than 1.");

        if (spacingBlocks <= 0)
            throwException("Structure \"" + structureName + "\" frequency \"spacing_blocks\" must be greater than 0.");

        if (separationBlocks < 0 || separationBlocks >= spacingBlocks)
            throwException("Structure \"" + structureName + "\" frequency \"separation_blocks\" " + separationBlocks
                    + " — separation must be at least 0 and less than spacing_blocks (" + spacingBlocks + ").");

        return new StructureFrequencyStruct(chance, spacingBlocks, separationBlocks, randomRotation);
    }

    // Fixed Placement Parsing \\

    private ObjectArrayList<StructureFixedPlacementStruct> parseFixedPlacements(JsonObject json, String structureName) {

        ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements = new ObjectArrayList<>();

        if (!json.has("fixed_placements"))
            return fixedPlacements;

        for (JsonElement element : json.getAsJsonArray("fixed_placements"))
            fixedPlacements.add(parseFixedPlacement(element.getAsJsonObject(), structureName));

        return fixedPlacements;
    }

    private StructureFixedPlacementStruct parseFixedPlacement(JsonObject entry, String structureName) {

        int worldX = JsonUtility.validateInt(entry, "x");
        int worldZ = JsonUtility.validateInt(entry, "z");
        boolean hasWorldY = entry.has("y");
        int worldY = hasWorldY ? entry.get("y").getAsInt() : 0;
        int quarterTurns = JsonUtility.getInt(entry, "rotation", 0);
        boolean enforceRules = JsonUtility.getBoolean(entry, "enforce_rules", false);

        int worldHeightBlocks = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;

        if (hasWorldY && (worldY < 0 || worldY >= worldHeightBlocks))
            throwException("Structure \"" + structureName + "\" fixed placement at (" + worldX + ", " + worldZ
                    + ") has y " + worldY + " — y must be between 0 and " + (worldHeightBlocks - 1) + ".");

        if (quarterTurns < 0 || quarterTurns >= EngineSetting.STRUCTURE_QUARTER_TURN_COUNT)
            throwException("Structure \"" + structureName + "\" fixed placement at (" + worldX + ", " + worldZ
                    + ") has rotation " + quarterTurns + " — rotation must be between 0 and "
                    + (EngineSetting.STRUCTURE_QUARTER_TURN_COUNT - 1) + " clockwise quarter turns.");

        return new StructureFixedPlacementStruct(worldX, worldZ, worldY, hasWorldY, quarterTurns, enforceRules);
    }

    // Vector Parsing \\

    private int[] parseVector(JsonObject json, String field) {

        JsonArray array = JsonUtility.validateArray(json, field, 3);
        return new int[] { array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt() };
    }

    // Bounds \\

    private int min(IntArrayList values) {

        int result = Integer.MAX_VALUE;

        for (int i = 0; i < values.size(); i++)
            result = Math.min(result, values.getInt(i));

        return result;
    }

    private int max(IntArrayList values) {

        int result = Integer.MIN_VALUE;

        for (int i = 0; i < values.size(); i++)
            result = Math.max(result, values.getInt(i));

        return result;
    }
}
