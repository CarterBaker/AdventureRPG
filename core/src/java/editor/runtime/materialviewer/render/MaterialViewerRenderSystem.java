package editor.runtime.materialviewer.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.materialviewer.MaterialViewerSetting;
import editor.runtime.materialviewer.light.MaterialViewerLightSystem;
import editor.runtime.materialviewer.util.MaterialShapeUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MaterialViewerRenderSystem extends SystemPackage {

    /*
     * Draws the material laid onto the shape chosen by the Shape option into
     * this window's scene target, with the viewer's material, which reads
     * each map from the image the brush lays it from, as MaterialsManager
     * hands it on as an editor texture, and lights it by the Light tool's
     * light, as an editor viewport has no lighting pass. Every shape is built
     * once, when the window opens, and the model only takes another's mesh
     * when the shape changes; a map's texture is handed on afresh only when
     * it changes, and the parameters every frame through the same vectors, so
     * nothing is made per frame. Nothing is drawn until every map's texture
     * is up.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private VAOManager vaoManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private ToolManager toolManager;
    private MaterialsManager materialsManager;
    private MaterialViewerLightSystem materialViewerLightSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Shapes — one mesh per shape, in TOOL_SHAPE_NAMES order, and the one the model draws
    private MeshInstance[] shapeMeshes;
    private ModelInstance model;
    private int shownShape;

    // Uniforms — each map's texture as last handed on, kept boxed so it is boxed only when it changes
    private int[] mapTextures;
    private Integer[] mapTextureValues;
    private Vector3 baseColor;
    private Vector4 surface;

    // Base \\

    @Override
    protected void create() {

        // Shapes
        this.shapeMeshes = new MeshInstance[EditorSetting.TOOL_SHAPE_NAMES.length];

        // Uniforms
        this.mapTextures = new int[MaterialViewerSetting.SHOWN_MAPS.length];
        this.mapTextureValues = new Integer[MaterialViewerSetting.SHOWN_MAPS.length];
        this.baseColor = new Vector3();
        this.surface = new Vector4();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.vaoManager = get(VAOManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.toolManager = get(ToolManager.class);
        this.materialsManager = get(MaterialsManager.class);
        this.materialViewerLightSystem = get(MaterialViewerLightSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.PAINTED_ITEM_VAO);
        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();

        for (int shape = 0; shape < shapeMeshes.length; shape++) {
            MaterialShapeUtility.build(shape, vertices, indices);
            shapeMeshes[shape] = meshManager.createMesh(vaoTemplate, vertices, indices);
        }

        this.shownShape = MaterialViewerSetting.SHAPE_SPHERE;
        this.model = modelManager.createModel(
                shapeMeshes[shownShape],
                materialManager.getMaterialIDFromMaterialName(MaterialViewerSetting.MATERIAL_VIEWER));
    }

    @Override
    protected void dispose() {

        for (int shape = 0; shape < shapeMeshes.length; shape++)
            if (shapeMeshes[shape] != null)
                modelManager.removeMesh(shapeMeshes[shape]);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        showShape();

        if (!resolveMaps())
            return;

        pushUniforms(model.getMaterial());

        renderManager.pushRenderCall(model, sceneFbo, MaterialViewerSetting.DEPTH_SHAPE, window);
        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // The model drawn with the mesh of the shape the Shape option names
    private void showShape() {

        int shape = Math.clamp(
                (int) toolManager.getOption(ToolCanvas.MATERIAL_VIEWER, ToolOption.SHAPE),
                0,
                shapeMeshes.length - 1);

        if (shape == shownShape)
            return;

        this.shownShape = shape;
        model.updateMeshData(shapeMeshes[shape].getMeshData());
    }

    // Every shown map's texture as it stands, boxed afresh only when it changed; false while one is not up yet
    private boolean resolveMaps() {

        boolean ready = true;

        for (int i = 0; i < mapTextures.length; i++) {

            int texture = materialsManager.usePreviewTexture(MaterialViewerSetting.SHOWN_MAPS[i]);

            if (texture != mapTextures[i] || mapTextureValues[i] == null) {
                mapTextures[i] = texture;
                mapTextureValues[i] = texture;
            }

            ready &= texture != 0;
        }

        return ready;
    }

    // The parameters, the tiles and the light handed on through the same vectors every frame
    private void pushUniforms(MaterialInstance material) {

        baseColor.set(
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_RED),
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_GREEN),
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_BLUE));
        surface.set(
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_TILING),
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_NORMAL_STRENGTH),
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_ROUGHNESS),
                materialsManager.getParameter(EditorSetting.MATERIALS_PARAMETER_METALLIC));

        for (int i = 0; i < mapTextureValues.length; i++)
            material.setUniform(MaterialViewerSetting.UNIFORM_MAPS[i], mapTextureValues[i]);

        material.setUniform(MaterialViewerSetting.UNIFORM_BASE_COLOR, baseColor);
        material.setUniform(MaterialViewerSetting.UNIFORM_SURFACE, surface);
        material.setUniform(
                MaterialViewerSetting.UNIFORM_LIGHT_DIRECTION,
                materialViewerLightSystem.getLightDirection());
    }
}
