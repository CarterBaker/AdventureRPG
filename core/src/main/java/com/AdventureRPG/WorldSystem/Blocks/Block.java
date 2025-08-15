package com.AdventureRPG.WorldSystem.Blocks;

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

    public Block(BlockAtlas blockAtlas, Builder builder, int id) {

        this.name = builder.name;
        this.id = id;

        this.up = (blockAtlas != null) ? blockAtlas.getIdByName(builder.up) : -9;
        this.north = (blockAtlas != null) ? blockAtlas.getIdByName(builder.north) : -9;
        this.south = (blockAtlas != null) ? blockAtlas.getIdByName(builder.south) : -9;
        this.east = (blockAtlas != null) ? blockAtlas.getIdByName(builder.east) : -9;
        this.west = (blockAtlas != null) ? blockAtlas.getIdByName(builder.west) : -9;
        this.down = (blockAtlas != null) ? blockAtlas.getIdByName(builder.down) : -9;

        this.state = builder.state;
    }
}
