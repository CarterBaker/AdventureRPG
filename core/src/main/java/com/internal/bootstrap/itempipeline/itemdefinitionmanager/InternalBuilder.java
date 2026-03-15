package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.util.ItemRegistryUtility;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses item definition JSON files and builds ItemDefinitionHandle instances.
     * Each JSON file may contain multiple item entries under an 'items' array.
     * Resolves mesh and material references from their respective managers.
     * Bootstrap-only.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
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
        String itemName = pathPrefix + "/" + localName;
        int itemID = ItemRegistryUtility.toItemIntID(itemName);
        float weight = JsonUtility.getFloat(itemJson, "weight", 1.0f);
        boolean twoHanded = JsonUtility.getBoolean(itemJson, "two_handed", false);
        boolean isBackpack = JsonUtility.getBoolean(itemJson, "is_backpack", false);

        String meshPath = JsonUtility.validateString(itemJson, "mesh");
        int meshID = meshManager.getMeshIDFromMeshName(meshPath);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshID(meshID);

        String materialPath = JsonUtility.getString(
                itemJson, "material", EngineSetting.DEFAULT_ITEM_MATERIAL);
        int materialID = materialManager.getMaterialIDFromMaterialName(materialPath);

        ItemDefinitionData itemDefinitionData = new ItemDefinitionData(
                itemName,
                itemID,
                weight,
                twoHanded,
                isBackpack,
                meshHandle,
                materialID);

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemDefinitionData);

        return item;
    }
}