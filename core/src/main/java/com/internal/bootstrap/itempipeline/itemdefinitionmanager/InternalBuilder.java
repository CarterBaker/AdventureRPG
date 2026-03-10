package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.util.ItemRegistryUtility;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
    }

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

    private ItemDefinitionHandle parseItem(JsonObject itemJson, String pathPrefix) {
        String localName = JsonUtility.validateString(itemJson, "name");
        String itemName = pathPrefix + "/" + localName;
        int itemID = ItemRegistryUtility.toItemIntID(itemName);

        float weight = JsonUtility.getFloat(itemJson, "weight", 1.0f);
        boolean twoHanded = JsonUtility.getBoolean(itemJson, "two_handed", false);
        boolean isBackpack = JsonUtility.getBoolean(itemJson, "is_backpack", false);

        // Mesh
        String meshPath = JsonUtility.validateString(itemJson, "mesh");
        int meshHandleID = meshManager.getMeshHandleIDFromMeshName(meshPath);
        MeshHandle meshHandle = meshManager.getMeshHandleFromMeshHandleID(meshHandleID);

        // Material — optional, falls back to engine default
        String materialPath = JsonUtility.getString(itemJson, "material",
                EngineSetting.DEFAULT_ITEM_MATERIAL);
        int materialID = materialManager.getMaterialIDFromMaterialName(materialPath);

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemName, itemID, weight, twoHanded, isBackpack, meshHandle, materialID);
        return item;
    }
}