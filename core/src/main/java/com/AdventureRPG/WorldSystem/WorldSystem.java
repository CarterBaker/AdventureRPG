package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.Util.*;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

import com.AdventureRPG.WorldSystem.Data.PNGReader;
import com.AdventureRPG.WorldSystem.Grid.Grid;
import com.AdventureRPG.WorldSystem.Regions.RegionManager;

public class WorldSystem {

    // Game Manager
    public final GameManager GameManager;

    // Settings
    public final Settings settings;

    // World System
    public final WorldLoader WorldLoader;
    public final PNGReader PNGReader;
    public final Grid Grid;
    public final RegionManager RegionManager;

    // Dependencies
    public final Vector2Int WORLD_Scale;

    public WorldSystem(GameManager GameManager, Settings settings) {

        // Game Manager
        this.GameManager = GameManager;

        // Settings
        this.settings = settings;

        // World System
        this.WorldLoader = new WorldLoader();
        this.PNGReader = new PNGReader(settings);
        this.Grid = new Grid();
        this.RegionManager = new RegionManager();

        // Dependencies
        WORLD_Scale = PNGReader.GetWorldScale();
    }

    public void Render() {

    }
}
