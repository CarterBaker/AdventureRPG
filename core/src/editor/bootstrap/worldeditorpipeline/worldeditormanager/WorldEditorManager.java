package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import java.io.File;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.rivermanager.RiverManager;
import application.bootstrap.worldpipeline.util.WorldElevationUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.cameraplannerpipeline.cameraplannermanager.CameraPlannerManager;
import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageStroke;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorMode;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import editor.bootstrap.worldeditorpipeline.worldbiome.WorldBiomeEntryStruct;
import editor.bootstrap.worldeditorpipeline.worldheight.WorldHeightEntryStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorManager extends ManagerPackage {

    /*
     * Owns the world editor's shared state: the active world's two layers,
     * its biome image and its height layer, each opened through ImageManager
     * on the world's own pixels so generation reads every stroke at once, the
     * layer being painted, the palette of biomes the image can paint, the
     * selected biome, the mode, Paint, Settlements or Cameras, and the status
     * line. The mode follows what is being worked on: selecting a biome or a
     * height paints, selecting a settlement or structure in the Hierarchy or
     * the Settlement Planner places those, and working in the Camera Planner
     * places cameras. Every World Editor works on one tool canvas, its tool
     * and options kept by ToolManager for the layer painted or for the mode
     * placing, and every stroke is made with BrushManager's brush in use and
     * the tool's own strength and twist, dithered on the biome image, whose
     * colors name biomes, and blended on the height layer. The height
     * layer's palette and selected level are kept through WorldHeightBranch,
     * and the placements the Select tool holds through WorldSelectionBranch.
     * Each edited region of either layer is handed to MapManager, which
     * regenerates only the map tiles it reaches, and to WorldStreamManager,
     * which restreams only the preview and Dev terrain it reaches once the
     * edits settle; the palette follows every live biome rebuild. Tools paint
     * the active layer and history undoes it, while saving and reloading take
     * both layers together. Biome edits from the Info Panel reach the engine,
     * and biome selection stays in step with the hierarchy, through
     * WorldBiomeBranch, and tree species edits reach it through
     * WorldTreeBranch. Settlements, structures and cameras hand-picked onto
     * the world are kept through WorldPlacementBranch and saved and reloaded
     * with the images; what is placed, by a tool or dropped from a planner,
     * is the design of the planner the mode places for, and each placement
     * picked is edited by the planner that designs its kind.
     */

    // Internal
    private WorldManager worldManager;
    private BiomeManager biomeManager;
    private MapManager mapManager;
    private WorldStreamManager worldStreamManager;
    private RiverManager riverManager;
    private ImageManager imageManager;
    private SettlementPlannerManager settlementPlannerManager;
    private CameraPlannerManager cameraPlannerManager;
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

    // Tools — what each tool strokes onto a layer, by the tool's ordinal; null for a tool that does not stroke
    private ImageStroke[] tool2Stroke;

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

        // Tools
        this.tool2Stroke = new ImageStroke[EditorTool.VALUES.length];
        this.tool2Stroke[EditorTool.BRUSH.ordinal()] = ImageStroke.PAINT;
        this.tool2Stroke[EditorTool.ERASE.ordinal()] = ImageStroke.PAINT;
        this.tool2Stroke[EditorTool.SMOOTH.ordinal()] = ImageStroke.SMOOTH;
        this.tool2Stroke[EditorTool.SWIRL.ordinal()] = ImageStroke.SWIRL;
        this.tool2Stroke[EditorTool.SCATTER.ordinal()] = ImageStroke.SCATTER;
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
        this.cameraPlannerManager = get(CameraPlannerManager.class);
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

    // Placing cameras, as working in the Camera Planner asks for
    public void showCameras() {
        show(WorldEditorMode.CAMERAS, activeLayer);
    }

    // The single call site of every mode and layer change, ending any stroke under way first; a selection belongs
    // to the mode it was made in
    private void show(WorldEditorMode mode, WorldEditorLayer layer) {

        if (mode == activeMode && layer == activeLayer)
            return;

        endStrokes();

        if (mode != activeMode)
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

    // What every World Editor works on: the layer painted, or the placements the mode places
    public ToolCanvas getToolCanvas() {
        return activeMode.isPlacing() ? activeMode.getToolCanvas() : activeLayer.getToolCanvas();
    }

    public void beginStroke(int pixelX, int pixelY) {

        ImageDocumentInstance image = getActiveImage();

        EditorTool tool = getTool();

        switch (tool) {

            case FILL -> {
                if (hasPaintColor(tool))
                    imageManager.fill(image, pixelX, pixelY, resolvePaintColor(tool));
            }

            case PICK -> {
                if (activeLayer == WorldEditorLayer.HEIGHTS)
                    pickHeight(image, pixelX, pixelY);
                else
                    pickBiome(image, pixelX, pixelY);
            }

            default -> beginBrush(image, pixelX, pixelY, tool);
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

    // A stroke of the tool's own kind begun with the brush in use and the tool's options; only painting takes a color
    private void beginBrush(ImageDocumentInstance image, int pixelX, int pixelY, EditorTool tool) {

        ImageStroke stroke = tool2Stroke[tool.ordinal()];

        if (stroke == null)
            return;

        if (stroke == ImageStroke.PAINT) {

            if (!hasPaintColor(tool))
                return;

            brush.setColor(resolvePaintColor(tool));
        }

        ToolCanvas canvas = getToolCanvas();

        brush.set(
                brushManager.getActiveBrush(),
                stroke,
                getBrushRadius(),
                toolManager.getOption(canvas, ToolOption.STRENGTH),
                toolManager.getOption(canvas, ToolOption.TWIST),
                canvas.isBlending());
        imageManager.beginStroke(image, pixelX, pixelY, brush);
    }

    // True when the tool has a value to lay down: Erase always does, the biome image needs a biome selected
    private boolean hasPaintColor(EditorTool tool) {

        if (tool == EditorTool.ERASE || activeLayer == WorldEditorLayer.HEIGHTS || findEntry(selectedBiomeName) != null)
            return true;

        setStatusMessage(EditorSetting.WORLD_EDITOR_STATUS_NO_BIOME);
        return false;
    }

    // Erase lays the neutral level down; anything else the selected level, or on the biome image the selected biome
    private int resolvePaintColor(EditorTool tool) {

        if (tool == EditorTool.ERASE)
            return WorldElevationUtility.toLayerColor(EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL);

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

    // The placing mode's design stood at an image position, as the Place tool or a drop from a planner does
    public void placeDesign(double imageX, double imageY) {
        worldPlacementBranch.place(getWorldHandle(), imageX, imageY);
    }

    // The design of the planner the mode places for, standing at a block column, null when nothing is chosen
    WorldPlacementStruct createDesign(long worldX, long worldZ) {
        return activeMode == WorldEditorMode.CAMERAS
                ? cameraPlannerManager.createNewPlacement(worldX, worldZ)
                : settlementPlannerManager.createPlacement(worldX, worldZ);
    }

    // A placement handed to the planner that designs its kind, edited there from then on
    void editPlacement(WorldPlacementStruct placement) {
        if (placement.getKind() == WorldPlacementKind.CAMERA)
            cameraPlannerManager.editPlacement(placement);
        else
            settlementPlannerManager.editPlacement(placement);
    }

    // The placement edited by the planner the mode places for let go
    void detachPlacement() {
        if (activeMode == WorldEditorMode.CAMERAS)
            cameraPlannerManager.detach();
        else
            settlementPlannerManager.detach();
    }

    // True for the placement either planner edits
    public boolean isPlacementEdited(WorldPlacementStruct placement) {
        return settlementPlannerManager.isEdited(placement) || cameraPlannerManager.isEdited(placement);
    }

    // A planner's live change to the placement it edits, returning why it was refused or null
    public String replacePlacement(WorldPlacementStruct previous, WorldPlacementStruct replacement) {
        return worldPlacementBranch.replace(getWorldHandle(), previous, replacement);
    }

    // One exact placement taken away, returning why it was not or null once it is gone
    public String removePlacement(WorldPlacementStruct placement) {
        return worldPlacementBranch.remove(getWorldHandle(), placement);
    }

    // Every hand placement a mode holds standing in one world image pixel, in the order they were placed
    public void collectPlacementsInPixel(
            WorldEditorMode mode,
            int pixelX,
            int pixelY,
            ObjectArrayList<WorldPlacementStruct> output) {
        worldPlacementBranch.collectInPixel(getWorldHandle(), mode, pixelX, pixelY, output);
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

    // The placement the mode holds nearest an image position within a reach in image pixels, null when none is near
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
        if (activeMode.isPlacing())
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

        if (activeMode.isPlacing())
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
                .append(EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR)
                .append(activeMode == WorldEditorMode.CAMERAS
                        ? cameraPlannerManager.describeDesign()
                        : settlementPlannerManager.describeDesign());
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
        return Math.round(toolManager.getOption(getToolCanvas(), ToolOption.SIZE));
    }

    public int getRevision() {
        return revision;
    }

    public int getPaletteRevision() {
        return paletteRevision;
    }
}
