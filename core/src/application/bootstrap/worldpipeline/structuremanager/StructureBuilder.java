package application.bootstrap.worldpipeline.structuremanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.structure.RoadData;
import application.bootstrap.worldpipeline.structure.StructureData;
import application.bootstrap.worldpipeline.structure.StructureElevationType;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureLayoutData;
import application.bootstrap.worldpipeline.structure.StructureLocationStruct;
import application.bootstrap.worldpipeline.structure.StructureRoadLinkData;
import application.bootstrap.worldpipeline.structure.StructureSpawnData;
import application.bootstrap.worldpipeline.structure.StructureTemplateData;
import application.bootstrap.worldpipeline.structure.StructureType;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.chars.Char2ShortOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class StructureBuilder extends BuilderPackage {

    /*
     * Parses a structure JSON into a StructureData and wraps it in a
     * StructureHandle. "type" is required and decides which body section is
     * read: "template" for STRUCTURE, "road" for ROAD, "layout" for
     * SETTLEMENT and DUNGEON. The shared sections — "elevation", "spawn",
     * "locations", and "road_link" — are optional on every placeable type.
     * Block names are resolved to IDs here, once, so stamping never touches
     * a name. Child structures a layout references are kept as names and
     * resolved on demand the first time a layout is planned, so a
     * settlement never forces its whole building catalogue to load at boot.
     * Every section is validated here so a malformed file fails at load
     * rather than halfway through generating a chunk.
     */

    private static final String AIR_ALIAS = "air";
    private static final char SKIP_CHARACTER = ' ';

    // Internal
    private BlockManager blockManager;

    // Base \\

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    // Build \\

    StructureHandle build(File file, File root) {

        String structureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short structureID = RegistryUtility.toShortID(structureName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        StructureType structureType = parseType(json, structureName);
        StructureElevationType elevationType = parseElevation(json, structureType, structureName);

        StructureSpawnData spawnData = null;
        ObjectArrayList<StructureLocationStruct> locations = new ObjectArrayList<>();
        StructureRoadLinkData roadLinkData = null;

        if (structureType.isPlaceable()) {
            spawnData = parseSpawn(json, structureName);
            parseLocations(json, structureName, locations);
            roadLinkData = parseRoadLink(json, structureName);
        }

        StructureTemplateData templateData = null;
        RoadData roadData = null;
        StructureLayoutData layoutData = null;
        int boundingRadius = 0;

        switch (structureType) {

            case STRUCTURE:
                templateData = parseTemplate(requireObject(json, "template", structureName), structureName);
                boundingRadius = computeTemplateRadius(templateData);
                break;

            case ROAD:
                roadData = parseRoad(requireObject(json, "road", structureName), structureName);
                break;

            case SETTLEMENT:
            case DUNGEON:
                layoutData = parseLayout(
                        requireObject(json, "layout", structureName), structureName, elevationType, spawnData);
                boundingRadius = layoutData.getRadiusBlocks()
                        + (layoutData.hasEntrance() ? layoutData.getEntranceMaxLengthBlocks() : 0);
                break;
        }

        StructureData structureData = new StructureData(
                structureName, structureID, structureType, elevationType,
                spawnData, locations, roadLinkData,
                templateData, roadData, layoutData,
                boundingRadius);

        StructureHandle structureHandle = create(StructureHandle.class);
        structureHandle.constructor(structureData);

        return structureHandle;
    }

    // Type \\

    private StructureType parseType(JsonObject json, String structureName) {

        String raw = JsonUtility.validateString(json, "type");

        try {
            return StructureType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return throwException("Structure \"" + structureName + "\" has unknown type \"" + raw
                    + "\" — expected one of ROAD, STRUCTURE, DUNGEON, SETTLEMENT.");
        }
    }

    private StructureElevationType parseElevation(JsonObject json, StructureType type, String structureName) {

        StructureElevationType fallback = type == StructureType.DUNGEON
                ? StructureElevationType.UNDERGROUND
                : StructureElevationType.SURFACE;

        if (!json.has("elevation"))
            return fallback;

        String raw = json.get("elevation").getAsString();

        try {
            return StructureElevationType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return throwException("Structure \"" + structureName + "\" has unknown elevation \"" + raw
                    + "\" — expected one of SURFACE, UNDERGROUND, ABSOLUTE.");
        }
    }

    // Spawn \\

    private StructureSpawnData parseSpawn(JsonObject json, String structureName) {

        if (!json.has("spawn"))
            return null;

        JsonObject spawn = json.getAsJsonObject("spawn");

        ObjectOpenHashSet<String> biomeNames = new ObjectOpenHashSet<>();

        if (spawn.has("biomes"))
            for (JsonElement element : spawn.getAsJsonArray("biomes"))
                biomeNames.add(element.getAsString());

        float frequency = JsonUtility.getFloat(spawn, "frequency", EngineSetting.STRUCTURE_DEFAULT_FREQUENCY);

        if (frequency <= 0f || frequency > 1f)
            throwException("Structure \"" + structureName + "\" spawn \"frequency\" is " + frequency
                    + " — it must be greater than 0 and no more than 1.");

        int spacingChunks = JsonUtility.getInt(
                spawn, "spacing_chunks", EngineSetting.STRUCTURE_DEFAULT_SPACING_CHUNKS);

        if (spacingChunks < 1)
            throwException("Structure \"" + structureName + "\" spawn \"spacing_chunks\" must be at least 1.");

        int minHeight = JsonUtility.getInt(spawn, "min_height", EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS);
        int maxHeight = JsonUtility.getInt(spawn, "max_height", EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS);

        if (minHeight > maxHeight)
            throwException("Structure \"" + structureName + "\" spawn \"min_height\" exceeds \"max_height\".");

        int[] depth = parseRange(spawn, "depth",
                EngineSetting.STRUCTURE_DEFAULT_MIN_DEPTH_BLOCKS,
                EngineSetting.STRUCTURE_DEFAULT_MAX_DEPTH_BLOCKS, structureName);

        int maxSlope = JsonUtility.getInt(spawn, "max_slope", EngineSetting.STRUCTURE_DEFAULT_MAX_SLOPE_BLOCKS);
        boolean allowWater = JsonUtility.getBoolean(spawn, "allow_water", false);

        return new StructureSpawnData(
                biomeNames, frequency, spacingChunks * EngineSetting.CHUNK_SIZE,
                minHeight, maxHeight, depth[0], depth[1], maxSlope, allowWater);
    }

    // Locations \\

    /*
     * Each location is either { "x": .., "z": .. } in world blocks or
     * { "pixel": [px, pz] } on the world map, which lands on that pixel's
     * centre — the natural unit when a location is picked by looking at the
     * painted map. "y" pins the height outright; "rotation" is a quarter-turn
     * count 0-3 and is rolled when omitted.
     */
    private void parseLocations(
            JsonObject json,
            String structureName,
            ObjectArrayList<StructureLocationStruct> outLocations) {

        if (!json.has("locations"))
            return;

        double blocksPerPixel = (double) EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;

        for (JsonElement element : json.getAsJsonArray("locations")) {

            JsonObject location = element.getAsJsonObject();
            double x;
            double z;

            if (location.has("pixel")) {
                JsonArray pixel = JsonUtility.validateArray(location, "pixel", 2);
                x = (pixel.get(0).getAsDouble() + 0.5) * blocksPerPixel;
                z = (pixel.get(1).getAsDouble() + 0.5) * blocksPerPixel;
            } else {
                if (!location.has("x") || !location.has("z"))
                    throwException("Structure \"" + structureName
                            + "\" has a location with neither \"pixel\" nor both \"x\" and \"z\".");
                x = location.get("x").getAsDouble();
                z = location.get("z").getAsDouble();
            }

            boolean hasY = location.has("y");
            int y = hasY ? location.get("y").getAsInt() : 0;

            int rotation = JsonUtility.getInt(location, "rotation", StructureLocationStruct.ROTATION_RANDOM);

            if (rotation != StructureLocationStruct.ROTATION_RANDOM && (rotation < 0 || rotation > 3))
                throwException("Structure \"" + structureName + "\" location rotation " + rotation
                        + " is out of range — use a quarter-turn count 0-3, or omit it to roll one.");

            outLocations.add(new StructureLocationStruct(x, z, hasY, y, rotation));
        }
    }

    // Road Link \\

    private StructureRoadLinkData parseRoadLink(JsonObject json, String structureName) {

        if (!json.has("road_link"))
            return null;

        JsonObject link = json.getAsJsonObject("road_link");

        String roadName = JsonUtility.validateString(link, "road");
        int maxDistance = JsonUtility.getInt(
                link, "max_distance", EngineSetting.ROAD_DEFAULT_MAX_DISTANCE_BLOCKS);
        int maxConnections = JsonUtility.getInt(
                link, "max_connections", EngineSetting.ROAD_DEFAULT_MAX_CONNECTIONS);

        if (maxDistance < EngineSetting.ROAD_PATH_GRID_BLOCKS || maxConnections < 1)
            throwException("Structure \"" + structureName + "\" \"road_link\" needs a \"max_distance\" of at least "
                    + EngineSetting.ROAD_PATH_GRID_BLOCKS + " and at least one connection.");

        return new StructureRoadLinkData(roadName, maxDistance, maxConnections);
    }

    // Template \\

    /*
     * "layers" lists horizontal slices bottom first. Each slice is an array
     * of rows, row 0 being the front (z = 0), and each row a string read
     * left to right along +X. Every character maps through "palette" to a
     * block name; a space always means "leave the terrain here", and the
     * palette value "air" is shorthand for the air block. All rows and all
     * layers must be the same size.
     */
    private StructureTemplateData parseTemplate(JsonObject template, String structureName) {

        Char2ShortOpenHashMap palette = parsePalette(requireObject(template, "palette", structureName), structureName);

        JsonArray layers = JsonUtility.validateArray(template, "layers");

        if (layers.isEmpty())
            throwException("Structure \"" + structureName + "\" template has no layers.");

        int sizeY = layers.size();
        JsonArray firstLayer = layers.get(0).getAsJsonArray();
        int sizeZ = firstLayer.size();
        int sizeX = sizeZ == 0 ? 0 : firstLayer.get(0).getAsString().length();

        if (sizeX == 0 || sizeZ == 0)
            throwException("Structure \"" + structureName + "\" template's first layer is empty.");

        short[] blocks = new short[sizeX * sizeY * sizeZ];

        for (int y = 0; y < sizeY; y++) {

            JsonArray rows = layers.get(y).getAsJsonArray();

            if (rows.size() != sizeZ)
                throwException("Structure \"" + structureName + "\" template layer " + y + " has " + rows.size()
                        + " rows but layer 0 has " + sizeZ + " — every layer must be the same size.");

            for (int z = 0; z < sizeZ; z++) {

                String row = rows.get(z).getAsString();

                if (row.length() != sizeX)
                    throwException("Structure \"" + structureName + "\" template layer " + y + " row " + z
                            + " is " + row.length() + " wide but the template is " + sizeX + " wide.");

                for (int x = 0; x < sizeX; x++) {

                    char character = row.charAt(x);
                    short blockID;

                    if (character == SKIP_CHARACTER)
                        blockID = StructureTemplateData.BLOCK_SKIP;
                    else if (palette.containsKey(character))
                        blockID = palette.get(character);
                    else
                        blockID = throwException("Structure \"" + structureName + "\" template uses character '"
                                + character + "' at layer " + y + " row " + z + " but its palette doesn't map it.");

                    blocks[x + sizeX * (z + sizeZ * y)] = blockID;
                }
            }
        }

        int anchorX = sizeX / 2;
        int anchorY = 0;
        int anchorZ = sizeZ / 2;

        if (template.has("anchor")) {
            JsonArray anchor = JsonUtility.validateArray(template, "anchor", 3);
            anchorX = anchor.get(0).getAsInt();
            anchorY = anchor.get(1).getAsInt();
            anchorZ = anchor.get(2).getAsInt();

            if (anchorX < 0 || anchorX >= sizeX || anchorY < 0 || anchorY >= sizeY || anchorZ < 0 || anchorZ >= sizeZ)
                throwException("Structure \"" + structureName + "\" template anchor lies outside the template.");
        }

        short foundationBlockID = template.has("foundation")
                ? resolveBlock(template.get("foundation").getAsString())
                : StructureTemplateData.BLOCK_SKIP;

        return new StructureTemplateData(
                sizeX, sizeY, sizeZ, blocks,
                anchorX, anchorY, anchorZ,
                foundationBlockID);
    }

    private Char2ShortOpenHashMap parsePalette(JsonObject paletteJson, String structureName) {

        Char2ShortOpenHashMap palette = new Char2ShortOpenHashMap();

        for (String key : paletteJson.keySet()) {

            if (key.length() != 1)
                throwException("Structure \"" + structureName + "\" palette key \"" + key
                        + "\" must be exactly one character.");

            if (key.charAt(0) == SKIP_CHARACTER)
                throwException("Structure \"" + structureName
                        + "\" palette maps ' ' — a space is reserved for \"leave the terrain here\".");

            palette.put(key.charAt(0), resolveBlock(paletteJson.get(key).getAsString()));
        }

        return palette;
    }

    // Measured from the anchor, not the template centre — the anchor is the placement point.
    private int computeTemplateRadius(StructureTemplateData template) {

        int reachX = Math.max(template.getAnchorX(), template.getSizeX() - template.getAnchorX());
        int reachZ = Math.max(template.getAnchorZ(), template.getSizeZ() - template.getAnchorZ());

        return (int) Math.ceil(Math.sqrt((double) reachX * reachX + (double) reachZ * reachZ)) + 1;
    }

    // Road \\

    private RoadData parseRoad(JsonObject road, String structureName) {

        int width = JsonUtility.getInt(road, "width", EngineSetting.ROAD_DEFAULT_WIDTH_BLOCKS);

        if (width < 1)
            throwException("Road \"" + structureName + "\" \"width\" must be at least 1.");

        WeightedBlocks surface = parseWeightedBlocks(road, "surface", structureName);

        short edgeBlockID = optionalBlock(road, "edge");
        short foundationBlockID = road.has("foundation")
                ? resolveBlock(road.get("foundation").getAsString())
                : surface.ids[0];

        int clearance = JsonUtility.getInt(road, "clearance", EngineSetting.ROAD_DEFAULT_CLEARANCE_BLOCKS);
        int maxFill = JsonUtility.getInt(road, "max_fill", EngineSetting.ROAD_DEFAULT_MAX_FILL_BLOCKS);
        float maxGrade = JsonUtility.getFloat(road, "max_grade", EngineSetting.ROAD_DEFAULT_MAX_GRADE);
        int smoothing = JsonUtility.getInt(
                road, "smoothing", EngineSetting.ROAD_DEFAULT_SMOOTHING_RADIUS_BLOCKS);

        if (maxGrade <= 0f)
            throwException("Road \"" + structureName + "\" \"max_grade\" must be greater than 0.");

        JsonObject tunnel = road.has("tunnel") ? road.getAsJsonObject("tunnel") : new JsonObject();
        int tunnelHeight = JsonUtility.getInt(tunnel, "height", EngineSetting.ROAD_DEFAULT_TUNNEL_HEIGHT_BLOCKS);
        short tunnelWall = tunnel.has("wall") ? resolveBlock(tunnel.get("wall").getAsString()) : foundationBlockID;
        short tunnelCeiling = tunnel.has("ceiling") ? resolveBlock(tunnel.get("ceiling").getAsString()) : tunnelWall;
        boolean alwaysTunnel = JsonUtility.getBoolean(road, "always_tunnel", false);

        if (tunnelHeight < 2)
            throwException("Road \"" + structureName + "\" tunnel \"height\" must be at least 2.");

        JsonObject bridge = road.has("bridge") ? road.getAsJsonObject("bridge") : new JsonObject();
        short bridgeDeck = bridge.has("deck") ? resolveBlock(bridge.get("deck").getAsString()) : foundationBlockID;
        short bridgeRail = optionalBlock(bridge, "rail");
        short bridgeSupport = bridge.has("support")
                ? resolveBlock(bridge.get("support").getAsString())
                : foundationBlockID;
        int supportSpacing = JsonUtility.getInt(
                bridge, "support_spacing", EngineSetting.ROAD_DEFAULT_SUPPORT_SPACING_BLOCKS);

        if (supportSpacing < 1)
            throwException("Road \"" + structureName + "\" bridge \"support_spacing\" must be at least 1.");

        return new RoadData(
                width, surface.ids, surface.cumulativeWeights, edgeBlockID, foundationBlockID,
                clearance, maxFill, maxGrade, smoothing,
                tunnelHeight, tunnelWall, tunnelCeiling, alwaysTunnel,
                bridgeDeck, bridgeRail, bridgeSupport, supportSpacing);
    }

    /*
     * "surface" is either a single block name or a list of
     * { "block": name, "weight": w } entries; weights are normalized into
     * a cumulative table.
     */
    private WeightedBlocks parseWeightedBlocks(JsonObject json, String field, String structureName) {

        if (!json.has(field))
            throwException("Road \"" + structureName + "\" is missing required \"" + field + "\".");

        JsonElement element = json.get(field);

        if (element.isJsonPrimitive())
            return new WeightedBlocks(
                    new short[] { resolveBlock(element.getAsString()) }, new float[] { 1f });

        JsonArray array = element.getAsJsonArray();

        if (array.isEmpty())
            throwException("Road \"" + structureName + "\" \"" + field + "\" is empty.");

        short[] ids = new short[array.size()];
        float[] cumulative = new float[array.size()];
        float total = 0f;

        for (int i = 0; i < array.size(); i++) {

            JsonObject entry = array.get(i).getAsJsonObject();
            ids[i] = resolveBlock(JsonUtility.validateString(entry, "block"));

            float weight = JsonUtility.getFloat(entry, "weight", 1f);

            if (weight <= 0f)
                throwException("Road \"" + structureName + "\" \"" + field + "\" weights must be positive.");

            total += weight;
            cumulative[i] = total;
        }

        for (int i = 0; i < cumulative.length; i++)
            cumulative[i] /= total;

        cumulative[cumulative.length - 1] = 1f;

        return new WeightedBlocks(ids, cumulative);
    }

    private static final class WeightedBlocks {

        final short[] ids;
        final float[] cumulativeWeights;

        WeightedBlocks(short[] ids, float[] cumulativeWeights) {
            this.ids = ids;
            this.cumulativeWeights = cumulativeWeights;
        }
    }

    // Layout \\

    private StructureLayoutData parseLayout(
            JsonObject layout,
            String structureName,
            StructureElevationType elevationType,
            StructureSpawnData spawnData) {

        String streetRoadName = JsonUtility.validateString(layout, "street");
        int radius = JsonUtility.validateInt(layout, "radius");

        if (radius < 8)
            throwException("Structure \"" + structureName + "\" layout \"radius\" must be at least 8.");

        int[] mainLength = parseRange(layout, "main_street_length", radius, radius * 2, structureName);

        JsonObject branches = layout.has("branches") ? layout.getAsJsonObject("branches") : new JsonObject();
        int[] branchCount = parseRange(branches, "count", 0, 0, structureName);
        int[] branchLength = parseRange(branches, "length", radius / 3, radius / 2, structureName);
        int branchDepth = JsonUtility.getInt(branches, "depth", 1);

        int lotSpacing = JsonUtility.getInt(layout, "lot_spacing", 2);
        int lotSetback = JsonUtility.getInt(layout, "lot_setback", 1);

        ObjectArrayList<String> landmarkNames = new ObjectArrayList<>();

        if (layout.has("landmarks"))
            for (JsonElement element : layout.getAsJsonArray("landmarks"))
                landmarkNames.add(element.getAsString());

        ObjectArrayList<String> buildingNames = new ObjectArrayList<>();
        FloatArrayList buildingWeights = new FloatArrayList();
        parseWeightedStructures(layout, "buildings", structureName, buildingNames, buildingWeights);

        ObjectArrayList<String> fillNames = new ObjectArrayList<>();
        FloatArrayList fillWeights = new FloatArrayList();
        parseWeightedStructures(layout, "fill", structureName, fillNames, fillWeights);

        int fillAttempts = JsonUtility.getInt(layout, "fill_attempts", fillNames.isEmpty() ? 0 : 16);

        boolean entrance = JsonUtility.getBoolean(
                layout, "entrance", elevationType == StructureElevationType.UNDERGROUND);
        String entranceRoadName = JsonUtility.getString(layout, "entrance_road", streetRoadName);

        int maxDepth = spawnData != null ? spawnData.getMaxDepthBlocks() : EngineSetting.STRUCTURE_DEFAULT_MAX_DEPTH_BLOCKS;
        int entranceMaxLength = JsonUtility.getInt(layout, "entrance_length",
                (int) Math.ceil(maxDepth / EngineSetting.ROAD_DEFAULT_MAX_GRADE) + 16);

        if (entrance && entranceMaxLength < 8)
            throwException("Structure \"" + structureName + "\" layout \"entrance_length\" must be at least 8.");

        return new StructureLayoutData(
                streetRoadName, radius,
                mainLength[0], mainLength[1],
                branchCount[0], branchCount[1],
                branchLength[0], branchLength[1],
                branchDepth,
                lotSpacing, lotSetback,
                landmarkNames,
                buildingNames, buildingWeights,
                fillNames, fillWeights, fillAttempts,
                entrance, entranceRoadName, entranceMaxLength);
    }

    private void parseWeightedStructures(
            JsonObject json,
            String field,
            String structureName,
            ObjectArrayList<String> outNames,
            FloatArrayList outWeights) {

        if (!json.has(field))
            return;

        for (JsonElement element : json.getAsJsonArray(field)) {

            if (element.isJsonPrimitive()) {
                outNames.add(element.getAsString());
                outWeights.add(1f);
                continue;
            }

            JsonObject entry = element.getAsJsonObject();
            float weight = JsonUtility.getFloat(entry, "weight", 1f);

            if (weight <= 0f)
                throwException("Structure \"" + structureName + "\" layout \"" + field
                        + "\" weights must be positive.");

            outNames.add(JsonUtility.validateString(entry, "structure"));
            outWeights.add(weight);
        }
    }

    // Shared Parsing \\

    // Either a single number (min == max) or a [min, max] pair.
    private int[] parseRange(JsonObject json, String field, int defaultMin, int defaultMax, String structureName) {

        if (!json.has(field))
            return new int[] { defaultMin, defaultMax };

        JsonElement element = json.get(field);

        if (element.isJsonPrimitive()) {
            int value = element.getAsInt();
            return new int[] { value, value };
        }

        JsonArray array = element.getAsJsonArray();

        if (array.size() != 2)
            throwException("Structure \"" + structureName + "\" \"" + field + "\" must be a number or [min, max].");

        int min = array.get(0).getAsInt();
        int max = array.get(1).getAsInt();

        if (min > max)
            throwException("Structure \"" + structureName + "\" \"" + field + "\" has min greater than max.");

        return new int[] { min, max };
    }

    private JsonObject requireObject(JsonObject json, String field, String structureName) {

        if (!json.has(field) || !json.get(field).isJsonObject())
            throwException("Structure \"" + structureName + "\" is missing required object \"" + field + "\".");

        return json.getAsJsonObject(field);
    }

    private short optionalBlock(JsonObject json, String field) {
        return json.has(field) ? resolveBlock(json.get(field).getAsString()) : StructureTemplateData.BLOCK_SKIP;
    }

    private short resolveBlock(String blockName) {

        if (blockName.equalsIgnoreCase(AIR_ALIAS))
            blockName = EngineSetting.AIR_BLOCK_NAME;

        return (short) blockManager.getBlockIDFromBlockName(blockName);
    }
}
