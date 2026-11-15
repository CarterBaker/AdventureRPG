package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
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
     * blue, each a slider across 0 to 1; in Texture Maps every map of the
     * texture, the one painted selected. Every value is read through
     * CharacterPaintPropertyBranch, the same path that edits it. A part with
     * no texture lists only why.
     */

    // Internal
    private CharacterPaintPropertyBranch characterPaintPropertyBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterPaintPropertyBranch = get(CharacterPaintPropertyBranch.class);
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

    // Every map of the texture painted, as Texture Maps lists them, the one painted selected
    void buildMapRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterPaintCanvasStruct canvas = characterPaintPropertyBranch.getCanvas();

        if (canvas == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_NO_TEXTURE));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_MAPS
                + CharacterViewUtility.toResourceLabel(canvas.getTextureName())));

        for (int map = 0; map < EditorSetting.CHARACTER_PAINT_MAP_LABELS.length; map++)
            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM, map),
                    EditorSetting.CHARACTER_PAINT_MAP_LABELS[map],
                    map == canvas.getMapIndex()));
    }
}
