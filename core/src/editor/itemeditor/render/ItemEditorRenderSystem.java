package editor.itemeditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import editor.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorRenderSystem extends SystemPackage {

    /*
     * Draws the active item, the sub-voxel floor grid, and the tool cursor into
     * this window's scene target. The grid stays bright across every block the
     * item's model spans. The cursor fills a cell for a cube and lies flat on
     * its plane for a wall. The item mesh is rebuilt in place through
     * SubVoxelManager only when the item or its revision changes.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private ItemEditorToolSystem itemEditorToolSystem;

    // Render Target
    private FBOInstance sceneFbo;

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
    private Vector3 cursorSize;
    private Vector2 gridExtent;

    // Base \\

    @Override
    protected void create() {
        this.cursorCell = new Vector3();
        this.cursorSize = new Vector3();
        this.gridExtent = new Vector2(1f, 1f);
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
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
        gridExtent.set(document.getModel().getBlocksX(), document.getModel().getBlocksZ());
        gridModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_EXTENT, gridExtent);

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
                setCubeCursor(hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ());
                cursorModel = placeCursorModel;
            }

            case WALL -> {
                if (!hit.hasWallPlacement())
                    return;
                setWallCursor(hit.getWallPlaceAxis(), hit.getWallPlaceX(), hit.getWallPlaceY(), hit.getWallPlaceZ());
                cursorModel = placeCursorModel;
            }

            case ERASE -> {
                if (!setTargetCursor(hit))
                    return;
                cursorModel = eraseCursorModel;
            }

            default -> {
                if (!setTargetCursor(hit))
                    return;
                cursorModel = paintCursorModel;
            }
        }

        cursorModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_CURSOR_CELL, cursorCell);
        cursorModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_CURSOR_SIZE, cursorSize);
        renderManager.pushRenderCall(cursorModel, sceneFbo, ItemEditorSetting.DEPTH_CURSOR, window);
    }

    private boolean setTargetCursor(SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return false;

        if (hit.isTargetWall())
            setWallCursor(hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());
        else
            setCubeCursor(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());

        return true;
    }

    private void setCubeCursor(int x, int y, int z) {
        cursorCell.set(x, y, z);
        cursorSize.set(1f, 1f, 1f);
    }

    private void setWallCursor(int axis, int x, int y, int z) {
        cursorCell.set(x, y, z);
        cursorSize.set(axis == 0 ? 0f : 1f, axis == 1 ? 0f : 1f, axis == 2 ? 0f : 1f);
    }
}
