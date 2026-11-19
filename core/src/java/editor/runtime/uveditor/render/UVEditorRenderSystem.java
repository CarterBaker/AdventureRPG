package editor.runtime.uveditor.render;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.runtime.EditorSetting;
import editor.runtime.uveditor.UVEditorSetting;
import editor.runtime.uveditor.tool.UVEditorToolSystem;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class UVEditorRenderSystem extends SystemPackage {

    /*
     * Draws the UV Editor into this window's scene target: the tile the
     * faces shown are textured with over the unit square of UV space, then
     * the UVs of those faces as one mesh of lines and dots the overlay
     * shader keeps a fixed number of pixels wide at any zoom. Every edge of
     * a face is drawn once, a seam in red and wider; every corner is a dot,
     * the selected ones in the selection's colour and the pinned ones in
     * red; the box being dragged is drawn over them. The overlay is rebuilt
     * in place only when the mesh, the selection, the editor's state or the
     * box has moved; the view's zoom and pan only move a uniform.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private TextureManager textureManager;
    private CharacterEditorManager characterEditorManager;
    private UVEditorViewSystem uvEditorViewSystem;
    private UVEditorToolSystem uvEditorToolSystem;

    // Render Target
    private FBOInstance sceneFbo;

    // Models
    private ModelInstance backgroundModel;
    private VAOHandle overlayVAO;
    private int overlayMaterialID;
    private MeshInstance overlayMesh;
    private ModelInstance overlayModel;
    private int shownTileID;

    // Geometry
    private FloatArrayList vertices;
    private IntArrayList indices;
    private IntArrayList faces;

    // State
    private CharacterMeshDocumentInstance builtDocument;
    private int builtRevision;
    private int builtManagerRevision;
    private int builtBoxRevision;

    // Scratch
    private float[] colorScratch;
    private Vector2 viewSize;

    // Base \\

    @Override
    protected void create() {

        // Geometry
        this.vertices = new FloatArrayList();
        this.indices = new IntArrayList();
        this.faces = new IntArrayList();

        // State
        this.builtRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtManagerRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtBoxRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownTileID = Integer.MIN_VALUE;

        // Scratch
        this.colorScratch = new float[UVEditorSetting.OVERLAY_COLOR_FLOATS];
        this.viewSize = new Vector2();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.textureManager = get(TextureManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.uvEditorViewSystem = get(UVEditorViewSystem.class);
        this.uvEditorToolSystem = get(UVEditorToolSystem.class);
    }

    @Override
    protected void awake() {

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, context.getWindow());
        this.backgroundModel = modelManager.createModel(
                meshManager.getMeshHandleFromMeshName(EditorSetting.MESH_EDITOR_VIEWPORT_QUAD),
                materialManager.getMaterialIDFromMaterialName(UVEditorSetting.MATERIAL_BACKGROUND));
        this.overlayVAO = vaoManager.getVAOHandleFromVAOName(EditorSetting.MESH_EDITOR_OVERLAY_VAO);
        this.overlayMaterialID = materialManager.getMaterialIDFromMaterialName(UVEditorSetting.MATERIAL_OVERLAY);
    }

    @Override
    protected void dispose() {
        if (overlayMesh != null)
            modelManager.removeMesh(overlayMesh);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        viewSize.set(window.getWidth(), window.getHeight());
        syncOverlay();

        backgroundModel.getMaterial().setUniform(
                EditorSetting.FLAT_VIEW_UNIFORM_UV_TO_CLIP, uvEditorViewSystem.getUVToClip());
        renderManager.pushRenderCall(backgroundModel, sceneFbo, UVEditorSetting.DEPTH_BACKGROUND, window);

        if (overlayModel != null && !indices.isEmpty()) {
            overlayModel.getMaterial().setUniform(
                    EditorSetting.FLAT_VIEW_UNIFORM_UV_TO_CLIP, uvEditorViewSystem.getUVToClip());
            overlayModel.getMaterial().setUniform(UVEditorSetting.UNIFORM_VIEW_SIZE, viewSize);
            renderManager.pushRenderCall(overlayModel, sceneFbo, UVEditorSetting.DEPTH_OVERLAY, window);
        }

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Overlay \\

    // Rebuilds when the part, its geometry or selection, the editor's state or the box has moved
    private void syncOverlay() {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();
        int documentRevision = document != null ? document.getRevision() : EngineSetting.INDEX_NOT_FOUND;

        if (document == builtDocument && documentRevision == builtRevision
                && characterEditorManager.getRevision() == builtManagerRevision
                && uvEditorToolSystem.getBoxRevision() == builtBoxRevision)
            return;

        this.builtDocument = document;
        this.builtRevision = documentRevision;
        this.builtManagerRevision = characterEditorManager.getRevision();
        this.builtBoxRevision = uvEditorToolSystem.getBoxRevision();

        vertices.clear();
        indices.clear();

        if (document != null)
            buildOverlay(document);

        appendBox();
        showTile(document);

        if (indices.isEmpty())
            return;

        if (overlayMesh == null) {
            this.overlayMesh = meshManager.createMesh(overlayVAO, vertices, indices);
            this.overlayModel = modelManager.createModel(overlayMesh, overlayMaterialID);
            return;
        }

        meshManager.updateMesh(overlayMesh, vertices, indices);
        overlayModel.updateMeshData(overlayMesh.getMeshData());
    }

    private void buildOverlay(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        characterEditorManager.collectUVFaces(faces);
        vertices.ensureCapacity(faces.size() * EngineSetting.QUAD_VERTEX_COUNT * 2
                * EngineSetting.QUAD_VERTEX_COUNT * UVEditorSetting.OVERLAY_VERTEX_FLOATS);

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int next = (c + 1) % EngineSetting.QUAD_VERTEX_COUNT;
                int vertexA = mesh.getFaceVertex(faceIndex, c);
                int vertexB = mesh.getFaceVertex(faceIndex, next);
                boolean seam = mesh.isSeam(vertexA, vertexB);

                setColor(seam ? UVEditorSetting.COLOR_SEAM : UVEditorSetting.COLOR_EDGE);

                appendLine(
                        mesh.getFaceU(faceIndex, c),
                        mesh.getFaceV(faceIndex, c),
                        mesh.getFaceU(faceIndex, next),
                        mesh.getFaceV(faceIndex, next),
                        seam ? UVEditorSetting.SEAM_HALF_PIXELS : UVEditorSetting.EDGE_HALF_PIXELS);
            }
        }

        for (int i = 0; i < faces.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                appendCorner(document, faces.getInt(i), c);
    }

    // A face corner's dot, selected and pinned corners standing out
    private void appendCorner(CharacterMeshDocumentInstance document, int faceIndex, int corner) {

        RiggedMeshStruct mesh = document.getMesh();
        boolean selected = document.isCornerSelected(faceIndex, corner);

        if (selected)
            setColor(UVEditorSetting.COLOR_SELECTED);
        else
            setColor(mesh.isCornerPinned(faceIndex, corner)
                    ? UVEditorSetting.COLOR_PINNED
                    : UVEditorSetting.COLOR_CORNER);

        appendDot(
                mesh.getFaceU(faceIndex, corner),
                mesh.getFaceV(faceIndex, corner),
                selected ? UVEditorSetting.CORNER_SELECTED_HALF_PIXELS : UVEditorSetting.CORNER_HALF_PIXELS);
    }

    // The box being dragged, its four sides
    private void appendBox() {

        if (!uvEditorToolSystem.isBoxSelecting())
            return;

        float minU = uvEditorToolSystem.getBoxMinU();
        float minV = uvEditorToolSystem.getBoxMinV();
        float maxU = uvEditorToolSystem.getBoxMaxU();
        float maxV = uvEditorToolSystem.getBoxMaxV();

        setColor(UVEditorSetting.COLOR_BOX);
        appendLine(minU, minV, maxU, minV, UVEditorSetting.BOX_HALF_PIXELS);
        appendLine(maxU, minV, maxU, maxV, UVEditorSetting.BOX_HALF_PIXELS);
        appendLine(maxU, maxV, minU, maxV, UVEditorSetting.BOX_HALF_PIXELS);
        appendLine(minU, maxV, minU, minV, UVEditorSetting.BOX_HALF_PIXELS);
    }

    // The tile of the first face shown under the square, or the checker alone; set only when it changes
    private void showTile(CharacterMeshDocumentInstance document) {

        int tileID = EngineSetting.INDEX_NOT_FOUND;

        if (document != null && !faces.isEmpty()) {

            RiggedMeshStruct mesh = document.getMesh();
            String textureName = mesh.getTextureName(mesh.getFaceTexture(faces.getInt(0)));

            if (textureManager.hasTexture(textureName))
                tileID = textureManager.getTileIDFromTextureName(textureName);
        }

        if (tileID == shownTileID)
            return;

        this.shownTileID = tileID;
        backgroundModel.getMaterial().setUniform(UVEditorSetting.UNIFORM_TILE_ID, tileID);
    }

    // Geometry \\

    // A square of corners around one point, each pushed its half size in pixels off it by the shader
    private void appendDot(float u, float v, float half) {

        int base = vertices.size() / UVEditorSetting.OVERLAY_VERTEX_FLOATS;

        putVertex(u, v, u, v, -half, -half);
        putVertex(u, v, u, v, half, -half);
        putVertex(u, v, u, v, half, half);
        putVertex(u, v, u, v, -half, half);
        putQuad(base);
    }

    // A strip between two points, each end pushed its half width in pixels across the line by the shader
    private void appendLine(float au, float av, float bu, float bv, float half) {

        int base = vertices.size() / UVEditorSetting.OVERLAY_VERTEX_FLOATS;

        putVertex(au, av, bu, bv, half, 0f);
        putVertex(au, av, bu, bv, -half, 0f);
        putVertex(bu, bv, au, av, half, 0f);
        putVertex(bu, bv, au, av, -half, 0f);
        putQuad(base);
    }

    private void putVertex(float u, float v, float otherU, float otherV, float shapeX, float shapeY) {

        vertices.add(u);
        vertices.add(v);
        vertices.add(0f);
        vertices.add(otherU);
        vertices.add(otherV);
        vertices.add(0f);
        vertices.add(shapeX);
        vertices.add(shapeY);

        for (int i = 0; i < colorScratch.length; i++)
            vertices.add(colorScratch[i]);
    }

    private void putQuad(int base) {
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base + 2);
        indices.add(base + 3);
        indices.add(base);
    }

    private void setColor(float[] color) {
        System.arraycopy(color, 0, colorScratch, 0, colorScratch.length);
    }
}
