package editor.runtime.characterview.render;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceIsolationStruct;
import application.bootstrap.entitypipeline.entitymanager.EntityRenderSystem;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneDisplay;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneKind;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewRenderSystem extends SystemPackage {

    /*
     * Draws a character viewport: the character CharacterViewPoseSystem holds,
     * lit by the viewport's own material, and its eyes and teeth by its own
     * copies of theirs, so it never batches with a game view's characters or
     * another viewport's, standing on a floor grid in this window's scene
     * target. The markers its tool asks for during update — a rig's bones
     * in the Bone Display's style and colours (addBones, the one path every
     * viewport draws bones through), bone joints, oriented boxes on a bone
     * and cubes or boxes placed by any matrix — go to an overlay above the
     * scene, so they show through the body. Marker models are pooled by
     * shape and only ever grow.
     * Tools drawing more of their own into the viewport use the same scene
     * and overlay targets; a part editor narrows what of the character is
     * drawn through the viewport's isolation.
     */

    // Internal
    private CharacterViewPoseSystem characterViewPoseSystem;
    private EntityRenderSystem entityRenderSystem;
    private RenderManager renderManager;
    private ModelManager modelManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private FBOInstance sceneFbo;
    private FBOInstance overlayFbo;

    // Models
    private ModelInstance gridModel;
    private MaterialInstance characterMaterial;
    private Object2ObjectOpenHashMap<MaterialInstance, MaterialInstance> featureMaterial2ViewMaterial;
    private AppearanceIsolationStruct isolation;

    // Markers — one pool of models per shape and per selected or not, by shape * 2 + selected
    private ObjectArrayList<ObjectArrayList<ModelInstance>> markerPools;
    private int[] markerCounts;
    private Vector4 markerColor;

    // Bones — whether each bone shown has a child shown, reused every frame
    private BooleanArrayList boneParents;

    // Scratch
    private Matrix4 modelMatrix;
    private Matrix4 markerScratch;
    private Matrix4 localScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;
    private Vector3 sizeScratch;
    private Vector3 stickAlong;
    private Vector3 stickSide;
    private Vector3 stickNormal;

    // Base \\

    @Override
    protected void create() {

        // Models
        this.featureMaterial2ViewMaterial = new Object2ObjectOpenHashMap<>();
        this.isolation = new AppearanceIsolationStruct();

        // Markers
        int poolCount = EditorSetting.CHARACTER_VIEW_MARKER_MESHES.length * 2;
        float[] color = EditorSetting.CHARACTER_VIEW_MARKER_COLOR;

        this.markerPools = new ObjectArrayList<>(poolCount);
        this.markerCounts = new int[poolCount];
        this.markerColor = new Vector4(color[0], color[1], color[2], color[3]);

        for (int i = 0; i < poolCount; i++)
            markerPools.add(new ObjectArrayList<>());

        // Bones
        this.boneParents = new BooleanArrayList();

        // Scratch
        this.modelMatrix = new Matrix4();
        this.markerScratch = new Matrix4();
        this.localScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
        this.sizeScratch = new Vector3();
        this.stickAlong = new Vector3();
        this.stickSide = new Vector3();
        this.stickNormal = new Vector3();
    }

    @Override
    protected void get() {
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.entityRenderSystem = get(EntityRenderSystem.class);
        this.renderManager = get(RenderManager.class);
        this.modelManager = get(ModelManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        this.sceneFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_SCENE, window);
        this.overlayFbo = fboManager.cloneFbo(EditorSetting.FBO_EDITOR_OVERLAY, window);
        this.gridModel = createModel(
                EditorSetting.CHARACTER_VIEW_MATERIAL_GRID,
                EditorSetting.CHARACTER_VIEW_MESH_GRID);
        this.characterMaterial = materialManager.cloneMaterial(EditorSetting.CHARACTER_VIEW_MATERIAL_CHARACTER);
    }

    private ModelInstance createModel(String materialName, String meshName) {
        return modelManager.createModel(
                meshManager.getMeshHandleFromMeshName(meshName),
                materialManager.getMaterialIDFromMaterialName(materialName));
    }

    // Bones \\

    // Every bone the source shows, as the display says: a segment from each bone's joint to every child's, a sphere at
    // a root's or a childless bone's joint, and with envelopes a sphere at every joint as wide as the bone; each in its
    // kind's colour, the selected bone in the selected colour
    public void addBones(CharacterBoneSource source, CharacterBoneDisplay display, Vector4[] kindColors) {

        if (!display.drawsBones())
            return;

        int boneCount = source.getBoneCount();
        int selected = source.getSelectedBone();

        boneParents.size(boneCount);

        for (int i = 0; i < boneCount; i++)
            boneParents.set(i, false);

        for (int i = 0; i < boneCount; i++) {

            int parent = source.getBoneParent(i);

            if (parent != EngineSetting.INDEX_NOT_FOUND && source.isBoneShown(i) && source.isBoneShown(parent))
                boneParents.set(parent, true);
        }

        for (int i = 0; i < boneCount; i++) {

            if (!source.isBoneShown(i))
                continue;

            int parent = source.getBoneParent(i);
            boolean parented = parent != EngineSetting.INDEX_NOT_FOUND && source.isBoneShown(parent);
            Vector4 color = kindColors[resolveKind(source, i).ordinal()];

            if (parented)
                addSegment(
                        source.getBoneWorldMatrix(parent),
                        source.getBoneWorldMatrix(i),
                        source.getBoneSize(parent),
                        display,
                        kindColors[resolveKind(source, parent).ordinal()],
                        parent == selected);

            if (display.drawsEnvelopes())
                addSphere(source.getBoneWorldMatrix(i),
                        source.getBoneSize(i) * EditorSetting.BONE_DISPLAY_ENVELOPE_SIZE_SHARE, color, i == selected);
            else if (!parented || !boneParents.getBoolean(i))
                addSphere(source.getBoneWorldMatrix(i), i == selected
                        ? EditorSetting.CHARACTER_VIEW_JOINT_SELECTED_SIZE
                        : EditorSetting.CHARACTER_VIEW_JOINT_SIZE, color, i == selected);
        }
    }

    // Hair for a lock bone, Face for one below a part taking the face, Body otherwise
    private CharacterBoneKind resolveKind(CharacterBoneSource source, int boneIndex) {

        if (source.getBoneName(boneIndex).startsWith(EditorSetting.CHARACTER_HAIR_LOCK_PREFIX))
            return CharacterBoneKind.HAIR;

        for (int bone = source.getBoneParent(boneIndex);
                bone != EngineSetting.INDEX_NOT_FOUND;
                bone = source.getBoneParent(bone))
            if (source.isFacialPartBone(bone))
                return CharacterBoneKind.FACE;

        return CharacterBoneKind.BODY;
    }

    // A sphere of a diameter in blocks at a joint
    private void addSphere(Matrix4 worldMatrix, float size, Vector4 color, boolean selected) {

        sizeScratch.set(size, size, size);
        RigMathUtility.setScale(matrixScratchA, sizeScratch);
        markerScratch.set(worldMatrix).multiply(matrixScratchA);

        pushMarker(EditorSetting.CHARACTER_VIEW_MARKER_SPHERE, selected, color);
    }

    // Markers \\

    // A cube at a bone's joint, turned with the bone
    public void addJointMarker(int boneIndex, boolean selected) {
        addWorldMarker(
                characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex),
                selected ? EditorSetting.CHARACTER_VIEW_JOINT_SELECTED_SIZE : EditorSetting.CHARACTER_VIEW_JOINT_SIZE,
                selected);
    }

    // A cube of a size in blocks where a matrix places it, turned with it
    public void addWorldMarker(Matrix4 worldMatrix, float size, boolean selected) {

        sizeScratch.set(size, size, size);
        RigMathUtility.setScale(matrixScratchA, sizeScratch);
        markerScratch.set(worldMatrix).multiply(matrixScratchA);

        pushMarker(EditorSetting.CHARACTER_VIEW_MARKER_CUBE, selected, markerColor);
    }

    // A box of a size in blocks centred where a matrix places it, turned with it
    public void addWorldBoxMarker(Matrix4 worldMatrix, Vector3 size) {

        RigMathUtility.setScale(matrixScratchA, size);
        markerScratch.set(worldMatrix).multiply(matrixScratchA);

        pushMarker(EditorSetting.CHARACTER_VIEW_MARKER_CUBE, false, markerColor);
    }

    // A bone's segment from one joint to another in the display's shape, as wide as the display makes a bone of its
    // length and size, its head at the first joint; nothing for two joints on one spot
    private void addSegment(
            Matrix4 fromWorld,
            Matrix4 toWorld,
            float size,
            CharacterBoneDisplay display,
            Vector4 color,
            boolean selected) {

        float[] from = fromWorld.val;
        float[] to = toWorld.val;
        float dx = to[12] - from[12];
        float dy = to[13] - from[13];
        float dz = to[14] - from[14];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length <= EditorSetting.CHARACTER_VIEW_STICK_MIN_LENGTH)
            return;

        float width = display.resolveWidth(length, size);
        boolean upright = Math.abs(dy / length) > EditorSetting.CHARACTER_VIEW_STICK_PARALLEL;

        stickAlong.set(dx / length, dy / length, dz / length);
        cross(upright ? 1f : 0f, upright ? 0f : 1f, 0f, stickAlong, stickSide);
        stickSide.normalize();
        cross(stickAlong.x, stickAlong.y, stickAlong.z, stickSide, stickNormal);
        markerScratch.set(
                stickSide.x * width, dx, stickNormal.x * width, from[12] + dx * 0.5f,
                stickSide.y * width, dy, stickNormal.y * width, from[13] + dy * 0.5f,
                stickSide.z * width, dz, stickNormal.z * width, from[14] + dz * 0.5f,
                0f, 0f, 0f, 1f);

        pushMarker(display.getSegmentShape(), selected, color);
    }

    // The cross product of a direction and a vector
    private void cross(float x, float y, float z, Vector3 other, Vector3 out) {
        out.set(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
    }

    // A box placed on a bone in the character's bind pose, as an equipment anchor places what it holds
    public void addBoxMarker(int boneIndex, Vector3 position, Vector3 rotation, Vector3 size, boolean selected) {

        AnimationStateHandle pose = characterViewPoseSystem.getAnimationStateHandle();

        RigMathUtility.composeLocal(position, rotation, size, localScratch, matrixScratchA, matrixScratchB);
        markerScratch.set(pose.getSkinningMatrix(boneIndex)).multiply(localScratch);

        pushMarker(EditorSetting.CHARACTER_VIEW_MARKER_CUBE, selected, markerColor);
    }

    // The marker matrix drawn in a shape, its colour given unless selected, which keeps the selected material's own
    private void pushMarker(int shape, boolean selected, Vector4 color) {

        int pool = shape * 2 + (selected ? 1 : 0);
        ModelInstance marker = acquireMarker(markerPools.get(pool), markerCounts[pool]++, shape, selected);

        marker.getMaterial().setUniform(EditorSetting.CHARACTER_VIEW_UNIFORM_MODEL, markerScratch);

        if (!selected)
            marker.getMaterial().setUniform(EditorSetting.CHARACTER_VIEW_UNIFORM_COLOR, color);

        renderManager.pushRenderCall(
                marker,
                overlayFbo,
                selected ? EditorSetting.CHARACTER_VIEW_DEPTH_SELECTED : EditorSetting.CHARACTER_VIEW_DEPTH_MARKER,
                context.getWindow());
    }

    private ModelInstance acquireMarker(ObjectArrayList<ModelInstance> pool, int index, int shape, boolean selected) {

        if (index == pool.size())
            pool.add(createModel(
                    selected
                            ? EditorSetting.CHARACTER_VIEW_MATERIAL_MARKER_SELECTED
                            : EditorSetting.CHARACTER_VIEW_MATERIAL_MARKER,
                    EditorSetting.CHARACTER_VIEW_MARKER_MESHES[shape]));

        return pool.get(index);
    }

    // Render \\

    @Override
    protected void render() {

        WindowInstance window = context.getWindow();

        if (characterViewPoseSystem.hasSubject())
            entityRenderSystem.pushPreview(
                    characterViewPoseSystem.getRaceGender(),
                    characterViewPoseSystem.getAppearanceHandle(),
                    characterViewPoseSystem.getAnimationStateHandle().getSkinningMatrices(),
                    modelMatrix,
                    characterMaterial,
                    isolation,
                    featureMaterial2ViewMaterial,
                    sceneFbo,
                    window);

        renderManager.pushRenderCall(gridModel, sceneFbo, EditorSetting.CHARACTER_VIEW_DEPTH_GRID, window);
        renderManager.ensureFboRendered(overlayFbo, window);

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
        fboRenderSystem.pushFbo(overlayFbo, EditorSetting.CHARACTER_VIEW_LAYER_OVERLAY, window);

        for (int i = 0; i < markerCounts.length; i++)
            markerCounts[i] = 0;
    }

    // Accessible \\

    // What of the character this viewport draws, everything until a part editor isolates its part
    public AppearanceIsolationStruct getIsolation() {
        return isolation;
    }

    public FBOInstance getSceneFbo() {
        return sceneFbo;
    }

    public FBOInstance getOverlayFbo() {
        return overlayFbo;
    }
}
