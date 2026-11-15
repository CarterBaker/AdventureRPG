package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;

class CharacterPaintPropertyBranch extends BranchPackage {

    /*
     * The one place every value of the Paint panel is read, stepped, typed,
     * cycled or acted on, by the key its row carries, for
     * CharacterPropertyBranch to hand those keys to: the
     * layer painted and its opacity on the canvas of the active part's
     * texture, and the colour's red, green and blue and its hue, saturation
     * and brightness (a grey keeps the hue last chosen, as a colour wheel
     * does); a map picked from the list; the actions add and remove layers.
     * Nothing here acts while a stroke is under way.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPaintBranch characterPaintBranch;

    // Scratch
    private Color colorScratch;

    // Base \\

    @Override
    protected void create() {
        this.colorScratch = new Color();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPaintBranch = get(CharacterPaintBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER,
                    EditorSetting.CHARACTER_PAINT_KEY_OPACITY,
                    EditorSetting.CHARACTER_PAINT_KEY_COLOR,
                    EditorSetting.CHARACTER_PAINT_KEY_HSB,
                    EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE -> true;
            default -> false;
        };
    }

    // Values \\

    String getValueText(String key) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER -> canvas != null
                    ? canvas.getLayerName(canvas.getActiveLayer())
                    : "";
            case EditorSetting.CHARACTER_PAINT_KEY_OPACITY -> canvas != null
                    ? CharacterViewUtility.formatNumber(canvas.getOpacity(canvas.getActiveLayer()))
                    : "";
            case EditorSetting.CHARACTER_PAINT_KEY_COLOR -> CharacterViewUtility.formatNumber(
                    characterPaintBranch.getColor(CharacterViewUtility.getKeyIndex(key)));
            case EditorSetting.CHARACTER_PAINT_KEY_HSB -> CharacterViewUtility.formatNumber(
                    getHSB(CharacterViewUtility.getKeyIndex(key)));
            default -> "";
        };
    }

    // The paint colour's hue, saturation or brightness, 0 to 1; a grey's hue is the one last chosen
    float getHSB(int index) {

        colorScratch.set(characterPaintBranch.getColor(0), characterPaintBranch.getColor(1),
                characterPaintBranch.getColor(2));

        return switch (index) {
            case 0 -> colorScratch.getSaturation() > 0f ? colorScratch.getHue() : characterPaintBranch.getHue();
            case 1 -> colorScratch.getSaturation();
            default -> colorScratch.getBrightness();
        };
    }

    // The paint colour from a hue, saturation and brightness, the hue kept for a grey
    void setHSB(float hue, float saturation, float brightness) {

        colorScratch.setHSB(hue, Math.clamp(saturation, 0f, 1f), Math.clamp(brightness, 0f, 1f));
        characterPaintBranch.setColor(colorScratch.r, colorScratch.g, colorScratch.b, hue);
    }

    boolean isValueValid(String key, String text) {

        String keyName = CharacterViewUtility.getKeyName(key);

        return isIdle() && CharacterViewUtility.isNumber(text)
                && (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR)
                        || keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_HSB)
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
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_HSB))
            write(key, getHSB(CharacterViewUtility.getKeyIndex(key))
                    + direction * EditorSetting.CHARACTER_PAINT_HSB_STEP);
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_OPACITY) && canvas != null)
            write(key, canvas.getOpacity(canvas.getActiveLayer())
                    + direction * EditorSetting.CHARACTER_PAINT_OPACITY_STEP);
    }

    // A colour channel, a hue, saturation or brightness, or the layer's opacity, through the one setter its slider,
    // steps and typed value share; the hue wraps round the wheel
    private void write(String key, float value) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR))
            characterPaintBranch.setColor(index, value);
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_HSB))
            setHSB(index == 0 ? value : getHSB(0), index == 1 ? value : getHSB(1), index == 2 ? value : getHSB(2));
        else
            characterPaintBranch.setOpacity(getCanvas(), value);
    }

    // Selection \\

    // A map picked from the list becomes the one painted
    void select(String key) {

        if (isIdle() && CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM))
            characterPaintBranch.setMapIndex(CharacterViewUtility.getKeyIndex(key));
    }

    // Choices \\

    void cycle(String key, int direction) {

        if (!isIdle())
            return;

        CharacterPaintCanvasStruct canvas = getCanvas();

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_PAINT_KEY_LAYER) && canvas != null)
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
