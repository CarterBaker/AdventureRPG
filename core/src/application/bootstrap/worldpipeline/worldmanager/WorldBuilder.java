package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.worldpipeline.world.WorldData;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;

class WorldBuilder extends BuilderPackage {

    /*
     * Parses a world PNG map and optional companion ARPG file into a WorldHandle.
     * All fields are resolved before WorldData construction — the handle is
     * never mutated after constructor() is called. Bootstrap-only. The
     * companion file is also the durable home for the world's generation
     * seed and its epoch start: once assigned, each is written back to disk
     * immediately so every future load reproduces the same terrain and the
     * same clock — the world keeps time from its epoch whether or not the
     * game is running.
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
        String calendarName = EngineSetting.DEFAULT_CALENDAR_NAME;
        float rotationSpeed = EngineSetting.DEFAULT_WORLD_ROTATION_SPEED;
        float axialTilt = EngineSetting.DEFAULT_AXIAL_TILT_DEGREES;
        float planetaryOffset = EngineSetting.DEFAULT_PLANETARY_OFFSET;

        File arpgFile = ArpgUtility.resolveCompanionFile(file);
        boolean arpgExisted = arpgFile.exists();
        ArpgObjectStruct arpg = arpgExisted ? ArpgUtility.loadObject(arpgFile) : new ArpgObjectStruct();

        if (arpgExisted) {

            if (arpg.has("gravity_multiplier"))
                gravityMultiplier = arpg.get("gravity_multiplier").getAsFloat();

            if (arpg.has("gravity_direction")) {
                ArpgArrayStruct dir = arpg.getAsArray("gravity_direction");
                gravityDirection = new Vector3(
                        dir.get(0).getAsFloat(),
                        dir.get(1).getAsFloat(),
                        dir.get(2).getAsFloat());
            }

            if (arpg.has("calendar"))
                calendarName = arpg.get("calendar").getAsString();

            if (arpg.has("rotation"))
                rotationSpeed = arpg.get("rotation").getAsFloat();

            if (arpg.has("axial_tilt"))
                axialTilt = arpg.get("axial_tilt").getAsFloat();

            if (arpg.has("planetary_offset"))
                planetaryOffset = wrapUnitFraction(arpg.get("planetary_offset").getAsFloat());
        }

        long seed = resolveWorldSeed(arpg, arpgFile);
        long worldEpochStart = resolveWorldEpochStart(arpg, arpgFile);

        WorldData data = new WorldData(
                worldName,
                worldID,
                file,
                pixmap,
                worldScale,
                gravityMultiplier,
                gravityDirection,
                calendarName,
                worldEpochStart,
                rotationSpeed,
                axialTilt,
                planetaryOffset,
                seed);

        WorldHandle handle = create(WorldHandle.class);
        handle.constructor(data);

        return handle;
    }

    // Seed \\

    private long resolveWorldSeed(ArpgObjectStruct arpg, File arpgFile) {

        if (arpg.has("seed"))
            return arpg.get("seed").getAsLong();

        long seed = ThreadLocalRandom.current().nextLong();
        arpg.addProperty("seed", seed);
        ArpgUtility.writeObject(arpgFile, arpg);

        return seed;
    }

    // Epoch \\

    private long resolveWorldEpochStart(ArpgObjectStruct arpg, File arpgFile) {

        if (arpg.has("epoch_start"))
            return arpg.get("epoch_start").getAsLong();

        long worldEpochStart = System.currentTimeMillis();
        arpg.addProperty("epoch_start", worldEpochStart);
        ArpgUtility.writeObject(arpgFile, arpg);

        return worldEpochStart;
    }

    // Helpers \\

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