package editor.runtime.charactereditor.overlay;

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
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterGroomStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStrandStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterStrandUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.edge.CharacterEditorEdgeSystem;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.runtime.charactereditor.stamp.CharacterEditorStampSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class CharacterEditorOverlaySystem extends SystemPackage {

    /*
     * Draws the active part's vertices over this tab's character while the
     * Mesh, Weights or UV tab is open, and its edges in any mesh tab while
     * the canvas's Wireframe is on, as one mesh of dots and lines the
     * overlay shader keeps a fixed number of pixels wide however far the
     * camera orbits. In Mesh and UV the selection and the active
     * vertex stand out, each selected vertex shaded toward the selection's
     * colour as far as it is selected, so a soft brush's falloff shows, and
     * the edges marked as seams are drawn wider in red, as Blender draws
     * them; in Weights each vertex is coloured by the selected bone's
     * weight on it. With
     * X-Ray off the overlay is depth tested against the character, nudged
     * toward the camera, so hidden vertices stay hidden; with it on it
     * draws over everything. The box being dragged, the brush of any mesh
     * mode, Sculpt's included, and the axis a transform is held to are
     * drawn on top as guides, and so is the edge an edge tool of the
     * Topology tab would act on. In Rig mode the colliders the rig's bones
     * carry are drawn over the body as wire spheres and capsules, the one
     * selected in the selection's colour. In Stamp mode every attach point
     * shows as a yellow dot, the one the part joins nearest larger, and the
     * part aimed, with its twin while mirroring, as a ghost of its edges
     * where it would join (every card of fur where it would lie), green where it can and red where it cannot, drawn
     * over everything. In Hair mode every strand of the groom is drawn as
     * its root, its guide line and its card's two sides, the selected ones
     * in the selection's colour. The overlay is rebuilt in place only when what it
     * shows has moved.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterEditorManager characterEditorManager;
    private BrushManager brushManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterEditorStampSystem characterEditorStampSystem;
    private CharacterEditorEdgeSystem characterEditorEdgeSystem;
    private StampManager stampManager;
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
    private MeshInstance colliderOverlay;
    private ModelInstance colliderModel;
    private MeshInstance stampOverlay;
    private ModelInstance stampModel;
    private MeshInstance groomOverlay;
    private ModelInstance groomModel;

    // Geometry
    private FloatArrayList vertices;
    private IntArrayList indices;
    private FloatArrayList guideVertices;
    private IntArrayList guideIndices;
    private FloatArrayList colliderVertices;
    private IntArrayList colliderIndices;
    private FloatArrayList stampVertices;
    private IntArrayList stampIndices;
    private FloatArrayList groomVertices;
    private IntArrayList groomIndices;
    private FloatArrayList cardRows;
    private LongOpenHashSet edges;

    // Ghost — the stamp whose edges are held, each edge's two vertices
    private StampHandle ghostStamp;
    private int ghostStampRevision;
    private IntArrayList ghostEdges;

    // State
    private CharacterMeshDocumentInstance builtDocument;
    private int builtRevision;
    private int builtManagerRevision;
    private boolean builtDots;
    private boolean builtWireframe;
    private int builtColliderRevision;
    private int builtStampRevision;
    private int builtStampManagerRevision;
    private int builtStampDocumentRevision;
    private CharacterGroomStruct builtGroom;
    private int builtGroomRevision;
    private int builtGroomManagerRevision;

    // Scratch
    private float[] colorScratch;
    private Vector3 pointA;
    private Vector3 pointB;
    private Vector3 rayDirection;
    private Vector3 colliderStart;
    private Vector3 colliderEnd;
    private Vector3 colliderAxis;
    private Vector3 colliderSide;
    private Vector3 colliderUp;
    private Vector2 screenCenter;
    private Vector2 screenEdge;
    private Matrix4 ghostScratch;

    // Base \\

    @Override
    protected void create() {

        // Geometry
        this.vertices = new FloatArrayList();
        this.indices = new IntArrayList();
        this.guideVertices = new FloatArrayList();
        this.guideIndices = new IntArrayList();
        this.colliderVertices = new FloatArrayList();
        this.colliderIndices = new IntArrayList();
        this.stampVertices = new FloatArrayList();
        this.stampIndices = new IntArrayList();
        this.groomVertices = new FloatArrayList();
        this.groomIndices = new IntArrayList();
        this.cardRows = new FloatArrayList();
        this.edges = new LongOpenHashSet();

        // Ghost
        this.ghostEdges = new IntArrayList();

        // State
        this.builtRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtManagerRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtColliderRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtStampRevision = EngineSetting.INDEX_NOT_FOUND;
        this.builtGroomRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.colorScratch = new float[CharacterEditorSetting.OVERLAY_COLOR_FLOATS];
        this.pointA = new Vector3();
        this.pointB = new Vector3();
        this.rayDirection = new Vector3();
        this.colliderStart = new Vector3();
        this.colliderEnd = new Vector3();
        this.colliderAxis = new Vector3();
        this.colliderSide = new Vector3();
        this.colliderUp = new Vector3();
        this.screenCenter = new Vector2();
        this.screenEdge = new Vector2();
        this.ghostScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.brushManager = get(BrushManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = get(CharacterEditorSculptSystem.class);
        this.characterEditorStampSystem = get(CharacterEditorStampSystem.class);
        this.characterEditorEdgeSystem = get(CharacterEditorEdgeSystem.class);
        this.stampManager = get(StampManager.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void awake() {
        this.overlayVAO = vaoManager.getVAOHandleFromVAOName(EditorSetting.MESH_EDITOR_OVERLAY_VAO);
        this.overlayMaterialID = materialManager.getMaterialIDFromMaterialName(CharacterEditorSetting.MATERIAL_OVERLAY);
    }

    @Override
    protected void dispose() {

        if (meshOverlay != null)
            modelManager.removeMesh(meshOverlay);

        if (guideOverlay != null)
            modelManager.removeMesh(guideOverlay);

        if (colliderOverlay != null)
            modelManager.removeMesh(colliderOverlay);

        if (stampOverlay != null)
            modelManager.removeMesh(stampOverlay);

        if (groomOverlay != null)
            modelManager.removeMesh(groomOverlay);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (characterEditorManager.getMode().showsColliders())
            pushColliderOverlay(window);

        if (!characterEditorManager.getMode().isMeshMode() || !characterEditorMeshViewSystem.hasActiveDocument())
            return;

        boolean xray = characterEditorManager.isXray();

        if (characterEditorManager.getMode().showsVertices() || characterEditorManager.isWireframe())
            pushMeshOverlay(window, xray);

        if (characterEditorManager.placesStamp())
            pushStampOverlay(window);

        if (characterEditorManager.getMode().groomsHair())
            pushGroomOverlay(window);

        if (buildGuides())
            renderManager.pushRenderCall(
                    guideModel,
                    characterViewRenderSystem.getOverlayFbo(),
                    CharacterEditorSetting.DEPTH_GUIDES,
                    window);
    }

    private void pushMeshOverlay(WindowInstance window, boolean xray) {

        syncMeshOverlay();

        if (meshModel != null && !indices.isEmpty()) {
            applyDepthBias(xray);
            renderManager.pushRenderCall(
                    meshModel,
                    xray ? characterViewRenderSystem.getOverlayFbo() : characterViewRenderSystem.getSceneFbo(),
                    CharacterEditorSetting.DEPTH_OVERLAY,
                    window);
        }
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

    // Groom Overlay \\

    private void pushGroomOverlay(WindowInstance window) {

        syncGroomOverlay();

        if (groomModel != null && !groomIndices.isEmpty())
            renderManager.pushRenderCall(
                    groomModel,
                    characterViewRenderSystem.getSceneFbo(),
                    CharacterEditorSetting.DEPTH_OVERLAY,
                    window);
    }

    // Rebuilds only when another groom opened, the one open changed, the editor's state (the pose among it) moved or
    // a pose plays, each strand drawn where the view poses it
    private void syncGroomOverlay() {

        CharacterGroomStruct groom = characterEditorManager.getGroom();

        if (groom == builtGroom && characterEditorManager.getGroomRevision() == builtGroomRevision
                && characterEditorManager.getRevision() == builtGroomManagerRevision
                && !characterEditorManager.isPosePlaying())
            return;

        this.builtGroom = groom;
        this.builtGroomRevision = characterEditorManager.getGroomRevision();
        this.builtGroomManagerRevision = characterEditorManager.getRevision();
        groomVertices.clear();
        groomIndices.clear();

        Matrix4[] skinningMatrices = characterEditorMeshViewSystem.getSkinningMatrices();

        if (groom != null)
            for (int s = 0; s < groom.getStrandCount(); s++) {

                CharacterStrandStruct strand = groom.getStrand(s);

                appendStrand(strand, characterEditorManager.resolveStrandSkinning(strand, skinningMatrices));
            }

        if (groomIndices.isEmpty())
            return;

        if (groomOverlay == null) {
            this.groomOverlay = meshManager.createMesh(overlayVAO, groomVertices, groomIndices);
            this.groomModel = modelManager.createModel(groomOverlay, overlayMaterialID);
            groomModel.getMaterial().setUniform(
                    CharacterEditorSetting.UNIFORM_DEPTH_BIAS, CharacterEditorSetting.OVERLAY_DEPTH_BIAS);
            return;
        }

        meshManager.updateMesh(groomOverlay, groomVertices, groomIndices);
        groomModel.updateMeshData(groomOverlay.getMeshData());
    }

    // A strand's root dot, its guide line through its points and its card's two sides, as the export lays the card,
    // carried by the strand's skinning where the view poses it (none in the bind pose)
    private void appendStrand(CharacterStrandStruct strand, Matrix4 skinning) {

        int rowFloats = CharacterStrandUtility.getRowFloats();
        int last = (EditorSetting.CHARACTER_HAIR_CARD_U.length - 1) * 3;

        setColor(strand.isSelected() ? CharacterEditorSetting.COLOR_SELECTED : CharacterEditorSetting.COLOR_STRAND);
        CharacterStrandUtility.resolveShownPoint(strand, 0, skinning, pointA);
        appendDot(groomVertices, groomIndices, pointA.x, pointA.y, pointA.z,
                CharacterEditorSetting.STRAND_ROOT_HALF_PIXELS);

        for (int i = 0; i + 1 < strand.getPointCount(); i++) {
            CharacterStrandUtility.resolveShownPoint(strand, i, skinning, pointA);
            CharacterStrandUtility.resolveShownPoint(strand, i + 1, skinning, pointB);
            appendLine(
                    groomVertices,
                    groomIndices,
                    pointA.x,
                    pointA.y,
                    pointA.z,
                    pointB.x,
                    pointB.y,
                    pointB.z,
                    CharacterEditorSetting.STRAND_HALF_PIXELS);
        }

        CharacterStrandUtility.resolveCardRows(strand, cardRows);
        setColor(CharacterEditorSetting.COLOR_STRAND_CARD);

        for (int i = 0; i + 1 < strand.getPointCount(); i++) {
            appendCardSide(i * rowFloats, (i + 1) * rowFloats, skinning);
            appendCardSide(i * rowFloats + last, (i + 1) * rowFloats + last, skinning);
        }
    }

    private void appendCardSide(int from, int to, Matrix4 skinning) {

        CharacterStrandUtility.resolveShownPoint(
                skinning, cardRows.getFloat(from), cardRows.getFloat(from + 1), cardRows.getFloat(from + 2), pointA);
        CharacterStrandUtility.resolveShownPoint(
                skinning, cardRows.getFloat(to), cardRows.getFloat(to + 1), cardRows.getFloat(to + 2), pointB);
        appendLine(
                groomVertices,
                groomIndices,
                pointA.x,
                pointA.y,
                pointA.z,
                pointB.x,
                pointB.y,
                pointB.z,
                CharacterEditorSetting.STRAND_CARD_HALF_PIXELS);
    }

    // Collider Overlay \\

    private void pushColliderOverlay(WindowInstance window) {

        syncColliderOverlay();

        if (colliderModel != null && !colliderIndices.isEmpty())
            renderManager.pushRenderCall(
                    colliderModel,
                    characterViewRenderSystem.getOverlayFbo(),
                    CharacterEditorSetting.DEPTH_GUIDES,
                    window);
    }

    // Rebuilds when the editor's state has moved, the rig's colliders and selection among it
    private void syncColliderOverlay() {

        if (characterEditorManager.getRevision() == builtColliderRevision)
            return;

        this.builtColliderRevision = characterEditorManager.getRevision();

        colliderVertices.clear();
        colliderIndices.clear();

        int selected = characterEditorManager.getSelectedRigColliderIndex();

        for (int c = 0; c < characterEditorManager.getRigColliderCount(); c++) {
            setColor(c == selected ? CharacterEditorSetting.COLOR_SELECTED : CharacterEditorSetting.COLOR_COLLIDER);
            appendCollider(c);
        }

        if (colliderIndices.isEmpty())
            return;

        if (colliderOverlay == null) {
            this.colliderOverlay = meshManager.createMesh(overlayVAO, colliderVertices, colliderIndices);
            this.colliderModel = modelManager.createModel(colliderOverlay, overlayMaterialID);
            colliderModel.getMaterial().setUniform(CharacterEditorSetting.UNIFORM_DEPTH_BIAS, 0f);
            return;
        }

        meshManager.updateMesh(colliderOverlay, colliderVertices, colliderIndices);
        colliderModel.updateMeshData(colliderOverlay.getMeshData());
    }

    // A sphere as three great circles; a capsule as a ring and two half circles at each end and four lines between
    private void appendCollider(int colliderIndex) {

        float radius = characterEditorManager.getRigColliderRadius(colliderIndex);
        float full = (float) (Math.PI * 2.0);

        characterEditorManager.resolveRigColliderPoint(colliderIndex, false, colliderStart);
        characterEditorManager.resolveRigColliderPoint(colliderIndex, true, colliderEnd);
        colliderAxis.set(colliderEnd).subtract(colliderStart);

        float length = colliderAxis.length();
        boolean capsule = characterEditorManager.isRigColliderCapsule(colliderIndex)
                && length > CharacterEditorSetting.COLLIDER_GUIDE_EPSILON;

        if (capsule)
            colliderAxis.divide(length);
        else
            colliderAxis.set(0f, 1f, 0f);

        resolveColliderSides();

        if (!capsule) {
            appendArc(colliderStart, colliderSide, colliderUp, radius, full);
            appendArc(colliderStart, colliderSide, colliderAxis, radius, full);
            appendArc(colliderStart, colliderUp, colliderAxis, radius, full);
            return;
        }

        float half = (float) Math.PI;

        appendArc(colliderStart, colliderSide, colliderUp, radius, full);
        appendArc(colliderEnd, colliderSide, colliderUp, radius, full);
        appendArc(colliderEnd, colliderSide, colliderAxis, radius, half);
        appendArc(colliderEnd, colliderUp, colliderAxis, radius, half);
        colliderAxis.multiply(-1f);
        appendArc(colliderStart, colliderSide, colliderAxis, radius, half);
        appendArc(colliderStart, colliderUp, colliderAxis, radius, half);
        appendSideLine(colliderSide, radius);
        appendSideLine(colliderSide, -radius);
        appendSideLine(colliderUp, radius);
        appendSideLine(colliderUp, -radius);
    }

    // Two directions square to the collider's axis and to each other, the first crossed from the up axis, or from
    // across when the collider stands upright
    private void resolveColliderSides() {

        float ax = colliderAxis.x;
        float ay = colliderAxis.y;
        float az = colliderAxis.z;
        boolean upright = Math.abs(ay) > CharacterEditorSetting.COLLIDER_GUIDE_UP_COSINE;
        float rx = upright ? 1f : 0f;
        float ry = upright ? 0f : 1f;

        colliderSide.set(-az * ry, az * rx, ax * ry - ay * rx);
        colliderSide.divide(colliderSide.length());
        colliderUp.set(
                ay * colliderSide.z - az * colliderSide.y,
                az * colliderSide.x - ax * colliderSide.z,
                ax * colliderSide.y - ay * colliderSide.x);
    }

    // The arc about a centre through the first direction toward the second, a full circle or a half
    private void appendArc(Vector3 centre, Vector3 from, Vector3 toward, float radius, float span) {

        int segments = Math.max(1, Math.round(
                CharacterEditorSetting.COLLIDER_GUIDE_SEGMENTS * span / (float) (Math.PI * 2.0)));

        for (int i = 0; i < segments; i++) {

            float a = span * i / segments;
            float b = span * (i + 1) / segments;
            float ca = (float) Math.cos(a) * radius;
            float sa = (float) Math.sin(a) * radius;
            float cb = (float) Math.cos(b) * radius;
            float sb = (float) Math.sin(b) * radius;

            appendLine(
                    colliderVertices,
                    colliderIndices,
                    centre.x + from.x * ca + toward.x * sa,
                    centre.y + from.y * ca + toward.y * sa,
                    centre.z + from.z * ca + toward.z * sa,
                    centre.x + from.x * cb + toward.x * sb,
                    centre.y + from.y * cb + toward.y * sb,
                    centre.z + from.z * cb + toward.z * sb,
                    CharacterEditorSetting.GUIDE_HALF_PIXELS);
        }
    }

    private void appendSideLine(Vector3 side, float reach) {
        appendLine(
                colliderVertices,
                colliderIndices,
                colliderStart.x + side.x * reach,
                colliderStart.y + side.y * reach,
                colliderStart.z + side.z * reach,
                colliderEnd.x + side.x * reach,
                colliderEnd.y + side.y * reach,
                colliderEnd.z + side.z * reach,
                CharacterEditorSetting.GUIDE_HALF_PIXELS);
    }

    // Stamp Overlay \\

    private void pushStampOverlay(WindowInstance window) {

        syncStampOverlay();

        if (stampModel != null && !stampIndices.isEmpty())
            renderManager.pushRenderCall(
                    stampModel,
                    characterViewRenderSystem.getOverlayFbo(),
                    CharacterEditorSetting.DEPTH_GUIDES,
                    window);
    }

    // Rebuilds when the placement, the editor's state or the body has moved
    private void syncStampOverlay() {

        int documentRevision = characterEditorMeshViewSystem.getActiveDocument().getRevision();

        if (characterEditorStampSystem.getRevision() == builtStampRevision
                && characterEditorManager.getRevision() == builtStampManagerRevision
                && documentRevision == builtStampDocumentRevision)
            return;

        this.builtStampRevision = characterEditorStampSystem.getRevision();
        this.builtStampManagerRevision = characterEditorManager.getRevision();
        this.builtStampDocumentRevision = documentRevision;

        stampVertices.clear();
        stampIndices.clear();

        CharacterPartPlacementStruct placement = characterEditorStampSystem.getPlacement();
        CharacterPartPlacementStruct twin = characterEditorStampSystem.getTwin();

        appendAttachPoints(placement);

        if (placement.isShown())
            appendGhost(placement);

        if (twin != null)
            appendGhost(twin);

        if (stampIndices.isEmpty())
            return;

        if (stampOverlay == null) {
            this.stampOverlay = meshManager.createMesh(overlayVAO, stampVertices, stampIndices);
            this.stampModel = modelManager.createModel(stampOverlay, overlayMaterialID);
            stampModel.getMaterial().setUniform(CharacterEditorSetting.UNIFORM_DEPTH_BIAS, 0f);
            return;
        }

        meshManager.updateMesh(stampOverlay, stampVertices, stampIndices);
        stampModel.updateMeshData(stampOverlay.getMeshData());
    }

    // Every attach point as a yellow dot, the one the part aimed joins nearest larger
    private void appendAttachPoints(CharacterPartPlacementStruct placement) {

        int active = placement.isShown() ? placement.getAttachPoint() : EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < characterEditorManager.getAttachPointCount(); i++) {

            characterEditorManager.getAttachPoint(i, characterEditorMeshViewSystem.getSkinningMatrices(), pointA);
            setColor(i == active ? CharacterEditorSetting.COLOR_ATTACH_ACTIVE : CharacterEditorSetting.COLOR_ATTACH);
            appendDot(
                    stampVertices,
                    stampIndices,
                    pointA.x,
                    pointA.y,
                    pointA.z,
                    i == active
                            ? CharacterEditorSetting.ATTACH_ACTIVE_HALF_PIXELS
                            : CharacterEditorSetting.ATTACH_HALF_PIXELS);
        }
    }

    // The part's edges carried to where it would join, or every fur card's where it would lie, as the view poses the
    // body, green where it can and red where it cannot
    private void appendGhost(CharacterPartPlacementStruct placement) {

        syncGhostEdges(placement.getStampHandle());
        setColor(placement.isValid()
                ? CharacterEditorSetting.COLOR_STAMP_VALID
                : CharacterEditorSetting.COLOR_STAMP_INVALID);

        if (!placement.getStampHandle().hasFur()) {
            ghostScratch.set(placement.getPosed()).multiply(placement.getTransform());
            appendGhostEdges(placement.getStampHandle().getMesh(), ghostScratch);
            return;
        }

        for (int c = 0; c < placement.getFurCount(); c++) {
            characterEditorManager.resolveFurFrame(
                    placement, c, characterEditorMeshViewSystem.getSkinningMatrices(), ghostScratch);
            appendGhostEdges(placement.getStampHandle().getMesh(), ghostScratch);
        }
    }

    // A stamp's edges carried by a transform
    private void appendGhostEdges(RiggedMeshStruct stampMesh, Matrix4 transform) {

        for (int i = 0; i < ghostEdges.size(); i += 2) {

            int a = ghostEdges.getInt(i);
            int b = ghostEdges.getInt(i + 1);

            CharacterPoseUtility.transformPoint(
                    transform, stampMesh.getX(a), stampMesh.getY(a), stampMesh.getZ(a), pointA);
            CharacterPoseUtility.transformPoint(
                    transform, stampMesh.getX(b), stampMesh.getY(b), stampMesh.getZ(b), pointB);

            appendLine(
                    stampVertices,
                    stampIndices,
                    pointA.x,
                    pointA.y,
                    pointA.z,
                    pointB.x,
                    pointB.y,
                    pointB.z,
                    CharacterEditorSetting.STAMP_EDGE_HALF_PIXELS);
        }
    }

    // Every edge of a stamp's mesh once, found again only for another stamp or one rebuilt
    private void syncGhostEdges(StampHandle stampHandle) {

        if (stampHandle == ghostStamp && stampManager.getRevision() == ghostStampRevision)
            return;

        this.ghostStamp = stampHandle;
        this.ghostStampRevision = stampManager.getRevision();

        RiggedMeshStruct stampMesh = stampHandle.getMesh();

        edges.clear();
        ghostEdges.clear();

        for (int f = 0; f < stampMesh.getFaceCount(); f++)
            for (int c = 0; c < stampMesh.getFaceCornerCount(f); c++) {

                int a = stampMesh.getFaceVertex(f, c);
                int b = stampMesh.getFaceVertex(f, stampMesh.getNextCorner(f, c));

                if (a != b && edges.add(RiggedMeshStruct.toEdgeKey(a, b))) {
                    ghostEdges.add(a);
                    ghostEdges.add(b);
                }
            }
    }

    // Mesh Overlay \\

    // Rebuilds when the part, its geometry or selection, the editor's state or a playing pose has moved
    private void syncMeshOverlay() {

        CharacterMeshDocumentInstance document = characterEditorMeshViewSystem.getActiveDocument();
        boolean posePlayed = characterEditorManager.isPosePlaying();
        boolean dots = characterEditorManager.getMode().showsVertices();
        boolean wireframe = characterEditorManager.isWireframe();

        if (document == builtDocument
                && document.getRevision() == builtRevision
                && characterEditorManager.getRevision() == builtManagerRevision
                && dots == builtDots
                && wireframe == builtWireframe
                && !posePlayed)
            return;

        characterEditorMeshViewSystem.refresh();
        document = characterEditorMeshViewSystem.getActiveDocument();

        if (document == null)
            return;

        this.builtDocument = document;
        this.builtRevision = document.getRevision();
        this.builtManagerRevision = characterEditorManager.getRevision();
        this.builtDots = dots;
        this.builtWireframe = wireframe;

        buildMeshOverlay(document, dots, wireframe);

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

    // The vertices' dots where the mode shows them and the edges while Wireframe is on
    private void buildMeshOverlay(CharacterMeshDocumentInstance document, boolean dots, boolean wireframe) {

        RiggedMeshStruct mesh = document.getMesh();
        boolean weights = characterEditorManager.getMode() == CharacterEditorMode.WEIGHTS;

        vertices.clear();
        indices.clear();
        vertices.ensureCapacity((mesh.getVertexCount() + mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT)
                * EngineSetting.QUAD_VERTEX_COUNT * CharacterEditorSetting.OVERLAY_VERTEX_FLOATS);

        if (dots)
            for (int v = 0; v < mesh.getVertexCount(); v++)
                if (characterEditorMeshViewSystem.isInRegion(v))
                    appendVertexDot(document, v, weights);

        if (wireframe)
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
            if (vertexIndex == document.getActiveVertex())
                setColor(CharacterEditorSetting.COLOR_ACTIVE);
            else
                mixColor(
                        CharacterEditorSetting.COLOR_VERTEX,
                        CharacterEditorSetting.COLOR_SELECTED,
                        document.getSelectionWeight(vertexIndex));
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

    // Every edge of every face once, brighter where both of its ends are selected, a seam in its own colour and wider
    private void appendEdges(CharacterMeshDocumentInstance document, boolean weights) {

        RiggedMeshStruct mesh = document.getMesh();

        edges.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, mesh.getNextCorner(f, c));

                if (a == b || !characterEditorMeshViewSystem.isInRegion(a)
                        || !characterEditorMeshViewSystem.isInRegion(b)
                        || !edges.add(RiggedMeshStruct.toEdgeKey(a, b)))
                    continue;

                boolean seam = !weights && mesh.isSeam(a, b);

                if (seam)
                    setColor(CharacterEditorSetting.COLOR_SEAM);
                else
                    setColor(!weights && document.isSelected(a) && document.isSelected(b)
                            ? CharacterEditorSetting.COLOR_EDGE_SELECTED
                            : CharacterEditorSetting.COLOR_EDGE);

                appendLine(
                        vertices,
                        indices,
                        characterEditorMeshViewSystem.getX(a),
                        characterEditorMeshViewSystem.getY(a),
                        characterEditorMeshViewSystem.getZ(a),
                        characterEditorMeshViewSystem.getX(b),
                        characterEditorMeshViewSystem.getY(b),
                        characterEditorMeshViewSystem.getZ(b),
                        seam ? CharacterEditorSetting.SEAM_HALF_PIXELS : CharacterEditorSetting.EDGE_HALF_PIXELS);
            }
    }

    // A bone's weight as heat: blue for none through green to red for all of it
    private void resolveHeat(float weight) {

        float[] low = weight < 0.5f ? CharacterEditorSetting.COLOR_HEAT_NONE : CharacterEditorSetting.COLOR_HEAT_HALF;
        float[] high = weight < 0.5f ? CharacterEditorSetting.COLOR_HEAT_HALF : CharacterEditorSetting.COLOR_HEAT_FULL;
        float t = weight < 0.5f ? weight * 2f : (weight - 0.5f) * 2f;

        mixColor(low, high, t);
    }

    private void mixColor(float[] from, float[] to, float t) {

        for (int i = 0; i < colorScratch.length; i++)
            colorScratch[i] = from[i] + (to[i] - from[i]) * t;
    }

    private void setColor(float[] color) {
        System.arraycopy(color, 0, colorScratch, 0, colorScratch.length);
    }

    // Guides \\

    // The box being dragged, the brush, a held transform's axis and proportional radius; false when none shows
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

        if (characterEditorSculptSystem.isProportionalShown())
            appendProportionalGuide();

        if (characterEditorEdgeSystem.hasHoveredEdge())
            appendEdgeGuide();

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

    // The edge of the preset's tip, each point of the circle pulled in to where its turned, squashed shape measures
    // one radius
    private void appendBrushGuide(float mouseX, float mouseY) {

        float radius = characterEditorManager.getBrushRadius();
        BrushHandle brush = brushManager.getActiveBrush();
        int segments = CharacterEditorSetting.BRUSH_GUIDE_SEGMENTS;

        setColor(CharacterEditorSetting.COLOR_GUIDE);

        for (int i = 0; i < segments; i++) {

            float fromX = (float) Math.cos(Math.PI * 2.0 * i / segments);
            float fromY = (float) Math.sin(Math.PI * 2.0 * i / segments);
            float toX = (float) Math.cos(Math.PI * 2.0 * (i + 1) / segments);
            float toY = (float) Math.sin(Math.PI * 2.0 * (i + 1) / segments);
            float fromReach = radius / BrushUtility.measure(brush, fromX, fromY);
            float toReach = radius / BrushUtility.measure(brush, toX, toY);

            appendScreenLine(
                    mouseX + fromX * fromReach,
                    mouseY + fromY * fromReach,
                    mouseX + toX * toReach,
                    mouseY + toY * toReach);
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

    // The proportional radius as a circle on screen about the pivot, as Blender draws it while a transform is held:
    // as wide as the radius measures at the pivot's depth
    private void appendProportionalGuide() {

        Vector3 pivot = characterEditorSculptSystem.getPivot();
        Vector3 view = context.getWindow().getActiveCamera().getDirection();
        Vector3 up = context.getWindow().getActiveCamera().getUp();
        float radius = characterEditorManager.getProportionalRadius();
        int segments = CharacterEditorSetting.PROPORTIONAL_GUIDE_SEGMENTS;

        pointA.set(view.y * up.z - view.z * up.y, view.z * up.x - view.x * up.z, view.x * up.y - view.y * up.x)
                .normalize();
        pointB.set(pivot).add(pointA.x * radius, pointA.y * radius, pointA.z * radius);

        if (!characterEditorMeshViewSystem.project(pivot.x, pivot.y, pivot.z, screenCenter)
                || !characterEditorMeshViewSystem.project(pointB.x, pointB.y, pointB.z, screenEdge))
            return;

        float reach = (float) Math.hypot(screenEdge.x - screenCenter.x, screenEdge.y - screenCenter.y);
        float centerX = screenCenter.x;
        float centerY = screenCenter.y;

        setColor(CharacterEditorSetting.COLOR_GUIDE);

        for (int i = 0; i < segments; i++) {

            double from = Math.PI * 2.0 * i / segments;
            double to = Math.PI * 2.0 * (i + 1) / segments;

            appendScreenLine(
                    centerX + (float) Math.cos(from) * reach,
                    centerY + (float) Math.sin(from) * reach,
                    centerX + (float) Math.cos(to) * reach,
                    centerY + (float) Math.sin(to) * reach);
        }
    }

    // The edge an edge tool would act on, brighter and wider than the wireframe
    private void appendEdgeGuide() {

        int a = characterEditorEdgeSystem.getHoveredVertex(false);
        int b = characterEditorEdgeSystem.getHoveredVertex(true);

        setColor(CharacterEditorSetting.COLOR_EDGE_HOVER);
        appendLine(
                guideVertices,
                guideIndices,
                characterEditorMeshViewSystem.getX(a),
                characterEditorMeshViewSystem.getY(a),
                characterEditorMeshViewSystem.getZ(a),
                characterEditorMeshViewSystem.getX(b),
                characterEditorMeshViewSystem.getY(b),
                characterEditorMeshViewSystem.getZ(b),
                CharacterEditorSetting.EDGE_HOVER_HALF_PIXELS);
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
    private void appendDot(FloatArrayList target, IntArrayList targetIndices, float x, float y, float z, float half) {

        int base = target.size() / CharacterEditorSetting.OVERLAY_VERTEX_FLOATS;

        putVertex(target, x, y, z, x, y, z, -half, -half);
        putVertex(target, x, y, z, x, y, z, half, -half);
        putVertex(target, x, y, z, x, y, z, half, half);
        putVertex(target, x, y, z, x, y, z, -half, half);
        putQuad(targetIndices, base);
    }

    // A strip between two points, each end pushed its half width in pixels across the line by the shader; across
    // flips with the line's direction, so the far end's sides are named the other way round
    private void appendLine(
            FloatArrayList target,
            IntArrayList targetIndices,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float half) {

        int base = target.size() / CharacterEditorSetting.OVERLAY_VERTEX_FLOATS;

        putVertex(target, ax, ay, az, bx, by, bz, half, 0f);
        putVertex(target, ax, ay, az, bx, by, bz, -half, 0f);
        putVertex(target, bx, by, bz, ax, ay, az, half, 0f);
        putVertex(target, bx, by, bz, ax, ay, az, -half, 0f);
        putQuad(targetIndices, base);
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

    private void putQuad(IntArrayList targetIndices, int base) {
        targetIndices.add(base);
        targetIndices.add(base + 1);
        targetIndices.add(base + 2);
        targetIndices.add(base + 2);
        targetIndices.add(base + 3);
        targetIndices.add(base);
    }
}
