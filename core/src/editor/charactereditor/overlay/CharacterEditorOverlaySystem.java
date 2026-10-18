package editor.charactereditor.overlay;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.charactereditor.CharacterEditorSetting;
import editor.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.characterview.render.CharacterViewRenderSystem;
import editor.runtime.EditorInputSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class CharacterEditorOverlaySystem extends SystemPackage {

    /*
     * Draws the active part's vertices and edges over this tab's character
     * while a mesh mode is open, as one mesh of dots and lines the overlay
     * shader keeps a fixed number of pixels wide however far the camera
     * orbits. In Mesh mode the selection and the active vertex stand out; in
     * Weights mode each vertex is coloured by the selected bone's weight on
     * it. With X-Ray off the overlay is depth tested against the character,
     * nudged toward the camera, so hidden vertices stay hidden; with it on it
     * draws over everything. The box being dragged, the weight brush and the
     * axis a transform is held to are drawn on top as guides. The overlay is
     * rebuilt in place only when what it shows has moved.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;
    private RenderManager renderManager;

    // Models
    private VAOHandle overlayVAO;
    private int overlayMaterialID;
    private MeshInstance meshOverlay;
    private ModelInstance meshModel;
    private boolean depthBiasApplied;
    private boolean depthBiasXray;
    private MeshInstance guideOverlay;
    private ModelInstance guideModel;

    // Geometry
    private FloatArrayList vertices;
    private ShortArrayList indices;
    private FloatArrayList guideVertices;
    private ShortArrayList guideIndices;
    private LongOpenHashSet edges;

    // State
    private CharacterMeshDocumentInstance builtDocument;
    private int builtRevision;
    private int builtManagerRevision;

    // Scratch
    private float[] colorScratch;
    private Vector3 pointA;
    private Vector3 pointB;
    private Vector3 rayDirection;

    // Base \\

    @Override
    protected void create() {

        // Geometry
        this.vertices = new FloatArrayList();
        this.indices = new ShortArrayList();
        this.guideVertices = new FloatArrayList();
        this.guideIndices = new ShortArrayList();
        this.edges = new LongOpenHashSet();

        // State
        this.builtRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtManagerRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.colorScratch = new float[CharacterEditorSetting.OVERLAY_COLOR_FLOATS];
        this.pointA = new Vector3();
        this.pointB = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = get(CharacterEditorSculptSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void awake() {
        this.overlayVAO = vaoManager.getVAOHandleFromVAOName(CharacterEditorSetting.MESH_OVERLAY_VAO);
        this.overlayMaterialID = materialManager.getMaterialIDFromMaterialName(CharacterEditorSetting.MATERIAL_OVERLAY);
    }

    @Override
    protected void dispose() {

        if (meshOverlay != null)
            modelManager.removeMesh(meshOverlay);

        if (guideOverlay != null)
            modelManager.removeMesh(guideOverlay);
    }

    // Update \\

    @Override
    protected void update() {

        if (!characterEditorManager.getMode().isMeshMode() || !characterEditorMeshViewSystem.hasActiveDocument())
            return;

        WindowInstance window = context.getWindow();
        boolean xray = characterEditorManager.isXray();

        syncMeshOverlay();

        if (meshModel != null && !indices.isEmpty()) {
            applyDepthBias(xray);
            renderManager.pushRenderCall(
                    meshModel,
                    xray ? characterViewRenderSystem.getOverlayFbo() : characterViewRenderSystem.getSceneFbo(),
                    CharacterEditorSetting.DEPTH_OVERLAY,
                    window);
        }

        if (buildGuides())
            renderManager.pushRenderCall(
                    guideModel,
                    characterViewRenderSystem.getOverlayFbo(),
                    CharacterEditorSetting.DEPTH_GUIDES,
                    window);
    }

    // Depth tested a hair toward the camera, or drawn over everything with X-Ray; set only when that flips
    private void applyDepthBias(boolean xray) {

        if (depthBiasApplied && depthBiasXray == xray)
            return;

        this.depthBiasApplied = true;
        this.depthBiasXray = xray;
        meshModel.getMaterial().setUniform(
                CharacterEditorSetting.UNIFORM_DEPTH_BIAS,
                xray ? 0f : CharacterEditorSetting.OVERLAY_DEPTH_BIAS);
    }

    // Mesh Overlay \\

    // Rebuilds when the part, its geometry or selection, the editor's state or a playing pose has moved
    private void syncMeshOverlay() {

        CharacterMeshDocumentInstance document = characterEditorMeshViewSystem.getActiveDocument();
        boolean posePlayed = characterEditorManager.getMode() == CharacterEditorMode.WEIGHTS
                && characterEditorManager.getPose().isPlayed();

        if (document == builtDocument
                && document.getRevision() == builtRevision
                && characterEditorManager.getRevision() == builtManagerRevision
                && !posePlayed)
            return;

        characterEditorMeshViewSystem.refresh();
        document = characterEditorMeshViewSystem.getActiveDocument();

        if (document == null)
            return;

        this.builtDocument = document;
        this.builtRevision = document.getRevision();
        this.builtManagerRevision = characterEditorManager.getRevision();

        buildMeshOverlay(document);

        if (indices.isEmpty())
            return;

        if (meshOverlay == null) {
            this.meshOverlay = meshManager.createMesh(overlayVAO, vertices, indices);
            this.meshModel = modelManager.createModel(meshOverlay, overlayMaterialID);
            return;
        }

        meshManager.updateMesh(meshOverlay, vertices, indices);
        meshModel.updateMeshData(meshOverlay.getMeshData());
    }

    private void buildMeshOverlay(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        boolean weights = characterEditorManager.getMode() == CharacterEditorMode.WEIGHTS;

        vertices.clear();
        indices.clear();
        vertices.ensureCapacity((mesh.getVertexCount() + mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT)
                * EngineSetting.QUAD_VERTEX_COUNT * CharacterEditorSetting.OVERLAY_VERTEX_FLOATS);

        for (int v = 0; v < mesh.getVertexCount(); v++)
            appendVertexDot(document, v, weights);

        appendEdges(document, weights);
    }

    private void appendVertexDot(CharacterMeshDocumentInstance document, int vertexIndex, boolean weights) {

        boolean selected = document.isSelected(vertexIndex);
        float half;

        if (weights) {
            resolveHeat(document.getMesh().getBoneWeight(vertexIndex, characterEditorManager.getSelectedBoneIndex()));
            half = selected
                    ? CharacterEditorSetting.WEIGHT_SELECTED_HALF_PIXELS
                    : CharacterEditorSetting.WEIGHT_HALF_PIXELS;
        } else {
            setColor(vertexIndex == document.getActiveVertex()
                    ? CharacterEditorSetting.COLOR_ACTIVE
                    : selected ? CharacterEditorSetting.COLOR_SELECTED : CharacterEditorSetting.COLOR_VERTEX);
            half = selected
                    ? CharacterEditorSetting.VERTEX_SELECTED_HALF_PIXELS
                    : CharacterEditorSetting.VERTEX_HALF_PIXELS;
        }

        appendDot(
                vertices,
                indices,
                characterEditorMeshViewSystem.getX(vertexIndex),
                characterEditorMeshViewSystem.getY(vertexIndex),
                characterEditorMeshViewSystem.getZ(vertexIndex),
                half);
    }

    // Every edge of every face once, brighter where both of its ends are selected
    private void appendEdges(CharacterMeshDocumentInstance document, boolean weights) {

        RiggedMeshStruct mesh = document.getMesh();

        edges.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (a == b || !edges.add((long) Math.min(a, b) << Integer.SIZE | Math.max(a, b)))
                    continue;

                setColor(!weights && document.isSelected(a) && document.isSelected(b)
                        ? CharacterEditorSetting.COLOR_EDGE_SELECTED
                        : CharacterEditorSetting.COLOR_EDGE);

                if (!appendLine(
                        vertices,
                        indices,
                        characterEditorMeshViewSystem.getX(a),
                        characterEditorMeshViewSystem.getY(a),
                        characterEditorMeshViewSystem.getZ(a),
                        characterEditorMeshViewSystem.getX(b),
                        characterEditorMeshViewSystem.getY(b),
                        characterEditorMeshViewSystem.getZ(b),
                        CharacterEditorSetting.EDGE_HALF_PIXELS))
                    return;
            }
    }

    // A bone's weight as heat: blue for none through green to red for all of it
    private void resolveHeat(float weight) {

        float[] low = weight < 0.5f ? CharacterEditorSetting.COLOR_HEAT_NONE : CharacterEditorSetting.COLOR_HEAT_HALF;
        float[] high = weight < 0.5f ? CharacterEditorSetting.COLOR_HEAT_HALF : CharacterEditorSetting.COLOR_HEAT_FULL;
        float t = weight < 0.5f ? weight * 2f : (weight - 0.5f) * 2f;

        for (int i = 0; i < colorScratch.length; i++)
            colorScratch[i] = low[i] + (high[i] - low[i]) * t;
    }

    private void setColor(float[] color) {
        System.arraycopy(color, 0, colorScratch, 0, colorScratch.length);
    }

    // Guides \\

    // The box being dragged, the weight brush and a held transform's axis; false when none shows
    private boolean buildGuides() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        guideVertices.clear();
        guideIndices.clear();

        if (characterEditorSculptSystem.isBoxSelecting())
            appendBoxGuide(rawInput.getMouseX(), rawInput.getMouseY());

        if (characterEditorSculptSystem.isBrushShown())
            appendBrushGuide(rawInput.getMouseX(), rawInput.getMouseY());

        if (characterEditorSculptSystem.hasTransformAxis())
            appendAxisGuide();

        if (guideIndices.isEmpty())
            return false;

        if (guideOverlay == null) {
            this.guideOverlay = meshManager.createMesh(overlayVAO, guideVertices, guideIndices);
            this.guideModel = modelManager.createModel(guideOverlay, overlayMaterialID);
            guideModel.getMaterial().setUniform(CharacterEditorSetting.UNIFORM_DEPTH_BIAS, 0f);
            return true;
        }

        meshManager.updateMesh(guideOverlay, guideVertices, guideIndices);
        guideModel.updateMeshData(guideOverlay.getMeshData());

        return true;
    }

    private void appendBoxGuide(float mouseX, float mouseY) {

        float pressX = characterEditorSculptSystem.getPressX();
        float pressY = characterEditorSculptSystem.getPressY();

        setColor(CharacterEditorSetting.COLOR_GUIDE);
        appendScreenLine(pressX, pressY, mouseX, pressY);
        appendScreenLine(mouseX, pressY, mouseX, mouseY);
        appendScreenLine(mouseX, mouseY, pressX, mouseY);
        appendScreenLine(pressX, mouseY, pressX, pressY);
    }

    private void appendBrushGuide(float mouseX, float mouseY) {

        float radius = characterEditorManager.getBrushRadius();
        int segments = CharacterEditorSetting.BRUSH_GUIDE_SEGMENTS;

        setColor(CharacterEditorSetting.COLOR_GUIDE);

        for (int i = 0; i < segments; i++) {

            double from = Math.PI * 2.0 * i / segments;
            double to = Math.PI * 2.0 * (i + 1) / segments;

            appendScreenLine(
                    mouseX + (float) Math.cos(from) * radius,
                    mouseY + (float) Math.sin(from) * radius,
                    mouseX + (float) Math.cos(to) * radius,
                    mouseY + (float) Math.sin(to) * radius);
        }
    }

    // A long line through the pivot along the held axis, in the axis's own colour
    private void appendAxisGuide() {

        Vector3 pivot = characterEditorSculptSystem.getPivot();
        Vector3 axis = characterEditorSculptSystem.getAxisVector();
        int axisIndex = characterEditorSculptSystem.getTransformAxis();
        float length = CharacterEditorSetting.AXIS_GUIDE_LENGTH;

        setColor(axisIndex == EngineSetting.INDEX_NOT_FOUND
                ? CharacterEditorSetting.COLOR_AXIS_NORMAL
                : CharacterEditorSetting.COLOR_AXES[axisIndex]);
        appendLine(
                guideVertices,
                guideIndices,
                pivot.x - axis.x * length,
                pivot.y - axis.y * length,
                pivot.z - axis.z * length,
                pivot.x + axis.x * length,
                pivot.y + axis.y * length,
                pivot.z + axis.z * length,
                CharacterEditorSetting.GUIDE_HALF_PIXELS);
    }

    // A line between two window pixels, placed just past the near plane so it shows over everything
    private void appendScreenLine(float x0, float y0, float x1, float y1) {

        float distance = context.getWindow().getActiveCamera().getNearPlane() * CharacterEditorSetting.GUIDE_NEAR_SCALE;

        characterEditorMeshViewSystem.resolveRay(x0, y0, pointA, rayDirection);
        pointA.add(rayDirection.x * distance, rayDirection.y * distance, rayDirection.z * distance);
        characterEditorMeshViewSystem.resolveRay(x1, y1, pointB, rayDirection);
        pointB.add(rayDirection.x * distance, rayDirection.y * distance, rayDirection.z * distance);

        appendLine(
                guideVertices,
                guideIndices,
                pointA.x,
                pointA.y,
                pointA.z,
                pointB.x,
                pointB.y,
                pointB.z,
                CharacterEditorSetting.GUIDE_HALF_PIXELS);
    }

    // Geometry \\

    // A square of corners around one point, each pushed its half size in pixels off it by the shader
    private void appendDot(FloatArrayList target, ShortArrayList targetIndices, float x, float y, float z, float half) {

        int base = target.size() / CharacterEditorSetting.OVERLAY_VERTEX_FLOATS;

        if (base + EngineSetting.QUAD_VERTEX_COUNT > EngineSetting.MESH_VERT_LIMIT)
            return;

        putVertex(target, x, y, z, x, y, z, -half, -half);
        putVertex(target, x, y, z, x, y, z, half, -half);
        putVertex(target, x, y, z, x, y, z, half, half);
        putVertex(target, x, y, z, x, y, z, -half, half);
        putQuad(targetIndices, base);
    }

    // A strip between two points, each end pushed its half width in pixels across the line by the shader; across
    // flips with the line's direction, so the far end's sides are named the other way round. False at the limit.
    private boolean appendLine(
            FloatArrayList target,
            ShortArrayList targetIndices,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float half) {

        int base = target.size() / CharacterEditorSetting.OVERLAY_VERTEX_FLOATS;

        if (base + EngineSetting.QUAD_VERTEX_COUNT > EngineSetting.MESH_VERT_LIMIT)
            return false;

        putVertex(target, ax, ay, az, bx, by, bz, half, 0f);
        putVertex(target, ax, ay, az, bx, by, bz, -half, 0f);
        putVertex(target, bx, by, bz, ax, ay, az, half, 0f);
        putVertex(target, bx, by, bz, ax, ay, az, -half, 0f);
        putQuad(targetIndices, base);

        return true;
    }

    private void putVertex(
            FloatArrayList target,
            float x,
            float y,
            float z,
            float otherX,
            float otherY,
            float otherZ,
            float shapeX,
            float shapeY) {

        target.add(x);
        target.add(y);
        target.add(z);
        target.add(otherX);
        target.add(otherY);
        target.add(otherZ);
        target.add(shapeX);
        target.add(shapeY);

        for (int i = 0; i < colorScratch.length; i++)
            target.add(colorScratch[i]);
    }

    private void putQuad(ShortArrayList targetIndices, int base) {
        targetIndices.add((short) base);
        targetIndices.add((short) (base + 1));
        targetIndices.add((short) (base + 2));
        targetIndices.add((short) (base + 2));
        targetIndices.add((short) (base + 3));
        targetIndices.add((short) base);
    }
}
