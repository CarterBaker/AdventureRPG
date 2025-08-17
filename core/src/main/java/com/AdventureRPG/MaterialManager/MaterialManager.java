package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.TextureManager.TextureManager;

public class MaterialManager {

    // Game Manager
    private final Settings settings;
    private final GameManager gameManager;
    private final TextureManager TextureManager;

    // Settings
    private final String MATERIAL_JSON_PATH;

    // Base \\

    public MaterialManager(GameManager gameManager) {

        // Game manager
        this.settings = gameManager.settings;
        this.gameManager = gameManager;
        this.TextureManager = gameManager.TextureManager;

        // Settings
        this.MATERIAL_JSON_PATH = settings.MATERIAL_JSON_PATH;

        assembleAllMaterials();
    }

    private void assembleAllMaterials() {

    }
}
