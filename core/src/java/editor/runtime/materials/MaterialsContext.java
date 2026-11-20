package editor.runtime.materials;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.materials.panel.MaterialsPanelSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialsContext extends ContextPackage
        implements PropertiesProvider, CharacterViewRowSource, FileMenuProvider {

    /*
     * The Materials tab, the editor's one list of materials: the material of
     * the workspace open, then every texture of every baked pack, pack by
     * pack, the one chosen marked and described. A material pressed is
     * chosen, the one the Hair tab's new strands grow in, its Brush paints,
     * the Paint tab's Material tool lays and the Material Viewer shows;
     * carried out of the tab and let go over an Item Editor, it colours the
     * part under the pointer. While it was worked in last the Properties tab
     * holds the chosen material's maps and parameters and the File menu makes,
     * saves, exports and imports materials of the workspace. All state lives
     * in MaterialsManager; this tab only shows it and hands presses and drags
     * to MaterialsBranch. The shared tool keys work while it is focused.
     */

    // Internal
    private MaterialsManager materialsManager;
    private MaterialsPanelSystem materialsPanelSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.materialsPanelSystem = create(MaterialsPanelSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.materialsManager = get(MaterialsManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // The Properties tab holds the chosen material's maps and parameters
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return materialsManager.getRevision() + materialsManager.getValueRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.MATERIALS_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        materialsManager.buildRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.MATERIALS_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        materialsManager.stepRow(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_MATERIALS;
    }
}
