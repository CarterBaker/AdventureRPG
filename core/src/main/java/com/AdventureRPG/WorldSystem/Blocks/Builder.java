package com.AdventureRPG.WorldSystem.Blocks;

public class Builder {

    // Reference
    public String name = "";

    // Texture
    public String up = "";
    public String north = "";
    public String south = "";
    public String east = "";
    public String west = "";
    public String down = "";

    // State
    public State state;

    // Base \\

    public Builder name(String name) {
        this.name = name;
        return this;
    }

    public Builder up(String up) {
        this.up = up;
        return this;
    }

    public Builder north(String north) {
        this.north = north;
        return this;
    }

    public Builder south(String south) {
        this.south = south;
        return this;
    }

    public Builder east(String east) {
        this.east = east;
        return this;
    }

    public Builder west(String west) {
        this.west = west;
        return this;
    }

    public Builder down(String down) {
        this.down = down;
        return this;
    }

    public Builder state(State state) {
        this.state = state;
        return this;
    }

    public Block build(BlockAtlas blockAtlas, int id) {
        return new Block(blockAtlas, this, id);
    }
}
