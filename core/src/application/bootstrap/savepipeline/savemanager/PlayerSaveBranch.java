package application.bootstrap.savepipeline.savemanager;

import java.io.File;

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
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class PlayerSaveBranch extends BranchPackage {

    /*
     * Captures a window's player as an ARPG character save and writes it to
     * disk. The character records its body size, its weight and, when it has
     * one, its appearance — skin and hair color, head proportion, and the
     * feature worn in every filled slot. The location records the world, the
     * chunk coordinate, and the chunk-local position the player stands at.
     * The inventory records the item in every filled equipment slot, how
     * many a stack holds, the slots left hidden, and — for any container
     * item — everything packed inside it with where it rests, however deeply
     * nested.
     */

    // Management \\

    void save(File characterFile, EntityInstance player) {

        ArpgObjectStruct playerArpg = new ArpgObjectStruct();
        playerArpg.add("character", buildCharacter(player));
        playerArpg.add("location", buildLocation(player));
        playerArpg.add("inventory", buildInventory(player.getInventoryHandle()));

        ArpgUtility.writeObject(characterFile, playerArpg);
    }

    // Build \\

    private ArpgObjectStruct buildCharacter(EntityInstance player) {

        ArpgObjectStruct characterArpg = new ArpgObjectStruct();
        characterArpg.add("size", buildVector(player.getSize()));
        characterArpg.addProperty("weight", player.getWeight());

        if (player.hasAppearance())
            characterArpg.add("appearance", buildAppearance(player.getAppearanceHandle()));

        return characterArpg;
    }

    private ArpgObjectStruct buildAppearance(AppearanceHandle appearanceHandle) {

        ArpgObjectStruct appearanceArpg = new ArpgObjectStruct();
        appearanceArpg.add("skin_color", buildColor(appearanceHandle.getSkinColor()));
        appearanceArpg.add("hair_color", buildColor(appearanceHandle.getHairColor()));
        appearanceArpg.add("head_proportion", buildVector(appearanceHandle.getHeadProportion()));
        appearanceArpg.add("features", buildFeatures(appearanceHandle));
        return appearanceArpg;
    }

    private ArpgObjectStruct buildFeatures(AppearanceHandle appearanceHandle) {

        ArpgObjectStruct featuresArpg = new ArpgObjectStruct();

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (appearanceHandle.hasFeature(featureSlot))
                featuresArpg.addProperty(
                        featureSlot.name().toLowerCase(),
                        appearanceHandle.getFeature(featureSlot).getFeatureName());

        return featuresArpg;
    }

    private ArpgObjectStruct buildInventory(InventoryHandle inventoryHandle) {

        ArpgObjectStruct equipmentArpg = new ArpgObjectStruct();
        ArpgArrayStruct hiddenArpg = new ArpgArrayStruct();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            if (inventoryHandle.hasItem(equipmentSlot))
                equipmentArpg.add(
                        ArpgUtility.toEnumName(equipmentSlot),
                        buildItem(inventoryHandle.getItem(equipmentSlot)));

            if (inventoryHandle.isHidden(equipmentSlot))
                hiddenArpg.add(ArpgUtility.toEnumName(equipmentSlot));
        }

        ArpgObjectStruct inventoryArpg = new ArpgObjectStruct();
        inventoryArpg.add("equipment", equipmentArpg);
        inventoryArpg.add("hidden", hiddenArpg);
        return inventoryArpg;
    }

    private ArpgObjectStruct buildItem(ItemInstance itemInstance) {

        ArpgObjectStruct itemArpg = new ArpgObjectStruct();
        itemArpg.addProperty("item", itemInstance.getItemDefinitionHandle().getItemName());

        if (itemInstance.getStackCount() > 1)
            itemArpg.addProperty("count", itemInstance.getStackCount());

        if (itemInstance.hasContainer())
            itemArpg.add("contents", buildContents(itemInstance.getContainerInstance()));

        return itemArpg;
    }

    private ArpgArrayStruct buildContents(ContainerInstance containerInstance) {

        ArpgArrayStruct contentsArpg = new ArpgArrayStruct();

        for (int i = 0; i < containerInstance.getSlots().size(); i++) {

            ContainerSlotStruct slot = containerInstance.getSlots().get(i);
            ArpgObjectStruct slotArpg = buildItem(slot.getItemInstance());
            slotArpg.addProperty("x", slot.getX());
            slotArpg.addProperty("y", slot.getY());
            slotArpg.addProperty("z", slot.getZ());
            slotArpg.addProperty("rotation", slot.getRotation());
            contentsArpg.add(slotArpg);
        }

        return contentsArpg;
    }

    private ArpgObjectStruct buildLocation(EntityInstance player) {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        long chunkCoordinate = worldPositionStruct.getChunkCoordinate();

        ArpgObjectStruct chunkArpg = new ArpgObjectStruct();
        chunkArpg.addProperty("x", Coordinate2Long.unpackX(chunkCoordinate));
        chunkArpg.addProperty("z", Coordinate2Long.unpackY(chunkCoordinate));

        ArpgObjectStruct locationArpg = new ArpgObjectStruct();
        locationArpg.addProperty("world", player.getWorldHandle().getWorldName());
        locationArpg.add("chunk", chunkArpg);
        locationArpg.add("position", buildVector(worldPositionStruct.getPosition()));
        return locationArpg;
    }

    // Utility \\

    private ArpgObjectStruct buildVector(Vector3 vector) {

        ArpgObjectStruct vectorArpg = new ArpgObjectStruct();
        vectorArpg.addProperty("x", vector.x);
        vectorArpg.addProperty("y", vector.y);
        vectorArpg.addProperty("z", vector.z);
        return vectorArpg;
    }

    private ArpgObjectStruct buildColor(Color color) {

        ArpgObjectStruct colorArpg = new ArpgObjectStruct();
        colorArpg.addProperty("r", color.r);
        colorArpg.addProperty("g", color.g);
        colorArpg.addProperty("b", color.b);
        return colorArpg;
    }
}