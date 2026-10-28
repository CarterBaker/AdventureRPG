package editor.bootstrap.biomeplannerpipeline.biomeplannermanager;

import java.util.Arrays;
import java.util.Objects;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class BiomePlannerManager extends ManagerPackage {

    /*
     * Owns the biome planner's grid: a square of cells, each standing for a
     * patch of world, whose centre is always the selected biome, the one the
     * Hierarchy, the Palette and the World Editor share. A cell painted with
     * a biome keeps it; every other cell follows the centre, so a fresh grid
     * is the selected biome alone, filling the world to show how it spawns,
     * and painting the cells around it shows how it meets its neighbours.
     * Whenever the selection, a painted cell or any biome changes, the grid
     * is laid again on its world through BiomeWorldBranch, which the Biome
     * Viewer flies over; a painted cell whose biome is gone follows the
     * centre again.
     */

    // Internal
    private WorldEditorManager worldEditorManager;
    private BiomeManager biomeManager;
    private BiomeWorldBranch biomeWorldBranch;

    // Grid — each cell's painted biome, null for a cell that follows the centre
    private String[] cellBiomeNames;
    private int[] cellColors;
    private String shownCentreName;
    private int shownBiomeRevision;
    private int shownPaletteRevision;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        int cells = EditorSetting.BIOME_PLANNER_GRID_SIDE * EditorSetting.BIOME_PLANNER_GRID_SIDE;

        // Internal
        this.biomeWorldBranch = create(BiomeWorldBranch.class);

        // Grid
        this.cellBiomeNames = new String[cells];
        this.cellColors = new int[cells];
        this.shownBiomeRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownPaletteRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.worldEditorManager = get(WorldEditorManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        String centreName = worldEditorManager.getSelectedBiomeName();
        int biomeRevision = biomeManager.getRevision();
        int paletteRevision = worldEditorManager.getPaletteRevision();

        if (Objects.equals(centreName, shownCentreName) && biomeRevision == shownBiomeRevision
                && paletteRevision == shownPaletteRevision)
            return;

        this.shownCentreName = centreName;
        this.shownBiomeRevision = biomeRevision;
        this.shownPaletteRevision = paletteRevision;
        layGrid();
    }

    // Grid \\

    // The selected biome painted into a cell; the centre is always the selected biome
    public void paintCell(int cell) {

        if (cell < 0 || cell >= cellBiomeNames.length)
            return;

        if (cell == getCentreCell()) {
            setStatusMessage(EditorSetting.BIOME_PLANNER_MESSAGE_CENTRE);
            return;
        }

        cellBiomeNames[cell] = worldEditorManager.getSelectedBiomeName();
        setStatusMessage(EditorSetting.BIOME_PLANNER_MESSAGE_PAINTED
                + worldEditorManager.getBiomeDisplayName(cellBiomeNames[cell]));
        layGrid();
    }

    // Every cell following the centre again, the selected biome filling the world
    public void resetGrid() {

        Arrays.fill(cellBiomeNames, null);
        setStatusMessage(EditorSetting.BIOME_PLANNER_MESSAGE_RESET);
        layGrid();
    }

    // Each cell's biome resolved to its color, a painted biome that can no longer be painted following the centre
    private void layGrid() {

        for (int cell = 0; cell < cellBiomeNames.length; cell++) {

            if (cellBiomeNames[cell] != null && worldEditorManager.getBiomeMapColor(cellBiomeNames[cell])
                    == EngineSetting.INDEX_NOT_FOUND)
                cellBiomeNames[cell] = null;

            int mapColor = worldEditorManager.getBiomeMapColor(getCellBiomeName(cell));

            cellColors[cell] = mapColor != EngineSetting.INDEX_NOT_FOUND
                    ? worldEditorManager.toImageColor(mapColor)
                    : EditorSetting.BIOME_PLANNER_EMPTY_COLOR;
        }

        biomeWorldBranch.paint(cellColors);
        notifyChanged();
    }

    // Biomes \\

    // The selected biome opened in the Hierarchy and Info Panel, so their biome tools work on it; false with none
    public boolean focusCentre() {

        String centreName = worldEditorManager.getSelectedBiomeName();

        if (centreName == null)
            return false;

        worldEditorManager.selectBiome(centreName);
        return true;
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    // Accessible \\

    // The biome a cell stands for: its own when painted, the selected biome otherwise
    public String getCellBiomeName(int cell) {
        return cellBiomeNames[cell] != null ? cellBiomeNames[cell] : worldEditorManager.getSelectedBiomeName();
    }

    public boolean isCellPainted(int cell) {
        return cellBiomeNames[cell] != null;
    }

    public int getCellMapColor(int cell) {
        return worldEditorManager.getBiomeMapColor(getCellBiomeName(cell));
    }

    public int getCellCount() {
        return cellBiomeNames.length;
    }

    public int getCentreCell() {
        return cellBiomeNames.length / 2;
    }

    // The planner's world, made the first time it is asked for
    public WorldHandle getWorldHandle() {
        return biomeWorldBranch.getWorldHandle();
    }

    public String getStatusText() {

        String centre = EditorSetting.BIOME_PLANNER_STATUS_CENTRE
                + worldEditorManager.getBiomeDisplayName(worldEditorManager.getSelectedBiomeName());

        return statusMessage != null ? centre + EditorSetting.WORLD_EDITOR_STATUS_SEPARATOR + statusMessage : centre;
    }

    public int getRevision() {
        return revision;
    }
}
