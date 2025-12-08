package com.AdventureRPG.worldmanager.biomes;

import com.badlogic.gdx.graphics.Color;

public class Biome {

    // Base
    public final String name;
    public final int id;
    public final Color biomeColor;

    // Composition
    public final int airBlock;

    // Base \\

    public Biome() {

        // Base
        this.name = "null";
        this.id = 0;
        this.biomeColor = Color.WHITE;

        // Composition
        this.airBlock = 0;
    }

    private Biome(Builder builder) {

        // Base
        this.name = builder.name;
        this.id = builder.id;
        this.biomeColor = builder.biomeColor;

        // Composition
        this.airBlock = builder.airBlock;
    }

    public static class Builder {

        // Base
        private String name;
        private int id;
        private Color biomeColor;

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

        public Builder biomeColor(Color biomeColor) {
            this.biomeColor = biomeColor;
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
