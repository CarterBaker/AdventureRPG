package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.awt.Color;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.JsonUtility;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.AdventureRPG.Core.Bootstrap.EngineConstant;

public class AliasLibrarySystem extends SystemFrame {

    // Engine
    private Gson gson;

    // Internal
    private AliasInstance[] aliases;
    private HashMap<String, Integer> aliasLookup;
    private int aliasCount;
    private File root;

    // Base \\

    @Override
    protected void create() {

        // Engine
        this.gson = gameEngine.gson;

        // Internal
        this.aliasLookup = new HashMap<>();
        this.aliases = new AliasInstance[16];
        this.aliasCount = 0;
        this.root = new File(EngineConstant.BLOCK_TEXTURE_ALIAS_PATH);
    }

    // File Extraction \\

    void loadAliases() {

        if (!root.exists() || !root.isDirectory()) // TODO: move to static helper class.
            throw new FileException.FileNotFoundException(root);

        Path directory = root.toPath();

        try (var stream = java.nio.file.Files.list(directory)) {

            stream
                    .filter(Files::isRegularFile)
                    .forEach(p -> {

                        File file = p.toFile();

                        if (!FileUtility.hasExtension(file, "json"))
                            return;

                        loadAliasFile(file);
                    });

        }

        catch (Exception e) { // TODO: Not the best error
            throw new FileException.FileNotFoundException(root);
        }
    }

    // JSon Parsing \\

    private void loadAliasFile(File file) {

        try (FileReader reader = new FileReader(file)) {

            // Extract alias name from filename
            String aliasType = FileUtility.getFileName(file);

            // Parse JSON
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            JsonArray colorArray = JsonUtility.validateArray(json, "defaultColor");
            JsonArray aliasesArray = JsonUtility.validateArray(json, "aliases");

            // Parse default color
            float r = colorArray.get(0).getAsFloat();
            float g = colorArray.get(1).getAsFloat();
            float b = colorArray.get(2).getAsFloat();
            Color defaultColor = new Color(r, g, b, 1.0f);

            // Create alias instance
            int aliasId = aliasCount;
            ensureCapacity(aliasId + 1);

            aliases[aliasId] = (AliasInstance) create(new AliasInstance(aliasType, defaultColor));
            aliasCount++;

            // Register all alias variations in lookup map
            for (int i = 0; i < aliasesArray.size(); i++) {

                String aliasVariation = aliasesArray.get(i).getAsString().toLowerCase();
                aliasLookup.put(aliasVariation, aliasId);
            }

            // Also register the file name itself as an alias
            aliasLookup.put(aliasType.toLowerCase(), aliasId);
        }

        catch (Exception e) { // TODO: Make my own error
            throw new FileException.FileNotFoundException(file);
        }
    }

    private void ensureCapacity(int requiredCapacity) {

        if (requiredCapacity <= aliases.length)
            return;

        int newCapacity = aliases.length * 2;

        while (newCapacity < requiredCapacity)
            newCapacity *= 2;

        aliases = Arrays.copyOf(aliases, newCapacity);
    }

    // Accessible \\

    AliasInstance[] getAllAliases() {
        return aliases;
    }

    int getOrDefault(String aliasVariation) {

        if (aliasVariation == null)
            return -1;

        String key = aliasVariation.toLowerCase();
        return aliasLookup.getOrDefault(key, -1);
    }

    AliasInstance getAlias(int aliasId) {

        if (aliasId < 0 || aliasId >= aliasCount)
            return null;

        return aliases[aliasId];
    }

    Color getDefaultColor(int id) {
        return aliases[id].defaultColor;
    }

    int getAliasCount() {
        return aliasCount;
    }

    boolean hasAlias(String aliasVariation) {
        return getOrDefault(aliasVariation) != -1;
    }
}