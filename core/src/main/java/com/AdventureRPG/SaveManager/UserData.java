package com.AdventureRPG.SaveManager;

import java.io.File;
import java.util.Random;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;

public class UserData extends SystemFrame {

    // Settings
    private File path;

    // Seed Management
    private long seed;
    private Random random;

    @Override
    protected void create() {

        // Seed Management
        this.random = new Random();
    }

    @Override
    protected void init() {

        // Settings
        this.path = engineManager.path;
    }

    @Override
    protected void awake() {

        // Seed Management
        generateSeed();
    }

    // Save System \\

    public void loadUserData(File Save) {

    }

    public void writeData() {

    }

    public void readData() {

    }

    // Seed Management

    // TODO: this will need to be assessed later for a potential refactor
    public long getSeed() {
        return seed;
    }

    public void generateSeed() {
        this.seed = random.nextLong();
    }

    public void generateSeed(String seed) {
        if (seed == null || seed.isEmpty()) {
            this.seed = random.nextLong();
        } else {
            try {
                // Try parsing as number (handles "123", "0xABC", etc.)
                this.seed = Long.parseLong(seed);
            } catch (NumberFormatException e) {
                // Use hash if not a valid number
                this.seed = seed.hashCode(); // Or use a better hash if you want full stability
            }
        }
    }

}
