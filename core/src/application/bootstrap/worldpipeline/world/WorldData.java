package application.bootstrap.worldpipeline.world;

import java.io.File;

import engine.assets.image.Pixmap;
import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldData extends DataPackage {

    /*
     * Immutable world definition loaded from a PNG map and optional companion
     * ARPG. Holds identity, pixel map, scale, gravity, rotation, tilt, the
     * planetary offset used to phase the day/night gradient across the
     * world's Y axis, and the generation seed. calendarName points to the
     * per-world calendar definition, which owns daysPerDay and every other
     * day/year shape setting. The world file is the image the pixel map was read from, for tools that
     * edit and save it, and the height layer the grey image beside it that
     * raises and lowers the ground the biomes shape, read from its own file
     * or neutral everywhere until one is painted. The placements are the
     * settlements and structures picked by hand for this world as it was
     * loaded, and naturalSettlements whether settlements also grow on their
     * own wherever its biomes allow. warpedBorders lets the borders between
     * its biomes wander off the map's pixel edges, as a real world's do; a
     * world without it keeps every border straight along those edges.
     */

    // Identity
    private final String worldName;
    private final int worldID;
    private final File worldFile;
    private final Pixmap world;
    private final Vector2Int worldScale;

    // Height
    private final File heightFile;
    private final Pixmap heightLayer;

    // Gravity
    private final float gravityMultiplier;
    private final Vector3 gravityDirection;

    // Time
    private final String calendarName;

    // Rotation
    private final float rotationSpeed;

    // Tilt — seasonal north-south drift of the weather flow, zero for an upright world
    private final float axialTilt;

    // Planetary Offset — fractional position (0-1) along this world's X span
    // that lines up with the calendar's global reference time at real-world
    // noon. See WorldWrapUtility.wrappedPlanetaryOffset.
    private final float planetaryOffset;

    // Generation — locked in from the companion ARPG at first load. Every
    // terrain, biome, and feature decision for this world must derive from
    // this value plus a deterministic position, never from mutable state.
    private final long seed;

    // Placement
    private final ObjectArrayList<WorldPlacementStruct> placements;
    private final boolean naturalSettlements;

    // Biomes
    private final boolean warpedBorders;

    // Constructor \\

    public WorldData(
            String worldName,
            int worldID,
            File worldFile,
            Pixmap world,
            Vector2Int worldScale,
            File heightFile,
            Pixmap heightLayer,
            float gravityMultiplier,
            Vector3 gravityDirection,
            String calendarName,
            float rotationSpeed,
            float axialTilt,
            float planetaryOffset,
            long seed,
            ObjectArrayList<WorldPlacementStruct> placements,
            boolean naturalSettlements,
            boolean warpedBorders) {

        // Identity
        this.worldName = worldName;
        this.worldID = worldID;
        this.worldFile = worldFile;
        this.world = world;
        this.worldScale = worldScale;

        // Height
        this.heightFile = heightFile;
        this.heightLayer = heightLayer;

        // Gravity
        this.gravityMultiplier = gravityMultiplier;
        this.gravityDirection = gravityDirection;

        // Time
        this.calendarName = calendarName;

        // Rotation
        this.rotationSpeed = rotationSpeed;

        // Tilt
        this.axialTilt = axialTilt;

        // Planetary Offset
        this.planetaryOffset = planetaryOffset;

        // Generation
        this.seed = seed;

        // Placement
        this.placements = placements;
        this.naturalSettlements = naturalSettlements;

        // Biomes
        this.warpedBorders = warpedBorders;
    }

    // Accessible \\

    public String getWorldName() {
        return worldName;
    }

    public int getWorldID() {
        return worldID;
    }

    public File getWorldFile() {
        return worldFile;
    }

    public Pixmap getWorld() {
        return world;
    }

    public Vector2Int getWorldScale() {
        return worldScale;
    }

    public File getHeightFile() {
        return heightFile;
    }

    public Pixmap getHeightLayer() {
        return heightLayer;
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

    public ObjectArrayList<WorldPlacementStruct> getPlacements() {
        return placements;
    }

    public boolean hasNaturalSettlements() {
        return naturalSettlements;
    }

    public boolean hasWarpedBorders() {
        return warpedBorders;
    }
}
