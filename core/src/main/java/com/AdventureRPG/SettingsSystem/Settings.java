package com.AdventureRPG.SettingsSystem;

import com.badlogic.gdx.graphics.Color;

public class Settings {

    // Debug Settings
    public final boolean debug = true; // Not accessible through json

    // Runtime Settings \\

    // Window Settings
    public float FOV;
    public int windowWidth;
    public int windowHeight;
    public int windowX;
    public int windowY;
    public boolean fullscreen;

    // Render Settings
    public int maxRenderDistance; // How many chunks around player

    // Constant Settings \\

    // Thread Settings
    public final int AVAILABLE_THREADS; // Maximum available threads to pool off

    // Path Settings
    public final String CALENDAR_JSON_PATH; // The location of the calendar json file
    public final String BLOCK_JSON_PATH; // The location of the blocks json file
    public final String BLOCK_TEXTURE_PATH; // The location of all block images
    public final String BIOME_JSON_PATH; // Location of all biome files
    public final String REGION_IMAGE_PATH; // The main image that controls the world

    // PBR Settings
    public final Color NORMAL_MAP_DEFAULT; // Default flat normal color (128,128,255) for missing _N maps
    public final Color HEIGHT_MAP_DEFAULT; // Default black (0,0,0) for missing _H maps
    public final Color METAL_MAP_DEFAULT; // Default black (0,0,0) for missing _M maps
    public final Color CUSTOM_MAP_DEFAULT; // Default black/neutral for missing _custom maps

    // Time Settings
    public final int MINUTES_PER_HOUR; // How many minutes are in an hour
    public final int HOURS_PER_DAY; // How many hours are in a day
    public final int DAYS_PER_DAY; // How many in-game days fit inside a real world day
    public final float MIDDAY_OFFSET; // The offset of days incomparison to real world days

    public final int STARTING_DAY; // The day the game will start in
    public final int STARTING_MONTH; // The month the game will start in
    public final int STARTING_YEAR; // The year the game will start in
    public final int STARTING_AGE; // The age the game will start in
    public final int YEARS_PER_AGE; // How many years are there in an age

    // Movement Settings
    public final float BASE_WALKING_SPEED; // Average human walking speed in m/s

    // Atlas Settings
    public final int BLOCK_TEXTURE_SIZE; // The size of all block images
    public final int BLOCK_ATLAS_PADDING; // The padding between block faces in the atlas
    public final int CHUNKS_PER_PIXEL; // How many chunks per pixel in the region map

    // Scale Settings
    public final float BLOCK_SIZE; // 1x1x1 block scale
    public final int CHUNK_SIZE; // The width of a chunk in blocks
    public final int WORLD_HEIGHT; // The height of the world in blocks

    // World Tick Settings
    public final float WORLD_TICK; // Internal clock used to throttle world load
    public final int MAX_CHUNK_LOADS_PER_FRAME; // 256 chunks per frame
    public final int MAX_CHUNK_LOADS_PER_TICK; // 120 frames per second

    // Base \\

    public Settings(Builder builder) {

        // Runtime Settings \\

        // Window Settings
        this.FOV = builder.FOV;
        this.windowWidth = builder.windowWidth;
        this.windowHeight = builder.windowHeight;
        this.windowX = builder.windowX;
        this.windowY = builder.windowY;
        this.fullscreen = builder.fullscreen;

        // Render Settings
        this.maxRenderDistance = builder.maxRenderDistance;

        // Constant Settings \\

        // Thread Settings
        this.AVAILABLE_THREADS = builder.AVAILABLE_THREADS;

        // Path Settings
        this.CALENDAR_JSON_PATH = builder.CALENDAR_JSON_PATH;
        this.BLOCK_JSON_PATH = builder.BLOCK_JSON_PATH;
        this.BLOCK_TEXTURE_PATH = builder.BLOCK_TEXTURE_PATH;
        this.BIOME_JSON_PATH = builder.BIOME_JSON_PATH;
        this.REGION_IMAGE_PATH = builder.REGION_IMAGE_PATH;

        // PBR Settings
        this.NORMAL_MAP_DEFAULT = builder.NORMAL_MAP_DEFAULT;
        this.HEIGHT_MAP_DEFAULT = builder.HEIGHT_MAP_DEFAULT;
        this.METAL_MAP_DEFAULT = builder.METAL_MAP_DEFAULT;
        this.CUSTOM_MAP_DEFAULT = builder.CUSTOM_MAP_DEFAULT;

        // Time Settings
        this.MINUTES_PER_HOUR = builder.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = builder.HOURS_PER_DAY;
        this.DAYS_PER_DAY = builder.DAYS_PER_DAY;
        this.MIDDAY_OFFSET = builder.MIDDAY_OFFSET;

        this.STARTING_DAY = builder.STARTING_DAY;
        this.STARTING_MONTH = builder.STARTING_MONTH;
        this.STARTING_YEAR = builder.STARTING_YEAR;
        this.STARTING_AGE = builder.STARTING_AGE;
        this.YEARS_PER_AGE = builder.YEARS_PER_AGE;

        // Movement Settings
        this.BASE_WALKING_SPEED = builder.BASE_WALKING_SPEED;

        // Atlas Settings
        this.BLOCK_TEXTURE_SIZE = builder.BLOCK_TEXTURE_SIZE;
        this.BLOCK_ATLAS_PADDING = builder.BLOCK_ATLAS_PADDING;
        this.CHUNKS_PER_PIXEL = builder.CHUNKS_PER_PIXEL;

        // Scale Settings
        this.BLOCK_SIZE = builder.BLOCK_SIZE;
        this.CHUNK_SIZE = builder.CHUNK_SIZE;
        this.WORLD_HEIGHT = builder.WORLD_HEIGHT;

        // World Tick Settings
        this.WORLD_TICK = builder.WORLD_TICK;
        this.MAX_CHUNK_LOADS_PER_FRAME = builder.MAX_CHUNK_LOADS_PER_FRAME;
        this.MAX_CHUNK_LOADS_PER_TICK = builder.MAX_CHUNK_LOADS_PER_TICK;
    }

    // Builder \\

    public static class Builder {

        // Runtime Settings \\

        // Window Settings
        public float FOV = 70;
        public int windowWidth = 1280;
        public int windowHeight = 720;
        public int windowX = -1;
        public int windowY = -1;
        public boolean fullscreen = false;

        // Render Settings
        public int maxRenderDistance = 48;

        // Constant Settings \\

        // Thread Settings
        private int AVAILABLE_THREADS = 3;

        // Path Settings
        private String CALENDAR_JSON_PATH = "calendar.json";
        private String BLOCK_JSON_PATH = "blocks.json";
        private String BLOCK_TEXTURE_PATH = "textures";
        private String BIOME_JSON_PATH = "biomes";
        private String REGION_IMAGE_PATH = "world/world.png";

        // PBR Settings
        private Color NORMAL_MAP_DEFAULT = new Color(0.5f, 0.5f, 1f, 1f);
        private Color HEIGHT_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f);
        private Color METAL_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f);
        private Color CUSTOM_MAP_DEFAULT = new Color(0f, 0f, 0f, 1f);

        // Time Settings
        private int MINUTES_PER_HOUR = 60;
        private int HOURS_PER_DAY = 24;
        private int DAYS_PER_DAY = 20;
        private float MIDDAY_OFFSET = 0.5f;

        private int STARTING_DAY = 15;
        private int STARTING_MONTH = 6;
        private int STARTING_YEAR = 1356;
        private int STARTING_AGE = 3;
        private int YEARS_PER_AGE = 1500;

        // Movement Settings
        private float BASE_WALKING_SPEED = 1.5f;

        // Atlas Settings
        private int BLOCK_TEXTURE_SIZE = 32;
        private int BLOCK_ATLAS_PADDING = 0;
        private int CHUNKS_PER_PIXEL = 16;

        // Scale Settings
        public float BLOCK_SIZE = 1;
        public int CHUNK_SIZE = 16;
        public int WORLD_HEIGHT = 1024;

        // World Tick Settings
        public float WORLD_TICK = 1;
        public int MAX_CHUNK_LOADS_PER_FRAME = 256;
        public int MAX_CHUNK_LOADS_PER_TICK = 16384;

        // Base \\

        // Runtime Settings \\

        // Window Settings \\

        public Builder FOV(float FOV) {
            this.FOV = FOV;
            return this;
        }

        public Builder windowWidth(int windowWidth) {
            this.windowWidth = windowWidth;
            return this;
        }

        public Builder windowHeight(int windowHeight) {
            this.windowHeight = windowHeight;
            return this;
        }

        public Builder windowX(int windowX) {
            this.windowX = windowX;
            return this;
        }

        public Builder windowY(int windowY) {
            this.windowY = windowY;
            return this;
        }

        public Builder fullscreen(boolean fullscreen) {
            this.fullscreen = fullscreen;
            return this;
        }

        // Render Settings \\

        public Builder maxRenderDistance(int maxRenderDistance) {
            this.maxRenderDistance = maxRenderDistance;
            return this;
        }

        // Constant Settings \\

        // Thread Settings \\

        public Builder AVAILABLE_THREADS(int AVAILABLE_THREADS) {
            this.AVAILABLE_THREADS = AVAILABLE_THREADS;
            return this;
        }

        // Paths Settings \\

        public Builder CALENDAR_JSON_PATH(String CALENDAR_JSON_PATH) {
            this.CALENDAR_JSON_PATH = CALENDAR_JSON_PATH;
            return this;
        }

        public Builder BLOCK_JSON_PATH(String BLOCK_JSON_PATH) {
            this.BLOCK_JSON_PATH = BLOCK_JSON_PATH;
            return this;
        }

        public Builder BLOCK_TEXTURE_PATH(String BLOCK_TEXTURE_PATH) {
            this.BLOCK_TEXTURE_PATH = BLOCK_TEXTURE_PATH;
            return this;
        }

        public Builder BIOME_JSON_PATH(String BIOME_JSON_PATH) {
            this.BIOME_JSON_PATH = BIOME_JSON_PATH;
            return this;
        }

        public Builder REGION_IMAGE_PATH(String REGION_IMAGE_PATH) {
            this.REGION_IMAGE_PATH = REGION_IMAGE_PATH;
            return this;
        }

        // PBR Settings \\

        public Builder NORMAL_MAP_DEFAULT(Color NORMAL_MAP_DEFAULT) {
            this.NORMAL_MAP_DEFAULT = NORMAL_MAP_DEFAULT;
            return this;
        }

        public Builder HEIGHT_MAP_DEFAULT(Color HEIGHT_MAP_DEFAULT) {
            this.HEIGHT_MAP_DEFAULT = HEIGHT_MAP_DEFAULT;
            return this;
        }

        public Builder METAL_MAP_DEFAULT(Color METAL_MAP_DEFAULT) {
            this.METAL_MAP_DEFAULT = METAL_MAP_DEFAULT;
            return this;
        }

        public Builder CUSTOM_MAP_DEFAULT(Color CUSTOM_MAP_DEFAULT) {
            this.CUSTOM_MAP_DEFAULT = CUSTOM_MAP_DEFAULT;
            return this;
        }

        // Time Settings \\

        public Builder MINUTES_PER_HOUR(int MINUTES_PER_HOUR) {
            this.MINUTES_PER_HOUR = MINUTES_PER_HOUR;
            return this;
        }

        public Builder HOURS_PER_DAY(int HOURS_PER_DAY) {
            this.HOURS_PER_DAY = HOURS_PER_DAY;
            return this;
        }

        public Builder DAYS_PER_DAY(int DAYS_PER_DAY) {
            this.DAYS_PER_DAY = DAYS_PER_DAY;
            return this;
        }

        public Builder MIDDAY_OFFSET(float MIDDAY_OFFSET) {
            this.MIDDAY_OFFSET = MIDDAY_OFFSET;
            return this;
        }

        public Builder STARTING_DAY(int STARTING_DAY) {
            this.STARTING_DAY = STARTING_DAY;
            return this;
        }

        public Builder STARTING_MONTH(int STARTING_MONTH) {
            this.STARTING_MONTH = STARTING_MONTH;
            return this;
        }

        public Builder STARTING_YEAR(int STARTING_YEAR) {
            this.STARTING_YEAR = STARTING_YEAR;
            return this;
        }

        public Builder STARTING_AGE(int STARTING_AGE) {
            this.STARTING_AGE = STARTING_AGE;
            return this;
        }

        public Builder YEARS_PER_AGE(int YEARS_PER_AGE) {
            this.YEARS_PER_AGE = YEARS_PER_AGE;
            return this;
        }

        // Movement Settings \\

        public Builder BASE_WALKING_SPEED(float BASE_WALKING_SPEED) {
            this.BASE_WALKING_SPEED = BASE_WALKING_SPEED;
            return this;
        }

        // Atlas Settings \\

        public Builder BLOCK_TEXTURE_SIZE(int BLOCK_TEXTURE_SIZE) {
            this.BLOCK_TEXTURE_SIZE = BLOCK_TEXTURE_SIZE;
            return this;
        }

        public Builder BLOCK_ATLAS_PADDING(int BLOCK_ATLAS_PADDING) {
            this.BLOCK_ATLAS_PADDING = BLOCK_ATLAS_PADDING;
            return this;
        }

        public Builder CHUNKS_PER_PIXEL(int CHUNKS_PER_PIXEL) {
            this.CHUNKS_PER_PIXEL = CHUNKS_PER_PIXEL;
            return this;
        }

        // Scale Settings \\

        public Builder BLOCK_SIZE(float BLOCK_SIZE) {
            this.BLOCK_SIZE = BLOCK_SIZE;
            return this;
        }

        public Builder CHUNK_SIZE(int CHUNK_SIZE) {
            this.CHUNK_SIZE = CHUNK_SIZE;
            return this;
        }

        public Builder WORLD_HEIGHT(int WORLD_HEIGHT) {
            this.WORLD_HEIGHT = WORLD_HEIGHT;
            return this;
        }

        // World Tick Settings \\

        public Builder WORLD_TICK(float WORLD_TICK) {
            this.WORLD_TICK = WORLD_TICK;
            return this;
        }

        public Builder MAX_CHUNK_LOADS_PER_FRAME(int MAX_CHUNK_LOADS_PER_FRAME) {
            this.MAX_CHUNK_LOADS_PER_FRAME = MAX_CHUNK_LOADS_PER_FRAME;
            return this;
        }

        public Builder MAX_CHUNK_LOADS_PER_TICK(int MAX_CHUNK_LOADS_PER_TICK) {
            this.MAX_CHUNK_LOADS_PER_TICK = MAX_CHUNK_LOADS_PER_TICK;
            return this;
        }

        // Builder \\

        public Settings build() {
            return new Settings(this);
        }
    }
}
