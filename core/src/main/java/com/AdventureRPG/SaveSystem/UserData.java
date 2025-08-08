package com.AdventureRPG.SaveSystem;

import java.util.Random;
import java.io.File;
import com.AdventureRPG.SettingsSystem.Settings;

public class UserData {

    // Save System
    private final Settings Settings;
    private final File path;

    // Seed Management
    private long Seed;

    public UserData(SaveSystem saveSystem) {

        // Save System
        this.Settings = saveSystem.settings;
        this.path = saveSystem.path;

        // Seed Management
        GenerateSeed();
    }

    // Save System \\

    public void LoadUserData(File Save) {

    }

    public void WriteData() {

    }

    public void ReadData() {

    }

    // Seed Management

    // TODO: this will need to be assessed later for a potential refactor
    public long Seed() {
        return Seed;
    }

    public void GenerateSeed() {
        this.Seed = new Random().nextLong();
    }

    public void GenerateSeed(String Seed) {
        if (Seed == null || Seed.isEmpty()) {
            this.Seed = new Random().nextLong();
        } else {
            try {
                // Try parsing as number (handles "123", "0xABC", etc.)
                this.Seed = Long.parseLong(Seed);
            } catch (NumberFormatException e) {
                // Use hash if not a valid number
                this.Seed = Seed.hashCode(); // Or use a better hash if you want full stability
            }
        }
    }

}
