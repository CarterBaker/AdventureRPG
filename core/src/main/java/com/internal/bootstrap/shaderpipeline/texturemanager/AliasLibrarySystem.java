package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.awt.Color;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/*
 * Loads alias definitions from JSON files in the alias directory. Each file
 * describes one alias type with a default fill colour and a list of accepted
 * name variants. Alias IDs are assigned in load order and are stable for the
 * lifetime of the TextureManager.
 */
public class AliasLibrarySystem extends SystemPackage {

    // Internal
    private AliasData[] aliases;
    private Object2IntOpenHashMap<String> aliasLookup;
    private int aliasCount;
    private File root;

    // Base \\

    @Override
    protected void create() {
        this.aliasLookup = new Object2IntOpenHashMap<>();
        this.aliases = new AliasData[16];
        this.aliasCount = 0;
        this.root = new File(EngineSetting.BLOCK_TEXTURE_ALIAS_PATH);
    }

    // File Extraction \\

    void loadAliases() {
        FileUtility.verifyDirectory(root, "[AliasLibrarySystem] The root folder could not be verified");
        List<File> aliasFiles = FileUtility.collectFilesShallow(root, EngineSetting.JSON_FILE_EXTENSIONS);
        for (File file : aliasFiles)
            loadAliasFile(file);
    }

    // JSON Parsing \\

    private void loadAliasFile(File file) {
        try {
            String aliasType = FileUtility.getFileName(file);

            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray colorArray = JsonUtility.validateArray(json, "defaultColor", 3);
            JsonArray aliasesArray = JsonUtility.validateArray(json, "aliases");

            float r = colorArray.get(0).getAsFloat();
            float g = colorArray.get(1).getAsFloat();
            float b = colorArray.get(2).getAsFloat();
            Color defaultColor = new Color(r, g, b, 1.0f);

            int aliasId = aliasCount;
            ensureCapacity(aliasId + 1);

            aliases[aliasId] = create(AliasData.class);
            aliases[aliasId].constructor(aliasType, defaultColor);
            aliasCount++;

            for (int i = 0; i < aliasesArray.size(); i++)
                aliasLookup.put(aliasesArray.get(i).getAsString().toLowerCase(), aliasId);

            aliasLookup.put(aliasType.toLowerCase(), aliasId);

        } catch (Exception e) {
            throwException("One or more json alias definitions could not be loaded", e);
        }
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= aliases.length)
            return;
        int newCapacity = aliases.length * 2;
        while (newCapacity < requiredCapacity)
            newCapacity *= 2;
        aliases = java.util.Arrays.copyOf(aliases, newCapacity);
    }

    // Accessible \\

    public AliasData[] getAllAliases() {
        return aliases;
    }

    public int get(String aliasVariation) {
        if (aliasVariation == null)
            return -1;
        return aliasLookup.getOrDefault(aliasVariation.toLowerCase(), -1);
    }

    public int getOrDefault(String aliasVariation) {
        if (aliasVariation == null)
            return -1;
        return aliasLookup.getOrDefault(aliasVariation.toLowerCase(), -1);
    }

    public AliasData getAlias(int aliasId) {
        if (aliasId < 0 || aliasId >= aliasCount)
            return null;
        return aliases[aliasId];
    }

    public Color getDefaultColor(int id) {
        return aliases[id].getAliasColor();
    }

    public int getAliasCount() {
        return aliasCount;
    }

    public boolean hasAlias(String aliasVariation) {
        return getOrDefault(aliasVariation) != -1;
    }
}