package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.WorldSystem.Biomes.Biome;

public class Block {

    public final String name;
    public final int ID;

    public final int top;
    public final int side;
    public final int bottom;

    public final State state;

    public Block(Builder builder) {

        this.name = builder.name;
        this.ID = builder.ID;
        this.top = builder.top;
        this.side = builder.side;
        this.bottom = builder.bottom;

        this.state = builder.state;
    }

    public static class Builder {

        private String name;
        private int ID;
        private int top;
        private int side;
        private int bottom;

        private State state;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ID(int ID) {
            this.ID = ID;
            return this;
        }

        public Builder top(int top) {
            this.top = top;
            return this;
        }

        public Builder side(int side) {
            this.side = side;
            return this;
        }

        public Builder bottom(int bottom) {
            this.bottom = bottom;
            return this;
        }

        public Builder state(State state) {
            this.state = state;
            return this;
        }

        public Block build() {
            return new Block(this);
        }
    }

}
