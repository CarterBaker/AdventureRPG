package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import java.io.File;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.rivermanager.RiverManager;
import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorMode;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldheight.WorldHeightEntryStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorManager extends ManagerPackage implements ToolCanvasProvider {

    /*
     * Owns the world editor's shared state: the active world's two layers,
     * its biome image and its height layer, each opened through ImageManager
     * on the world's own pixels so generation reads every stroke at once, the
     * layer being painted, the palette of biomes the image can paint, the
     * selected biome, the mode, Paint or Settlements, and the status line.
     * The mode follows what is being worked on: selecting a biome or a height
     * paints, selecting a settlement or structure in the Hierarchy or the
     * Settlement Planner places. Every World Editor is one tool canvas, its
     * tool and brush size kept by ToolManager for the layer painted or for
     * placing, and every stroke made with BrushManager's preset in use,
     * dithered on the biome image, whose colors name biomes, and blended on
     * the height layer. The height layer's palette and selected level are
     * kept through WorldHeightBranch, and the placements the Select tool
     * holds through WorldSelectionBranch.
     * Each edited region of either layer is handed to MapManager, which
     * regenerates only the map tiles it reaches, and to WorldStreamManager,
     * which restreams only the preview and Dev terrain it reaches once the
     * edits settle; the palette follows every live biome rebuild. Tools paint
     * the active layer and history undoes it, while saving and reloading take
     * both layers together. Biome edits from the Info Panel reach the engine,
     * and biome selection stays in step with the hierarchy, through
     * WorldBiomeBranch, and tree species edits reach it through
     * WorldTreeBranch. Settlements and structures hand-picked onto the world
     * are kept through WorldPlacementBranch and saved and reloaded with the
     * images; what is placed, by a tool or dropped from the planner, and the
     * placement being edited, is the Settlement Planner's design.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private MapManager mapManager;
    private WorldStreamManager worldStreamManager;
    private RiverManager riverManager;
    private ImageManager imageManager;
    private SettlementPlannerManager settlementPlannerManager;
    private ToolManager toolManager;
    private BrushManager brushManager;
    private WorldBiomeBranch worldBiomeBranch;
    private WorldHeightBranch worldHeightBranch;
    private WorldPlacementBranch worldPlacementBranch;
    private WorldSelectionBranch worldSelectionBranch;

    // World
    private WorldHandle worldHandle;
    private ImageDocumentInstance worldImage;
    private ImageDocumentInstance heightImage;
    private WorldEditorLayer activeLayer;

    // Palette
    private ObjectArrayList<WorldBiomeEntryStruct> palette;
    private ObjectArrayList<String> paintedNames;
    private IntArrayList paintedColors;
    private int paletteBiomeRevision;
    private int paletteRevision;

    // Brush
    private WorldEditorMode activeMode;
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
        this.worldHeightBranch = create(WorldHeightBranch.class);
        create(WorldTreeBranch.class);
        this.worldPlacementBranch = create(WorldPlacementBranch.class);
        this.worldSelectionBranch = create(WorldSelectionBranch.class);

        // World
        this.activeLayer = WorldEditorLayer.BIOMES;

        // Palette
        this.palette = new ObjectArrayList<>();
        this.paintedNames = new ObjectArrayList<>();
        this.paintedColors = new IntArrayList();
        this.paletteBiomeRevision = EngineSetting.INDEX_NOT_FOUND;

        // Brush
        this.activeMode = WorldEditorMode.PAINT;
        this.brush = new ImageBrushStruct();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.mapManager = get(MapManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.riverManager = get(RiverManager.class);
        this.imageManager = get(ImageManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        syncPalette();

        if (worldHandle != null)
            worldSelectionBranch.sync(worldHandle);
    }

    // World \\

    public ImageDocumentInstance getWorldImage() {

        syncWorld();
        return worldImage;
    }

    public ImageDocumentInstance getHeightImage() {

        syncWorld();
        return heightImage;
    }

    // The image the active layer paints
    public ImageDocumentInstance getActiveImage() {
        return activeLayer == WorldEditorLayer.HEIGHTS ? getHeightImage() : getWorldImage();
    }

    private void syncWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == worldHandle)
            return;

        this.worldHandle = activeWorld;
        this.worldImage = openLayerImage(
                activeWorld,
                activeWorld.getWorldName(),
                activeWorld.getWorldFile(),
                activeWorld.getWorld());
        this.heightImage = openLayerImage(
                activeWorld,
                activeWorld.getWorldName() + EngineSetting.WORLD_HEIGHT_LAYER_SUFFIX,
                activeWorld.getHeightFile(),
                activeWorld.getHeightLayer());
        notifyChanged();
    }

    // A layer opened on the world's own pixels, every region edited on it restreamed
    private ImageDocumentInstance openLayerImage(
            WorldHandle layerWorldHandle,
            String imageName,
            File file,
            Pixmap pixmap) {

        if (!imageManager.hasImage(imageName))
            imageManager.addEditListener(imageName, region -> restreamPixels(
                    layerWorldHandle, region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY()));

        return imageManager.openImage(imageName, file, pixmap, true);
    }

    // Every map tile and preview chunk a change to these world pixels can reach, regenerated, and the world drained
    // again so its rivers follow the edit wherever they run
    void restreamPixels(
            WorldHandle editedWorldHandle,
            int minPixelX,
            int minPixelY,
            int maxPixelX,
            int maxPixelY) {

        mapManager.invalidateWorldPixels(editedWorldHandle, minPixelX, minPixelY, maxPixelX, maxPixelY);
        worldStreamManager.requestLiveRebuild(editedWorldHandle, minPixelX, minPixelY, maxPixelX, maxPixelY);
        riverManager.invalidateNetwork(editedWorldHandle);
    }

    // Every map tile and preview chunk of a world regenerated, as after a change that can reach anywhere in it
    public void restreamWorld(WorldHandle editedWorldHandle) {

        Pixmap world = editedWorldHandle.getWorld();

        restreamPixels(editedWorldHandle, 0, 0, world.getWidth() - 1, world.getHeight() - 1);
    }

    // Layers \\

    public void toggleLayer() {
        show(WorldEditorMode.PAINT, activeLayer.toggle());
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

        show(WorldEditorMode.PAINT, WorldEditorLayer.BIOMES);

        if (!biomeName.equals(selectedBiomeName)) {
            this.selectedBiomeName = biomeName;
            notifyPaletteChanged();
        }

        return true;
    }

    public void selectBiomeAt(WorldHandle biomeWorldHandle, double worldX, double worldZ) {
        worldBiomeBranch.selectBiomeAt(biomeWorldHandle, worldX, worldZ);
    }

    public void selectHeightLevel(int level) {

        show(WorldEditorMode.PAINT, WorldEditorLayer.HEIGHTS);

        if (worldHeightBranch.select(level))
            notifyPaletteChanged();
    }

    // Modes \\

    // Placing settlements and structures, as selecting one in the Hierarchy or the Settlement Planner asks for
    public void showPlacements() {
        show(WorldEditorMode.SETTLEMENTS, activeLayer);
    }

    // The single call site of every mode and layer change, ending any stroke under way first
    private void show(WorldEditorMode mode, WorldEditorLayer layer) {

        if (mode == activeMode && layer == activeLayer)
            return;

        endStrokes();

        if (mode != WorldEditorMode.SETTLEMENTS)
            worldSelectionBranch.clear();

        boolean layerChanged = layer != activeLayer;

        this.activeMode = mode;
        this.activeLayer = layer;

        if (layerChanged)
            notifyPaletteChanged();
        else
            notifyChanged();
    }

    // Tools \\

    // What every World Editor works on: the layer painted, or the placements
    @Override
    public ToolCanvas getToolCanvas() {
        return activeMode == WorldEditorMode.SETTLEMENTS ? ToolCanvas.SETTLEMENTS : activeLayer.getToolCanvas();
    }

    public void beginStroke(int pixelX, int pixelY) {

        ImageDocumentInstance image = getActiveImage();

        switch (getTool()) {

            case BRUSH -> {
                if (hasPaintColor() && refreshBrush(resolvePaintColor()))
                    imageManager.beginStroke(image, pixelX, pixelY, brush);
            }

            case ERASE -> {
                if (refreshBrush(WorldElevationUtility.toLayerColor(EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL)))
                    imageManager.beginStroke(image, pixelX, pixelY, brush);
            }

            case FILL -> {
                if (hasPaintColor())
                    imageManager.fill(image, pixelX, pixelY, resolvePaintColor());
            }

            case PICK -> {
                if (activeLayer == WorldEditorLayer.HEIGHTS)
                    pickHeight(image, pixelX, pixelY);
                else
                    pickBiome(image, pixelX, pixelY);
            }

            default -> {
            }
        }
    }

    public void continueStroke(int pixelX, int pixelY) {
        if (getTool().isStroke())
            imageManager.continueStroke(getActiveImage(), pixelX, pixelY, brush);
    }

    public void endStroke() {
        imageManager.endStroke(getActiveImage());
    }

    private void endStrokes() {
        imageManager.endStroke(getWorldImage());
        imageManager.endStroke(getHeightImage());
    }

    // The preset in use made the brush with a color to lay down; false when the preset's mode is refused here
    private boolean refreshBrush(int color) {

        ToolCanvas canvas = getToolCanvas();
        BrushHandle preset = brushManager.getActiveBrush();

        if (!canvas.supports(preset.getMode())) {
            setStatusMessage(preset.getMode().getLabel() + EditorSetting.TOOL_MESSAGE_MODE_REFUSED);
            return false;
        }

        brush.set(preset, color, getBrushRadius(), canvas.isBlending());
        return true;
    }

    // True when the active layer has a value to paint; the biome image needs a biome selected
    private boolean hasPaintColor() {

        if (activeLayer == WorldEditorLayer.HEIGHTS || findEntry(selectedBiomeName) != null)
            return true;

        setStatusMessage(EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME);
        return false;
    }

    // The selected level on the height layer, the selected biome's color on the biome image
    private int resolvePaintColor() {

        if (activeLayer == WorldEditorLayer.HEIGHTS)
            return WorldElevationUtility.toLayerColor(worldHeightBranch.getSelectedLevel());

        return toImageColor(findEntry(selectedBiomeName).getMapColor());
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

    private void pickHeight(ImageDocumentInstance image, int pixelX, int pixelY) {

        int level = WorldElevationUtility.toLevel(imageManager.pickColor(image, pixelX, pixelY));

        selectHeightLevel(level);
        setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_PICKED + worldHeightBranch.describeLevel(level));
    }

    // World image pixels are RGBA8888 and always opaque; map colors are 0xRRGGBB
    public int toImageColor(int mapColor) {
        return (mapColor << Byte.SIZE) | EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    private int toMapColor(int imageColor) {
        return imageColor >>> Byte.SIZE;
    }

    // Placement \\

    // A placing tool's click at an image position, in image pixels with fractions kept
    public void applyPlacement(double imageX, double imageY) {

        WorldHandle world = getWorldHandle();

        switch (getTool()) {
            case PLACE -> worldPlacementBranch.place(world, imageX, imageY);
            case ERASE -> worldPlacementBranch.clear(world, imageX, imageY);
            default -> {
            }
        }
    }

    // The Settlement Planner's design stood at an image position, as the Place tool or a drop from the planner does
    public void placeDesign(double imageX, double imageY) {
        worldPlacementBranch.place(getWorldHandle(), imageX, imageY);
    }

    // The Settlement Planner's live change to the placement it edits, returning why it was refused or null
    public String replacePlacement(WorldPlacementStruct previous, WorldPlacementStruct replacement) {
        return worldPlacementBranch.replace(getWorldHandle(), previous, replacement);
    }

    // One exact placement taken away, returning why it was not or null once it is gone
    public String removePlacement(WorldPlacementStruct placement) {
        return worldPlacementBranch.remove(getWorldHandle(), placement);
    }

    // Every hand placement standing in one world image pixel, in the order they were placed
    public void collectPlacementsInPixel(int pixelX, int pixelY, ObjectArrayList<WorldPlacementStruct> output) {
        worldPlacementBranch.collectInPixel(getWorldHandle(), pixelX, pixelY, output);
    }

    // The block column under an image position, in image pixels with fractions kept
    public long toBlock(double imagePosition) {
        return worldPlacementBranch.toBlock(imagePosition);
    }

    // The world image pixel a placement stands in
    public int toPixelX(WorldPlacementStruct placement) {
        return worldPlacementBranch.toPixelX(getWorldHandle(), placement);
    }

    public int toPixelY(WorldPlacementStruct placement) {
        return worldPlacementBranch.toPixelY(getWorldHandle(), placement);
    }

    public WorldPlacementStruct[] getPlacements() {
        return worldManager.getPlacements(getWorldHandle());
    }

    // Selection \\

    // The placement nearest an image position within a reach in image pixels, null when none stands that near
    public WorldPlacementStruct findPlacementNear(double imageX, double imageY, double reachPixels) {
        return worldSelectionBranch.findNear(getWorldHandle(), imageX, imageY, reachPixels);
    }

    public void clickSelection(WorldPlacementStruct placement, boolean add, boolean subtract) {
        worldSelectionBranch.click(placement, add, subtract);
    }

    public void boxSelection(
            double minImageX,
            double minImageY,
            double maxImageX,
            double maxImageY,
            boolean add,
            boolean subtract) {
        worldSelectionBranch.box(getWorldHandle(), minImageX, minImageY, maxImageX, maxImageY, add, subtract);
    }

    public void moveSelection(double deltaImageX, double deltaImageY) {
        worldSelectionBranch.move(getWorldHandle(), toBlock(deltaImageX), toBlock(deltaImageY));
    }

    // Everything selected taken away, only while placing
    public void deleteSelection() {
        if (activeMode == WorldEditorMode.SETTLEMENTS)
            worldSelectionBranch.delete(getWorldHandle());
    }

    public boolean isPlacementSelected(WorldPlacementStruct placement) {
        return worldSelectionBranch.isSelected(placement);
    }

    // History \\

    public void undo() {
        if (!imageManager.undo(getActiveImage()))
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_TO_UNDO);
    }

    public void redo() {
        if (!imageManager.redo(getActiveImage()))
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    // Disk \\

    public void saveWorldImage() {

        ImageDocumentInstance image = getWorldImage();
        ImageDocumentInstance height = getHeightImage();

        if (!imageManager.saveImage(image)) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_SAVE_FAILED + image.getImageName());
            return;
        }

        if (!imageManager.saveImage(height)) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_SAVE_FAILED + height.getImageName());
            return;
        }

        setStatusMessage(worldPlacementBranch.save(worldHandle)
                ? EditorSetting.WORLD_EDITOR_MESSAGE_SAVED + image.getImageName()
                : worldPlacementBranch.getSaveRefusal());
    }

    // A height layer never saved has no file to reload from, so it keeps what is painted on it
    public void reloadWorldImage() {

        ImageDocumentInstance image = getWorldImage();
        ImageDocumentInstance height = getHeightImage();

        if (!imageManager.reloadImage(image)) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_RELOAD_FAILED + image.getImageName());
            return;
        }

        if (height.getFile().exists() && !imageManager.reloadImage(height)) {
            setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_RELOAD_FAILED + height.getImageName());
            return;
        }

        setStatusMessage(worldPlacementBranch.reload(worldHandle)
                ? EditorSetting.WORLD_EDITOR_MESSAGE_RELOADED + image.getImageName()
                : EditorSetting.WORLD_EDITOR_MESSAGE_PLACEMENTS_RELOAD_FAILED + image.getImageName());
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
        StringBuilder status = new StringBuilder(image.getImageName());

        if (image.isDirty() || getHeightImage().isDirty())
            status.append(EditorSetting.WORLD_EDITOR_DIRTY_MARKER);

        status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(activeMode.getLabel());

        if (activeMode == WorldEditorMode.SETTLEMENTS)
            appendPlacementStatus(status);
        else
            status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(activeLayer.getLabel())
                    .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(getTool().getLabel())
                    .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR)
                    .append(brushManager.getActiveBrush().getDisplayName())
                    .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR)
                    .append(EditorSetting.TOOL_STATUS_SIZE).append(getBrushRadius())
                    .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR)
                    .append(describeSelection());

        if (statusMessage != null)
            status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    private String describeSelection() {

        if (activeLayer == WorldEditorLayer.HEIGHTS)
            return worldHeightBranch.describeLevel(worldHeightBranch.getSelectedLevel());

        WorldBiomeEntryStruct entry = findEntry(selectedBiomeName);

        return entry != null ? entry.getDisplayName() : EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME;
    }

    private void appendPlacementStatus(StringBuilder status) {
        status.append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(getTool().getLabel())
                .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR).append(settlementPlannerManager.describeDesign());
    }

    public String describePixel(int pixelX, int pixelY) {

        WorldBiomeEntryStruct entry = findEntryForColor(
                toMapColor(imageManager.pickColor(getWorldImage(), pixelX, pixelY)));

        int level = WorldElevationUtility.toLevel(imageManager.pickColor(getHeightImage(), pixelX, pixelY));

        return EditorSetting.WORLD_EDITOR_STATUS_X + pixelX
                + EditorSetting.WORLD_EDITOR_STATUS_Y + pixelY
                + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR
                + (entry != null ? entry.getDisplayName() : EditorSetting.WORLD_EDITOR_STATUS_UNPAINTED)
                + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR
                + worldHeightBranch.describeLevel(level);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {

        syncWorld();
        return worldHandle;
    }

    public WorldEditorLayer getLayer() {
        return activeLayer;
    }

    public ObjectArrayList<WorldHeightEntryStruct> getHeightPalette() {
        return worldHeightBranch.getPalette();
    }

    public int getSelectedHeightLevel() {
        return worldHeightBranch.getSelectedLevel();
    }

    public ObjectArrayList<WorldBiomeEntryStruct> getPalette() {
        return palette;
    }

    public String getSelectedBiomeName() {
        return selectedBiomeName;
    }

    // The 0xRRGGBB color a painted biome is painted in, -1 for a biome the image cannot paint
    public int getBiomeMapColor(String biomeName) {

        WorldBiomeEntryStruct entry = findEntry(biomeName);

        return entry != null ? entry.getMapColor() : EngineSetting.INDEX_NOT_FOUND;
    }

    public String getBiomeDisplayName(String biomeName) {

        WorldBiomeEntryStruct entry = findEntry(biomeName);

        return entry != null ? entry.getDisplayName() : EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME;
    }

    public WorldEditorMode getMode() {
        return activeMode;
    }

    // The tool chosen for what every World Editor works on
    public EditorTool getTool() {
        return toolManager.getTool(getToolCanvas());
    }

    // The radius a stroke stamps, in image pixels
    public int getBrushRadius() {
        return Math.round(toolManager.getSize(getToolCanvas()));
    }

    public int getRevision() {
        return revision;
    }

    public int getPaletteRevision() {
        return paletteRevision;
    }
}
