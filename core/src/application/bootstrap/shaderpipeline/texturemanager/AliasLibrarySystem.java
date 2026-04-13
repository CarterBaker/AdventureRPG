package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.io.File;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.core.engine.SystemPackage;
import application.core.settings.EngineSetting;
import application.core.util.FileUtility;
import application.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/*
 * Loads alias definitions from JSON files in the alias directory. Populated
 * once in awake() before any texture load fires. Read-only for the remainder
 * of the TextureManager lifetime. AliasStructs are plain bootstrap containers
 * held in a plain array — no engine lifecycle needed.
 */
public class AliasLibrarySystem extends SystemPackage {

    // Internal
    private AliasStruct[] aliases;
    private Object2IntOpenHashMap<String> aliasLookup;
    private int aliasCount;
    private File root;

    // Base \\

    @Override
    protected void create() {

        this.aliasLookup = new Object2IntOpenHashMap<>();
        this.aliases = new AliasStruct[EngineSetting.SHADER_ALIAS_LIBRARY_INITIAL_CAPACITY];
        this.aliasCount = 0;
        this.root = new File(EngineSetting.BLOCK_TEXTURE_ALIAS_PATH);
    }

    // Load \\

    public void loadAliases() {

        FileUtility.verifyDirectory(root, "Alias root directory not found: " + root.getAbsolutePath());

        List<File> aliasFiles = FileUtility.collectFilesShallow(root, EngineSetting.JSON_FILE_EXTENSIONS);

        for (File file : aliasFiles)
            loadAliasFile(file);
    }

    private void loadAliasFile(File file) {

        try {

            String aliasType = FileUtility.getFileName(file);
            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray colorArray = JsonUtility.validateArray(json, "defaultColor", 3);
            JsonArray aliasesArray = JsonUtility.validateArray(json, "aliases");

            float r = colorArray.get(0).getAsFloat();
            float g = colorArray.get(1).getAsFloat();
            float b = colorArray.get(2).getAsFloat();
            Color defaultColor = new Color(r, g, b, EngineSetting.SHADER_ALIAS_DEFAULT_ALPHA);

            String uniformName = json.has("uniformName")
                    ? json.get("uniformName").getAsString()
                    : null;

            int aliasId = aliasCount;
            ensureCapacity(aliasId + 1);

            aliases[aliasId] = new AliasStruct(aliasType, defaultColor, uniformName);
            aliasCount++;

            for (int i = 0; i < aliasesArray.size(); i++)
                aliasLookup.put(aliasesArray.get(i).getAsString().toLowerCase(), aliasId);

            aliasLookup.put(aliasType.toLowerCase(), aliasId);
        } catch (Exception e) {
            throwException("One or more JSON alias definitions could not be loaded", e);
        }
    }

    private void ensureCapacity(int requiredCapacity) {

        if (requiredCapacity <= aliases.length)
            return;

        int newCapacity = aliases.length * EngineSetting.SHADER_ALIAS_LIBRARY_GROWTH_FACTOR;

        while (newCapacity < requiredCapacity)
            newCapacity *= EngineSetting.SHADER_ALIAS_LIBRARY_GROWTH_FACTOR;

        aliases = java.util.Arrays.copyOf(aliases, newCapacity);
    }

    // Accessible \\

    public AliasStruct[] getAllAliases() {
        return aliases;
    }

    public int getAliasCount() {
        return aliasCount;
    }

    public int get(String aliasVariation) {
        if (aliasVariation == null)
            return EngineSetting.INDEX_NOT_FOUND;
        return aliasLookup.getOrDefault(aliasVariation.toLowerCase(), EngineSetting.INDEX_NOT_FOUND);
    }

    public int getOrDefault(String aliasVariation) {
        if (aliasVariation == null)
            return EngineSetting.INDEX_NOT_FOUND;
        return aliasLookup.getOrDefault(aliasVariation.toLowerCase(), EngineSetting.INDEX_NOT_FOUND);
    }

    public AliasStruct getAlias(int aliasId) {
        if (aliasId < 0 || aliasId >= aliasCount)
            return null;
        return aliases[aliasId];
    }

    public Color getDefaultColor(int id) {
        return aliases[id].getAliasColor();
    }

    public String getUniformName(int aliasId) {
        if (aliasId < 0 || aliasId >= aliasCount)
            return null;
        return aliases[aliasId].getUniformName();
    }

    public boolean hasAlias(String aliasVariation) {
        return getOrDefault(aliasVariation) != EngineSetting.INDEX_NOT_FOUND;
    }
}