package editor.runtime.properties;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewStepTarget;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.properties.panel.BiomePropertiesSystem;
import editor.runtime.properties.panel.InfoPropertiesSystem;
import editor.runtime.properties.panel.PropertiesSystem;
import editor.runtime.properties.panel.RowPropertiesSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class PropertiesContext extends ContextPackage implements CharacterViewStepTarget, FileMenuProvider {

    /*
     * The editor's one Properties tab, shared by every window: it shows what
     * the window worked in last provides — a tool window's rows, what it
     * picks from listed first and each number of what is picked on a slider
     * typed into its box; for the Hierarchy and every content tool the entry
     * selected there with every field to edit, the editor's Info Panel; or
     * for the World Editor's Biomes layer the biome palette over that entry.
     * Its toolbar and status line work on the entry selected in the
     * Hierarchy, and its File and Edit menus on the window whose rows it
     * shows. Every edit goes through the manager that owns it, so every
     * window shows it at once. The shared tool keys work while this tab is
     * focused.
     */

    // Internal
    private PropertiesManager propertiesManager;
    private PropertiesSystem propertiesSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private RowPropertiesSystem rowPropertiesSystem;
    private InfoPropertiesSystem infoPropertiesSystem;
    private BiomePropertiesSystem biomePropertiesSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.propertiesSystem = create(PropertiesSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.rowPropertiesSystem = create(RowPropertiesSystem.class);
        this.infoPropertiesSystem = create(InfoPropertiesSystem.class);
        this.biomePropertiesSystem = create(BiomePropertiesSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.propertiesManager = get(PropertiesManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public void stepRow(String key, int steps) {
        rowPropertiesSystem.stepRow(key, steps);
    }

    // File Menu \\

    // The File and Edit menus of the window whose rows are shown, the Info Panel's otherwise
    @Override
    public FileMenuTableStruct getFileMenuTable() {

        PropertiesProvider provider = propertiesManager.getProvider();

        return provider instanceof FileMenuProvider fileMenuProvider
                && provider.getPropertiesContent() == PropertiesContent.ROWS
                ? fileMenuProvider.getFileMenuTable()
                : EditorSetting.FILE_MENU_INFO;
    }
}
