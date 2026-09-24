package editor.itemeditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import editor.itemeditor.tool.ItemEditorToolSystem;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorRenderSystem extends SystemPackage {

    /*
     * Draws the active item, the sub-voxel floor grid, and the tool cursor into
     * this window's scene target. The item mesh is rebuilt in place through
     * SubVoxelManager only when the item or its revision changes.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private ItemEditorToolSystem itemEditorToolSystem;

    // Render Target
    private FboInstance sceneFbo;

    // Models
    private MeshInstance itemMesh;
    private ModelInstance itemModel;
    private ModelInstance gridModel;
    private ModelInstance placeCursorModel;
    private ModelInstance eraseCursorModel;
    private ModelInstance paintCursorModel;

    // State
    private ItemDocumentInstance renderedDocument;
    private int renderedRevision;
    private Vector3 cursorCell;

    // Base \\

    @Override
    protected void create() {
        this.cursorCell = new Vector3();
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.itemEditorToolSystem = get(ItemEditorToolSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());

        this.gridModel = createModel(ItemEditorSetting.MESH_GRID, ItemEditorSetting.MATERIAL_GRID);
        this.placeCursorModel = createModel(ItemEditorSetting.MESH_CURSOR, ItemEditorSetting.MATERIAL_CURSOR_PLACE);
        this.eraseCursorModel = createModel(ItemEditorSetting.MESH_CURSOR, ItemEditorSetting.MATERIAL_CURSOR_ERASE);
        this.paintCursorModel = createModel(ItemEditorSetting.MESH_CURSOR, ItemEditorSetting.MATERIAL_CURSOR_PAINT);
    }

    @Override
    protected void dispose() {

        if (itemMesh != null)
            modelManager.removeMesh(itemMesh);
    }

    private ModelInstance createModel(String meshName, String materialName) {

        ModelInstance model = modelManager.createModel(
                meshManager.getMeshHandleFromMeshName(meshName),
                materialManager.getMaterialIDFromMaterialName(materialName));

        model.getMaterial().setUniform(
                ItemEditorSetting.UNIFORM_RESOLUTION,
                (float) EngineSetting.SUB_VOXEL_RESOLUTION);

        return model;
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        syncItemMesh();

        if (renderedDocument != null && itemModel != null && !renderedDocument.getModel().isEmpty())
            renderManager.pushRenderCall(itemModel, sceneFbo, ItemEditorSetting.DEPTH_MODEL, window);

        renderManager.pushRenderCall(gridModel, sceneFbo, ItemEditorSetting.DEPTH_GRID, window);
        pushCursor(window);

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Item Mesh \\

    private void syncItemMesh() {

        ItemDocumentInstance document = itemEditorManager.getActiveDocument();

        if (document == null) {
            renderedDocument = null;
            return;
        }

        if (document == renderedDocument && document.getRevision() == renderedRevision)
            return;

        renderedDocument = document;
        renderedRevision = document.getRevision();

        if (document.getModel().isEmpty())
            return;

        if (itemMesh == null) {
            itemMesh = subVoxelManager.createMesh(document.getModel());
            itemModel = modelManager.createModel(
                    itemMesh,
                    materialManager.getMaterialIDFromMaterialName(ItemEditorSetting.MATERIAL_MODEL));
            return;
        }

        subVoxelManager.updateMesh(itemMesh, document.getModel());
        itemModel.updateMeshData(itemMesh.getMeshData());
    }

    // Cursor \\

    private void pushCursor(WindowInstance window) {

        SubVoxelHitStruct hit = itemEditorToolSystem.getHit();
        ModelInstance cursorModel;

        switch (itemEditorManager.getTool()) {

            case PLACE -> {
                if (!hit.hasPlacement())
                    return;
                cursorCell.set(hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ());
                cursorModel = placeCursorModel;
            }

            case ERASE -> {
                if (!hit.hasTarget())
                    return;
                cursorCell.set(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());
                cursorModel = eraseCursorModel;
            }

            default -> {
                if (!hit.hasTarget())
                    return;
                cursorCell.set(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());
                cursorModel = paintCursorModel;
            }
        }

        cursorModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_CURSOR_CELL, cursorCell);
        renderManager.pushRenderCall(cursorModel, sceneFbo, ItemEditorSetting.DEPTH_CURSOR, window);
    }
}
