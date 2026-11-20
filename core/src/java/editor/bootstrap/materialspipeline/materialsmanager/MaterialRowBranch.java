package editor.bootstrap.materialspipeline.materialsmanager;

import java.io.File;

import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MaterialRowBranch extends BranchPackage {

    /*
     * The Outliner's rows for the material chosen. A texture of the game
     * lists itself, stepped through every material, and how a material is
     * made from it; a material of the workspace lists its base, the texture
     * or image each map is taken from, stepped through the base and every
     * material or chosen as an image file, and its parameters, each a slider
     * across its range. Every row steps,
     * cycles and takes a typed value through MaterialsManager, so the
     * viewer, the brush and the file all follow.
     */

    // Internal
    private MaterialsManager materialsManager;

    // Base \\

    @Override
    protected void get() {
        this.materialsManager = get(MaterialsManager.class);
    }

    // Rows \\

    void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        MaterialDocumentStruct document = materialsManager.getChosenDocument();

        if (document == null) {
            buildTextureRows(rows, materialsManager.getSelectedMaterial());
            return;
        }

        rows.add(CharacterViewRowStruct.header(document.isDirty()
                ? document.getMaterialName() + EditorSetting.CHARACTER_VIEW_DIRTY_MARKER
                : document.getMaterialName()));
        rows.add(CharacterViewRowStruct.choice(
                EditorSetting.MATERIALS_KEY_BASE,
                EditorSetting.MATERIALS_LABEL_BASE,
                CharacterViewUtility.toResourceLabel(document.getBaseTexture())));
        rows.add(CharacterViewRowStruct.header(EditorSetting.MATERIALS_HEADER_MAPS));

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_LABELS.length; m++) {
            rows.add(CharacterViewRowStruct.choice(
                    CharacterViewUtility.toKey(EditorSetting.MATERIALS_KEY_MAP, m),
                    EditorSetting.CHARACTER_PAINT_MAP_LABELS[m],
                    toSourceLabel(document, m)));
            rows.add(CharacterViewRowStruct.action(
                    CharacterViewUtility.toKey(EditorSetting.MATERIALS_KEY_IMAGE, m),
                    EditorSetting.MATERIALS_LABEL_IMAGE_PREFIX + EditorSetting.CHARACTER_PAINT_MAP_LABELS[m]
                            + EditorSetting.MATERIALS_LABEL_IMAGE_SUFFIX));
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.MATERIALS_HEADER_PARAMETERS));

        for (int p = 0; p < EditorSetting.MATERIALS_PARAMETER_KEYS.length; p++)
            rows.add(CharacterViewRowStruct.ranged(
                    CharacterViewUtility.toKey(EditorSetting.MATERIALS_KEY_PARAMETER, p),
                    EditorSetting.MATERIALS_PARAMETER_LABELS[p],
                    CharacterViewUtility.formatNumber(document.getParameter(p)),
                    document.getParameter(p),
                    EditorSetting.MATERIALS_PARAMETER_MIN[p],
                    EditorSetting.MATERIALS_PARAMETER_MAX[p],
                    EditorSetting.MATERIALS_PARAMETER_STEP[p]));
    }

    // A texture of the game, stepped through every material, and how to make a material of the workspace from it
    private void buildTextureRows(ObjectArrayList<CharacterViewRowStruct> rows, String textureName) {
        rows.add(CharacterViewRowStruct.header(CharacterViewUtility.toResourceLabel(textureName)));
        rows.add(CharacterViewRowStruct.choice(
                EditorSetting.MATERIALS_KEY_MATERIAL,
                EditorSetting.MATERIALS_LABEL_MATERIAL,
                CharacterViewUtility.toResourceLabel(textureName)));
        rows.add(CharacterViewRowStruct.header(EditorSetting.MATERIALS_TEXT_NEW_HINT));
    }

    // What a map is taken from: its image file by name, its own texture, or the base
    private String toSourceLabel(MaterialDocumentStruct document, int map) {

        if (document.hasMapImage(map))
            return FileUtility.getFileName(document.getMapImage(map));

        return document.getMapTexture(map).isEmpty()
                ? EditorSetting.MATERIALS_TEXT_FROM_BASE
                : CharacterViewUtility.toResourceLabel(document.getMapTexture(map));
    }

    // Step \\

    // A parameter moved by whole steps of its own
    void step(String key, int steps) {

        MaterialDocumentStruct document = materialsManager.getChosenDocument();
        int parameter = resolveParameter(key);

        if (document == null || parameter == EngineSetting.INDEX_NOT_FOUND)
            return;

        materialsManager.setParameter(
                parameter,
                document.getParameter(parameter) + steps * EditorSetting.MATERIALS_PARAMETER_STEP[parameter],
                key);
    }

    // The material, the base or a map's texture stepped to the next or previous material, a map's passing its base
    void cycle(String key, int direction) {

        MaterialDocumentStruct document = materialsManager.getChosenDocument();
        int map = resolveMap(key);

        if (key.equals(EditorSetting.MATERIALS_KEY_MATERIAL))
            materialsManager.cycleMaterial(direction);
        else if (document != null && key.equals(EditorSetting.MATERIALS_KEY_BASE))
            materialsManager.setBaseTexture(materialsManager.stepMaterialName(document.getBaseTexture(), direction));
        else if (document != null && map != EngineSetting.INDEX_NOT_FOUND)
            materialsManager.setMapTexture(map, stepSource(document.getMapTexture(map), direction));
    }

    // The texture after or before a map's own, through the base between the last material and the first
    private String stepSource(String textureName, int direction) {

        String stepped = materialsManager.stepMaterialName(textureName, direction);

        if (textureName.isEmpty())
            return stepped;

        return stepped.equals(materialsManager.stepMaterialName("", direction)) ? "" : stepped;
    }

    // Values \\

    String getValueText(String key) {

        MaterialDocumentStruct document = materialsManager.getChosenDocument();
        int parameter = resolveParameter(key);
        int map = resolveMap(key);

        if (document == null)
            return materialsManager.getSelectedMaterial();

        if (parameter != EngineSetting.INDEX_NOT_FOUND)
            return CharacterViewUtility.formatNumber(document.getParameter(parameter));

        if (map != EngineSetting.INDEX_NOT_FOUND)
            return document.getMapTexture(map);

        return document.getBaseTexture();
    }

    // A number for a parameter, a material's name for the material and the base, either or nothing for a map
    boolean isValueValid(String key, String text) {

        if (resolveParameter(key) != EngineSetting.INDEX_NOT_FOUND)
            return CharacterViewUtility.isNumber(text);

        if (resolveMap(key) != EngineSetting.INDEX_NOT_FOUND && text.isEmpty())
            return true;

        return materialsManager.hasMaterial(text);
    }

    void setValue(String key, String text) {

        MaterialDocumentStruct document = materialsManager.getChosenDocument();
        int parameter = resolveParameter(key);
        int map = resolveMap(key);

        if (!isValueValid(key, text))
            return;

        if (document == null)
            materialsManager.selectMaterial(text);
        else if (parameter != EngineSetting.INDEX_NOT_FOUND)
            materialsManager.setParameter(parameter, CharacterViewUtility.parseNumber(text), null);
        else if (map != EngineSetting.INDEX_NOT_FOUND)
            materialsManager.setMapTexture(map, text);
        else
            materialsManager.setBaseTexture(text);
    }

    // Images \\

    // The map an image row names taken from the image file chosen
    void setImage(String key, File imageFile) {

        int map = resolveIndex(key, EditorSetting.MATERIALS_KEY_IMAGE, EditorSetting.CHARACTER_PAINT_MAP_LABELS.length);

        if (map != EngineSetting.INDEX_NOT_FOUND)
            materialsManager.setMapImage(map, imageFile);
    }

    // Keys \\

    // The parameter a key names, none for a key that names none
    private int resolveParameter(String key) {
        return resolveIndex(key, EditorSetting.MATERIALS_KEY_PARAMETER, EditorSetting.MATERIALS_PARAMETER_KEYS.length);
    }

    // The map a key names, none for a key that names none
    private int resolveMap(String key) {
        return resolveIndex(key, EditorSetting.MATERIALS_KEY_MAP, EditorSetting.CHARACTER_PAINT_MAP_LABELS.length);
    }

    private int resolveIndex(String key, String name, int count) {

        int index = CharacterViewUtility.getKeyName(key).equals(name)
                ? CharacterViewUtility.getKeyIndex(key)
                : EngineSetting.INDEX_NOT_FOUND;

        return index < count ? index : EngineSetting.INDEX_NOT_FOUND;
    }
}
