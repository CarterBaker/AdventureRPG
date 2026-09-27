package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

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
     * An item without a display name is titled from its local name split into
     * words. Bootstrap-only.
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
        ItemShapeStruct shape = parseShape(meshPath, itemName);
        Vector3Int containerSize = parseContainerSize(itemArpg, itemName);

        if (equipmentType == EquipmentType.BACKPACK && containerSize == null)
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
                containerSize,
                meshHandle,
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

    private ItemShapeStruct parseShape(String meshPath, String itemName) {

        File meshFile = ArpgUtility.resolveFile(meshRoot, meshPath);
        ArpgObjectStruct meshArpg = ArpgUtility.loadObject(meshFile);

        if (!subVoxelManager.hasSubVoxels(meshArpg))
            return throwException("Item '" + itemName + "' uses mesh '" + meshPath
                    + "', which is not a sub-voxel model. Every item is built from sub-voxels.");

        SubVoxelModelStruct model = subVoxelManager.parseModel(meshArpg);

        if (model.isEmpty())
            return throwException("Item '" + itemName + "' uses mesh '" + meshPath
                    + "', which fills no sub-voxel cells, so it cannot take up space in a container.");

        return new ItemShapeStruct(model);
    }

    private Vector3Int parseContainerSize(ArpgObjectStruct itemArpg, String itemName) {

        if (!ArpgUtility.hasObject(itemArpg, "container"))
            return null;

        ArpgObjectStruct containerArpg = itemArpg.getAsObject("container");
        Vector3Int containerSize = new Vector3Int(
                ArpgUtility.validateInt(containerArpg, "x"),
                ArpgUtility.validateInt(containerArpg, "y"),
                ArpgUtility.validateInt(containerArpg, "z"));

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
