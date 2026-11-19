package editor.runtime.itemeditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import java.util.Arrays;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.itemeditor.ItemEditorSetting;
import editor.runtime.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorRenderSystem extends SystemPackage {

    /*
     * Draws the active item, the sub-voxel floor grid, and the tool cursor into
     * this window's scene target. The grid stays bright across every block the
     * item's model spans. Brush and Erase show the stamp itself, every
     * sub-voxel it would build or dig drawn as cubes greedily merged like the
     * item's own, so a soft tip shows thinner toward its feathered edge; the
     * other tools' cursor fills a cell for a cube and lies flat on its plane
     * for a wall. The item mesh is rebuilt in place through SubVoxelManager
     * only when the item or its revision changes, the stamp only when the
     * pointer's hit, the tool, its options, the brush or the item change.
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
    private ToolManager toolManager;
    private BrushManager brushManager;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private MeshInstance itemMesh;
    private ModelInstance itemModel;
    private ModelInstance gridModel;
    private ModelInstance placeCursorModel;
    private ModelInstance eraseCursorModel;
    private ModelInstance paintCursorModel;

    // Stamp — the cubes the brush would reach, as a model of one part, its mesh, drawn built or dug
    private SubVoxelModelStruct stampModel;
    private MeshInstance stampMesh;
    private ModelInstance stampBuildModel;
    private ModelInstance stampDigModel;

    // State
    private ItemDocumentInstance renderedDocument;
    private int renderedRevision;
    private Vector3 cursorCell;
    private Vector3 cursorSize;
    private Vector2 gridExtent;
    private ItemDocumentInstance stampDocument;
    private int[] stampKey;
    private int[] shownStampKey;
    private boolean stampShown;

    // Base \\

    @Override
    protected void create() {
        this.cursorCell = new Vector3();
        this.cursorSize = new Vector3();
        this.gridExtent = new Vector2(1f, 1f);
        this.stampKey = new int[ItemEditorSetting.STAMP_KEY_LENGTH];
        this.shownStampKey = new int[ItemEditorSetting.STAMP_KEY_LENGTH];
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
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
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

        if (stampMesh != null)
            modelManager.removeMesh(stampMesh);
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

    // Wall marks the wall it would stand; Erase over a wall marks the wall alone; Brush and Erase otherwise show their
    // stamp; the rest mark the cube or wall struck
    private void pushCursor(WindowInstance window) {

        SubVoxelHitStruct hit = itemEditorToolSystem.getHit();
        EditorTool tool = itemEditorManager.getTool();
        boolean wallErased = tool == EditorTool.ERASE && hit.hasTarget() && hit.isTargetWall();

        if (tool == EditorTool.WALL) {

            if (!hit.hasWallPlacement())
                return;

            setWallCursor(hit.getWallPlaceAxis(), hit.getWallPlaceX(), hit.getWallPlaceY(), hit.getWallPlaceZ());
            pushCellCursor(placeCursorModel, window);
            return;
        }

        if (itemEditorManager.isStampTool() && !wallErased) {
            pushStamp(window, hit, tool);
            return;
        }

        if (setTargetCursor(hit))
            pushCellCursor(wallErased ? eraseCursorModel : paintCursorModel, window);
    }

    private void pushCellCursor(ModelInstance cursorModel, WindowInstance window) {
        cursorModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_CURSOR_CELL, cursorCell);
        cursorModel.getMaterial().setUniform(ItemEditorSetting.UNIFORM_CURSOR_SIZE, cursorSize);
        renderManager.pushRenderCall(cursorModel, sceneFbo, ItemEditorSetting.DEPTH_CURSOR, window);
    }

    // Stamp \\

    // The stamp's cubes, built in the place color or dug in the erase color
    private void pushStamp(WindowInstance window, SubVoxelHitStruct hit, EditorTool tool) {

        if (!syncStamp(hit, tool))
            return;

        renderManager.pushRenderCall(
                tool == EditorTool.BRUSH ? stampBuildModel : stampDigModel,
                sceneFbo,
                ItemEditorSetting.DEPTH_CURSOR,
                window);
    }

    // The stamp worked out again and its mesh rebuilt in place only when something it shows moved; whether it reaches
    // any cube
    private boolean syncStamp(SubVoxelHitStruct hit, EditorTool tool) {

        ItemDocumentInstance document = itemEditorManager.getActiveDocument();

        if (document == null)
            return false;

        writeStampKey(document, hit, tool);

        if (document == stampDocument && Arrays.equals(stampKey, shownStampKey))
            return stampShown;

        this.stampDocument = document;
        System.arraycopy(stampKey, 0, shownStampKey, 0, stampKey.length);
        resolveStampModel(document.getModel());

        this.stampShown = itemEditorManager.buildStampPreview(hit, stampModel);

        if (stampShown)
            rebuildStampMesh();

        return stampShown;
    }

    private void writeStampKey(ItemDocumentInstance document, SubVoxelHitStruct hit, EditorTool tool) {

        stampKey[0] = document.getRevision();
        stampKey[1] = tool.ordinal();
        stampKey[2] = toolManager.getOptionRevision();
        stampKey[3] = brushManager.getRevision();
        stampKey[4] = document.getSelectedPartIndex();
        stampKey[5] = (hit.hasTarget() ? 1 : 0) | (hit.isTargetWall() ? 2 : 0) | (hit.hasPlacement() ? 4 : 0);
        stampKey[6] = hit.getTargetX();
        stampKey[7] = hit.getTargetY();
        stampKey[8] = hit.getTargetZ();
        stampKey[9] = hit.getPlaceX();
        stampKey[10] = hit.getPlaceY();
        stampKey[11] = hit.getPlaceZ();
    }

    // A stamp model of the item's size and one part, made again only when the item's size changes
    private void resolveStampModel(SubVoxelModelStruct model) {

        if (stampModel != null
                && stampModel.getBlocksX() == model.getBlocksX()
                && stampModel.getBlocksY() == model.getBlocksY()
                && stampModel.getBlocksZ() == model.getBlocksZ())
            return;

        this.stampModel = new SubVoxelModelStruct(model.getBlocksX(), model.getBlocksY(), model.getBlocksZ());
        stampModel.addPart(new SubVoxelPartStruct(
                ItemEditorSetting.STAMP_PART_NAME, model.getPart(0).getTextureName()));
    }

    private void rebuildStampMesh() {

        if (stampMesh == null) {
            this.stampMesh = subVoxelManager.createMesh(stampModel);
            this.stampBuildModel = modelManager.createModel(
                    stampMesh, materialManager.getMaterialIDFromMaterialName(ItemEditorSetting.MATERIAL_STAMP_BUILD));
            this.stampDigModel = modelManager.createModel(
                    stampMesh, materialManager.getMaterialIDFromMaterialName(ItemEditorSetting.MATERIAL_STAMP_DIG));
            return;
        }

        subVoxelManager.updateMesh(stampMesh, stampModel);
        stampBuildModel.updateMeshData(stampMesh.getMeshData());
        stampDigModel.updateMeshData(stampMesh.getMeshData());
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
