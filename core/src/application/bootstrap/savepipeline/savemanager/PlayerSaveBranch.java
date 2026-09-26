package application.bootstrap.savepipeline.savemanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.container.ContainerSlotStruct;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.util.io.JsonUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class PlayerSaveBranch extends BranchPackage {

    /*
     * Captures a window's player as character save JSON and writes it to
     * disk. The character records its body size, its weight and, when it has
     * one, its appearance — skin and hair color, head proportion, and the
     * feature worn in every filled slot. The location records the world, the
     * chunk coordinate, and the chunk-local position the player stands at.
     * The inventory records the item in every filled equipment slot, the
     * slots left hidden, and — for any container item — everything packed
     * inside it with where it rests, however deeply nested.
     */

    // Management \\

    void save(File characterFile, EntityInstance player) {

        JsonObject playerJson = new JsonObject();
        playerJson.add("character", buildCharacter(player));
        playerJson.add("location", buildLocation(player));
        playerJson.add("inventory", buildInventory(player.getInventoryHandle()));

        JsonUtility.writeJsonObject(characterFile, playerJson, internal.gson);
    }

    // Build \\

    private JsonObject buildCharacter(EntityInstance player) {

        JsonObject characterJson = new JsonObject();
        characterJson.add("size", buildVector(player.getSize()));
        characterJson.addProperty("weight", player.getWeight());

        if (player.hasAppearance())
            characterJson.add("appearance", buildAppearance(player.getAppearanceHandle()));

        return characterJson;
    }

    private JsonObject buildAppearance(AppearanceHandle appearanceHandle) {

        JsonObject appearanceJson = new JsonObject();
        appearanceJson.add("skin_color", buildColor(appearanceHandle.getSkinColor()));
        appearanceJson.add("hair_color", buildColor(appearanceHandle.getHairColor()));
        appearanceJson.add("head_proportion", buildVector(appearanceHandle.getHeadProportion()));
        appearanceJson.add("features", buildFeatures(appearanceHandle));
        return appearanceJson;
    }

    private JsonObject buildFeatures(AppearanceHandle appearanceHandle) {

        JsonObject featuresJson = new JsonObject();

        for (FeatureSlot featureSlot : FeatureSlot.values())
            if (appearanceHandle.hasFeature(featureSlot))
                featuresJson.addProperty(
                        featureSlot.name().toLowerCase(),
                        appearanceHandle.getFeature(featureSlot).getFeatureName());

        return featuresJson;
    }

    private JsonObject buildInventory(InventoryHandle inventoryHandle) {

        JsonObject equipmentJson = new JsonObject();
        JsonArray hiddenJson = new JsonArray();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {

            if (inventoryHandle.hasItem(equipmentSlot))
                equipmentJson.add(
                        JsonUtility.toEnumName(equipmentSlot),
                        buildItem(inventoryHandle.getItem(equipmentSlot)));

            if (inventoryHandle.isHidden(equipmentSlot))
                hiddenJson.add(JsonUtility.toEnumName(equipmentSlot));
        }

        JsonObject inventoryJson = new JsonObject();
        inventoryJson.add("equipment", equipmentJson);
        inventoryJson.add("hidden", hiddenJson);
        return inventoryJson;
    }

    private JsonObject buildItem(ItemInstance itemInstance) {

        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("item", itemInstance.getItemDefinitionHandle().getItemName());

        if (itemInstance.hasContainer())
            itemJson.add("contents", buildContents(itemInstance.getContainerInstance()));

        return itemJson;
    }

    private JsonArray buildContents(ContainerInstance containerInstance) {

        JsonArray contentsJson = new JsonArray();

        for (int i = 0; i < containerInstance.getSlots().size(); i++) {

            ContainerSlotStruct slot = containerInstance.getSlots().get(i);
            JsonObject slotJson = buildItem(slot.getItemInstance());
            slotJson.addProperty("x", slot.getX());
            slotJson.addProperty("y", slot.getY());
            slotJson.addProperty("z", slot.getZ());
            slotJson.addProperty("rotation", slot.getRotation());
            contentsJson.add(slotJson);
        }

        return contentsJson;
    }

    private JsonObject buildLocation(EntityInstance player) {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        long chunkCoordinate = worldPositionStruct.getChunkCoordinate();

        JsonObject chunkJson = new JsonObject();
        chunkJson.addProperty("x", Coordinate2Long.unpackX(chunkCoordinate));
        chunkJson.addProperty("z", Coordinate2Long.unpackY(chunkCoordinate));

        JsonObject locationJson = new JsonObject();
        locationJson.addProperty("world", player.getWorldHandle().getWorldName());
        locationJson.add("chunk", chunkJson);
        locationJson.add("position", buildVector(worldPositionStruct.getPosition()));
        return locationJson;
    }

    // Utility \\

    private JsonObject buildVector(Vector3 vector) {

        JsonObject vectorJson = new JsonObject();
        vectorJson.addProperty("x", vector.x);
        vectorJson.addProperty("y", vector.y);
        vectorJson.addProperty("z", vector.z);
        return vectorJson;
    }

    private JsonObject buildColor(Color color) {

        JsonObject colorJson = new JsonObject();
        colorJson.addProperty("r", color.r);
        colorJson.addProperty("g", color.g);
        colorJson.addProperty("b", color.b);
        return colorJson;
    }
}