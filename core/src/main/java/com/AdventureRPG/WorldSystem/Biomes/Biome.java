package com.AdventureRPG.WorldSystem.Biomes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.AdventureRPG.Util.Vector2Int;

public class Biome {

    // Base

    public final String name;
    public final int ID;

    // Blending

    public final Set<Integer> similarBiomes;

    // Elevation

    public final int elevation;
    public final int elevationBlending;

    // Water Biomes

    public final boolean aquatic;
    public final boolean ocean;

    public final float waterNoiseScaleX;
    public final float watereNoiseScaleY;

    // SubTerrainian Biomes

    public final boolean allowCaves;
    public final boolean subTerrainian;
    public final boolean allowSurfaceBreak;

    public final float caveNoiseScaleX;
    public final float caveNoiseScaleY;
    public final float caveNoiseScaleZ;
    public final float caveThreshold;

    // Block Composition

    private final Map<Integer, Vector2Int> BiomeComposition;

    // Biome Construction

    public Biome() {

        this.name = "null";
        this.ID = 0;

        this.similarBiomes = new HashSet<>();

        this.elevation = 0;
        this.elevationBlending = 0;

        this.aquatic = false;
        this.ocean = false;

        this.allowCaves = false;
        this.subTerrainian = false;
        this.allowSurfaceBreak = false;

        this.caveNoiseScaleX = 0f;
        this.caveNoiseScaleY = 0f;
        this.caveNoiseScaleZ = 0f;
        this.caveThreshold = 0f;

        this.BiomeComposition = new HashMap<>();
    }

    private Biome(Builder builder) {

        this.name = builder.name;
        this.ID = builder.ID;

        this.similarBiomes = builder.similarBiomes;

        this.elevation = builder.elevation;
        this.elevationBlending = builder.elevationBlending;

        this.aquatic = builder.aquatic;
        this.ocean = builder.ocean;

        this.allowCaves = builder.allowCaves;
        this.subTerrainian = builder.subTerrainian;
        this.allowSurfaceBreak = builder.allowSurfaceBreak;

        this.caveNoiseScaleX = builder.caveNoiseScaleX;
        this.caveNoiseScaleY = builder.caveNoiseScaleY;
        this.caveNoiseScaleZ = builder.caveNoiseScaleZ;
        this.caveThreshold = builder.caveThreshold;

        this.BiomeComposition = builder.BiomeComposition;
    }

    public static class Builder {

        private String name;
        private int ID;

        private Set<Integer> similarBiomes = new HashSet<>();

        private int elevation;
        private int elevationBlending;

        private boolean aquatic;
        private boolean ocean;

        private boolean allowCaves;
        private boolean subTerrainian;
        private boolean allowSurfaceBreak;

        private float caveNoiseScaleX;
        private float caveNoiseScaleY;
        private float caveNoiseScaleZ;
        private float caveThreshold;

        private Map<Integer, Vector2Int> BiomeComposition = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ID(int ID) {
            this.ID = ID;
            return this;
        }

        public Builder elevation(int elevation) {
            this.elevation = elevation;
            return this;
        }

        public Builder elevationBlending(int elevationBlending) {
            this.elevationBlending = elevationBlending;
            return this;
        }

        public Builder aquatic(boolean aquatic) {
            this.aquatic = aquatic;
            return this;
        }

        public Builder ocean(boolean ocean) {
            this.ocean = ocean;
            return this;
        }

        public Builder allowCaves(boolean allowCaves) {
            this.allowCaves = allowCaves;
            return this;
        }

        public Builder subTerrainian(boolean subTerrainian) {
            this.subTerrainian = subTerrainian;
            return this;
        }

        public Builder allowSurfaceBreak(boolean allowSurfaceBreak) {
            this.allowSurfaceBreak = allowSurfaceBreak;
            return this;
        }

        public Builder similarBiomes(Set<Integer> similarBiomes) {
            this.similarBiomes = similarBiomes;
            return this;
        }

        public Builder BiomeComposition(Map<Integer, Vector2Int> BiomeComposition) {
            this.BiomeComposition = BiomeComposition;
            return this;
        }

        public Builder caveNoiseScaleX(float caveNoiseScaleX) {
            this.caveNoiseScaleX = caveNoiseScaleX;
            return this;
        }

        public Builder caveNoiseScaleY(float caveNoiseScaleY) {
            this.caveNoiseScaleY = caveNoiseScaleY;
            return this;
        }

        public Builder caveNoiseScaleZ(float caveNoiseScaleZ) {
            this.caveNoiseScaleZ = caveNoiseScaleZ;
            return this;
        }

        public Builder caveThreshold(float caveThreshold) {
            this.caveThreshold = caveThreshold;
            return this;
        }

        public Biome build() {
            return new Biome(this);
        }
    }

    // Accessible

    public int getBlockForElevation(int blockPosition, int MIN_WORLD_ELEVATION, int elevation) {

        // Total height span for this column
        int columnHeight = elevation - MIN_WORLD_ELEVATION;
        if (columnHeight <= 0)
            return -1; // Invalid column

        // Convert actual Y into biome-relative depth
        int relativeDepth = elevation - blockPosition;
        if (relativeDepth < 0)
            relativeDepth = 0; // Above surface, clamp

        // Track best match
        int selectedBlockID = 0;
        int smallestDepthRange = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Vector2Int> entry : BiomeComposition.entrySet()) {
            int blockID = entry.getKey();
            Vector2Int depthRange = entry.getValue(); // x = minDepth, y = maxDepth

            int min = Math.min(depthRange.x, depthRange.y);
            int max = Math.max(depthRange.x, depthRange.y);

            // Check if this block range includes the depth
            if (relativeDepth >= min && relativeDepth <= max) {
                int rangeSize = max - min;
                // Prefer most specific match (smallest range)
                if (rangeSize < smallestDepthRange) {
                    selectedBlockID = blockID;
                    smallestDepthRange = rangeSize;
                }
            }
        }

        // Fallback: if no match found, return some default block or -1
        return selectedBlockID;
    }

    public boolean isCave(float noiseValue) {
        return noiseValue > caveThreshold;
    }
}
