package application.bootstrap.worldpipeline.world;

import engine.assets.image.Pixmap;
import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

public class WorldData extends DataPackage {

    /*
     * Immutable world definition loaded from a PNG map and optional companion
     * JSON. Holds identity, pixel map, scale, gravity, rotation, tilt, the
     * planetary offset used to phase the day/night gradient across the
     * world's Y axis, and the generation seed. calendarName points to the
     * per-world calendar definition, which owns daysPerDay and every other
     * day/year shape setting. worldEpochStart is the one mutable field —
     * written from the save file at runtime, never from the world
     * definition itself.
     */

    // Identity
    private final String worldName;
    private final int worldID;
    private final Pixmap world;
    private final Vector2Int worldScale;

    // Gravity
    private final float gravityMultiplier;
    private final Vector3 gravityDirection;

    // Time
    private final String calendarName;
    private long worldEpochStart;

    // Rotation
    private final float rotationSpeed;

    // Tilt — drives the seasonal north-south drift of the global weather
    // noise field in GlobalNoiseBranch. Zero means a perfectly upright
    // world: storm tracks never migrate north/south, only the steady
    // east-west rotation scroll applies.
    private final float axialTilt;

    // Planetary Offset — fractional position (0-1) along this world's Y span
    // that lines up with the calendar's global reference time at real-world
    // noon. See WorldWrapUtility.wrappedPlanetaryOffset.
    private final float planetaryOffset;

    // Generation — locked in from the companion JSON at first load. Every
    // terrain, biome, and feature decision for this world must derive from
    // this value plus a deterministic position, never from mutable state.
    private final long seed;

    // Constructor \\

    public WorldData(
            String worldName,
            int worldID,
            Pixmap world,
            Vector2Int worldScale,
            float gravityMultiplier,
            Vector3 gravityDirection,
            String calendarName,
            float rotationSpeed,
            float axialTilt,
            float planetaryOffset,
            long seed) {

        // Identity
        this.worldName = worldName;
        this.worldID = worldID;
        this.world = world;
        this.worldScale = worldScale;

        // Gravity
        this.gravityMultiplier = gravityMultiplier;
        this.gravityDirection = gravityDirection;

        // Time
        this.calendarName = calendarName;
        this.worldEpochStart = -1L;

        // Rotation
        this.rotationSpeed = rotationSpeed;

        // Tilt
        this.axialTilt = axialTilt;

        // Planetary Offset
        this.planetaryOffset = planetaryOffset;

        // Generation
        this.seed = seed;
    }

    // Accessible \\

    public String getWorldName() {
        return worldName;
    }

    public int getWorldID() {
        return worldID;
    }

    public Pixmap getWorld() {
        return world;
    }

    public Vector2Int getWorldScale() {
        return worldScale;
    }

    public float getGravityMultiplier() {
        return gravityMultiplier;
    }

    public Vector3 getGravityDirection() {
        return gravityDirection;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public long getWorldEpochStart() {
        return worldEpochStart;
    }

    public void setWorldEpochStart(long worldEpochStart) {
        this.worldEpochStart = worldEpochStart;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public float getAxialTilt() {
        return axialTilt;
    }

    public float getPlanetaryOffset() {
        return planetaryOffset;
    }

    public long getSeed() {
        return seed;
    }
}