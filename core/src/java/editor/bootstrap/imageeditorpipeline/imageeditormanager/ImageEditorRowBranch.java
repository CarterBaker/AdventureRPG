package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.bootstrap.imagepipeline.util.MapRowUtility;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ImageEditorRowBranch extends BranchPackage {

    /*
     * The one place the Image Editor's rows are listed and every value they
     * show is read, stepped, typed, cycled or acted on, by the key its row
     * carries: in the Properties tab the image worked, stepped through every
     * image saved or open, its size, and the layer painted with its name and
     * opacity; in the Maps tab every map, the one shown selected and those
     * made marked, with its settings, Generate and Clear below. The status
     * closes both lists. Nothing here acts with no image open but the step
     * through the images.
     */

    // Internal
    private ImageEditorManager imageEditorManager;
    private ImageEditorLayerBranch imageEditorLayerBranch;
    private ImageEditorMapBranch imageEditorMapBranch;

    // Base \\

    @Override
    protected void get() {
        this.imageEditorManager = get(ImageEditorManager.class);
        this.imageEditorLayerBranch = get(ImageEditorLayerBranch.class);
        this.imageEditorMapBranch = get(ImageEditorMapBranch.class);
    }

    // Rows \\

    void buildPropertyRows(ImageProjectInstance project, ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.choice(
                EditorSetting.IMAGE_EDITOR_KEY_IMAGE,
                EditorSetting.IMAGE_EDITOR_LABEL_IMAGE,
                project != null ? project.getImageName() : EditorSetting.IMAGE_EDITOR_VALUE_NO_IMAGE));

        if (project == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.IMAGE_EDITOR_HEADER_NO_IMAGE));
            addStatus(rows);
            return;
        }

        ImageLayerStackStruct stack = project.getStack();

        rows.add(CharacterViewRowStruct.header(EditorSetting.IMAGE_EDITOR_HEADER_SIZE + project.getWidth()
                + EditorSetting.IMAGE_EDITOR_SIZE_SEPARATOR + project.getHeight()
                + (project.isDirty() ? EditorSetting.IMAGE_EDITOR_UNSAVED_SUFFIX : "")));
        rows.add(CharacterViewRowStruct.header(EditorSetting.IMAGE_EDITOR_HEADER_LAYER));
        rows.add(CharacterViewRowStruct.text(
                EditorSetting.IMAGE_EDITOR_KEY_LAYER_NAME,
                EditorSetting.IMAGE_EDITOR_LABEL_LAYER_NAME,
                stack.getLayerName(stack.getActiveLayer())));

        float opacity = stack.getOpacity(stack.getActiveLayer());

        rows.add(CharacterViewRowStruct.ranged(
                EditorSetting.IMAGE_EDITOR_KEY_OPACITY,
                EditorSetting.IMAGE_EDITOR_LABEL_OPACITY,
                CharacterViewUtility.formatNumber(opacity),
                opacity,
                EditorSetting.IMAGE_EDITOR_OPACITY_MIN,
                EditorSetting.IMAGE_EDITOR_OPACITY_MAX,
                EditorSetting.IMAGE_EDITOR_OPACITY_STEP));
        addStatus(rows);
    }

    void buildMapRows(ImageProjectInstance project, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (project == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.IMAGE_EDITOR_HEADER_NO_IMAGE));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.IMAGE_EDITOR_HEADER_MAPS + project.getImageName()));
        MapRowUtility.addMapItems(
                rows,
                EditorSetting.IMAGE_EDITOR_KEY_MAP_ITEM,
                ImageMap.VALUES,
                project.getShownMap(),
                imageEditorMapBranch.getGeneratedMask(project));
        MapRowUtility.addSettingRows(
                rows,
                project.getShownMap(),
                project.getMapSettings(project.getShownMap()),
                EditorSetting.IMAGE_EDITOR_KEY_MAP_SETTING,
                EditorSetting.IMAGE_EDITOR_KEY_MAP_GENERATE,
                EditorSetting.IMAGE_EDITOR_KEY_MAP_CLEAR);
        addStatus(rows);
    }

    private void addStatus(ObjectArrayList<CharacterViewRowStruct> rows) {
        rows.add(CharacterViewRowStruct.header(imageEditorManager.getStatusMessage()));
    }

    // Keys \\

    // A map picked from the Maps tab becomes the one shown and painted
    void select(ImageProjectInstance project, String key) {
        if (project != null && CharacterViewUtility.getKeyName(key).equals(EditorSetting.IMAGE_EDITOR_KEY_MAP_ITEM))
            imageEditorMapBranch.showMap(project, ImageMap.VALUES[CharacterViewUtility.getKeyIndex(key)]);
    }

    void cycle(ImageProjectInstance project, String key, int direction) {
        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.IMAGE_EDITOR_KEY_IMAGE))
            imageEditorManager.cycleImage(direction);
    }

    void step(ImageProjectInstance project, String key, int steps) {

        if (project == null)
            return;

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_OPACITY))
            write(project, key, getNumber(project, key) + steps * EditorSetting.IMAGE_EDITOR_OPACITY_STEP);
        else if (keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_MAP_SETTING))
            write(project, key, getNumber(project, key) + steps * toOption(key).getStep());
        else if (keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_IMAGE))
            imageEditorManager.cycleImage(steps);
    }

    void act(ImageProjectInstance project, String key) {

        if (project == null) {
            imageEditorManager.setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NO_IMAGE);
            return;
        }

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.IMAGE_EDITOR_KEY_LAYER_ADD -> imageEditorLayerBranch.addLayer(project);
            case EditorSetting.IMAGE_EDITOR_KEY_LAYER_REMOVE -> imageEditorLayerBranch.removeLayer(project);
            case EditorSetting.IMAGE_EDITOR_KEY_MAP_GENERATE -> imageEditorMapBranch.generate(project);
            case EditorSetting.IMAGE_EDITOR_KEY_MAP_CLEAR -> imageEditorMapBranch.clear(project);
            default -> {
            }
        }
    }

    // Values \\

    String getValueText(ImageProjectInstance project, String key) {

        if (project == null)
            return "";

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_LAYER_NAME))
            return project.getStack().getLayerName(project.getStack().getActiveLayer());

        if (isNumberKey(keyName))
            return CharacterViewUtility.formatNumber(getNumber(project, key));

        return "";
    }

    boolean isValueValid(ImageProjectInstance project, String key, String text) {

        if (project == null || text == null)
            return false;

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_LAYER_NAME))
            return !text.isBlank();

        return isNumberKey(keyName) && CharacterViewUtility.isNumber(text);
    }

    void setValue(ImageProjectInstance project, String key, String text) {

        if (!isValueValid(project, key, text))
            return;

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.IMAGE_EDITOR_KEY_LAYER_NAME)) {
            imageEditorLayerBranch.renameLayer(project, text);
            return;
        }

        write(project, key, CharacterViewUtility.parseNumber(text));
    }

    // The opacity or a map's setting through the one setter its slider, steps and typed value share
    private void write(ImageProjectInstance project, String key, float value) {

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.IMAGE_EDITOR_KEY_OPACITY))
            imageEditorLayerBranch.setOpacity(project, value);
        else
            imageEditorMapBranch.setValue(project, toOption(key), value);
    }

    private float getNumber(ImageProjectInstance project, String key) {

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.IMAGE_EDITOR_KEY_OPACITY))
            return project.getStack().getOpacity(project.getStack().getActiveLayer());

        return toOption(key).get(project.getMapSettings(project.getShownMap()));
    }

    // Utility \\

    private boolean isNumberKey(String keyName) {
        return keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_OPACITY)
                || keyName.equals(EditorSetting.IMAGE_EDITOR_KEY_MAP_SETTING);
    }

    private MapGenerateOption toOption(String key) {
        return MapGenerateOption.VALUES[CharacterViewUtility.getKeyIndex(key)];
    }
}
