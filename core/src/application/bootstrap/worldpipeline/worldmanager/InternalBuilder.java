package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.world.WorldData;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BuilderPackage;
import engine.settings.EngineSetting;
import engine.util.FileUtility;
import engine.util.JsonUtility;
import engine.util.RegistryUtility;
import engine.util.image.Pixmap;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

class InternalBuilder extends BuilderPackage {

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
        }

        WorldData data = new WorldData(
                worldName,
                worldID,
                pixmap,
                worldScale,
                gravityMultiplier,
                gravityDirection,
                daysPerDay,
                calendarName);

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