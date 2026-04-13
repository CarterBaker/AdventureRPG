package application.bootstrap.worldpipeline.world;

import application.core.engine.HandlePackage;
import application.core.util.image.Pixmap;
import application.core.util.mathematics.vectors.Vector2Int;
import application.core.util.mathematics.vectors.Vector3;

public class WorldHandle extends HandlePackage {

    /*
     * Persistent world definition owned by WorldManager for the engine lifetime.
     * Wraps WorldData and delegates all access through it. worldEpochStart is
     * the only mutable value — written once from the save file at runtime.
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

    public float getDaysPerDay() {
        return data.getDaysPerDay();
    }

    public String getCalendarName() {
        return data.getCalendarName();
    }

    public long getWorldEpochStart() {
        return data.getWorldEpochStart();
    }

    public void setWorldEpochStart(long worldEpochStart) {
        data.setWorldEpochStart(worldEpochStart);
    }
}