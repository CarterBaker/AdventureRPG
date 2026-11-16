package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.imagepipeline.util.MapRowUtility;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPaintRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Paint tabs: in the Properties tab the texture
     * painted and the layer painted, picked in the Layers tab, with its
     * opacity above the base; in the
     * Color Wheel its hue, saturation and brightness and its red, green and
     * blue, each a slider across 0 to 1; in the Maps tab every map of the
     * texture, the one painted selected, with its settings, Generate and
     * Clear below it. Every value is read through
     * CharacterPaintPropertyBranch, the same path that edits it. A part with
     * no texture lists only why.
     */

    // Internal
    private CharacterPaintPropertyBranch characterPaintPropertyBranch;
    private CharacterMapBranch characterMapBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterPaintPropertyBranch = get(CharacterPaintPropertyBranch.class);
        this.characterMapBranch = get(CharacterMapBranch.class);
    }

    // Properties \\

    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterPaintCanvasStruct canvas = characterPaintPropertyBranch.getCanvas();

        if (canvas == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_NO_TEXTURE));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_PAINT
                + CharacterViewUtility.toResourceLabel(canvas.getTextureName())));
        rows.add(CharacterViewRowStruct.header(canvas.getLayerName(canvas.getActiveLayer())));

        if (canvas.getActiveLayer() > 0)
            rows.add(CharacterViewRowStruct.number(
                    EditorSetting.CHARACTER_PAINT_KEY_OPACITY,
                    EditorSetting.CHARACTER_PAINT_LABEL_OPACITY,
                    characterPaintPropertyBranch.getValueText(EditorSetting.CHARACTER_PAINT_KEY_OPACITY)));
    }

    // The colour as the Color Wheel lists it: hue, saturation and brightness, then red, green and blue
    void buildColorRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_HSB));
        addChannels(rows, EditorSetting.CHARACTER_PAINT_KEY_HSB, EditorSetting.CHARACTER_PAINT_HSB_LABELS,
                EditorSetting.CHARACTER_PAINT_HSB_STEP);
        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_RGB));
        addChannels(rows, EditorSetting.CHARACTER_PAINT_KEY_COLOR, EditorSetting.CHARACTER_PAINT_COLOR_LABELS,
                EditorSetting.CHARACTER_PAINT_COLOR_STEP);
    }

    private void addChannels(
            ObjectArrayList<CharacterViewRowStruct> rows,
            String keyName,
            String[] labels,
            float step) {

        for (int channel = 0; channel < labels.length; channel++) {

            String key = CharacterViewUtility.toKey(keyName, channel);
            String text = characterPaintPropertyBranch.getValueText(key);

            rows.add(CharacterViewRowStruct.ranged(
                    key,
                    labels[channel],
                    text,
                    CharacterViewUtility.parseNumber(text),
                    EditorSetting.CHARACTER_PAINT_CHANNEL_MIN,
                    EditorSetting.CHARACTER_PAINT_CHANNEL_MAX,
                    step));
        }
    }

    // Every map of the texture painted, as the Maps tab lists them, the one painted selected, and its settings
    void buildMapRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterPaintCanvasStruct canvas = characterPaintPropertyBranch.getCanvas();

        if (canvas == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_NO_TEXTURE));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_MAPS
                + CharacterViewUtility.toResourceLabel(canvas.getTextureName())));
        MapRowUtility.addMapItems(
                rows,
                EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM,
                EditorSetting.CHARACTER_PAINT_IMAGE_MAPS,
                characterMapBranch.getPaintedMap(),
                0L);
        MapRowUtility.addSettingRows(
                rows,
                characterMapBranch.getPaintedMap(),
                characterMapBranch.getSettings(canvas.getTextureName()),
                EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING,
                EditorSetting.CHARACTER_PAINT_KEY_MAP_GENERATE,
                EditorSetting.CHARACTER_PAINT_KEY_MAP_CLEAR);
    }
}
