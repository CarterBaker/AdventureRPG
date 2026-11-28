package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import application.bootstrap.worldpipeline.util.WorldPlacementArpgUtility;
import application.bootstrap.worldpipeline.world.WorldData;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.world.WorldShape;
import engine.assets.image.Pixmap;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.registry.RegistryUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WorldBuilder extends BuilderPackage {

    /*
     * Parses a world PNG map and optional companion ARPG file into a WorldHandle.
     * All fields are resolved before WorldData construction — the handle is
     * never mutated after constructor() is called. Bootstrap-only. The
     * companion file is also the durable home for the world's generation
     * seed: once assigned, it is written back to disk immediately so every
     * future load reproduces the same terrain. Its hand-picked settlement
     * and structure placements, whether settlements also grow on their own,
     * and whether its time ticks, are read from it too.
     * The height layer beside the world image is read with it and must match
     * its size; a world without one gets a neutral layer, written to disk only
     * once the editor saves what is painted on it. A virtual world is built
     * over an image held in memory instead, for the editor's previews: it has
     * no files, a neutral height layer, every default, a seed taken from its
     * name so it generates the same each time, nothing placed or grown on
     * it, time held at noon so it is always seen in daylight, and biome borders running straight along its pixel edges, so the
     * areas painted on it keep their shape. The shape says whether the world
     * wraps as a torus or is a globe, whose image must hold its six cube faces
     * side by side, and a globe's cube rotation turns its block grid's cube
     * inside the globe; a virtual world is always a torus.
     */

    // Internal
    private WorldManager worldManager;

    // Base \\

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    // Build \\

    WorldHandle build(File file, File root, String worldName) {

        int worldID = worldManager.registerWorldName(worldName);
        Pixmap pixmap = new Pixmap(file);
        Vector2Int worldScale = calculateWorldScale(pixmap);
        File heightFile = WorldElevationUtility.resolveHeightLayerFile(file);
        Pixmap heightLayer = resolveHeightLayer(heightFile, pixmap);

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

        ObjectArrayList<WorldPlacementStruct> placements = WorldPlacementArpgUtility.parsePlacements(
                arpg, worldName);
        boolean naturalSettlements = ArpgUtility.getBoolean(
                arpg, "natural_settlements", EngineSetting.DEFAULT_NATURAL_SETTLEMENTS);
        boolean tickTime = ArpgUtility.getBoolean(arpg, "tick_time", EngineSetting.DEFAULT_TICK_TIME);

        long seed = resolveWorldSeed(arpg, arpgFile);

        WorldShape shape = ArpgUtility.getEnum(arpg, "shape", WorldShape.class, WorldShape.TORUS);
        Vector3 cubeRotation = resolveCubeRotation(arpg, worldName);

        if (shape == WorldShape.GLOBE)
            validateGlobeImage(file, pixmap);

        WorldData data = new WorldData(
                worldName,
                worldID,
                file,
                pixmap,
                worldScale,
                shape,
                cubeRotation,
                resolveGlobeFromCube(cubeRotation),
                heightFile,
                heightLayer,
                gravityMultiplier,
                gravityDirection,
                calendarName,
                tickTime,
                rotationSpeed,
                axialTilt,
                planetaryOffset,
                seed,
                placements,
                naturalSettlements,
                true);

        WorldHandle handle = create(WorldHandle.class);
        handle.constructor(data);

        return handle;
    }

    WorldHandle buildVirtual(String worldName, Pixmap pixmap) {

        WorldData data = new WorldData(
                worldName,
                worldManager.registerWorldName(worldName),
                null,
                pixmap,
                calculateWorldScale(pixmap),
                WorldShape.TORUS,
                new Vector3(),
                resolveGlobeFromCube(new Vector3()),
                null,
                WorldElevationUtility.createNeutralLayer(pixmap.getWidth(), pixmap.getHeight()),
                EngineSetting.DEFAULT_GRAVITY_MULTIPLIER,
                new Vector3(
                        EngineSetting.DEFAULT_GRAVITY_X,
                        EngineSetting.DEFAULT_GRAVITY_Y,
                        EngineSetting.DEFAULT_GRAVITY_Z),
                EngineSetting.DEFAULT_CALENDAR_NAME,
                false,
                EngineSetting.DEFAULT_WORLD_ROTATION_SPEED,
                EngineSetting.DEFAULT_AXIAL_TILT_DEGREES,
                EngineSetting.DEFAULT_PLANETARY_OFFSET,
                RegistryUtility.toNameSeed(worldName),
                new ObjectArrayList<>(),
                false,
                false);

        WorldHandle handle = create(WorldHandle.class);
        handle.constructor(data);

        return handle;
    }

    // Shape \\

    private Vector3 resolveCubeRotation(ArpgObjectStruct arpg, String worldName) {

        Vector3 cubeRotation = new Vector3();

        if (!arpg.has("cube_rotation"))
            return cubeRotation;

        ArpgArrayStruct degrees = arpg.getAsArray("cube_rotation");

        if (degrees.size() != 3)
            throwException("World \"" + worldName + "\" has a cube_rotation of " + degrees.size()
                    + " values — it takes three, the degrees about X, Y and Z.");

        cubeRotation.set(
                degrees.get(0).getAsFloat(),
                degrees.get(1).getAsFloat(),
                degrees.get(2).getAsFloat());

        float limit = EngineSetting.GLOBE_CUBE_ROTATION_MAX_DEGREES;

        if (Math.abs(cubeRotation.x) > limit || Math.abs(cubeRotation.y) > limit || Math.abs(cubeRotation.z) > limit)
            throwException("World \"" + worldName + "\" has a cube_rotation beyond " + limit
                    + " degrees about an axis — each of its three angles lies within -" + limit + " and " + limit
                    + ".");

        return cubeRotation;
    }

    private Matrix3Double resolveGlobeFromCube(Vector3 cubeRotation) {

        Matrix3Double globeFromCube = new Matrix3Double();
        GlobeUtility.resolveGlobeFromCube(cubeRotation, globeFromCube);

        return globeFromCube;
    }

    private void validateGlobeImage(File file, Pixmap pixmap) {

        if (pixmap.getWidth() != pixmap.getHeight() * EngineSetting.GLOBE_FACE_COUNT)
            throwException("Globe world image \"" + file.getPath() + "\" is " + pixmap.getWidth() + "x"
                    + pixmap.getHeight() + " — a globe's image holds its " + EngineSetting.GLOBE_FACE_COUNT
                    + " square cube faces side by side, so it is " + EngineSetting.GLOBE_FACE_COUNT
                    + " times as wide as it is tall.");
    }

    // Height \\

    private Pixmap resolveHeightLayer(File heightFile, Pixmap world) {

        if (!heightFile.exists())
            return WorldElevationUtility.createNeutralLayer(world.getWidth(), world.getHeight());

        Pixmap heightLayer = new Pixmap(heightFile);

        if (heightLayer.getWidth() != world.getWidth() || heightLayer.getHeight() != world.getHeight())
            throwException("Height layer \"" + heightFile.getPath() + "\" is " + heightLayer.getWidth() + "x"
                    + heightLayer.getHeight() + " but its world image is " + world.getWidth() + "x"
                    + world.getHeight() + " — a height layer holds one level per world pixel and must match "
                    + "its world image's size exactly.");

        return heightLayer;
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