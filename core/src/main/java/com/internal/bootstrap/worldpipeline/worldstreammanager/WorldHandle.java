package com.internal.bootstrap.worldpipeline.worldstreammanager;

import com.badlogic.gdx.graphics.Pixmap;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector2Int;

public class WorldHandle extends HandlePackage {

    private String worldName;
    private int worldID;
    private Pixmap world;
    private Vector2Int worldScale;

    void constructor(
            String worldName,
            int worldID,
            Pixmap world,
            Vector2Int worldScale) {

        this.worldName = worldName;
        this.worldID = worldID;
        this.world = world;
        this.worldScale = worldScale;
    }

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
}