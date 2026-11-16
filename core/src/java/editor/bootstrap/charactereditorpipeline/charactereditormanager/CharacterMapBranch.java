package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CharacterMapBranch extends BranchPackage {

    /*
     * The Maps tab's work on a character's texture, the same work the Image
     * Editor does on an image, through the same generators: each map of a
     * texture keeps its own settings (MapGenerateOption) for the session,
     * Generate works the map painted out of the texture's albedo as painted,
     * layers and all, at the texture's own size, and Clear lays the map's
     * default back; either paints over the map's layer as one undo step, so
     * Save writes and bakes it and Undo takes it back. The albedo is the
     * layers themselves and is never generated.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPaintBranch characterPaintBranch;
    private ImageManager imageManager;

    // Settings — per texture, per ImageMap ordinal
    private Object2ObjectOpenHashMap<String, float[][]> textureName2Settings;

    // Base \\

    @Override
    protected void create() {
        this.textureName2Settings = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPaintBranch = get(CharacterPaintBranch.class);
        this.imageManager = get(ImageManager.class);
    }

    // Settings \\

    // The settings of a texture's map painted, each map its own, at their defaults until first changed
    float[] getSettings(String textureName) {

        float[][] settings = textureName2Settings.get(textureName);

        if (settings == null) {

            settings = new float[ImageMap.VALUES.length][];

            for (int i = 0; i < settings.length; i++)
                settings[i] = MapGenerateOption.createDefaults();

            textureName2Settings.put(textureName, settings);
        }

        return settings[getPaintedMap().ordinal()];
    }

    void setValue(String textureName, MapGenerateOption option, float value) {
        getSettings(textureName)[option.ordinal()] = option.clamp(value);
        characterEditorManager.notifyChanged();
    }

    // The map painted, as the Maps tab names it
    ImageMap getPaintedMap() {
        return EditorSetting.CHARACTER_PAINT_IMAGE_MAPS[characterPaintBranch.getMapIndex()];
    }

    // Generate \\

    // The map painted worked out of the albedo as painted and laid over its layer as one undo step
    void generate(String textureName) {

        ImageMap map = getPaintedMap();

        if (!map.isGenerated()) {
            characterEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_ALBEDO);
            return;
        }

        Pixmap albedo = characterPaintBranch.resolveCanvas(textureName, 0).getComposite();
        int[] pixels = new int[albedo.getWidth() * albedo.getHeight()];

        imageManager.generateMap(map, albedo, getSettings(textureName), pixels);
        characterPaintBranch.writeMap(textureName, characterPaintBranch.getMapIndex(), pixels);
        characterEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_GENERATED + map.getLabel());
    }

    // The map painted laid back to its default everywhere, as one undo step
    void clear(String textureName) {

        ImageMap map = getPaintedMap();

        if (!map.isGenerated()) {
            characterEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_ALBEDO);
            return;
        }

        CharacterPaintCanvasStruct canvas = characterPaintBranch.resolveCanvas(
                textureName,
                characterPaintBranch.getMapIndex());
        int[] pixels = new int[canvas.getWidth() * canvas.getHeight()];

        Arrays.fill(pixels, canvas.getDefaultColor());
        characterPaintBranch.writeMap(textureName, characterPaintBranch.getMapIndex(), pixels);
        characterEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_CLEARED + map.getLabel());
    }
}
