package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.nio.ByteBuffer;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.bootstrap.imagepipeline.util.MapGenerateUtility;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CharacterMapBranch extends BranchPackage {

    /*
     * Generates a texture's material maps from its albedo, as a map
     * generator does: the settings (MapGenerateOption) held while the
     * Generate Maps dialog is open, a preview of any map or of the result
     * lit by them worked out at preview size whenever a setting moves, and
     * Generate working out every map asked for at the texture's own size and
     * painting each over its map's layer as one undo step, so Save writes
     * them and Undo takes them back. The source is the albedo as painted,
     * layers and all.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterPaintBranch characterPaintBranch;

    // Options — by MapGenerateOption ordinal, the map the preview shows and a revision moved by any change
    private float[] values;
    private int previewMap;
    private int revision;

    // Source — the texture the maps are made for, null while the dialog is closed
    private String textureName;

    // Preview — its size and buffers, held for the editor's life
    private int previewWidth;
    private int previewRows;
    private int[] previewPixels;
    private float[] previewLuminance;
    private float[] previewHeight;
    private float[] previewOcclusion;
    private float[] previewBlurred;
    private float[] previewScratch;

    // Scratch
    private float[] normalScratch;

    // Base \\

    @Override
    protected void create() {

        int previewCount = EditorSetting.MAP_GENERATE_PREVIEW_SIZE * EditorSetting.MAP_GENERATE_PREVIEW_SIZE;

        // Options
        this.values = new float[MapGenerateOption.VALUES.length];

        for (MapGenerateOption option : MapGenerateOption.VALUES)
            values[option.ordinal()] = option.getDefault();

        // Preview
        this.previewPixels = new int[previewCount];
        this.previewLuminance = new float[previewCount];
        this.previewHeight = new float[previewCount];
        this.previewOcclusion = new float[previewCount];
        this.previewBlurred = new float[previewCount];
        this.previewScratch = new float[previewCount];

        // Scratch
        this.normalScratch = new float[EngineSetting.COLOR_CHANNEL_COUNT - 1];
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterPaintBranch = get(CharacterPaintBranch.class);
    }

    // Dialog \\

    // Opens on the active part's texture, its albedo sampled down to the preview's size; false with none to read
    boolean open(CharacterMeshDocumentInstance document) {

        if (document == null || document.getMesh().getTextureCount() == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.MAP_GENERATE_MESSAGE_NO_TEXTURE);
            return false;
        }

        this.textureName = document.getMesh().getTextureName(0);

        Pixmap albedo = characterPaintBranch
                .resolveCanvas(textureName, EditorSetting.MAP_GENERATE_SOURCE_MAP)
                .getComposite();
        float scale = (float) EditorSetting.MAP_GENERATE_PREVIEW_SIZE
                / Math.max(albedo.getWidth(), albedo.getHeight());

        this.previewWidth = Math.max(1, Math.round(albedo.getWidth() * scale));
        this.previewRows = Math.max(1, Math.round(albedo.getHeight() * scale));

        for (int y = 0; y < previewRows; y++)
            for (int x = 0; x < previewWidth; x++)
                previewPixels[y * previewWidth + x] = albedo.getPixel(
                        Math.min(albedo.getWidth() - 1, (int) (x / scale)),
                        Math.min(albedo.getHeight() - 1, (int) (y / scale)));

        revision++;
        return true;
    }

    void close() {
        this.textureName = null;
    }

    boolean isOpen() {
        return textureName != null;
    }

    // Options \\

    float getValue(MapGenerateOption option) {
        return values[option.ordinal()];
    }

    void setValue(MapGenerateOption option, float value) {
        values[option.ordinal()] = option.clamp(value);
        revision++;
    }

    void step(MapGenerateOption option, int direction) {
        setValue(option, option.isToggle()
                ? 1f - getValue(option)
                : getValue(option) + direction * option.getStep());
    }

    void stepPreview(int direction) {
        this.previewMap = Math.floorMod(previewMap + direction, EditorSetting.MAP_GENERATE_PREVIEW_LABELS.length);
        revision++;
    }

    int getPreviewMap() {
        return previewMap;
    }

    int getRevision() {
        return revision;
    }

    String getTextureName() {
        return textureName;
    }

    int getPreviewWidth() {
        return previewWidth;
    }

    int getPreviewRows() {
        return previewRows;
    }

    // Preview \\

    // The map shown worked out at preview size and written as RGBA bytes, rows bottom first as a texture takes them
    void renderPreview(ByteBuffer out) {

        resolveMaps(previewPixels, previewWidth, previewRows, previewLuminance, previewHeight, previewOcclusion,
                previewBlurred, previewScratch, scaleRadius(previewWidth));

        out.clear();

        for (int row = previewRows - 1; row >= 0; row--)
            for (int x = 0; x < previewWidth; x++) {

                int pixel = resolvePreviewPixel(row * previewWidth + x, x, row);

                for (int channel = 0; channel < EngineSetting.COLOR_CHANNEL_COUNT; channel++)
                    out.put((byte) Math.round(
                            MapGenerateUtility.toChannel(pixel, channel) * EngineSetting.COLOR_CHANNEL_BYTE_MAX));
            }

        out.flip();
    }

    private int resolvePreviewPixel(int index, int x, int row) {

        return switch (previewMap) {
            case EditorSetting.MAP_GENERATE_PREVIEW_RESULT -> resolveLit(index, x, row);
            case EditorSetting.MAP_GENERATE_PREVIEW_NORMAL -> resolveNormalPixel(previewHeight, previewWidth,
                    previewRows, x, row);
            case EditorSetting.MAP_GENERATE_PREVIEW_HEIGHT -> MapGenerateUtility.toGrey(previewHeight[index]);
            case EditorSetting.MAP_GENERATE_PREVIEW_OCCLUSION -> MapGenerateUtility.toGrey(previewOcclusion[index]);
            case EditorSetting.MAP_GENERATE_PREVIEW_ROUGHNESS -> MapGenerateUtility.toGrey(resolveRoughness(
                    previewLuminance[index]));
            default -> MapGenerateUtility.toGrey(getValue(MapGenerateOption.METALLIC));
        };
    }

    // The albedo lit by one light across the generated normal and darkened by the occlusion, the end result
    private int resolveLit(int index, int x, int row) {

        MapGenerateUtility.resolveNormal(previewHeight, previewWidth, previewRows, x, row,
                getValue(MapGenerateOption.NORMAL_STRENGTH), normalScratch);

        float[] light = EditorSetting.MAP_GENERATE_LIGHT;
        float facing = 0f;

        for (int axis = 0; axis < light.length; axis++)
            facing += (normalScratch[axis] * 2f - 1f) * light[axis];

        float shade = (EditorSetting.MAP_GENERATE_AMBIENT + Math.max(0f, facing)) * previewOcclusion[index];
        int albedo = previewPixels[index];

        return MapGenerateUtility.toColor(
                MapGenerateUtility.toChannel(albedo, 0) * shade,
                MapGenerateUtility.toChannel(albedo, 1) * shade,
                MapGenerateUtility.toChannel(albedo, 2) * shade);
    }

    // Generate \\

    // Every map asked for worked out at the texture's size and painted over its layer, each one undo step
    void generate() {

        if (textureName == null || characterPaintBranch.isStroking())
            return;

        Pixmap albedo = characterPaintBranch
                .resolveCanvas(textureName, EditorSetting.MAP_GENERATE_SOURCE_MAP)
                .getComposite();
        int width = albedo.getWidth();
        int rows = albedo.getHeight();
        int count = width * rows;
        int[] pixels = new int[count];
        float[] luminance = new float[count];
        float[] height = new float[count];
        float[] occlusion = new float[count];
        float[] blurred = new float[count];
        float[] scratch = new float[count];
        int[] written = new int[count];

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < width; x++)
                pixels[y * width + x] = albedo.getPixel(x, y);

        resolveMaps(pixels, width, rows, luminance, height, occlusion, blurred, scratch, scaleRadius(width));

        if (isOn(MapGenerateOption.WRITE_NORMAL)) {
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < width; x++)
                    written[y * width + x] = resolveNormalPixel(height, width, rows, x, y);
            characterPaintBranch.writeMap(textureName, EditorSetting.MAP_GENERATE_NORMAL_MAP, written);
        }

        if (isOn(MapGenerateOption.WRITE_HEIGHT))
            writeGreys(EditorSetting.MAP_GENERATE_HEIGHT_MAP, height, written, count);

        if (isOn(MapGenerateOption.WRITE_OCCLUSION))
            writeGreys(EditorSetting.MAP_GENERATE_OCCLUSION_MAP, occlusion, written, count);

        if (isOn(MapGenerateOption.WRITE_ROUGHNESS)) {
            for (int i = 0; i < count; i++)
                written[i] = MapGenerateUtility.toGrey(resolveRoughness(luminance[i]));
            characterPaintBranch.writeMap(textureName, EditorSetting.MAP_GENERATE_ROUGHNESS_MAP, written);
        }

        if (isOn(MapGenerateOption.WRITE_METALLIC)) {
            for (int i = 0; i < count; i++)
                written[i] = MapGenerateUtility.toGrey(getValue(MapGenerateOption.METALLIC));
            characterPaintBranch.writeMap(textureName, EditorSetting.MAP_GENERATE_METALLIC_MAP, written);
        }

        characterEditorManager.setStatusMessage(EditorSetting.MAP_GENERATE_MESSAGE_DONE);
    }

    private void writeGreys(int map, float[] values, int[] written, int count) {

        for (int i = 0; i < count; i++)
            written[i] = MapGenerateUtility.toGrey(values[i]);

        characterPaintBranch.writeMap(textureName, map, written);
    }

    // Maps \\

    // The brightness, height and occlusion of an image, its radii scaled from the preview's to its own size
    private void resolveMaps(
            int[] pixels,
            int width,
            int rows,
            float[] luminance,
            float[] height,
            float[] occlusion,
            float[] blurred,
            float[] scratch,
            float radiusScale) {

        MapGenerateUtility.toLuminance(pixels, luminance, width * rows);
        MapGenerateUtility.resolveHeight(
                luminance, height, blurred, scratch, width, rows,
                Math.max(1, Math.round(getValue(MapGenerateOption.HEIGHT_DETAIL) * radiusScale)),
                Math.round(getValue(MapGenerateOption.HEIGHT_SOFTNESS) * radiusScale),
                getValue(MapGenerateOption.HEIGHT_CONTRAST),
                isOn(MapGenerateOption.HEIGHT_INVERT));
        MapGenerateUtility.resolveOcclusion(
                height, occlusion, scratch, width, rows,
                Math.max(1, Math.round(getValue(MapGenerateOption.OCCLUSION_RADIUS) * radiusScale)),
                getValue(MapGenerateOption.OCCLUSION_STRENGTH));
    }

    private int resolveNormalPixel(float[] height, int width, int rows, int x, int y) {

        MapGenerateUtility.resolveNormal(height, width, rows, x, y,
                getValue(MapGenerateOption.NORMAL_STRENGTH), normalScratch);

        return MapGenerateUtility.toColor(normalScratch[0], normalScratch[1], normalScratch[2]);
    }

    private float resolveRoughness(float luminance) {
        return MapGenerateUtility.resolveRoughness(luminance,
                getValue(MapGenerateOption.ROUGHNESS_BASE), getValue(MapGenerateOption.ROUGHNESS_VARIATION));
    }

    // Radii are set in pixels of the texture at its own size; the preview, smaller, shrinks them alike
    private float scaleRadius(int width) {

        Pixmap albedo = characterPaintBranch
                .resolveCanvas(textureName, EditorSetting.MAP_GENERATE_SOURCE_MAP)
                .getComposite();

        return (float) width / albedo.getWidth();
    }

    private boolean isOn(MapGenerateOption option) {
        return getValue(option) >= EditorSetting.MAP_GENERATE_TOGGLE_ON;
    }
}
