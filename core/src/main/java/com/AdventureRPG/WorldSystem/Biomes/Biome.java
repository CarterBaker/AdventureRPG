package com.AdventureRPG.WorldSystem.Biomes;

public class Biome {

    // Base
    public final String name;
    public final int id;

    // Composition
    public final int airBlock;

    // Base \\

    public Biome() {

        // Base
        this.name = "null";
        this.id = 0;

        // Composition
        this.airBlock = 0;
    }

    private Biome(Builder builder) {

        // Base
        this.name = builder.name;
        this.id = builder.id;

        // Composition
        this.airBlock = builder.airBlock;
    }

    public static class Builder {

        // Base
        private String name;
        private int id;

        // Composition
        private int airBlock;

        // Base \\

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        // Composition \\

        public Builder airBlock(int airBlock) {
            this.airBlock = airBlock;
            return this;
        }

        // Builder \\

        public Biome build() {
            return new Biome(this);
        }
    }
}
