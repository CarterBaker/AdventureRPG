package com.internal.bootstrap.worldpipeline.world;

import com.badlogic.gdx.graphics.Pixmap;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector2Int;
import com.internal.core.util.mathematics.vectors.Vector3;

public class WorldHandle extends HandlePackage {

    // Existing
    private String worldName;
    private int worldID;
    private Pixmap world;
    private Vector2Int worldScale;

    // Gravity
    private float gravityMultiplier = 1.0f;
    private Vector3 gravityDirection = new Vector3(0, -1, 0);

    // Time
    private float daysPerDay = 20.0f;
    private long worldEpochStart = -1L;
    private String calendarName = "standard/Default";

    // Constructor \\

    public void constructor(
            String worldName,
            int worldID,
            Pixmap world,
            Vector2Int worldScale) {

        this.worldName = worldName;
        this.worldID = worldID;
        this.world = world;
        this.worldScale = worldScale;
    }

    // Getters — Existing \\

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

    // Getters — Gravity \\

    public float getGravityMultiplier() {
        return gravityMultiplier;
    }

    public Vector3 getGravityDirection() {
        return gravityDirection;
    }

    // Getters — Time \\

    public float getDaysPerDay() {
        return daysPerDay;
    }

    public long getWorldEpochStart() {
        return worldEpochStart;
    }

    public String getCalendarName() {
        return calendarName;
    }

    // Setters \\

    public void setGravityMultiplier(float gravityMultiplier) {
        this.gravityMultiplier = gravityMultiplier;
    }

    public void setGravityDirection(Vector3 gravityDirection) {
        this.gravityDirection = gravityDirection;
    }

    public void setDaysPerDay(float daysPerDay) {
        this.daysPerDay = daysPerDay;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public void setWorldEpochStart(long worldEpochStart) {
        this.worldEpochStart = worldEpochStart;
    }
}