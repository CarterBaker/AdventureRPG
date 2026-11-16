package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;

class CharacterPaintPropertyBranch extends BranchPackage {

    /*
     * The one place every value of the Paint panel is read, stepped, typed,
     * cycled or acted on, by the key its row carries, for
     * CharacterPropertyBranch to hand those keys to: the
     * opacity of the layer painted on the canvas of the active part's
     * texture, the layer itself picked in the Layers tab, and the colour's red, green and blue and its hue, saturation
     * and brightness (a grey keeps the hue last chosen, as a colour wheel
     * does); a map picked from the list, the settings of the map painted and
     * its Generate and Clear in the Maps tab; the actions add and remove
     * layers. Nothing here acts while a stroke is under way.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPaintBranch characterPaintBranch;
    private CharacterMapBranch characterMapBranch;

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
        this.characterMapBranch = get(CharacterMapBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_PAINT_KEY_OPACITY,
                    EditorSetting.CHARACTER_PAINT_KEY_COLOR,
                    EditorSetting.CHARACTER_PAINT_KEY_HSB,
                    EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM,
                    EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING,
                    EditorSetting.CHARACTER_PAINT_KEY_MAP_GENERATE,
                    EditorSetting.CHARACTER_PAINT_KEY_MAP_CLEAR,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD,
                    EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE -> true;
            default -> false;
        };
    }

    // Values \\

    String getValueText(String key) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_PAINT_KEY_OPACITY -> canvas != null
                    ? CharacterViewUtility.formatNumber(canvas.getOpacity(canvas.getActiveLayer()))
                    : "";
            case EditorSetting.CHARACTER_PAINT_KEY_COLOR -> CharacterViewUtility.formatNumber(
                    characterPaintBranch.getColor(CharacterViewUtility.getKeyIndex(key)));
            case EditorSetting.CHARACTER_PAINT_KEY_HSB -> CharacterViewUtility.formatNumber(
                    getHSB(CharacterViewUtility.getKeyIndex(key)));
            case EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING -> canvas != null
                    ? CharacterViewUtility.formatNumber(toOption(key).get(
                            characterMapBranch.getSettings(canvas.getTextureName())))
                    : "";
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
                        || (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_OPACITY)
                                || keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING))
                                && getCanvas() != null);
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
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING) && canvas != null)
            write(key, toOption(key).get(characterMapBranch.getSettings(canvas.getTextureName()))
                    + direction * toOption(key).getStep());
    }

    // A colour channel, a hue, saturation or brightness, the layer's opacity or a map's setting, through the one
    // setter its slider, steps and typed value share; the hue wraps round the wheel
    private void write(String key, float value) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_COLOR))
            characterPaintBranch.setColor(index, value);
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_HSB))
            setHSB(index == 0 ? value : getHSB(0), index == 1 ? value : getHSB(1), index == 2 ? value : getHSB(2));
        else if (keyName.equals(EditorSetting.CHARACTER_PAINT_KEY_MAP_SETTING))
            characterMapBranch.setValue(getCanvas().getTextureName(), toOption(key), value);
        else
            characterPaintBranch.setOpacity(getCanvas(), value);
    }

    // Selection \\

    // A map picked from the list becomes the one painted
    void select(String key) {

        if (isIdle() && CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_PAINT_KEY_MAP_ITEM))
            characterPaintBranch.setMapIndex(CharacterViewUtility.getKeyIndex(key));
    }

    // Layers \\

    // A layer picked in the Layers tab becomes the one painted
    void selectLayer(int layerIndex) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        if (isIdle() && canvas != null)
            characterPaintBranch.selectLayer(canvas, layerIndex);
    }

    // A layer's eye in the Layers tab shows or hides it
    void toggleLayerVisible(int layerIndex) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        if (isIdle() && canvas != null)
            characterPaintBranch.toggleLayerVisible(canvas, layerIndex);
    }

    // Actions \\

    void act(String key) {

        CharacterPaintCanvasStruct canvas = getCanvas();

        if (!isIdle() || canvas == null)
            return;

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER_ADD -> characterPaintBranch.addLayer(canvas);
            case EditorSetting.CHARACTER_PAINT_KEY_LAYER_REMOVE -> characterPaintBranch.removeLayer(canvas);
            case EditorSetting.CHARACTER_PAINT_KEY_MAP_GENERATE -> characterMapBranch.generate(canvas.getTextureName());
            case EditorSetting.CHARACTER_PAINT_KEY_MAP_CLEAR -> characterMapBranch.clear(canvas.getTextureName());
            default -> {
            }
        }
    }

    // Utility \\

    // The canvas of the active part's texture under the map painted, null with no part or texture open
    CharacterPaintCanvasStruct getCanvas() {
        return characterPaintBranch.resolveActiveCanvas(characterEditorManager.getActiveMeshDocument());
    }

    private MapGenerateOption toOption(String key) {
        return MapGenerateOption.VALUES[CharacterViewUtility.getKeyIndex(key)];
    }

    private boolean isIdle() {
        return !characterPaintBranch.isStroking();
    }
}
