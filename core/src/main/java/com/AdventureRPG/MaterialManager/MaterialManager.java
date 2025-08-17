package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class MaterialManager {

    // Game Manager
    private final Settings settings;
    private final GameManager gameManager;

    // Base \\

    public MaterialManager(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;
        this.gameManager = gameManager;
    }

}
