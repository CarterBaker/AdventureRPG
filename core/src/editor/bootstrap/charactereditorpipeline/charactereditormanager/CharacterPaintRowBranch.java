package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPaintRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Paint panel: the texture painted under its map,
     * the layer painted with its opacity above the base, and the colour's
     * three channels, every value read through CharacterPaintPropertyBranch,
     * the same path that edits it. A part with no texture lists only why.
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
        addChoice(rows, EditorSetting.CHARACTER_PAINT_KEY_MAP, EditorSetting.CHARACTER_PAINT_LABEL_MAP);
        addChoice(rows, EditorSetting.CHARACTER_PAINT_KEY_LAYER, EditorSetting.CHARACTER_PAINT_LABEL_LAYER);

        if (canvas.getActiveLayer() > 0)
            rows.add(CharacterViewRowStruct.number(
                    EditorSetting.CHARACTER_PAINT_KEY_OPACITY,
                    EditorSetting.CHARACTER_PAINT_LABEL_OPACITY,
                    characterPaintPropertyBranch.getValueText(EditorSetting.CHARACTER_PAINT_KEY_OPACITY)));

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_PAINT_HEADER_COLOR));

        for (int channel = 0; channel < EditorSetting.CHARACTER_PAINT_COLOR_LABELS.length; channel++) {

            String key = CharacterViewUtility.toKey(EditorSetting.CHARACTER_PAINT_KEY_COLOR, channel);

            rows.add(CharacterViewRowStruct.number(
                    key,
                    EditorSetting.CHARACTER_PAINT_COLOR_LABELS[channel],
                    characterPaintPropertyBranch.getValueText(key)));
        }
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.choice(key, label, characterPaintPropertyBranch.getValueText(key)));
    }
}
