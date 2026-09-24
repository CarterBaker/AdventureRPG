package application.bootstrap.worldpipeline.world;

import engine.assets.image.Pixmap;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

public class WorldHandle extends HandlePackage {

    /*
     * Persistent world definition owned by WorldManager for the engine lifetime.
     * Wraps WorldData and delegates all access through it.
     */

    // Internal
    private WorldData data;

    // Constructor \\

    public void constructor(WorldData data) {
        this.data = data;
    }

    // Accessible \\

    public WorldData getWorldData() {
        return data;
    }

    public String getWorldName() {
        return data.getWorldName();
    }

    public int getWorldID() {
        return data.getWorldID();
    }

    public Pixmap getWorld() {
        return data.getWorld();
    }

    public Vector2Int getWorldScale() {
        return data.getWorldScale();
    }

    public float getGravityMultiplier() {
        return data.getGravityMultiplier();
    }

    public Vector3 getGravityDirection() {
        return data.getGravityDirection();
    }

    public String getCalendarName() {
        return data.getCalendarName();
    }

    public long getWorldEpochStart() {
        return data.getWorldEpochStart();
    }

    public float getRotationSpeed() {
        return data.getRotationSpeed();
    }

    public float getAxialTilt() {
        return data.getAxialTilt();
    }

    public float getPlanetaryOffset() {
        return data.getPlanetaryOffset();
    }

    public long getSeed() {
        return data.getSeed();
    }
}