package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.ContainerSpaceStruct;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemDefinitionBuilder extends BuilderPackage {

    /*
     * Parses item definition ARPG files and builds ItemDefinitionHandle instances.
     * Each ARPG file may contain multiple item entries under an 'items' array.
     * Resolves mesh and material references from their respective managers,
     * and reads the item's mesh file through SubVoxelManager to find the shape
     * it fills inside a container — every item mesh must be a sub-voxel model.
     * A container's space either lies inside that model at an offset, clear
     * of its cubes, or is a pocket shown as its own box of walls; either may
     * name the parts of its lid, left off its model while it stands open. An item
     * without a display name is titled from its local name split into words.
     * Bootstrap-only.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private SubVoxelManager subVoxelManager;

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
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
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
        int itemID = ItemRegistryUtility.toItemIntID(itemName);

        String displayName = ArpgUtility.getString(itemArpg, "display_name", EngineSetting.ITEM_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = toDisplayName(localName);

        String description = ArpgUtility.getString(itemArpg, "description", EngineSetting.ITEM_DESCRIPTION_NONE);
        ItemCategory category = ArpgUtility.getEnum(itemArpg, "category", ItemCategory.class, ItemCategory.MISC);

        float weight = ArpgUtility.getFloat(itemArpg, "weight", 1.0f);
        boolean twoHanded = ArpgUtility.getBoolean(itemArpg, "two_handed", false);

        EquipmentType equipmentType = ArpgUtility.getEnum(itemArpg, "equip", EquipmentType.class, EquipmentType.NONE);
        float[] stats = parseStats(itemArpg);

        String meshPath = ArpgUtility.validateString(itemArpg, "mesh");
        int meshID = meshManager.getMeshIDFromMeshName(meshPath);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshID(meshID);
        SubVoxelModelStruct model = parseModel(meshPath, itemName);
        ItemShapeStruct shape = new ItemShapeStruct(model);
        ContainerSpaceStruct containerSpace = parseContainerSpace(itemArpg, model, itemName);
        MeshData openMeshData = buildOpenMesh(itemArpg, model, containerSpace, itemName);
        MeshData pocketMeshData = buildPocketMesh(containerSpace);

        if (equipmentType == EquipmentType.BACKPACK && containerSpace == null)
            throwException("Item '" + itemName + "' is worn as a backpack but declares no container.");

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
                equipmentType,
                stats,
                shape,
                containerSpace,
                meshHandle,
                openMeshData,
                pocketMeshData,
                materialID);

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

    // Container \\

    private ContainerSpaceStruct parseContainerSpace(
            ArpgObjectStruct itemArpg,
            SubVoxelModelStruct model,
            String itemName) {

        if (!ArpgUtility.hasObject(itemArpg, "container"))
            return null;

        ArpgObjectStruct containerArpg = itemArpg.getAsObject("container");
        Vector3Int size = parseVector(containerArpg);

        if (size.x <= 0 || size.y <= 0 || size.z <= 0)
            throwException("Item '" + itemName + "' declares a container with a non-positive size.");

        if (size.x * size.y * size.z > EngineSetting.CONTAINER_MAX_CELLS)
            throwException("Item '" + itemName + "' declares a container over the limit of "
                    + EngineSetting.CONTAINER_MAX_CELLS + " sub-voxels.");

        String textureName = ArpgUtility.getString(containerArpg, "texture", EngineSetting.CONTAINER_TEXTURE_NONE);

        if (!ArpgUtility.hasObject(containerArpg, "offset"))
            return new ContainerSpaceStruct(
                    size, null, textureName.isEmpty() ? model.getPart(0).getTextureName() : textureName);

        if (!textureName.isEmpty())
            throwException("Item '" + itemName + "' names a pocket texture for a container inside its own model.");

        Vector3Int offset = parseVector(containerArpg.getAsObject("offset"));

        validateSpace(model, size, offset, itemName);

        return new ContainerSpaceStruct(size, offset, null);
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

    // The model drawn in the world while open, without its lid's parts — null when it names none
    private MeshData buildOpenMesh(
            ArpgObjectStruct itemArpg,
            SubVoxelModelStruct model,
            ContainerSpaceStruct containerSpace,
            String itemName) {

        if (containerSpace == null)
            return null;

        ArpgObjectStruct containerArpg = itemArpg.getAsObject("container");

        if (!ArpgUtility.hasArray(containerArpg, "lid") || containerArpg.getAsArray("lid").isEmpty())
            return null;

        ArpgArrayStruct lidArpg = containerArpg.getAsArray("lid");
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

    private int findPart(SubVoxelModelStruct model, String partName, String itemName) {

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            if (model.getPart(partIndex).getPartName().equals(partName))
                return partIndex;

        return throwException("Item '" + itemName + "' names lid part '" + partName + "', which its model lacks.");
    }

    // Text \\

    private String toDisplayName(String localName) {

        String spaced = String.join(
                EngineSetting.ITEM_NAME_WORD_SEPARATOR,
                localName.split(EngineSetting.ITEM_NAME_WORD_BOUNDARY_PATTERN));

        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
