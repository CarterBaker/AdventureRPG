package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.TextureManager.TextureManager;

public class Block {

    public final String name;
    public final int id;

    public final int up;
    public final int north;
    public final int south;
    public final int east;
    public final int west;
    public final int down;

    public final State state;

    public Block(TextureManager textureManager, Builder builder, int id) {

        this.name = builder.name;
        this.id = id;

        this.up = (textureManager != null) ? textureManager.getIDFromTexture(builder.up) : -9;
        this.north = (textureManager != null) ? textureManager.getIDFromTexture(builder.north) : -9;
        this.south = (textureManager != null) ? textureManager.getIDFromTexture(builder.south) : -9;
        this.east = (textureManager != null) ? textureManager.getIDFromTexture(builder.east) : -9;
        this.west = (textureManager != null) ? textureManager.getIDFromTexture(builder.west) : -9;
        this.down = (textureManager != null) ? textureManager.getIDFromTexture(builder.down) : -9;

        this.state = builder.state;
    }
}
