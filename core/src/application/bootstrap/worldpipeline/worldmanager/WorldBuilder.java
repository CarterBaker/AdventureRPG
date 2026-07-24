package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.world.WorldData;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;

class WorldBuilder extends BuilderPackage {

    /*
     * Parses a world PNG map and optional companion JSON into a WorldHandle.
     * All fields are resolved before WorldData construction — the handle is
     * never mutated after constructor() is called. Bootstrap-only.
     */

    // Build \\

    WorldHandle build(File file, File root, String worldName) {

        int worldID = RegistryUtility.toIntID(worldName);
        Pixmap pixmap = new Pixmap(file);
        Vector2Int worldScale = calculateWorldScale(pixmap);

        float gravityMultiplier = EngineSetting.DEFAULT_GRAVITY_MULTIPLIER;
        Vector3 gravityDirection = new Vector3(
                EngineSetting.DEFAULT_GRAVITY_X,
                EngineSetting.DEFAULT_GRAVITY_Y,
                EngineSetting.DEFAULT_GRAVITY_Z);
        float daysPerDay = EngineSetting.DEFAULT_DAYS_PER_DAY;
        String calendarName = EngineSetting.DEFAULT_CALENDAR_NAME;
        float rotationSpeed = EngineSetting.DEFAULT_WORLD_ROTATION_SPEED;
        float axialTilt = EngineSetting.DEFAULT_AXIAL_TILT_DEGREES;
        float planetaryOffset = EngineSetting.DEFAULT_PLANETARY_OFFSET;

        File jsonFile = resolveCompanionJson(file);

        if (jsonFile.exists()) {
            JsonObject json = JsonUtility.loadJsonObject(jsonFile);

            if (json.has("gravity_multiplier"))
                gravityMultiplier = json.get("gravity_multiplier").getAsFloat();

            if (json.has("gravity_direction")) {
                JsonArray dir = json.getAsJsonArray("gravity_direction");
                gravityDirection = new Vector3(
                        dir.get(0).getAsFloat(),
                        dir.get(1).getAsFloat(),
                        dir.get(2).getAsFloat());
            }

            if (json.has("days_per_day"))
                daysPerDay = json.get("days_per_day").getAsFloat();

            if (json.has("calendar"))
                calendarName = json.get("calendar").getAsString();

            if (json.has("rotation"))
                rotationSpeed = json.get("rotation").getAsFloat();

            if (json.has("axial_tilt"))
                axialTilt = json.get("axial_tilt").getAsFloat();

            if (json.has("planetary_offset"))
                planetaryOffset = json.get("planetary_offset").getAsFloat();
        }

        WorldData data = new WorldData(
                worldName,
                worldID,
                pixmap,
                worldScale,
                gravityMultiplier,
                gravityDirection,
                daysPerDay,
                calendarName,
                rotationSpeed,
                axialTilt,
                planetaryOffset);

        WorldHandle handle = create(WorldHandle.class);
        handle.constructor(data);

        return handle;
    }

    // Helpers \\

    private File resolveCompanionJson(File pngFile) {

        String path = pngFile.getPath();
        int dot = path.lastIndexOf('.');
        String jsonPath = (dot >= 0 ? path.substring(0, dot) : path) + ".json";

        return new File(jsonPath);
    }

    private Vector2Int calculateWorldScale(Pixmap pixmap) {

        int worldWidth = pixmap.getWidth() * EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;
        int worldHeight = pixmap.getHeight() * EngineSetting.CHUNKS_PER_PIXEL * EngineSetting.CHUNK_SIZE;

        return new Vector2Int(worldWidth, worldHeight);
    }
}