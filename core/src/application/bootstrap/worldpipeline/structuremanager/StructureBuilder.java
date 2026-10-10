package application.bootstrap.worldpipeline.structuremanager;

import java.io.File;
import java.util.Arrays;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.furnishingpipeline.util.FurnishingArpgUtility;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.block.SubBlockShape;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.structure.StructureData;
import application.bootstrap.worldpipeline.structure.StructureFixedPlacementStruct;
import application.bootstrap.worldpipeline.structure.StructureFrequencyStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructurePaletteEntryStruct;
import application.bootstrap.worldpipeline.structure.StructureRulesStruct;
import application.bootstrap.worldpipeline.structure.StructureSurfaceType;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class StructureBuilder extends BuilderPackage {

    /*
     * Parses structure ARPG into a StructureData and wraps it in a
     * StructureHandle. A structure may keep its own "palette" of blocks keyed
     * by single characters, each a block with its facing, spin and an
     * optional sub-block "shape" or raw "mask" for a partial block, and an
     * optional "covering" grown over it to its "coverage" level. Its
     * "layers" lay those keys row by row, a row running along x and each row
     * one step along z, over one or more heights, with a space leaving a cell
     * untouched; its "blocks" then lay single "position"s or inclusive
     * "from"/"to" boxes, each naming a block or a palette key. Everything is
     * applied in order, so a later entry overwrites an earlier one at the
     * same cell. The "front" names the side its entrance faces, "furnishings"
     * the places it comes furnished at, and "clear_terrain" whether the
     * ground inside its footprint is carved away before it is laid. Every
     * block, biome, table and value is resolved and validated here, so a
     * malformed structure fails at boot.
     */

    // Internal
    private StructureManager structureManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;
    private BiomeManager biomeManager;
    private FurnishingManager furnishingManager;

    // Palette
    private Object2ObjectOpenHashMap<String, StructurePaletteEntryStruct> key2PaletteEntry;

    // Block Accumulation
    private IntArrayList offsetX;
    private IntArrayList offsetY;
    private IntArrayList offsetZ;
    private ShortArrayList blockIDs;
    private ShortArrayList blockOrientations;
    private ByteArrayList blockMasks;
    private ShortArrayList blockCoverages;
    private ObjectArrayList<DynamicGeometryType> blockGeometry;
    private Long2IntOpenHashMap position2BlockIndex;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.key2PaletteEntry = new Object2ObjectOpenHashMap<>();

        // Block Accumulation
        this.offsetX = new IntArrayList();
        this.offsetY = new IntArrayList();
        this.offsetZ = new IntArrayList();
        this.blockIDs = new ShortArrayList();
        this.blockOrientations = new ShortArrayList();
        this.blockMasks = new ByteArrayList();
        this.blockCoverages = new ShortArrayList();
        this.blockGeometry = new ObjectArrayList<>();
        this.position2BlockIndex = new Long2IntOpenHashMap();
        this.position2BlockIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.furnishingManager = get(FurnishingManager.class);
    }

    // Build \\

    StructureHandle build(File file, String structureName) {

        short structureID = structureManager.registerStructureName(structureName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        int[] origin = parseOrigin(arpg);

        clearBlocks();
        parsePalette(arpg, structureName);
        parseLayers(arpg, structureName, origin);
        parseBlocks(arpg, structureName, origin);

        if (blockIDs.isEmpty())
            throwException("Structure \"" + structureName
                    + "\" must lay at least one block through its \"layers\" or \"blocks\".");

        int minOffsetX = min(offsetX);
        int maxOffsetX = max(offsetX);
        int minOffsetZ = min(offsetZ);
        int maxOffsetZ = max(offsetZ);
        int horizontalReachBlocks = Math.max(
                Math.max(-minOffsetX, maxOffsetX),
                Math.max(-minOffsetZ, maxOffsetZ));

        int width = maxOffsetX - minOffsetX + 1;
        int depth = maxOffsetZ - minOffsetZ + 1;
        boolean[] footprint = new boolean[width * depth];
        int[] footprintTopOffsetY = new int[width * depth];
        int[] footprintTopColors = new int[width * depth];
        int[] footprintSideColors = new int[width * depth];

        buildFootprint(
                minOffsetX, minOffsetZ, width,
                footprint, footprintTopOffsetY, footprintTopColors, footprintSideColors);

        int yOffsetBlocks = ArpgUtility.getInt(
                arpg, "y_offset_blocks", EngineSetting.DEFAULT_STRUCTURE_Y_OFFSET_BLOCKS);

        boolean foundation = arpg.has("foundation_block");
        short foundationBlockID = foundation
                ? parseFoundationBlockID(arpg, structureName)
                : EngineSetting.REGISTRY_RESERVED_ID;
        boolean clearTerrain = ArpgUtility.getBoolean(arpg, "clear_terrain", false);

        Direction3Vector front = parseFront(arpg, structureName);
        ObjectArrayList<FurnishingSlotStruct> furnishings = FurnishingArpgUtility.parseSlots(
                arpg, "furnishings", structureName, furnishingManager);

        StructureRulesStruct rules = parseRules(arpg, structureName);
        StructureFrequencyStruct frequency = parseFrequency(arpg, structureName);
        ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements = parseFixedPlacements(arpg, structureName);

        StructureData structureData = new StructureData(
                structureName, structureID, RegistryUtility.toNameSeed(structureName),
                offsetX.toIntArray(), offsetY.toIntArray(), offsetZ.toIntArray(),
                blockIDs.toShortArray(), blockOrientations.toShortArray(), blockMasks.toByteArray(),
                blockCoverages.toShortArray(),
                blockGeometry.toArray(new DynamicGeometryType[0]),
                minOffsetX, maxOffsetX, min(offsetY), max(offsetY), minOffsetZ, maxOffsetZ, horizontalReachBlocks,
                footprint, footprintTopOffsetY, footprintTopColors, footprintSideColors,
                yOffsetBlocks, foundationBlockID, foundation, clearTerrain,
                front, furnishings,
                rules, frequency, fixedPlacements);

        StructureHandle structureHandle = create(StructureHandle.class);
        structureHandle.constructor(structureData);

        return structureHandle;
    }

    // Origin Parsing \\

    private int[] parseOrigin(ArpgObjectStruct arpg) {

        if (!arpg.has("origin"))
            return new int[3];

        return parseVector(arpg, "origin");
    }

    // Palette Parsing \\

    private void parsePalette(ArpgObjectStruct arpg, String structureName) {

        key2PaletteEntry.clear();

        if (!ArpgUtility.hasObject(arpg, "palette"))
            return;

        ArpgObjectStruct paletteArpg = arpg.getAsObject("palette");

        for (String key : paletteArpg.keySet()) {

            if (key.length() != 1 || key.charAt(0) == EngineSetting.STRUCTURE_LAYER_SKIP_CHARACTER)
                throwException("Structure \"" + structureName + "\" palette key \"" + key
                        + "\" must be a single character other than a space.");

            key2PaletteEntry.put(key, parseEntry(paletteArpg.getAsObject(key), structureName));
        }
    }

    // A block with its orientation and mask, named inline or by palette key
    private StructurePaletteEntryStruct parseEntry(ArpgObjectStruct entry, String structureName) {

        boolean hasKey = entry.has("palette");

        if (hasKey == entry.has("block"))
            throwException("Structure \"" + structureName
                    + "\" entry must name exactly one of \"block\" or \"palette\".");

        if (hasKey) {

            String key = entry.get("palette").getAsString();
            StructurePaletteEntryStruct paletteEntry = key2PaletteEntry.get(key);

            if (paletteEntry == null)
                throwException("Structure \"" + structureName + "\" names palette key \"" + key
                        + "\", which its \"palette\" does not list.");

            return paletteEntry;
        }

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(ArpgUtility.validateString(entry, "block"));

        return new StructurePaletteEntryStruct(
                blockHandle,
                parseOrientation(entry, blockHandle, structureName),
                parseMask(entry, blockHandle, structureName),
                parseCoverage(entry, blockHandle, structureName));
    }

    // Layer Parsing \\

    private void parseLayers(ArpgObjectStruct arpg, String structureName, int[] origin) {

        if (!ArpgUtility.hasArray(arpg, "layers"))
            return;

        for (ArpgElementStruct element : arpg.getAsArray("layers"))
            parseLayer(element.getAsObject(), structureName, origin);
    }

    private void parseLayer(ArpgObjectStruct layer, String structureName, int[] origin) {

        int baseY = ArpgUtility.validateInt(layer, "y");
        int height = ArpgUtility.getInt(layer, "height", 1);
        ArpgArrayStruct rows = ArpgUtility.validateArray(layer, "rows");

        if (height < 1)
            throwException("Structure \"" + structureName + "\" layer at y " + baseY + " has height " + height
                    + " — a layer covers at least one height.");

        for (int y = baseY; y < baseY + height; y++)
            for (int z = 0; z < rows.size(); z++)
                parseRow(rows.get(z).getAsString(), y, z, structureName, origin);
    }

    private void parseRow(String row, int y, int z, String structureName, int[] origin) {

        for (int x = 0; x < row.length(); x++) {

            char key = row.charAt(x);

            if (key == EngineSetting.STRUCTURE_LAYER_SKIP_CHARACTER)
                continue;

            StructurePaletteEntryStruct entry = key2PaletteEntry.get(String.valueOf(key));

            if (entry == null)
                throwException("Structure \"" + structureName + "\" layer at y " + y + " uses key '" + key
                        + "', which its \"palette\" does not list.");

            putBlock(x - origin[0], y - origin[1], z - origin[2], entry, structureName);
        }
    }

    // Block Parsing \\

    private void clearBlocks() {

        offsetX.clear();
        offsetY.clear();
        offsetZ.clear();
        blockIDs.clear();
        blockOrientations.clear();
        blockMasks.clear();
        blockCoverages.clear();
        blockGeometry.clear();
        position2BlockIndex.clear();
    }

    private void parseBlocks(ArpgObjectStruct arpg, String structureName, int[] origin) {

        if (!ArpgUtility.hasArray(arpg, "blocks"))
            return;

        for (ArpgElementStruct element : arpg.getAsArray("blocks"))
            parseBlockEntry(element.getAsObject(), structureName, origin);
    }

    private void parseBlockEntry(ArpgObjectStruct entry, String structureName, int[] origin) {

        StructurePaletteEntryStruct paletteEntry = parseEntry(entry, structureName);

        boolean hasPosition = entry.has("position");
        boolean hasBox = entry.has("from") || entry.has("to");

        if (hasPosition == hasBox)
            throwException("Structure \"" + structureName + "\" block entry \""
                    + paletteEntry.getBlockHandle().getBlockName()
                    + "\" must declare either \"position\" or both \"from\" and \"to\" — not both, not neither.");

        if (hasPosition) {
            int[] position = parseVector(entry, "position");
            putBlock(position[0] - origin[0], position[1] - origin[1], position[2] - origin[2],
                    paletteEntry, structureName);
            return;
        }

        int[] from = parseVector(entry, "from");
        int[] to = parseVector(entry, "to");

        parseBlockBox(from, to, origin, paletteEntry, structureName);
    }

    private void parseBlockBox(
            int[] from,
            int[] to,
            int[] origin,
            StructurePaletteEntryStruct entry,
            String structureName) {

        int minX = Math.min(from[0], to[0]) - origin[0];
        int maxX = Math.max(from[0], to[0]) - origin[0];
        int minY = Math.min(from[1], to[1]) - origin[1];
        int maxY = Math.max(from[1], to[1]) - origin[1];
        int minZ = Math.min(from[2], to[2]) - origin[2];
        int maxZ = Math.max(from[2], to[2]) - origin[2];

        long volume = ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);

        if (volume > EngineSetting.STRUCTURE_MAX_BLOCK_COUNT)
            throwException("Structure \"" + structureName + "\" box of \""
                    + entry.getBlockHandle().getBlockName()
                    + "\" covers " + volume + " blocks, which exceeds the structure limit of "
                    + EngineSetting.STRUCTURE_MAX_BLOCK_COUNT + ".");

        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    putBlock(x, y, z, entry, structureName);
    }

    private void putBlock(
            int x,
            int y,
            int z,
            StructurePaletteEntryStruct entry,
            String structureName) {

        validateExtent(x, structureName);
        validateExtent(y, structureName);
        validateExtent(z, structureName);

        BlockHandle blockHandle = entry.getBlockHandle();
        long position = Coordinate3Long.pack(x, y, z);
        int index = position2BlockIndex.get(position);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            blockIDs.set(index, blockHandle.getBlockID());
            blockOrientations.set(index, entry.getOrientation());
            blockMasks.set(index, (byte) entry.getMask());
            blockCoverages.set(index, entry.getCoverage());
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
        blockOrientations.add(entry.getOrientation());
        blockMasks.add((byte) entry.getMask());
        blockCoverages.add(entry.getCoverage());
        blockGeometry.add(blockHandle.getGeometry());
    }

    private void validateExtent(int offset, String structureName) {

        if (Math.abs(offset) > EngineSetting.STRUCTURE_MAX_EXTENT_BLOCKS)
            throwException("Structure \"" + structureName + "\" places a block " + offset
                    + " blocks from its origin — every block must lie within "
                    + EngineSetting.STRUCTURE_MAX_EXTENT_BLOCKS + " blocks of the origin on each axis.");
    }

    // Orientation Parsing \\

    private short parseOrientation(ArpgObjectStruct entry, BlockHandle blockHandle, String structureName) {

        int spinCount = EngineSetting.STRUCTURE_ORIENTATION_SPIN_COUNT;

        if (blockHandle.getRotationType() == BlockRotationType.NONE)
            return EngineSetting.DEFAULT_BLOCK_ORIENTATION;

        Direction3Vector facing = entry.has("facing")
                ? parseFacing(entry.get("facing").getAsString(), structureName)
                : Direction3Vector.VALUES[EngineSetting.DEFAULT_BLOCK_ORIENTATION / spinCount];

        int spin = ArpgUtility.getInt(entry, "spin", EngineSetting.DEFAULT_BLOCK_ORIENTATION % spinCount);

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

    // Coverage Parsing \\

    // The covering grown over a block at its level, the full level unless it names one, NONE when it names none
    private short parseCoverage(ArpgObjectStruct entry, BlockHandle blockHandle, String structureName) {

        String coveringName = ArpgUtility.getString(entry, "covering", EngineSetting.STRUCTURE_COVERING_NONE);

        if (coveringName.equals(EngineSetting.STRUCTURE_COVERING_NONE)) {

            if (entry.has("coverage"))
                throwException("Structure \"" + structureName + "\" block \"" + blockHandle.getBlockName()
                        + "\" declares a \"coverage\" level but names no \"covering\".");

            return CoverageUtility.NONE;
        }

        return coveringManager.resolveCoverage(
                coveringName,
                ArpgUtility.getInt(entry, "coverage", CoverageUtility.LEVEL_MAX),
                blockHandle.getBlockID(),
                structureName);
    }

    // Mask Parsing \\

    // The octants a block fills: a named "shape", a raw "mask", or the whole cell
    private int parseMask(ArpgObjectStruct entry, BlockHandle blockHandle, String structureName) {

        boolean hasShape = entry.has("shape");
        boolean hasMask = entry.has("mask");

        if (hasShape && hasMask)
            throwException("Structure \"" + structureName + "\" block \"" + blockHandle.getBlockName()
                    + "\" declares both \"shape\" and \"mask\" — name one.");

        int mask = hasShape
                ? ArpgUtility.getEnum(entry, "shape", SubBlockShape.class, SubBlockShape.FULL).getMask()
                : ArpgUtility.getInt(entry, "mask", SubBlockUtility.MASK_FULL);

        if (mask <= SubBlockUtility.MASK_EMPTY || mask > SubBlockUtility.MASK_FULL)
            throwException("Structure \"" + structureName + "\" block \"" + blockHandle.getBlockName()
                    + "\" has mask " + mask + " — a mask fills from 1 to " + SubBlockUtility.MASK_FULL
                    + " octants' bits; lay air to empty a cell.");

        if (SubBlockUtility.isSubdivided(mask) && blockHandle.getGeometry() != DynamicGeometryType.FULL)
            throwException("Structure \"" + structureName + "\" block \"" + blockHandle.getBlockName()
                    + "\" is partial, but only a FULL-geometry block can be laid as sub-blocks.");

        return mask;
    }

    // Front Parsing \\

    private Direction3Vector parseFront(ArpgObjectStruct arpg, String structureName) {

        Direction3Vector front = ArpgUtility.getEnum(arpg, "front", Direction3Vector.class, Direction3Vector.NORTH);

        if (front.y != 0)
            throwException("Structure \"" + structureName + "\" \"front\" must be NORTH, EAST, SOUTH, or WEST.");

        return front;
    }

    // Footprint \\

    // Per column: whether any block is listed, and the top solid block with the colors it is seen in from afar
    private void buildFootprint(
            int minOffsetX,
            int minOffsetZ,
            int width,
            boolean[] footprint,
            int[] topOffsetY,
            int[] topColors,
            int[] sideColors) {

        Arrays.fill(topOffsetY, EngineSetting.STRUCTURE_FOOTPRINT_EMPTY);

        int[] lowOffsetY = new int[topOffsetY.length];
        Arrays.fill(lowOffsetY, Integer.MAX_VALUE);

        for (int i = 0; i < blockIDs.size(); i++) {

            int column = (offsetZ.getInt(i) - minOffsetZ) * width + offsetX.getInt(i) - minOffsetX;
            footprint[column] = true;

            BlockHandle blockHandle = resolveVisibleBlock(i);

            if (blockHandle == null)
                continue;

            int y = offsetY.getInt(i);
            lowOffsetY[column] = Math.min(lowOffsetY[column], y);

            if (y <= topOffsetY[column])
                continue;

            topOffsetY[column] = y;
            topColors[column] = coveringManager.resolveCoveredColor(
                    blockHandle.getMapColorForFace(Direction3Vector.UP),
                    blockCoverages.getShort(i),
                    EngineSetting.PACKED_COLOR_WHITE,
                    false);
        }

        for (int column = 0; column < topOffsetY.length; column++)
            if (topOffsetY[column] != EngineSetting.STRUCTURE_FOOTPRINT_EMPTY)
                sideColors[column] = resolveSideColor(
                        minOffsetX + column % width, minOffsetZ + column / width,
                        lowOffsetY[column], topOffsetY[column], topColors[column]);
    }

    // The wall a column shows from the side: the first visible block at or below its middle height
    private int resolveSideColor(int x, int z, int lowY, int topY, int topColor) {

        for (int y = (lowY + topY) / 2; y >= lowY; y--) {

            int index = position2BlockIndex.get(Coordinate3Long.pack(x, y, z));

            if (index == EngineSetting.INDEX_NOT_FOUND)
                continue;

            BlockHandle blockHandle = resolveVisibleBlock(index);

            if (blockHandle != null)
                return blockHandle.getMapColorForFace(Direction3Vector.NORTH);
        }

        return topColor;
    }

    // The block laid at an index when it is solid and has colors to be seen in, otherwise null
    private BlockHandle resolveVisibleBlock(int index) {

        DynamicGeometryType geometry = blockGeometry.get(index);

        if (geometry == DynamicGeometryType.NONE || geometry == DynamicGeometryType.LIQUID)
            return null;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockIDs.getShort(index));

        return blockHandle.hasMapColor() ? blockHandle : null;
    }

    // Foundation Parsing \\

    private short parseFoundationBlockID(ArpgObjectStruct arpg, String structureName) {
        return blockManager.getSolidBlockHandleFromBlockName(
                arpg.get("foundation_block").getAsString(), structureName).getBlockID();
    }

    // Rules Parsing \\

    private StructureRulesStruct parseRules(ArpgObjectStruct arpg, String structureName) {

        ArpgObjectStruct rulesArpg = arpg.has("rules") ? arpg.getAsObject("rules") : new ArpgObjectStruct();

        ShortOpenHashSet biomeIDs = parseBiomes(rulesArpg);
        StructureSurfaceType surfaceType = parseSurfaceType(rulesArpg, structureName);

        int minGroundHeightBlocks = ArpgUtility.getInt(
                rulesArpg, "min_ground_height_blocks", EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS);
        int maxGroundHeightBlocks = ArpgUtility.getInt(
                rulesArpg, "max_ground_height_blocks", EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS);

        if (minGroundHeightBlocks > maxGroundHeightBlocks)
            throwException("Structure \"" + structureName + "\" \"min_ground_height_blocks\" ("
                    + minGroundHeightBlocks + ") exceeds \"max_ground_height_blocks\" ("
                    + maxGroundHeightBlocks + ").");

        boolean slopeLimited = rulesArpg.has("max_slope_blocks");
        int maxSlopeBlocks = slopeLimited
                ? rulesArpg.get("max_slope_blocks").getAsInt()
                : EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS - EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS;

        if (maxSlopeBlocks < 0)
            throwException("Structure \"" + structureName + "\" \"max_slope_blocks\" must not be negative.");

        return new StructureRulesStruct(
                biomeIDs, surfaceType,
                minGroundHeightBlocks, maxGroundHeightBlocks,
                maxSlopeBlocks, slopeLimited);
    }

    private ShortOpenHashSet parseBiomes(ArpgObjectStruct rulesArpg) {

        ShortOpenHashSet biomeIDs = new ShortOpenHashSet();

        if (!rulesArpg.has("biomes"))
            return biomeIDs;

        for (ArpgElementStruct element : rulesArpg.getAsArray("biomes"))
            biomeIDs.add(biomeManager.getBiomeIDFromBiomeName(element.getAsString()));

        return biomeIDs;
    }

    private StructureSurfaceType parseSurfaceType(ArpgObjectStruct rulesArpg, String structureName) {

        if (!rulesArpg.has("surface"))
            return StructureSurfaceType.ANY;

        String raw = rulesArpg.get("surface").getAsString();

        try {
            return StructureSurfaceType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return throwException("Structure \"" + structureName + "\" has invalid surface \"" + raw
                    + "\" — expected LAND, UNDERWATER, or ANY.", e);
        }
    }

    // Frequency Parsing \\

    private StructureFrequencyStruct parseFrequency(ArpgObjectStruct arpg, String structureName) {

        if (!arpg.has("frequency"))
            return null;

        ArpgObjectStruct frequencyArpg = arpg.getAsObject("frequency");

        float chance = ArpgUtility.validateFloat(frequencyArpg, "chance");
        int spacingBlocks = ArpgUtility.validateInt(frequencyArpg, "spacing_blocks");
        int separationBlocks = ArpgUtility.getInt(
                frequencyArpg, "separation_blocks", EngineSetting.DEFAULT_STRUCTURE_SEPARATION_BLOCKS);
        boolean randomRotation = ArpgUtility.getBoolean(frequencyArpg, "random_rotation", false);

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

    private ObjectArrayList<StructureFixedPlacementStruct> parseFixedPlacements(
            ArpgObjectStruct arpg,
            String structureName) {

        ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements = new ObjectArrayList<>();

        if (!arpg.has("fixed_placements"))
            return fixedPlacements;

        for (ArpgElementStruct element : arpg.getAsArray("fixed_placements"))
            fixedPlacements.add(parseFixedPlacement(element.getAsObject(), structureName));

        return fixedPlacements;
    }

    private StructureFixedPlacementStruct parseFixedPlacement(ArpgObjectStruct entry, String structureName) {

        int worldX = ArpgUtility.validateInt(entry, "x");
        int worldZ = ArpgUtility.validateInt(entry, "z");
        boolean hasWorldY = entry.has("y");
        int worldY = hasWorldY ? entry.get("y").getAsInt() : 0;
        int quarterTurns = ArpgUtility.getInt(entry, "rotation", 0);
        boolean enforceRules = ArpgUtility.getBoolean(entry, "enforce_rules", false);

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

    private int[] parseVector(ArpgObjectStruct arpg, String field) {

        ArpgArrayStruct array = ArpgUtility.validateArray(arpg, field, 3);
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
