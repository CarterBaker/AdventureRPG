package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.ContainerSpaceStruct;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemActionStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemActionTrigger;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.itempipeline.itemdefinition.LidClearanceStruct;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemDefinitionBuilder extends BuilderPackage {

    /*
     * Parses item definition ARPG files and builds ItemDefinitionHandle instances.
     * Each ARPG file may contain multiple item entries under an 'items' array.
     * Resolves mesh and material references from their respective managers,
     * and reads the item's mesh file through SubVoxelManager — every item mesh
     * must be a sub-voxel model. The shape an item claims in a container and
     * in the world is the boxes its 'space' lists inside that model's grid, or
     * the model's own cubes and walls when it lists none. That grid spans as
     * many blocks as the model does, so furniture claims its full size. A
     * container's model spans one block, and its space
     * either lies inside that model at an offset, clear of its cubes, or is a
     * pocket shown as its own box of walls; either must name the parts of its
     * lid, left off its model while it stands open. The lid's clearance — the
     * cells that must be empty before it opens — is the boxes the container's
     * 'clearance' lists, or when it lists none the space the lid swings
     * through: the cells over its upper surface, as high as the lid is short
     * across, so a block or an item anywhere in its way keeps it shut. An item
     * without a display name is titled from its local name split into words.
     * A tool names its tool type, whose model it is drawn with unless it names
     * its own mesh, and the highest break tier it can break. A stackable item
     * holds up to its stack size in one item, which a container cannot. An
     * item's "actions" each name a "trigger", optionally the item held "with"
     * it, whether it "consumes" one of that, and the model "parts" it must be
     * aimed at, and the item it "becomes"; one may "fire" an item from a
     * "muzzle" in model sub-voxels along an "aim" at a "speed". "pick_up_as"
     * names the item it is picked up as, and "plants" the tree a seed grows
     * into. "sows" names the covering an item lays over the block it is used
     * on and "nurtures" the levels it adds there, which a sower must name; an
     * item that only nurtures feeds whatever already covers the block. A
     * container carries no actions, so its contents are never lost.
     * Bootstrap-only.
     */

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private SubVoxelManager subVoxelManager;
    private ToolTypeManager toolTypeManager;
    private CoveringManager coveringManager;

    // Directory
    private File meshRoot;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.meshRoot = new File(EngineSetting.MESH_PATH);
    }

    @Override
    protected void get() {

        // Internal
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.toolTypeManager = get(ToolTypeManager.class);
        this.coveringManager = get(CoveringManager.class);
    }

    // Build \\

    ObjectArrayList<ItemDefinitionHandle> build(File arpgFile, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, arpgFile);
        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct itemArray = ArpgUtility.validateArray(rootArpg, "items");
        ObjectArrayList<ItemDefinitionHandle> items = new ObjectArrayList<>();

        for (int i = 0; i < itemArray.size(); i++) {
            ArpgObjectStruct itemArpg = itemArray.get(i).getAsObject();
            ItemDefinitionHandle item = parseItem(itemArpg, pathPrefix);
            if (item != null)
                items.add(item);
        }

        return items;
    }

    // Parse \\

    private ItemDefinitionHandle parseItem(ArpgObjectStruct itemArpg, String pathPrefix) {

        String localName = ArpgUtility.validateString(itemArpg, "name");
        String itemName = ItemRegistryUtility.toItemName(pathPrefix, localName);
        int itemID = itemDefinitionManager.registerItemName(itemName);

        String displayName = ArpgUtility.getString(itemArpg, "display_name", EngineSetting.ITEM_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = toDisplayName(localName);

        String description = ArpgUtility.getString(itemArpg, "description", EngineSetting.ITEM_DESCRIPTION_NONE);
        ItemCategory category = ArpgUtility.getEnum(itemArpg, "category", ItemCategory.class, ItemCategory.MISC);

        float weight = ArpgUtility.getFloat(itemArpg, "weight", 1.0f);
        boolean twoHanded = ArpgUtility.getBoolean(itemArpg, "two_handed", false);
        boolean solid = ArpgUtility.getBoolean(itemArpg, "solid", true);

        EquipmentType equipmentType = ArpgUtility.getEnum(itemArpg, "equip", EquipmentType.class, EquipmentType.NONE);
        float[] stats = parseStats(itemArpg);

        short toolTypeID = parseToolType(itemArpg);
        int toolTier = parseToolTier(itemArpg, toolTypeID, itemName);
        int stackSize = parseStackSize(itemArpg, itemName);

        String meshPath = resolveMeshPath(itemArpg, toolTypeID, itemName);
        int meshID = meshManager.getMeshIDFromMeshName(meshPath);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshID(meshID);
        SubVoxelModelStruct model = parseModel(meshPath, itemName);
        ItemShapeStruct shape = parseShape(itemArpg, model, itemName);
        ContainerSpaceStruct containerSpace = parseContainerSpace(itemArpg, model, shape, itemName);
        MeshData openMeshData = buildOpenMesh(itemArpg, model, containerSpace, itemName);
        MeshData pocketMeshData = buildPocketMesh(containerSpace);

        if (equipmentType == EquipmentType.BACKPACK && containerSpace == null)
            throwException("Item '" + itemName + "' is worn as a backpack but declares no container.");

        if (stackSize > 1 && containerSpace != null)
            throwException("Item '" + itemName + "' is a container and cannot stack — each one keeps its own contents.");

        ObjectArrayList<ItemActionStruct> actions = parseActions(itemArpg, model, itemName);

        if (!actions.isEmpty() && containerSpace != null)
            throwException("Item '" + itemName + "' is a container with actions; turning it into another item "
                    + "would lose its contents.");

        String plantsTreeName = ArpgUtility.getString(itemArpg, "plants", EngineSetting.ITEM_PLANTS_NONE);
        String sowsCoveringName = parseSows(itemArpg, plantsTreeName, itemName);
        int nurtureLevels = parseNurtures(itemArpg, sowsCoveringName, itemName);

        String materialPath = ArpgUtility.getString(
                itemArpg, "material", EngineSetting.DEFAULT_ITEM_MATERIAL);
        int materialID = materialManager.getMaterialIDFromMaterialName(materialPath);

        ItemDefinitionData itemDefinitionData = new ItemDefinitionData(
                itemName,
                localName,
                itemID,
                displayName,
                description,
                category,
                weight,
                twoHanded,
                solid,
                equipmentType,
                stats,
                shape,
                containerSpace,
                meshHandle,
                openMeshData,
                pocketMeshData,
                materialID,
                toolTypeID,
                toolTier,
                stackSize,
                EngineSetting.BLOCK_PIECE_NONE,
                actions,
                ArpgUtility.getString(itemArpg, "pick_up_as", EngineSetting.ITEM_PICK_UP_AS_SELF),
                plantsTreeName,
                sowsCoveringName,
                nurtureLevels);

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemDefinitionData);

        return item;
    }

    private float[] parseStats(ArpgObjectStruct itemArpg) {

        float[] stats = new float[ItemStat.VALUES.length];

        if (!ArpgUtility.hasObject(itemArpg, "stats"))
            return stats;

        ArpgObjectStruct statsArpg = itemArpg.getAsObject("stats");

        for (String statName : statsArpg.keySet())
            stats[ArpgUtility.toEnum(statName, ItemStat.class).ordinal()] = statsArpg.get(statName).getAsFloat();

        return stats;
    }

    // Coverage \\

    private String parseSows(ArpgObjectStruct itemArpg, String plantsTreeName, String itemName) {

        String sowsCoveringName = ArpgUtility.getString(itemArpg, "sows", EngineSetting.ITEM_SOWS_NONE);

        if (sowsCoveringName.equals(EngineSetting.ITEM_SOWS_NONE))
            return sowsCoveringName;

        if (!plantsTreeName.equals(EngineSetting.ITEM_PLANTS_NONE))
            throwException("Item '" + itemName + "' both \"plants\" a tree and \"sows\" a covering — name one.");

        coveringManager.getCoveringHandleFromCoveringName(sowsCoveringName);

        return sowsCoveringName;
    }

    private int parseNurtures(ArpgObjectStruct itemArpg, String sowsCoveringName, String itemName) {

        int nurtureLevels = ArpgUtility.getInt(itemArpg, "nurtures", EngineSetting.ITEM_NURTURES_NONE);

        if (nurtureLevels < 0 || nurtureLevels > CoverageUtility.LEVEL_MAX)
            throwException("Item '" + itemName + "' \"nurtures\" " + nurtureLevels
                    + " levels — it must run from 0 to " + CoverageUtility.LEVEL_MAX + ".");

        if (nurtureLevels == EngineSetting.ITEM_NURTURES_NONE && !sowsCoveringName.equals(EngineSetting.ITEM_SOWS_NONE))
            throwException("Item '" + itemName + "' \"sows\" a covering but \"nurtures\" no levels — name how many "
                    + "levels one use lays.");

        return nurtureLevels;
    }

    // Tool \\

    private short parseToolType(ArpgObjectStruct itemArpg) {

        String toolTypeName = ArpgUtility.getString(itemArpg, "tool", EngineSetting.ITEM_TOOL_NONE);

        if (toolTypeName.isEmpty())
            return EngineSetting.TOOL_NONE;

        return toolTypeManager.getToolTypeIDFromToolTypeName(toolTypeName);
    }

    private int parseToolTier(ArpgObjectStruct itemArpg, short toolTypeID, String itemName) {

        if (toolTypeID == EngineSetting.TOOL_NONE)
            return EngineSetting.DEFAULT_TOOL_TIER;

        int toolTier = ArpgUtility.getInt(itemArpg, "tool_tier", EngineSetting.DEFAULT_TOOL_TIER);

        if (toolTier < 0)
            throwException("Item '" + itemName + "' declares a negative \"tool_tier\".");

        return toolTier;
    }

    // Stacking \\

    private int parseStackSize(ArpgObjectStruct itemArpg, String itemName) {

        int stackSize = ArpgUtility.getInt(itemArpg, "stack", EngineSetting.DEFAULT_ITEM_STACK_SIZE);

        if (stackSize < 1 || stackSize > EngineSetting.MAX_ITEM_STACK_SIZE)
            throwException("Item '" + itemName + "' declares a \"stack\" outside 1 to "
                    + EngineSetting.MAX_ITEM_STACK_SIZE + ".");

        return stackSize;
    }

    // Model \\

    // A tool without a mesh of its own is drawn with its tool type's model
    private String resolveMeshPath(ArpgObjectStruct itemArpg, short toolTypeID, String itemName) {

        String meshPath = ArpgUtility.getString(itemArpg, "mesh", EngineSetting.ITEM_MESH_NONE);

        if (!meshPath.isEmpty())
            return meshPath;

        if (toolTypeID == EngineSetting.TOOL_NONE)
            return throwException("Item '" + itemName + "' names no \"mesh\".");

        String modelPath = toolTypeManager.getToolTypeHandleFromToolTypeID(toolTypeID).getDefaultModelPath();

        if (modelPath.isEmpty())
            return throwException("Item '" + itemName + "' names no \"mesh\", and its tool type has no \"model\".");

        return modelPath;
    }

    private SubVoxelModelStruct parseModel(String meshPath, String itemName) {

        File meshFile = ArpgUtility.resolveFile(meshRoot, meshPath);
        ArpgObjectStruct meshArpg = ArpgUtility.loadObject(meshFile);

        if (!subVoxelManager.hasSubVoxels(meshArpg))
            return throwException("Item '" + itemName + "' uses mesh '" + meshPath
                    + "', which is not a sub-voxel model. Every item is built from sub-voxels.");

        SubVoxelModelStruct model = subVoxelManager.parseModel(meshArpg);

        if (model.isEmpty())
            return throwException("Item '" + itemName + "' uses mesh '" + meshPath
                    + "', which holds no sub-voxel cubes or walls, so it cannot take up space in a container.");

        return model;
    }

    // Shape \\

    // The boxes the item's file claims inside its model grid, or the model's own cells when it names none
    private ItemShapeStruct parseShape(ArpgObjectStruct itemArpg, SubVoxelModelStruct model, String itemName) {

        if (!ArpgUtility.hasArray(itemArpg, "space"))
            return new ItemShapeStruct(model);

        ArpgArrayStruct spaceArpg = itemArpg.getAsArray("space");

        if (spaceArpg.isEmpty())
            return throwException("Item '" + itemName + "' declares a space with no boxes.");

        boolean[] occupied = new boolean[model.getSizeX() * model.getSizeY() * model.getSizeZ()];

        for (int i = 0; i < spaceArpg.size(); i++)
            claimBox(occupied, model, spaceArpg.get(i).getAsObject(), itemName);

        return new ItemShapeStruct(occupied, model.getSizeX(), model.getSizeY(), model.getSizeZ());
    }

    private void claimBox(boolean[] occupied, SubVoxelModelStruct model, ArpgObjectStruct boxArpg, String itemName) {

        Vector3Int size = parseVector(boxArpg);
        Vector3Int offset = parseBoxOffset(boxArpg);

        validateBox(
                size,
                offset,
                new Vector3Int(),
                new Vector3Int(model.getSizeX(), model.getSizeY(), model.getSizeZ()),
                "space",
                itemName);

        for (int z = offset.z; z < offset.z + size.z; z++)
            for (int y = offset.y; y < offset.y + size.y; y++)
                for (int x = offset.x; x < offset.x + size.x; x++)
                    occupied[ItemShapeStruct.toCellIndex(x, y, z, model.getSizeX(), model.getSizeY())] = true;
    }

    private Vector3Int parseBoxOffset(ArpgObjectStruct boxArpg) {
        return ArpgUtility.hasObject(boxArpg, "offset")
                ? parseVector(boxArpg.getAsObject("offset"))
                : new Vector3Int();
    }

    // A box must have a positive size and lie within cells [low, high) of the model grid on each axis
    private void validateBox(
            Vector3Int size,
            Vector3Int offset,
            Vector3Int low,
            Vector3Int high,
            String fieldName,
            String itemName) {

        if (size.x <= 0 || size.y <= 0 || size.z <= 0)
            throwException("Item '" + itemName + "' declares a " + fieldName + " box with a non-positive size.");

        if (offset.x < low.x || offset.y < low.y || offset.z < low.z
                || offset.x + size.x > high.x
                || offset.y + size.y > high.y
                || offset.z + size.z > high.z)
            throwException("Item '" + itemName + "' declares a " + fieldName + " box outside cells ("
                    + low.x + ", " + low.y + ", " + low.z + ") to (" + high.x + ", " + high.y + ", " + high.z
                    + ") of its model grid.");
    }

    // Container \\

    private ContainerSpaceStruct parseContainerSpace(
            ArpgObjectStruct itemArpg,
            SubVoxelModelStruct model,
            ItemShapeStruct shape,
            String itemName) {

        if (!ArpgUtility.hasObject(itemArpg, "container"))
            return null;

        if (!model.isSingleBlock())
            throwException("Item '" + itemName + "' is a container with a model of " + model.getBlocksX() + " x "
                    + model.getBlocksY() + " x " + model.getBlocksZ() + " blocks. A container's model spans one "
                    + "block, so its lid has a block of clearance to open into.");

        ArpgObjectStruct containerArpg = itemArpg.getAsObject("container");
        Vector3Int size = parseVector(containerArpg);

        if (size.x <= 0 || size.y <= 0 || size.z <= 0)
            throwException("Item '" + itemName + "' declares a container with a non-positive size.");

        if (size.x * size.y * size.z > EngineSetting.CONTAINER_MAX_CELLS)
            throwException("Item '" + itemName + "' declares a container over the limit of "
                    + EngineSetting.CONTAINER_MAX_CELLS + " sub-voxels.");

        String textureName = ArpgUtility.getString(containerArpg, "texture", EngineSetting.CONTAINER_TEXTURE_NONE);
        LidClearanceStruct lidClearance = parseLidClearance(containerArpg, model, shape, itemName);

        if (!ArpgUtility.hasObject(containerArpg, "offset"))
            return new ContainerSpaceStruct(
                    size,
                    null,
                    textureName.isEmpty() ? model.getPart(0).getTextureName() : textureName,
                    lidClearance);

        if (!textureName.isEmpty())
            throwException("Item '" + itemName + "' names a pocket texture for a container inside its own model.");

        Vector3Int offset = parseVector(containerArpg.getAsObject("offset"));

        validateSpace(model, size, offset, itemName);

        return new ContainerSpaceStruct(size, offset, null, lidClearance);
    }

    private Vector3Int parseVector(ArpgObjectStruct vectorArpg) {
        return new Vector3Int(
                ArpgUtility.validateInt(vectorArpg, "x"),
                ArpgUtility.validateInt(vectorArpg, "y"),
                ArpgUtility.validateInt(vectorArpg, "z"));
    }

    // A space inside the model must fit its grid and be clear of cubes, with walls only on its boundary
    private void validateSpace(SubVoxelModelStruct model, Vector3Int size, Vector3Int offset, String itemName) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int[] min = { offset.x, offset.y, offset.z };
        int[] max = { offset.x + size.x, offset.y + size.y, offset.z + size.z };

        for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++)
            if (min[axis] < 0 || max[axis] > resolution)
                throwException("Item '" + itemName + "' declares a container that does not fit inside its model.");

        for (int z = min[2]; z <= max[2]; z++)
            for (int y = min[1]; y <= max[1]; y++)
                for (int x = min[0]; x <= max[0]; x++) {

                    boolean inside = x < max[0] && y < max[1] && z < max[2];

                    if (inside && model.isFilled(x, y, z))
                        throwException("Item '" + itemName + "' has a cube at (" + x + ", " + y + ", " + z
                                + ") inside its container's space.");

                    int[] position = { x, y, z };

                    for (int axis = 0; axis < EngineSetting.SUB_VOXEL_AXIS_COUNT; axis++)
                        if (isInteriorWall(model, axis, position, min, max))
                            throwException("Item '" + itemName + "' has a wall at (" + x + ", " + y + ", " + z
                                    + ") cutting through its container's space.");
                }
    }

    private boolean isInteriorWall(SubVoxelModelStruct model, int axis, int[] position, int[] min, int[] max) {

        for (int i = 0; i < EngineSetting.SUB_VOXEL_AXIS_COUNT; i++) {

            boolean covered = i == axis
                    ? position[i] > min[i] && position[i] < max[i]
                    : position[i] < max[i];

            if (!covered)
                return false;
        }

        return model.hasWall(axis, position[0], position[1], position[2]);
    }

    // Lid \\

    private LidClearanceStruct parseLidClearance(
            ArpgObjectStruct containerArpg,
            SubVoxelModelStruct model,
            ItemShapeStruct shape,
            String itemName) {

        if (!ArpgUtility.hasArray(containerArpg, "lid") || containerArpg.getAsArray("lid").isEmpty())
            return throwException("Item '" + itemName + "' is a container without a lid. A container names "
                    + "the model parts of its lid, which must stand clear before it opens.");

        if (ArpgUtility.hasArray(containerArpg, "clearance"))
            return parseDeclaredClearance(containerArpg.getAsArray("clearance"), itemName);

        return resolveLidClearance(containerArpg.getAsArray("lid"), model, shape, itemName);
    }

    // The boxes the container lists, each allowed to reach one block past its model grid
    private LidClearanceStruct parseDeclaredClearance(ArpgArrayStruct clearanceArpg, String itemName) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int span = resolution * EngineSetting.ITEM_CLEARANCE_GRID_SPAN;
        boolean[] held = new boolean[span * span * span];
        IntArrayList cellX = new IntArrayList();
        IntArrayList cellY = new IntArrayList();
        IntArrayList cellZ = new IntArrayList();

        for (int i = 0; i < clearanceArpg.size(); i++) {

            ArpgObjectStruct boxArpg = clearanceArpg.get(i).getAsObject();
            Vector3Int size = parseVector(boxArpg);
            Vector3Int offset = parseBoxOffset(boxArpg);

            validateBox(
                    size,
                    offset,
                    new Vector3Int(-resolution),
                    new Vector3Int(span - resolution),
                    "clearance",
                    itemName);

            for (int z = offset.z; z < offset.z + size.z; z++)
                for (int y = offset.y; y < offset.y + size.y; y++)
                    for (int x = offset.x; x < offset.x + size.x; x++) {

                        int index = (x + resolution) + span * ((y + resolution) + span * (z + resolution));

                        if (held[index])
                            continue;

                        held[index] = true;
                        cellX.add(x);
                        cellY.add(y);
                        cellZ.add(z);
                    }
        }

        return new LidClearanceStruct(cellX.toIntArray(), cellY.toIntArray(), cellZ.toIntArray());
    }

    // The space the lid swings through — the cells over its upper surface, up to its shorter width and at most one
    // block over the model grid, that the container's own shape leaves open
    private LidClearanceStruct resolveLidClearance(
            ArpgArrayStruct lidArpg,
            SubVoxelModelStruct model,
            ItemShapeStruct shape,
            String itemName) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int top = resolution * EngineSetting.ITEM_CLEARANCE_GRID_SPAN - resolution;
        boolean[] lid = ItemShapeStruct.resolveOccupied(isolateParts(model, lidArpg, itemName));
        int swing = resolveLidSwing(lid);
        IntArrayList cellX = new IntArrayList();
        IntArrayList cellY = new IntArrayList();
        IntArrayList cellZ = new IntArrayList();

        for (int z = 0; z < resolution; z++)
            for (int x = 0; x < resolution; x++) {

                int reached = 0;

                for (int y = 0; y < resolution; y++) {

                    if (!lid[ItemShapeStruct.toCellIndex(x, y, z, resolution, resolution)])
                        continue;

                    if (y + 1 < resolution && lid[ItemShapeStruct.toCellIndex(x, y + 1, z, resolution, resolution)])
                        continue;

                    for (int rise = Math.max(y + 1, reached); rise <= y + swing && rise < top; rise++) {

                        reached = rise + 1;

                        if (shape.claimsGridCell(x, rise, z))
                            continue;

                        cellX.add(x);
                        cellY.add(rise);
                        cellZ.add(z);
                    }
                }
            }

        return new LidClearanceStruct(cellX.toIntArray(), cellY.toIntArray(), cellZ.toIntArray());
    }

    // How far the lid reaches up as it opens — its shorter width across the model grid
    private int resolveLidSwing(boolean[] lid) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int minX = resolution, minZ = resolution, maxX = -1, maxZ = -1;

        for (int z = 0; z < resolution; z++)
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {

                    if (!lid[ItemShapeStruct.toCellIndex(x, y, z, resolution, resolution)])
                        continue;

                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }

        return maxX < 0 ? 0 : Math.min(maxX - minX, maxZ - minZ) + 1;
    }

    // The model drawn in the world while open, without its lid's parts — null for an item that is no container
    private MeshData buildOpenMesh(
            ArpgObjectStruct itemArpg,
            SubVoxelModelStruct model,
            ContainerSpaceStruct containerSpace,
            String itemName) {

        if (containerSpace == null)
            return null;

        ArpgArrayStruct lidArpg = itemArpg.getAsObject("container").getAsArray("lid");
        SubVoxelModelStruct openModel = new SubVoxelModelStruct(model);

        for (int i = 0; i < lidArpg.size(); i++)
            openModel.removePart(findPart(openModel, lidArpg.get(i).getAsString(), itemName));

        if (openModel.isEmpty())
            return throwException("Item '" + itemName + "' is nothing but its lid, so it has nothing to show open.");

        return subVoxelManager.createMesh(openModel).getMeshData();
    }

    // The box of walls a pocket's space is shown in — null for a space inside the model
    private MeshData buildPocketMesh(ContainerSpaceStruct containerSpace) {

        if (containerSpace == null || !containerSpace.isPocket())
            return null;

        return subVoxelManager.createPocketMesh(
                containerSpace.getSize(), containerSpace.getPocketTextureName()).getMeshData();
    }

    // A copy of the model holding only the parts named
    private SubVoxelModelStruct isolateParts(SubVoxelModelStruct model, ArpgArrayStruct partsArpg, String itemName) {

        boolean[] kept = new boolean[model.getPartCount()];
        SubVoxelModelStruct isolated = new SubVoxelModelStruct(model);

        for (int i = 0; i < partsArpg.size(); i++)
            kept[findPart(model, partsArpg.get(i).getAsString(), itemName)] = true;

        for (int partIndex = isolated.getPartCount() - 1; partIndex >= 0; partIndex--)
            if (!kept[partIndex])
                isolated.removePart(partIndex);

        return isolated;
    }

    private int findPart(SubVoxelModelStruct model, String partName, String itemName) {

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            if (model.getPart(partIndex).getPartName().equals(partName))
                return partIndex;

        return throwException("Item '" + itemName + "' names part '" + partName + "', which its model lacks.");
    }

    // Actions \\

    private ObjectArrayList<ItemActionStruct> parseActions(
            ArpgObjectStruct itemArpg,
            SubVoxelModelStruct model,
            String itemName) {

        ObjectArrayList<ItemActionStruct> actions = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(itemArpg, "actions"))
            return actions;

        ArpgArrayStruct actionsArpg = itemArpg.getAsArray("actions");

        for (int i = 0; i < actionsArpg.size(); i++)
            actions.add(parseAction(actionsArpg.get(i).getAsObject(), model, itemName));

        return actions;
    }

    private ItemActionStruct parseAction(ArpgObjectStruct actionArpg, SubVoxelModelStruct model, String itemName) {

        ItemActionTrigger trigger = ArpgUtility.toEnum(
                ArpgUtility.validateString(actionArpg, "trigger"), ItemActionTrigger.class);
        String heldItemName = ArpgUtility.getString(actionArpg, "with", EngineSetting.ITEM_ACTION_HELD_ANY);
        boolean consumesHeld = ArpgUtility.getBoolean(actionArpg, "consumes", false);

        if (consumesHeld && heldItemName.equals(EngineSetting.ITEM_ACTION_HELD_ANY))
            throwException("Item '" + itemName + "' has an action that consumes what is held without naming it.");

        ItemShapeStruct partShape = ArpgUtility.hasArray(actionArpg, "parts")
                ? new ItemShapeStruct(isolateParts(model, actionArpg.getAsArray("parts"), itemName))
                : null;
        String becomesItemName = ArpgUtility.validateString(actionArpg, "becomes");

        if (!ArpgUtility.hasObject(actionArpg, "fire"))
            return new ItemActionStruct(
                    trigger,
                    heldItemName,
                    consumesHeld,
                    partShape,
                    becomesItemName,
                    EngineSetting.ITEM_ACTION_FIRE_NONE,
                    new Vector3(),
                    new Vector3(),
                    0f);

        ArpgObjectStruct fireArpg = actionArpg.getAsObject("fire");
        Vector3 aim = parseFloatVector(fireArpg, "aim");

        if (aim.lengthSquared() <= EngineSetting.DIVISION_EPSILON)
            throwException("Item '" + itemName + "' fires along an aim of no length.");

        return new ItemActionStruct(
                trigger,
                heldItemName,
                consumesHeld,
                partShape,
                becomesItemName,
                ArpgUtility.validateString(fireArpg, "item"),
                parseFloatVector(fireArpg, "muzzle"),
                aim.normalize(),
                ArpgUtility.validateFloat(fireArpg, "speed"));
    }

    private Vector3 parseFloatVector(ArpgObjectStruct objectArpg, String key) {

        ArpgArrayStruct vectorArpg = ArpgUtility.validateArray(objectArpg, key, EngineSetting.AXIS_COUNT);

        return new Vector3(
                vectorArpg.get(0).getAsFloat(),
                vectorArpg.get(1).getAsFloat(),
                vectorArpg.get(2).getAsFloat());
    }

    // Text \\

    private String toDisplayName(String localName) {

        String spaced = String.join(
                EngineSetting.ITEM_NAME_WORD_SEPARATOR,
                localName.split(EngineSetting.ITEM_NAME_WORD_BOUNDARY_PATTERN));

        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
