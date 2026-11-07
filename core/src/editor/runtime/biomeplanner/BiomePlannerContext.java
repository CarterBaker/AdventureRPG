package editor.runtime.biomeplanner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.biomeplanner.menu.BiomePlannerMenuSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class BiomePlannerContext extends ContextPackage implements PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for making biomes, the way the Settlement Planner is for
     * settlements: every biome the world can paint, the selected one
     * highlighted, and a grid of cells around it whose centre is always the
     * selected biome. Clicking a cell paints the selected biome into it, so
     * a biome can be set beside the ones it is meant to meet; the Biome
     * Viewer flies over the world the grid lays out. New and Duplicate make
     * biomes through the Info Panel, where every field of one is edited.
     */

    // Internal
    private BiomePlannerMenuSystem biomePlannerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.biomePlannerMenuSystem = create(BiomePlannerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_BIOME_PLANNER;
    }
}
