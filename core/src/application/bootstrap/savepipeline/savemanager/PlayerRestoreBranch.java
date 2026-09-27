package application.bootstrap.savepipeline.savemanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class PlayerRestoreBranch extends BranchPackage {

    /*
     * Applies a character save to a window's player in place. The save is fully
     * validated first, so a stale one changes nothing; the location is wrapped
     * into world bounds; inventory restores leniently, logging and skipping
     * items that no longer exist or fit.
     */

    // Internal
    private FeatureManager featureManager;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
    }

    // Management \\

    boolean restore(ArpgObjectStruct playerArpg, EntityInstance player) {

        if (!isPlayerValid(playerArpg, player))
            return false;

        restoreLocation(ArpgUtility.validateObject(playerArpg, "location"), player);
        restoreCharacter(ArpgUtility.validateObject(playerArpg, "character"), player);
        restoreInventory(playerArpg, player.getInventoryHandle());

        return true;
    }

    // Location \\

    private void restoreLocation(ArpgObjectStruct locationArpg, EntityInstance player) {

        ArpgObjectStruct chunkArpg = ArpgUtility.validateObject(locationArpg, "chunk");
        long chunkCoordinate = Coordinate2Long.pack(
                ArpgUtility.validateInt(chunkArpg, "x"),
                ArpgUtility.validateInt(chunkArpg, "z"));

        player.setLocation(
                WorldWrapUtility.wrapAroundChunk(parseVector(locationArpg, "position")),
                WorldWrapUtility.wrapAroundWorld(player.getWorldHandle(), chunkCoordinate));
    }

    // Character \\

    private void restoreCharacter(ArpgObjectStruct characterArpg, EntityInstance player) {

        player.setSize(parseVector(characterArpg, "size"));
        player.setWeight(ArpgUtility.validateFloat(characterArpg, "weight"));

        if (player.hasAppearance())
            restoreAppearance(ArpgUtility.validateObject(characterArpg, "appearance"), player.getAppearanceHandle());
    }

    private void restoreAppearance(ArpgObjectStruct appearanceArpg, AppearanceHandle appearanceHandle) {

        ArpgObjectStruct skinColorArpg = ArpgUtility.validateObject(appearanceArpg, "skin_color");
        ArpgObjectStruct hairColorArpg = ArpgUtility.validateObject(appearanceArpg, "hair_color");
        Vector3 headProportion = parseVector(appearanceArpg, "head_proportion");

        appearanceHandle.setSkinColor(
                ArpgUtility.validateFloat(skinColorArpg, "r"),
                ArpgUtility.validateFloat(skinColorArpg, "g"),
                ArpgUtility.validateFloat(skinColorArpg, "b"));
        appearanceHandle.setHairColor(
                ArpgUtility.validateFloat(hairColorArpg, "r"),
                ArpgUtility.validateFloat(hairColorArpg, "g"),
                ArpgUtility.validateFloat(hairColorArpg, "b"));
        appearanceHandle.setHeadProportion(headProportion.x, headProportion.y, headProportion.z);

        restoreFeatures(ArpgUtility.validateObject(appearanceArpg, "features"), appearanceHandle);
    }

    private void restoreFeatures(ArpgObjectStruct featuresArpg, AppearanceHandle appearanceHandle) {

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = featureSlot.name().toLowerCase();

            if (featuresArpg.has(key))
                appearanceHandle.setFeature(featureManager.getFeatureHandleFromFeatureName(
                        ArpgUtility.validateString(featuresArpg, key)));
            else
                appearanceHandle.clearFeature(featureSlot);
        }
    }

    // Inventory \\

    private void restoreInventory(ArpgObjectStruct playerArpg, InventoryHandle inventoryHandle) {

        inventoryHandle.clear();

        if (!ArpgUtility.hasObject(playerArpg, "inventory"))
            return;

        ArpgObjectStruct inventoryArpg = playerArpg.getAsObject("inventory");

        if (ArpgUtility.hasObject(inventoryArpg, "equipment"))
            restoreEquipment(inventoryArpg.getAsObject("equipment"), inventoryHandle);

        if (ArpgUtility.hasArray(inventoryArpg, "hidden"))
            restoreHidden(inventoryArpg.getAsArray("hidden"), inventoryHandle);
    }

    private void restoreEquipment(ArpgObjectStruct equipmentArpg, InventoryHandle inventoryHandle) {

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            String key = ArpgUtility.toEnumName(equipmentSlot);

            if (!ArpgUtility.hasObject(equipmentArpg, key))
                continue;

            ItemInstance itemInstance = restoreItem(equipmentArpg.getAsObject(key));

            if (itemInstance == null)
                continue;

            if (inventoryHandle.canEquip(equipmentSlot, itemInstance))
                inventoryHandle.equip(equipmentSlot, itemInstance);
            else
                errorLog("Saved item '" + itemInstance.getItemDefinitionHandle().getItemName()
                        + "' no longer fits equipment slot '" + key + "' and was left out.");
        }
    }

    private void restoreHidden(ArpgArrayStruct hiddenArpg, InventoryHandle inventoryHandle) {

        for (int i = 0; i < hiddenArpg.size(); i++) {

            ArpgElementStruct slotArpg = hiddenArpg.get(i);

            for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
                if (slotArpg.isValue()
                        && ArpgUtility.toEnumName(equipmentSlot).equals(slotArpg.getAsString()))
                    inventoryHandle.setHidden(equipmentSlot, true);
        }
    }

    private ItemInstance restoreItem(ArpgObjectStruct itemArpg) {

        if (!ArpgUtility.hasString(itemArpg, "item"))
            return null;

        String itemName = itemArpg.get("item").getAsString();

        if (!itemDefinitionManager.hasItem(itemName)) {
            errorLog("Saved item '" + itemName + "' no longer exists and was left out.");
            return null;
        }

        ItemInstance itemInstance = itemManager.createItem(itemName);

        if (itemInstance.hasContainer() && ArpgUtility.hasArray(itemArpg, "contents"))
            restoreContents(itemArpg.getAsArray("contents"), itemInstance.getContainerInstance());

        return itemInstance;
    }

    private void restoreContents(ArpgArrayStruct contentsArpg, ContainerInstance containerInstance) {

        for (int i = 0; i < contentsArpg.size(); i++) {

            if (!contentsArpg.get(i).isObject())
                continue;

            ArpgObjectStruct slotArpg = contentsArpg.get(i).getAsObject();
            ItemInstance itemInstance = restoreItem(slotArpg);

            if (itemInstance == null)
                continue;

            if (isPlacementValid(slotArpg, itemInstance, containerInstance)) {
                containerInstance.place(
                        itemInstance,
                        slotArpg.get("x").getAsInt(),
                        slotArpg.get("y").getAsInt(),
                        slotArpg.get("z").getAsInt(),
                        slotArpg.get("rotation").getAsInt());
                continue;
            }

            if (containerInstance.autoPlace(itemInstance) == null)
                errorLog("Saved item '" + itemInstance.getItemDefinitionHandle().getItemName()
                        + "' no longer fits its container and was left out.");
        }
    }

    private boolean isPlacementValid(
            ArpgObjectStruct slotArpg,
            ItemInstance itemInstance,
            ContainerInstance containerInstance) {
        return ArpgUtility.hasNumber(slotArpg, "x")
                && ArpgUtility.hasNumber(slotArpg, "y")
                && ArpgUtility.hasNumber(slotArpg, "z")
                && ArpgUtility.hasNumber(slotArpg, "rotation")
                && containerInstance.accepts(itemInstance)
                && containerInstance.fits(
                        itemInstance,
                        slotArpg.get("x").getAsInt(),
                        slotArpg.get("y").getAsInt(),
                        slotArpg.get("z").getAsInt(),
                        slotArpg.get("rotation").getAsInt());
    }

    // Utility \\

    private Vector3 parseVector(ArpgObjectStruct arpg, String key) {

        ArpgObjectStruct vectorArpg = ArpgUtility.validateObject(arpg, key);

        return new Vector3(
                ArpgUtility.validateFloat(vectorArpg, "x"),
                ArpgUtility.validateFloat(vectorArpg, "y"),
                ArpgUtility.validateFloat(vectorArpg, "z"));
    }

    // Validation \\

    private boolean isPlayerValid(ArpgObjectStruct playerArpg, EntityInstance player) {
        return ArpgUtility.hasObject(playerArpg, "location")
                && ArpgUtility.hasObject(playerArpg, "character")
                && isLocationValid(playerArpg.getAsObject("location"), player)
                && isCharacterValid(playerArpg.getAsObject("character"), player);
    }

    private boolean isLocationValid(ArpgObjectStruct locationArpg, EntityInstance player) {

        if (!ArpgUtility.hasString(locationArpg, "world") || !ArpgUtility.hasObject(locationArpg, "chunk"))
            return false;

        ArpgObjectStruct chunkArpg = locationArpg.getAsObject("chunk");

        return locationArpg.get("world").getAsString().equals(player.getWorldHandle().getWorldName())
                && ArpgUtility.hasNumber(chunkArpg, "x")
                && ArpgUtility.hasNumber(chunkArpg, "z")
                && isVectorValid(locationArpg, "position");
    }

    private boolean isCharacterValid(ArpgObjectStruct characterArpg, EntityInstance player) {

        if (!isVectorValid(characterArpg, "size") || !ArpgUtility.hasNumber(characterArpg, "weight"))
            return false;

        if (!isVectorPositive(characterArpg.getAsObject("size")) || characterArpg.get("weight").getAsFloat() <= 0f)
            return false;

        if (!player.hasAppearance())
            return !ArpgUtility.hasObject(characterArpg, "appearance");

        return ArpgUtility.hasObject(characterArpg, "appearance")
                && isAppearanceValid(
                        characterArpg.getAsObject("appearance"),
                        player.getEntityData().getAppearanceData());
    }

    private boolean isAppearanceValid(ArpgObjectStruct appearanceArpg, AppearanceData appearanceData) {
        return isColorValid(appearanceArpg, "skin_color")
                && isColorValid(appearanceArpg, "hair_color")
                && isVectorValid(appearanceArpg, "head_proportion")
                && isVectorPositive(appearanceArpg.getAsObject("head_proportion"))
                && ArpgUtility.hasObject(appearanceArpg, "features")
                && areFeaturesValid(appearanceArpg.getAsObject("features"), appearanceData);
    }

    private boolean areFeaturesValid(ArpgObjectStruct featuresArpg, AppearanceData appearanceData) {

        int filledSlotCount = 0;

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = featureSlot.name().toLowerCase();

            if (!featuresArpg.has(key)) {

                if (featureSlot.isRequired())
                    return false;

                continue;
            }

            if (!isFeatureValid(featuresArpg, key, featureSlot, appearanceData))
                return false;

            filledSlotCount++;
        }

        return filledSlotCount == featuresArpg.size();
    }

    private boolean isFeatureValid(
            ArpgObjectStruct featuresArpg,
            String key,
            FeatureSlot featureSlot,
            AppearanceData appearanceData) {

        if (!ArpgUtility.hasString(featuresArpg, key))
            return false;

        String featureName = featuresArpg.get(key).getAsString();

        if (!featureManager.isFeatureAvailable(featureName))
            return false;

        FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(featureName);

        return featureHandle.getFeatureSlot() == featureSlot && appearanceData.isCompatible(featureHandle);
    }

    private boolean isVectorValid(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasObject(arpg, key))
            return false;

        ArpgObjectStruct vectorArpg = arpg.getAsObject(key);

        return ArpgUtility.hasNumber(vectorArpg, "x")
                && ArpgUtility.hasNumber(vectorArpg, "y")
                && ArpgUtility.hasNumber(vectorArpg, "z");
    }

    private boolean isVectorPositive(ArpgObjectStruct vectorArpg) {
        return vectorArpg.get("x").getAsFloat() > 0f
                && vectorArpg.get("y").getAsFloat() > 0f
                && vectorArpg.get("z").getAsFloat() > 0f;
    }

    private boolean isColorValid(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasObject(arpg, key))
            return false;

        ArpgObjectStruct colorArpg = arpg.getAsObject(key);

        return ArpgUtility.hasNumber(colorArpg, "r")
                && ArpgUtility.hasNumber(colorArpg, "g")
                && ArpgUtility.hasNumber(colorArpg, "b");
    }
}