package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

class CharacterPaintPropertyBranch extends BranchPackage {

    /*
     * The one place every value of the Paint panel is read, stepped, typed,
     * cycled or acted on, by the key its row carries, for
     * CharacterPropertyBranch to hand those keys to: the map painted, the
     * layer painted and its opacity on the canvas of the active part's
     * texture, and the colour's red, green and blue; the actions add and
     * remove layers. Nothing here acts while a stroke is under way.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPaintBranch characterPaintBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPaintBranch = get(CharacterPaintBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_PAINT_KEY_MAP,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER,
                    EditorSetting.CHARACTER_PAINT_KEY_OPACITY,
                    EditorSetting.CHARACTER_PAINT_KEY_COLOR,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE -> true;
            default -> false;
        };
    }

    // Values \\

    String getValueText(String key) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_PAINT_KEY_MAP -> EditorSetting.CHARACTER_PAINT_MAP_LABELS[
                    characterPaintBranch.getMapIndex()];
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER -> canvas != null
                    ? canvas.getLayerName(canvas.getActiveLayer())
                    : "";
            case EditorSetting.CHARACTER_PAINT_KEY_OPACITY -> canvas != null
                    ? CharacterViewUtility.formatNumber(canvas.getOpacity(canvas.getActiveLayer()))
                    : "";
            case EditorSetting.CHARACTER_PAINT_KEY_COLOR -> CharacterViewUtility.formatNumber(
                    characterPaintBranch.getColor(CharacterViewUtility.getKeyIndex(key)));
            default -> "";
        };
    }

    boolean isValueValid(String key, String text) {

        String keyName = CharacterViewUtility.getKeyName(key);

        return isIdle() && CharacterViewUtility.isNumber(text)
                && (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR)
                        || keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_OPACITY) && getCanvas() != null);
    }

    void setValue(String key, String text) {

        if (!isValueValid(key, text))
            return;

        write(key, CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        if (!isIdle())
            return;

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterPaintCanvasStruct canvas = getCanvas();

        if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR))
            write(key, characterPaintBranch.getColor(CharacterViewUtility.getKeyIndex(key))
                    + direction * EditorSetting.CHARACTER_PAINT_COLOR_STEP);
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_OPACITY) && canvas != null)
            write(key, canvas.getOpacity(canvas.getActiveLayer())
                    + direction * EditorSetting.CHARACTER_PAINT_OPACITY_STEP);
    }

    // A colour channel or the layer's opacity, through the one setter its slider, steps and typed value share
    private void write(String key, float value) {

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR))
            characterPaintBranch.setColor(CharacterViewUtility.getKeyIndex(key), value);
        else
            characterPaintBranch.setOpacity(getCanvas(), value);
    }

    // Choices \\

    void cycle(String key, int direction) {

        if (!isIdle())
            return;

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterPaintCanvasStruct canvas = getCanvas();

        if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_MAP))
            characterPaintBranch.stepMap(direction);
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_LAYER) && canvas != null)
            characterPaintBranch.stepLayer(canvas, direction);
    }

    // Actions \\

    void act(String key) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        if (!isIdle() || canvas == null)
            return;

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD -> characterPaintBranch.addLayer(canvas);
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE -> characterPaintBranch.removeLayer(canvas);
            default -> {
            }
        }
    }

    // Utility \\

    // The canvas of the active part's texture under the map painted, null with no part or texture open
    CharacterPaintCanvasStruct getCanvas() {
        return characterPaintBranch.resolveActiveCanvas(characterEditorManager.getActiveMeshDocument());
    }

    private boolean isIdle() {
        return !characterPaintBranch.isStroking();
    }
}
