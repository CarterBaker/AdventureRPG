package editor.runtime.architecturebuilder;

import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.architecturebuilder.menu.ArchitectureBuilderMenuSystem;
import editor.runtime.architecturebuilder.render.ArchitectureBuilderRenderSystem;
import editor.runtime.architecturebuilder.view.ArchitectureBuilderViewSystem;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureBuilderContext extends ContextPackage
        implements ViewCameraProvider, LayersProvider, PaletteRowSource, PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for building architectures, the style blueprints every
     * settlement's structures are built in: the blocks each block slot is
     * laid with (Siding Wall, Roof Cover, Floor Boards...) and the furniture
     * each furnishing table stands as (Beds Parents, Beds Kids Bunk...). The
     * slots are its layers in the Layers tab, grouped as structures name
     * them; the one picked is shown in the viewport, a block slot as a wall
     * of its block and a furniture slot as its piece, and a group or a kind
     * as every slot it holds side by side. The Palette tab lists every
     * architecture to work on, then what the slot picked may be filled with,
     * a row chosen filling it; the Properties tab holds the architecture's
     * whole entry, and the File menu makes, saves and reverts architectures.
     * All state lives in ArchitectureBuilderManager; this tab only shows it.
     */

    // Internal
    private ArchitectureBuilderManager architectureBuilderManager;
    private EditorInputSystem editorInputSystem;
    private ArchitectureBuilderViewSystem architectureBuilderViewSystem;
    private ArchitectureBuilderRenderSystem architectureBuilderRenderSystem;
    private ArchitectureBuilderMenuSystem architectureBuilderMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.architectureBuilderViewSystem = create(ArchitectureBuilderViewSystem.class);
        create(ViewCameraSystem.class);
        this.architectureBuilderRenderSystem = create(ArchitectureBuilderRenderSystem.class);
        this.architectureBuilderMenuSystem = create(ArchitectureBuilderMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.architectureBuilderManager = get(ArchitectureBuilderManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return architectureBuilderViewSystem;
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return architectureBuilderManager.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return ArchitectureBuilderSetting.LAYER_TITLE;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        architectureBuilderManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        architectureBuilderManager.selectLayer(layerKey);
    }

    // No slot can be hidden
    @Override
    public void toggleLayerVisible(String layerKey) {
    }

    // Slots come from the structures that name them, so the stack takes no new layers
    @Override
    public String getAddLayerKey() {
        return "";
    }

    @Override
    public String getRemoveLayerKey() {
        return "";
    }

    // Palette \\

    // Every architecture, then what the slot picked may be filled with
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return architectureBuilderManager.getRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        architectureBuilderManager.buildPaletteRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return ArchitectureBuilderSetting.ROW_TEMPLATES;
    }

    // The palette holds no numbers to step
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // Properties \\

    // The architecture's whole entry, selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ARCHITECTURES;
    }
}
