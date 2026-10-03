package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorTool;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorManager extends ManagerPackage {

    /*
     * Owns the world editor's shared state: the active world's image, opened
     * through ImageManager on the world's own pixels so the biome field reads
     * every stroke at once, the palette of biomes the image can paint, the
     * selected biome, tool and brush radius, and the status line. Each edited
     * region is handed to MapManager, which regenerates only the map tiles it
     * reaches, and to WorldStreamManager, which streams every preview's
     * terrain again once the edits settle; the palette follows every live
     * biome rebuild. Biome edits from the Info Panel reach the engine, and
     * biome selection stays in step with the hierarchy, through
     * WorldBiomeBranch.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private MapManager mapManager;
    private WorldStreamManager worldStreamManager;
    private ImageManager imageManager;
    private WorldBiomeBranch worldBiomeBranch;

    // World
    private WorldHandle worldHandle;
    private ImageDocumentInstance worldImage;

    // Palette
    private ObjectArrayList<WorldBiomeEntryStruct> palette;
    private ObjectArrayList<String> paintedNames;
    private IntArrayList paintedColors;
    private int paletteBiomeRevision;
    private int paletteRevision;

    // Brush
    private WorldEditorTool activeTool;
    private int brushRadius;
    private String selectedBiomeName;
    private ImageBrushStruct brush;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.worldBiomeBranch = create(WorldBiomeBranch.class);

        // Palette
        this.palette = new ObjectArrayList<>();
        this.paintedNames = new ObjectArrayList<>();
        this.paintedColors = new IntArrayList();
        this.paletteBiomeRevision = EngineSetting.INDEX_NOT_FOUND;

        // Brush
        this.activeTool = WorldEditorTool.BRUSH;
        this.brushRadius = EditorSetting.WORLD_EDITOR_BRUSH_RADIUS_DEFAULT;
        this.brush = new ImageBrushStruct();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.mapManager = get(MapManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.imageManager = get(ImageManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        syncPalette();
    }

    // World \\

    public ImageDocumentInstance getWorldImage() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == worldHandle)
            return worldImage;

        String imageName = activeWorld.getWorldName();

        if (!imageManager.hasImage(imageName))
            imageManager.addEditListener(imageName, region -> {
                mapManager.invalidateWorldPixels(
                        activeWorld, region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY());
                worldStreamManager.requestLiveRebuild();
            });

        this.worldHandle = activeWorld;
        this.worldImage = imageManager.openImage(imageName, activeWorld.getWorldFile(), activeWorld.getWorld(), true);
        notifyChanged();

        return worldImage;
    }

    // Palette \\

    private void syncPalette() {

        int biomeRevision = biomeManager.getRevision();

        if (biomeRevision == paletteBiomeRevision)
            return;

        this.paletteBiomeRevision = biomeRevision;
        biomeManager.collectPaintedBiomes(paintedNames, paintedColors);
        palette.clear();

        for (int i = 0; i < paintedNames.size(); i++)
            palette.add(new WorldBiomeEntryStruct(
                    paintedNames.get(i),
                    biomeManager.getDisplayName(biomeManager.getBiomeHandleFromBiomeName(paintedNames.get(i))),
                    paintedColors.getInt(i)));

        palette.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDisplayName(), b.getDisplayName()));

        if (findEntry(selectedBiomeName) == null)
            this.selectedBiomeName = palette.isEmpty() ? null : palette.get(0).getBiomeName();

        notifyPaletteChanged();
    }

    private WorldBiomeEntryStruct findEntry(String biomeName) {

        for (int i = 0; i < palette.size(); i++)
            if (palette.get(i).getBiomeName().equals(biomeName))
                return palette.get(i);

        return null;
    }

    private WorldBiomeEntryStruct findEntryForColor(int mapColor) {

        for (int i = 0; i < palette.size(); i++)
            if (palette.get(i).getMapColor() == mapColor)
                return palette.get(i);

        return null;
    }

    public void selectBiome(String biomeName) {

        if (applyBiomeSelection(biomeName))
            worldBiomeBranch.selectInHierarchy(biomeName);
    }

    // Selects a painted biome as the brush without touching the hierarchy, which may be what chose it
    boolean applyBiomeSelection(String biomeName) {

        if (findEntry(biomeName) == null)
            return false;

        if (!biomeName.equals(selectedBiomeName)) {
            this.selectedBiomeName = biomeName;
            notifyPaletteChanged();
        }

        return true;
    }

    public void selectBiomeAt(WorldHandle biomeWorldHandle, double worldX, double worldZ) {
        worldBiomeBranch.selectBiomeAt(biomeWorldHandle, worldX, worldZ);
    }

    // Tools \\

    public void setTool(WorldEditorTool tool) {

        this.activeTool = tool;
        notifyChanged();
    }

    public void resizeBrush(int step) {

        this.brushRadius = Math.max(
                EditorSetting.WORLD_EDITOR_BRUSH_RADIUS_MIN,
                Math.min(EditorSetting.WORLD_EDITOR_BRUSH_RADIUS_MAX, brushRadius + step));
        notifyChanged();
    }

    public void beginStroke(int pixelX, int pixelY) {

        ImageDocumentInstance image = getWorldImage();

        switch (activeTool) {

            case BRUSH -> {
                if (refreshBrush())
                    imageManager.beginStroke(image, pixelX, pixelY, brush);
            }

            case FILL -> {
                if (refreshBrush())
                    imageManager.fill(image, pixelX, pixelY, brush.getColor());
            }

            case PICK -> pickBiome(image, pixelX, pixelY);
        }
    }

    public void continueStroke(int pixelX, int pixelY) {

        if (activeTool == WorldEditorTool.BRUSH)
            imageManager.continueStroke(getWorldImage(), pixelX, pixelY, brush);
    }

    public void endStroke() {
        imageManager.endStroke(getWorldImage());
    }

    private boolean refreshBrush() {

        WorldBiomeEntryStruct entry = findEntry(selectedBiomeName);

        if (entry == null) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME);
            return false;
        }

        brush.set(toImageColor(entry.getMapColor()), brushRadius);
        return true;
    }

    private void pickBiome(ImageDocumentInstance image, int pixelX, int pixelY) {

        WorldBiomeEntryStruct entry = findEntryForColor(toMapColor(imageManager.pickColor(image, pixelX, pixelY)));

        if (entry == null) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_UNKNOWN_COLOR);
            return;
        }

        selectBiome(entry.getBiomeName());
        setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_PICKED + entry.getDisplayName());
    }

    // World image pixels are RGBA8888 and always opaque; map colors are 0xRRGGBB
    private int toImageColor(int mapColor) {
        return (mapColor << Byte.SIZE) | EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    private int toMapColor(int imageColor) {
        return imageColor >>> Byte.SIZE;
    }

    // History \\

    public void undo() {
        if (!imageManager.undo(getWorldImage()))
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_TO_UNDO);
    }

    public void redo() {
        if (!imageManager.redo(getWorldImage()))
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    // Disk \\

    public void saveWorldImage() {

        ImageDocumentInstance image = getWorldImage();

        setStatusMessage(imageManager.saveImage(image)
                ? EditorSetting.WORLD_EDITOR_MESSAGE_SAVED + image.getImageName()
                : EditorSetting.WORLD_EDITOR_MESSAGE_SAVE_FAILED + image.getImageName());
    }

    public void reloadWorldImage() {

        ImageDocumentInstance image = getWorldImage();

        setStatusMessage(imageManager.reloadImage(image)
                ? EditorSetting.WORLD_EDITOR_MESSAGE_RELOADED + image.getImageName()
                : EditorSetting.WORLD_EDITOR_MESSAGE_RELOAD_FAILED + image.getImageName());
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void notifyPaletteChanged() {
        paletteRevision++;
        notifyChanged();
    }

    void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    public String getStatusText() {

        ImageDocumentInstance image = getWorldImage();
        WorldBiomeEntryStruct entry = findEntry(selectedBiomeName);
        StringBuilder status = new StringBuilder(image.getImageName());

        if (image.isDirty())
            status.append(EditorSetting.WORLD_EDITOR_DIRTY_MARKER);

        status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(activeTool.getLabel())
                .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(EditorSetting.WORLD_EDITOR_STATUS_RADIUS)
                .append(brushRadius)
                .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR)
                .append(entry != null ? entry.getDisplayName() : EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME);

        if (statusMessage != null)
            status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    public String describePixel(int pixelX, int pixelY) {

        WorldBiomeEntryStruct entry = findEntryForColor(
                toMapColor(imageManager.pickColor(getWorldImage(), pixelX, pixelY)));

        return EditorSetting.WORLD_EDITOR_STATUS_X + pixelX
                + EditorSetting.WORLD_EDITOR_STATUS_Y + pixelY
                + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR
                + (entry != null ? entry.getDisplayName() : EditorSetting.WORLD_EDITOR_STATUS_UNPAINTED);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {

        getWorldImage();
        return worldHandle;
    }

    public ObjectArrayList<WorldBiomeEntryStruct> getPalette() {
        return palette;
    }

    public String getSelectedBiomeName() {
        return selectedBiomeName;
    }

    public WorldEditorTool getTool() {
        return activeTool;
    }

    public int getBrushRadius() {
        return brushRadius;
    }

    public int getRevision() {
        return revision;
    }

    public int getPaletteRevision() {
        return paletteRevision;
    }
}
