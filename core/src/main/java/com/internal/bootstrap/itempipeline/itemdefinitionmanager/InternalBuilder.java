package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.util.ItemRegistryUtility;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

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

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemName, itemID, weight, twoHanded, isBackpack);
        return item;
    }
}