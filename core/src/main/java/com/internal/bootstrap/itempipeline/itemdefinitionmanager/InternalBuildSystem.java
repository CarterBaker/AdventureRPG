package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.itempipeline.util.ItemRegistryUtility;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalBuildSystem extends SystemPackage {

    // Compile \

    ObjectArrayList<ItemDefinitionData> compileItems(File jsonFile, File root) {
        // e.g. root=items/ file=items/weapons/iron.json -> prefix="weapons/iron"
        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);

        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);
        JsonArray itemArray = JsonUtility.validateArray(rootJson, "items");

        ObjectArrayList<ItemDefinitionData> items = new ObjectArrayList<>();

        for (int i = 0; i < itemArray.size(); i++) {
            JsonObject itemJson = itemArray.get(i).getAsJsonObject();
            ItemDefinitionData item = parseItem(itemJson, pathPrefix);
            if (item != null)
                items.add(item);
        }

        return items;
    }

    // Parse \

    private ItemDefinitionData parseItem(JsonObject itemJson, String pathPrefix) {
        // Full name: "weapons/iron/sword", "weapons/iron/axe", etc.
        String localName = JsonUtility.validateString(itemJson, "name");
        String itemName = pathPrefix + "/" + localName;
        int itemID = ItemRegistryUtility.toItemIntID(itemName);

        ItemDefinitionData item = create(ItemDefinitionData.class);
        item.constructor(itemName, itemID);
        return item;
    }

}