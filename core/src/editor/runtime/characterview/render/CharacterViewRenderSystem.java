package editor.runtime.characterview.render;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.EntityRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewRenderSystem extends SystemPackage {

    /*
     * Draws a character viewport: the character CharacterViewPoseSystem holds,
     * lit by the viewport's own material so it never batches with a game
     * view's characters, standing on a floor grid in this window's scene
     * target. The markers its tool asks for during update — bone joints and
     * oriented boxes on a bone — go to an overlay above the scene, so they
     * show through the body. Marker models are pooled and only ever grow.
     * Tools drawing more of their own into the viewport use the same scene
     * and overlay targets.
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

    // Markers
    private ObjectArrayList<ModelInstance> markerModels;
    private ObjectArrayList<ModelInstance> selectedMarkerModels;
    private int markerCount;
    private int selectedMarkerCount;

    // Scratch
    private Matrix4 modelMatrix;
    private Matrix4 markerScratch;
    private Matrix4 localScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;
    private Vector3 sizeScratch;

    // Base \\

    @Override
    protected void create() {

        // Markers
        this.markerModels = new ObjectArrayList<>();
        this.selectedMarkerModels = new ObjectArrayList<>();

        // Scratch
        this.modelMatrix = new Matrix4();
        this.markerScratch = new Matrix4();
        this.localScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
        this.sizeScratch = new Vector3();
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

    // Markers \\

    // A cube at a bone's joint, turned with the bone
    public void addJointMarker(int boneIndex, boolean selected) {

        float size = selected
                ? EditorSetting.CHARACTER_VIEW_JOINT_SELECTED_SIZE
                : EditorSetting.CHARACTER_VIEW_JOINT_SIZE;

        sizeScratch.set(size, size, size);
        RigMathUtility.setScale(matrixScratchA, sizeScratch);
        markerScratch.set(characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex));
        markerScratch.multiply(matrixScratchA);

        pushMarker(selected);
    }

    // A box placed on a bone in the character's bind pose, as an equipment anchor places what it holds
    public void addBoxMarker(int boneIndex, Vector3 position, Vector3 rotation, Vector3 size, boolean selected) {

        AnimationStateHandle pose = characterViewPoseSystem.getAnimationStateHandle();

        RigMathUtility.composeLocal(position, rotation, size, localScratch, matrixScratchA, matrixScratchB);
        markerScratch.set(pose.getSkinningMatrix(boneIndex)).multiply(localScratch);

        pushMarker(selected);
    }

    private void pushMarker(boolean selected) {

        ModelInstance marker = selected
                ? acquireMarker(selectedMarkerModels, selectedMarkerCount++,
                        EditorSetting.CHARACTER_VIEW_MATERIAL_MARKER_SELECTED)
                : acquireMarker(markerModels, markerCount++, EditorSetting.CHARACTER_VIEW_MATERIAL_MARKER);

        marker.getMaterial().setUniform(EditorSetting.CHARACTER_VIEW_UNIFORM_MODEL, markerScratch);
        renderManager.pushRenderCall(
                marker,
                overlayFbo,
                selected ? EditorSetting.CHARACTER_VIEW_DEPTH_SELECTED : EditorSetting.CHARACTER_VIEW_DEPTH_MARKER,
                context.getWindow());
    }

    private ModelInstance acquireMarker(ObjectArrayList<ModelInstance> pool, int index, String materialName) {

        if (index == pool.size())
            pool.add(createModel(materialName, EditorSetting.CHARACTER_VIEW_MESH_MARKER));

        return pool.get(index);
    }

    // Render \\

    @Override
    protected void render() {

        WindowInstance window = context.getWindow();

        if (characterViewPoseSystem.hasSubject())
            entityRenderSystem.pushPreview(
                    characterViewPoseSystem.getEntityData(),
                    characterViewPoseSystem.getAppearanceHandle(),
                    characterViewPoseSystem.getAnimationStateHandle().getSkinningMatrices(),
                    modelMatrix,
                    characterMaterial,
                    sceneFbo,
                    window);

        renderManager.pushRenderCall(gridModel, sceneFbo, EditorSetting.CHARACTER_VIEW_DEPTH_GRID, window);
        renderManager.ensureFboRendered(overlayFbo, window);

        fboRenderSystem.pushFbo(sceneFbo, RuntimeSetting.LAYER_WORLD, window);
        fboRenderSystem.pushFbo(overlayFbo, EditorSetting.CHARACTER_VIEW_LAYER_OVERLAY, window);

        markerCount = 0;
        selectedMarkerCount = 0;
    }

    // Accessible \\

    public FBOInstance getSceneFbo() {
        return sceneFbo;
    }

    public FBOInstance getOverlayFbo() {
        return overlayFbo;
    }
}
