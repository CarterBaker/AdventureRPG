package application.bootstrap.savepipeline.savemanager;

import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.BranchPackage;
import engine.util.io.JsonUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class PlayerRestoreBranch extends BranchPackage {

    /*
     * Applies a character save to a window's player in place. The whole save
     * is validated before anything changes — its world must be the one the
     * player lives in and every feature must still exist and fit the
     * character — so a stale or malformed save leaves the player untouched.
     * The location is wrapped back into chunk and world bounds the same way
     * movement wraps it.
     */

    // Internal
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Management \\

    boolean restore(JsonObject playerJson, EntityInstance player) {

        if (!isPlayerValid(playerJson, player))
            return false;

        restoreLocation(JsonUtility.validateObject(playerJson, "location"), player);
        restoreCharacter(JsonUtility.validateObject(playerJson, "character"), player);

        return true;
    }

    // Location \\

    private void restoreLocation(JsonObject locationJson, EntityInstance player) {

        JsonObject chunkJson = JsonUtility.validateObject(locationJson, "chunk");
        long chunkCoordinate = Coordinate2Long.pack(
                JsonUtility.validateInt(chunkJson, "x"),
                JsonUtility.validateInt(chunkJson, "z"));

        player.setLocation(
                WorldWrapUtility.wrapAroundChunk(parseVector(locationJson, "position")),
                WorldWrapUtility.wrapAroundWorld(player.getWorldHandle(), chunkCoordinate));
    }

    // Character \\

    private void restoreCharacter(JsonObject characterJson, EntityInstance player) {

        player.setSize(parseVector(characterJson, "size"));
        player.setWeight(JsonUtility.validateFloat(characterJson, "weight"));

        if (player.hasAppearance())
            restoreAppearance(JsonUtility.validateObject(characterJson, "appearance"), player.getAppearanceHandle());
    }

    private void restoreAppearance(JsonObject appearanceJson, AppearanceHandle appearanceHandle) {

        JsonObject skinColorJson = JsonUtility.validateObject(appearanceJson, "skin_color");
        JsonObject hairColorJson = JsonUtility.validateObject(appearanceJson, "hair_color");
        Vector3 headProportion = parseVector(appearanceJson, "head_proportion");

        appearanceHandle.setSkinColor(
                JsonUtility.validateFloat(skinColorJson, "r"),
                JsonUtility.validateFloat(skinColorJson, "g"),
                JsonUtility.validateFloat(skinColorJson, "b"));
        appearanceHandle.setHairColor(
                JsonUtility.validateFloat(hairColorJson, "r"),
                JsonUtility.validateFloat(hairColorJson, "g"),
                JsonUtility.validateFloat(hairColorJson, "b"));
        appearanceHandle.setHeadProportion(headProportion.x, headProportion.y, headProportion.z);

        restoreFeatures(JsonUtility.validateObject(appearanceJson, "features"), appearanceHandle);
    }

    private void restoreFeatures(JsonObject featuresJson, AppearanceHandle appearanceHandle) {

        for (FeatureSlot featureSlot : FeatureSlot.values()) {

            String key = featureSlot.name().toLowerCase();

            if (featuresJson.has(key))
                appearanceHandle.setFeature(featureManager.getFeatureHandleFromFeatureName(
                        JsonUtility.validateString(featuresJson, key)));
            else
                appearanceHandle.clearFeature(featureSlot);
        }
    }

    // Utility \\

    private Vector3 parseVector(JsonObject json, String key) {

        JsonObject vectorJson = JsonUtility.validateObject(json, key);

        return new Vector3(
                JsonUtility.validateFloat(vectorJson, "x"),
                JsonUtility.validateFloat(vectorJson, "y"),
                JsonUtility.validateFloat(vectorJson, "z"));
    }

    // Validation \\

    private boolean isPlayerValid(JsonObject playerJson, EntityInstance player) {
        return JsonUtility.hasObject(playerJson, "location")
                && JsonUtility.hasObject(playerJson, "character")
                && isLocationValid(playerJson.getAsJsonObject("location"), player)
                && isCharacterValid(playerJson.getAsJsonObject("character"), player);
    }

    private boolean isLocationValid(JsonObject locationJson, EntityInstance player) {

        if (!JsonUtility.hasString(locationJson, "world") || !JsonUtility.hasObject(locationJson, "chunk"))
            return false;

        JsonObject chunkJson = locationJson.getAsJsonObject("chunk");

        return locationJson.get("world").getAsString().equals(player.getWorldHandle().getWorldName())
                && JsonUtility.hasNumber(chunkJson, "x")
                && JsonUtility.hasNumber(chunkJson, "z")
                && isVectorValid(locationJson, "position");
    }

    private boolean isCharacterValid(JsonObject characterJson, EntityInstance player) {

        if (!isVectorValid(characterJson, "size") || !JsonUtility.hasNumber(characterJson, "weight"))
            return false;

        if (!isVectorPositive(characterJson.getAsJsonObject("size")) || characterJson.get("weight").getAsFloat() <= 0f)
            return false;

        if (!player.hasAppearance())
            return !JsonUtility.hasObject(characterJson, "appearance");

        return JsonUtility.hasObject(characterJson, "appearance")
                && isAppearanceValid(
                        characterJson.getAsJsonObject("appearance"),
                        player.getEntityData().getAppearanceData());
    }

    private boolean isAppearanceValid(JsonObject appearanceJson, AppearanceData appearanceData) {
        return isColorValid(appearanceJson, "skin_color")
                && isColorValid(appearanceJson, "hair_color")
                && isVectorValid(appearanceJson, "head_proportion")
                && isVectorPositive(appearanceJson.getAsJsonObject("head_proportion"))
                && JsonUtility.hasObject(appearanceJson, "features")
                && areFeaturesValid(appearanceJson.getAsJsonObject("features"), appearanceData);
    }

    private boolean areFeaturesValid(JsonObject featuresJson, AppearanceData appearanceData) {

        int filledSlotCount = 0;

        for (FeatureSlot featureSlot : FeatureSlot.values()) {

            String key = featureSlot.name().toLowerCase();

            if (!featuresJson.has(key)) {

                if (featureSlot.isRequired())
                    return false;

                continue;
            }

            if (!isFeatureValid(featuresJson, key, featureSlot, appearanceData))
                return false;

            filledSlotCount++;
        }

        return filledSlotCount == featuresJson.size();
    }

    private boolean isFeatureValid(
            JsonObject featuresJson,
            String key,
            FeatureSlot featureSlot,
            AppearanceData appearanceData) {

        if (!JsonUtility.hasString(featuresJson, key))
            return false;

        String featureName = featuresJson.get(key).getAsString();

        if (!featureManager.isFeatureAvailable(featureName))
            return false;

        FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(featureName);

        return featureHandle.getFeatureSlot() == featureSlot && appearanceData.isCompatible(featureHandle);
    }

    private boolean isVectorValid(JsonObject json, String key) {

        if (!JsonUtility.hasObject(json, key))
            return false;

        JsonObject vectorJson = json.getAsJsonObject(key);

        return JsonUtility.hasNumber(vectorJson, "x")
                && JsonUtility.hasNumber(vectorJson, "y")
                && JsonUtility.hasNumber(vectorJson, "z");
    }

    private boolean isVectorPositive(JsonObject vectorJson) {
        return vectorJson.get("x").getAsFloat() > 0f
                && vectorJson.get("y").getAsFloat() > 0f
                && vectorJson.get("z").getAsFloat() > 0f;
    }

    private boolean isColorValid(JsonObject json, String key) {

        if (!JsonUtility.hasObject(json, key))
            return false;

        JsonObject colorJson = json.getAsJsonObject(key);

        return JsonUtility.hasNumber(colorJson, "r")
                && JsonUtility.hasNumber(colorJson, "g")
                && JsonUtility.hasNumber(colorJson, "b");
    }
}