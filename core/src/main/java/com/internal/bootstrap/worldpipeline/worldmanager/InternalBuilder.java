package com.internal.bootstrap.worldpipeline.worldmanager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector2Int;
import com.internal.core.util.mathematics.vectors.Vector3;

import java.io.File;

class InternalBuilder extends BuilderPackage {

    // Build \\

    WorldHandle build(File file, File root, int worldID) {

        String worldName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        Pixmap world = new Pixmap(Gdx.files.internal(file.getPath()));
        Vector2Int worldScale = calculateWorldScale(world);

        WorldHandle worldHandle = create(WorldHandle.class);
        worldHandle.constructor(worldName, worldID, world, worldScale);

        File jsonFile = getCompanionJson(file);
        if (jsonFile.exists())
            applyJson(worldHandle, jsonFile);

        return worldHandle;
    }

    // Companion JSON \\

    private File getCompanionJson(File pngFile) {
        String path = pngFile.getPath();
        int dot = path.lastIndexOf('.');
        String jsonPath = (dot >= 0 ? path.substring(0, dot) : path) + ".json";
        return new File(jsonPath);
    }

    private void applyJson(WorldHandle handle, File jsonFile) {

        JsonObject json = JsonUtility.loadJsonObject(jsonFile);

        if (json.has("gravity_multiplier"))
            handle.setGravityMultiplier(json.get("gravity_multiplier").getAsFloat());

        if (json.has("gravity_direction")) {
            JsonArray dir = json.getAsJsonArray("gravity_direction");
            handle.setGravityDirection(new Vector3(
                    dir.get(0).getAsFloat(),
                    dir.get(1).getAsFloat(),
                    dir.get(2).getAsFloat()));
        }

        if (json.has("days_per_day"))
            handle.setDaysPerDay(json.get("days_per_day").getAsFloat());

        if (json.has("calendar"))
            handle.setCalendarName(json.get("calendar").getAsString());

        // worldEpochStart is NEVER in JSON — it lives in the save file only
    }

    // Scale \\

    private Vector2Int calculateWorldScale(Pixmap pixMap) {
        int worldWidth = pixMap.getWidth() * EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;
        int worldHeight = pixMap.getHeight() * EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;
        return new Vector2Int(worldWidth, worldHeight);
    }
}