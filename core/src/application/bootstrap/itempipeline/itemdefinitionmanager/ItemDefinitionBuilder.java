package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
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
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemDefinitionBuilder extends BuilderPackage {

    /*
     * Parses item definition JSON files and builds ItemDefinitionHandle instances.
     * Each JSON file may contain multiple item entries under an 'items' array.
     * Resolves mesh and material references from their respective managers,
     * and reads the item's mesh file as sub-voxels through SubVoxelManager to
     * find the shape it fills inside a container. An item without a display
     * name is titled from its local name split into words. Bootstrap-only.
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
        this.meshRoot = new File(EngineSetting.MESH_JSON_PATH);
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Build \\

    ObjectArrayList<ItemDefinitionHandle> build(File jsonFile, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);
        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);
        JsonArray itemArray = JsonUtility.validateArray(rootJson, "items");
        ObjectArrayList<ItemDefinitionHandle> items = new ObjectArrayList<>();

        for (int i = 0; i < itemArray.size(); i++) {
            JsonObject itemJson = itemArray.get(i).getAsJsonObject();
            ItemDefinitionHandle item = parseItem(itemJson, pathPrefix);
            if (item != null)
                items.add(item);
        }

        return items;
    }

    // Parse \\

    private ItemDefinitionHandle parseItem(JsonObject itemJson, String pathPrefix) {

        String localName = JsonUtility.validateString(itemJson, "name");
        String itemName = ItemRegistryUtility.toItemName(pathPrefix, localName);
        int itemID = ItemRegistryUtility.toItemIntID(itemName);

        String displayName = JsonUtility.getString(itemJson, "display_name", EngineSetting.ITEM_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = toDisplayName(localName);

        String description = JsonUtility.getString(itemJson, "description", EngineSetting.ITEM_DESCRIPTION_NONE);
        ItemCategory category = JsonUtility.getEnum(itemJson, "category", ItemCategory.class, ItemCategory.MISC);

        float weight = JsonUtility.getFloat(itemJson, "weight", 1.0f);
        boolean twoHanded = JsonUtility.getBoolean(itemJson, "two_handed", false);

        EquipmentType equipmentType = JsonUtility.getEnum(itemJson, "equip", EquipmentType.class, EquipmentType.NONE);
        float[] stats = parseStats(itemJson);

        String meshPath = JsonUtility.validateString(itemJson, "mesh");
        int meshID = meshManager.getMeshIDFromMeshName(meshPath);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshID(meshID);
        ItemShapeStruct shape = parseShape(meshPath, itemName);
        Vector3Int containerSize = parseContainerSize(itemJson, itemName);

        if (equipmentType == EquipmentType.BACKPACK && containerSize == null)
            throwException("Item '" + itemName + "' is worn as a backpack but declares no container.");

        String materialPath = JsonUtility.getString(
                itemJson, "material", EngineSetting.DEFAULT_ITEM_MATERIAL);
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
                containerSize,
                meshHandle,
                materialID);

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemDefinitionData);

        return item;
    }

    private float[] parseStats(JsonObject itemJson) {

        float[] stats = new float[ItemStat.values().length];

        if (!JsonUtility.hasObject(itemJson, "stats"))
            return stats;

        JsonObject statsJson = itemJson.getAsJsonObject("stats");

        for (String statName : statsJson.keySet())
            stats[JsonUtility.toEnum(statName, ItemStat.class).ordinal()] = statsJson.get(statName).getAsFloat();

        return stats;
    }

    private ItemShapeStruct parseShape(String meshPath, String itemName) {

        File meshFile = new File(meshRoot, meshPath + "." + EngineSetting.MESH_FILE_EXTENSION);
        SubVoxelModelStruct model = subVoxelManager.resolveModel(
                JsonUtility.loadJsonObject(meshFile),
                EngineSetting.ITEM_SHAPE_FALLBACK_TEXTURE);

        if (model == null || model.isEmpty())
            return throwException("Item '" + itemName + "' uses mesh '" + meshPath
                    + "', which fills no sub-voxel cells, so it cannot take up space in a container.");

        return new ItemShapeStruct(model);
    }

    private Vector3Int parseContainerSize(JsonObject itemJson, String itemName) {

        if (!JsonUtility.hasObject(itemJson, "container"))
            return null;

        JsonObject containerJson = itemJson.getAsJsonObject("container");
        Vector3Int containerSize = new Vector3Int(
                JsonUtility.validateInt(containerJson, "x"),
                JsonUtility.validateInt(containerJson, "y"),
                JsonUtility.validateInt(containerJson, "z"));

        if (containerSize.x <= 0 || containerSize.y <= 0 || containerSize.z <= 0)
            throwException("Item '" + itemName + "' declares a container with a non-positive size.");

        if (containerSize.x * containerSize.y * containerSize.z > EngineSetting.CONTAINER_MAX_CELLS)
            throwException("Item '" + itemName + "' declares a container over the limit of "
                    + EngineSetting.CONTAINER_MAX_CELLS + " sub-voxels.");

        return containerSize;
    }

    // Text \\

    private String toDisplayName(String localName) {

        String spaced = String.join(
                EngineSetting.ITEM_NAME_WORD_SEPARATOR,
                localName.split(EngineSetting.ITEM_NAME_WORD_BOUNDARY_PATTERN));

        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
