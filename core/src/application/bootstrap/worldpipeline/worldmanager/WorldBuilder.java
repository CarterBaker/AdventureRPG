package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
     * never mutated after constructor() is called. Bootstrap-only. The
     * companion JSON is also the durable home for the world's generation
     * seed: once assigned, it is written back to disk immediately so every
     * future load of this world, on any machine, reproduces the same seed
     * and therefore the same terrain.
     */

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

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
        String calendarName = EngineSetting.DEFAULT_CALENDAR_NAME;
        float rotationSpeed = EngineSetting.DEFAULT_WORLD_ROTATION_SPEED;
        float axialTilt = EngineSetting.DEFAULT_AXIAL_TILT_DEGREES;
        float planetaryOffset = EngineSetting.DEFAULT_PLANETARY_OFFSET;

        File jsonFile = resolveCompanionJson(file);
        boolean jsonExisted = jsonFile.exists();
        JsonObject json = jsonExisted ? JsonUtility.loadJsonObject(jsonFile) : new JsonObject();

        if (jsonExisted) {

            if (json.has("gravity_multiplier"))
                gravityMultiplier = json.get("gravity_multiplier").getAsFloat();

            if (json.has("gravity_direction")) {
                JsonArray dir = json.getAsJsonArray("gravity_direction");
                gravityDirection = new Vector3(
                        dir.get(0).getAsFloat(),
                        dir.get(1).getAsFloat(),
                        dir.get(2).getAsFloat());
            }

            if (json.has("calendar"))
                calendarName = json.get("calendar").getAsString();

            if (json.has("rotation"))
                rotationSpeed = json.get("rotation").getAsFloat();

            if (json.has("axial_tilt"))
                axialTilt = json.get("axial_tilt").getAsFloat();

            if (json.has("planetary_offset"))
                planetaryOffset = wrapUnitFraction(json.get("planetary_offset").getAsFloat());
        }

        long seed = resolveWorldSeed(json, jsonFile, worldName);

        WorldData data = new WorldData(
                worldName,
                worldID,
                pixmap,
                worldScale,
                gravityMultiplier,
                gravityDirection,
                calendarName,
                rotationSpeed,
                axialTilt,
                planetaryOffset,
                seed);

        WorldHandle handle = create(WorldHandle.class);
        handle.constructor(data);

        return handle;
    }

    // Seed \\

    /*
     * Reads "seed" from the companion JSON if present. If it's missing —
     * either the field or the whole file — a new seed is rolled once and
     * persisted immediately, so this is the only moment a world's seed is
     * ever chosen. Every subsequent load reads the same value back.
     */
    private long resolveWorldSeed(JsonObject json, File jsonFile, String worldName) {

        if (json.has("seed"))
            return json.get("seed").getAsLong();

        long seed = ThreadLocalRandom.current().nextLong();
        json.addProperty("seed", seed);
        persistCompanionJson(json, jsonFile, worldName);

        return seed;
    }

    private void persistCompanionJson(JsonObject json, File jsonFile, String worldName) {

        try (FileWriter writer = new FileWriter(jsonFile)) {
            PRETTY_GSON.toJson(json, writer);
        } catch (IOException e) {
            throwException("Failed to persist generated seed for world: \"" + worldName + "\"", e);
        }
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

    // planetary_offset is a fractional position (0-1) along the world's Y
    // span — wrap any out-of-range authored value once, at load time.
    private float wrapUnitFraction(float value) {
        float wrapped = value % 1.0f;
        if (wrapped < 0f)
            wrapped += 1.0f;
        return wrapped;
    }
}