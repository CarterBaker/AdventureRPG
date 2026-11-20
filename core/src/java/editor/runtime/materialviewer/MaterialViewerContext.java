package editor.runtime.materialviewer;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.materialviewer.frame.MaterialViewerFrameSystem;
import editor.runtime.materialviewer.light.MaterialViewerLightSystem;
import editor.runtime.materialviewer.menu.MaterialViewerMenuSystem;
import editor.runtime.materialviewer.render.MaterialViewerRenderSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialViewerContext extends ContextPackage
        implements ViewCameraProvider, ToolCanvasProvider, PropertiesProvider, CharacterViewRowSource, FileMenuProvider {

    /*
     * Editor tab showing the material chosen in the Materials tab on a shape,
     * as a material editor's preview does: a sphere, a cube, a cylinder or a
     * plane, picked by the Tools tab's Shape option, every map read from the
     * texture it is taken from and shaped by the material's parameters, lit
     * by one light the Light tool turns when dragged from the shape. The
     * orbit button turns the view, the pan button slides it and scrolling
     * zooms. While it was worked in last the Properties tab holds the
     * material's maps and parameters and the File menu makes, saves, exports and
     * imports materials of the workspace. All state lives in MaterialsManager;
     * this tab only shows it.
     */

    // Internal
    private MaterialsManager materialsManager;
    private EditorInputSystem editorInputSystem;
    private MaterialViewerFrameSystem materialViewerFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private MaterialViewerLightSystem materialViewerLightSystem;
    private MaterialViewerRenderSystem materialViewerRenderSystem;
    private MaterialViewerMenuSystem materialViewerMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.materialViewerFrameSystem = create(MaterialViewerFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.materialViewerLightSystem = create(MaterialViewerLightSystem.class);
        this.materialViewerRenderSystem = create(MaterialViewerRenderSystem.class);
        this.materialViewerMenuSystem = create(MaterialViewerMenuSystem.class);
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

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return materialViewerFrameSystem;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.MATERIAL_VIEWER;
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
