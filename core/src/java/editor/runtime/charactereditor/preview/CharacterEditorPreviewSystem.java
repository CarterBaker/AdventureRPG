package editor.runtime.charactereditor.preview;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterBoneSource;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorPreviewSystem extends SystemPackage implements CharacterBoneSource {

    /*
     * Drives a character tool tab's viewport from the character editor. A new
     * preview of the active race is put on the animation tree of the gender
     * it is made as; that gender — the model it requires, moving by its own
     * tree, and the features it wears — the sliders,
     * the tone worn, the build and the head shape are applied whenever the
     * editor's state moves; and each frame the preview is posed — in the
     * bind pose, one of the poses the rig names, or played in the chosen
     * movement, held in the exact bind pose while its rig is edited, and
     * played with its springs simulated while Preview Springs is on — with
     * the edit pose's turns laid over it on the mesh and weights layers, and
     * given the markers of the mode: the bones in Mesh and Weights,
     * the rig's bones as they are being edited in Rig, each drawn as the
     * Properties tab's Bone Display says (CharacterBoneSource, answering for
     * whichever of the two is drawn), every equipment anchor in Equipment, the
     * selected one highlighted.
     * The window's own mode decides all of it, so two tool tabs each show
     * the character their own way side by side.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private AnimationManager animationManager;

    // Bound
    private CharacterDocumentInstance boundDocument;
    private int boundRevision;
    private int lookRevision;
    private CharacterEditorMode lookMode;

    // Bones — what the viewport's bones are read from while they are drawn: the rig being edited at its bind joints,
    // or the posed character, and the region showing them
    private boolean drawsEditedRig;
    private CharacterRegionStruct boneRegion;

    // Scratch
    private Vector3 heldSize;
    private Vector3 restTurn;

    // Base \\

    @Override
    protected void create() {

        // Bound
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        float size = CharacterEditorSetting.HELD_ANCHOR_MARKER_SIZE;
        this.heldSize = new Vector3(size, size, size);
        this.restTurn = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        CharacterDocumentInstance document = characterEditorManager.getActiveDocument();

        if (document == null || !document.hasPreview()) {
            characterViewPoseSystem.clear();
            this.boundDocument = null;
            return;
        }

        if (document != boundDocument || document.getPreviewRevision() != boundRevision)
            bind(document);

        if (characterEditorManager.getRevision() != lookRevision
                || characterEditorModeSystem.getMode() != lookMode)
            applyLook();

        applyEditPose(characterEditorModeSystem.getMode().isMeshMode()
                && characterEditorModeSystem.getMode().picksPose());
        strikePose();
        pushMarkers();
    }

    private void bind(CharacterDocumentInstance document) {

        characterViewPoseSystem.bind(document.getPreviewData(), characterEditorManager.getPreviewGenderID());
        this.boundDocument = document;
        this.boundRevision = document.getPreviewRevision();
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // Look \\

    private void applyLook() {

        this.lookRevision = characterEditorManager.getRevision();
        this.lookMode = characterEditorModeSystem.getMode();
        characterViewPoseSystem.setGender(characterEditorManager.getPreviewGenderID());

        if (characterViewPoseSystem.hasAppearance())
            characterEditorManager.applyLook(
                    characterViewPoseSystem.getAppearanceHandle(),
                    characterViewPoseSystem.getRaceData().getAppearanceData(),
                    lookMode.isMeshMode(),
                    false);
    }

    // Pose \\

    // The pose the mode stands in, or the pose chosen: the bind pose exactly as the mesh file holds it while a model
    // is edited and Bind is chosen, one of the rig's own poses struck still, or a movement played; the rig is edited in
    // the bind pose alone, and Preview Springs plays the movement chosen on a modelling layer, idle at Bind, with its
    // springs simulated
    private void strikePose() {

        CharacterEditorMode mode = characterEditorModeSystem.getMode();
        CharacterEditorPose pose = mode.getStandingPose() != null
                ? mode.getStandingPose()
                : characterEditorManager.getPose();
        boolean springs = mode.isModeling() && characterEditorManager.isSpringPreview();

        if (springs) {
            play(pose.isPlayed() ? pose : CharacterEditorPose.IDLE);
            return;
        }

        if (mode.holdsBindPose()) {
            characterViewPoseSystem.poseBind();
            return;
        }

        int clipIndex = mode.getStandingPose() == null
                ? characterEditorManager.getPoseClipIndex()
                : EngineSetting.INDEX_NOT_FOUND;

        if (clipIndex != EngineSetting.INDEX_NOT_FOUND && strikeClip(clipIndex, mode.isMeshMode()))
            return;

        if (pose.isPlayed())
            play(pose);
        else if (mode.isMeshMode())
            characterViewPoseSystem.poseBind();
        else
            characterViewPoseSystem.poseRest();
    }

    // The edit pose's turn on every bone where the layer takes it, none elsewhere, laid over whatever pose is struck
    private void applyEditPose(boolean posed) {

        int boneCount = characterViewPoseSystem.getRaceData().getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++)
            characterViewPoseSystem.setPosture(i, posed ? characterEditorManager.getEditPoseTurn(i) : restTurn);
    }

    // One of the rig's own poses, its clip struck at its start, unshaped while a model is edited; false when the rig
    // names a clip that is not loaded or moves another rig
    private boolean strikeClip(int clipIndex, boolean unshaped) {

        RigHandle rigHandle = characterViewPoseSystem.getRaceData().getRigHandle();
        String clipName = rigHandle.getPoseName(clipIndex);

        if (!animationManager.hasClip(clipName))
            return false;

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (clip.getRigHandle() != rigHandle)
            return false;

        if (unshaped)
            characterViewPoseSystem.poseBind();

        characterViewPoseSystem.poseClip(clip, 0f);
        return true;
    }

    private void play(CharacterEditorPose pose) {
        characterViewPoseSystem.setParameter(AnimationParameter.SPEED, pose.getSpeed());
        characterViewPoseSystem.play(
                pose.getMovementState(), EntityAction.NONE, 0f, 0, pose.getSpeed(), internal.getDeltaTime());
    }

    // Markers \\

    private void pushMarkers() {

        CharacterEditorMode mode = characterEditorModeSystem.getMode();

        if (mode.drawsBones())
            pushBones(mode == CharacterEditorMode.RIG);
        else if (mode == CharacterEditorMode.EQUIPMENT)
            pushAnchorMarkers();
    }

    // Every bone the region shows, as the Properties tab's Bone Display draws them: the posed character's, or the rig
    // being edited at its bind joints with the selected bone's size about its joint
    private void pushBones(boolean editedRig) {

        this.drawsEditedRig = editedRig;
        this.boneRegion = characterEditorModeSystem.getRegion();
        characterViewRenderSystem.addBones(
                this,
                characterEditorManager.getBoneDisplay(),
                characterEditorManager.getBoneColors());

        int selected = getSelectedBone();

        if (editedRig && selected != EngineSetting.INDEX_NOT_FOUND)
            characterViewRenderSystem.addWorldBoxMarker(
                    characterEditorManager.getRigBoneWorldMatrix(selected),
                    characterEditorManager.getRigBoneSize(selected));
    }

    private void pushAnchorMarkers() {

        ObjectArrayList<EquipmentAnchorStruct> anchors = characterViewPoseSystem.getRaceData().getEquipmentAnchors();

        for (int i = 0; i < anchors.size(); i++) {

            EquipmentAnchorStruct anchor = anchors.get(i);

            characterViewRenderSystem.addBoxMarker(
                    anchor.getBoneIndex(),
                    anchor.getPosition(),
                    anchor.getRotation(),
                    anchor.isHeld() ? heldSize : anchor.getSize(),
                    i == characterEditorManager.getSelectedAnchorIndex());
        }
    }

    // Bones \\

    @Override
    public int getBoneCount() {
        return drawsEditedRig ? characterEditorManager.getRigBoneCount() : getPosedRig().getBoneCount();
    }

    @Override
    public int getBoneParent(int boneIndex) {
        return drawsEditedRig
                ? characterEditorManager.getRigBoneParent(boneIndex)
                : getPosedRig().getBone(boneIndex).getParentIndex();
    }

    @Override
    public String getBoneName(int boneIndex) {
        return drawsEditedRig
                ? characterEditorManager.getRigBoneName(boneIndex)
                : getPosedRig().getBone(boneIndex).getName();
    }

    @Override
    public Matrix4 getBoneWorldMatrix(int boneIndex) {
        return drawsEditedRig
                ? characterEditorManager.getRigBoneWorldMatrix(boneIndex)
                : characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex);
    }

    // The bone's width, the mean of its size across
    @Override
    public float getBoneSize(int boneIndex) {

        Vector3 size = drawsEditedRig
                ? characterEditorManager.getRigBoneSize(boneIndex)
                : getPosedRig().getBone(boneIndex).getSize();

        return (size.x + size.z) * 0.5f;
    }

    @Override
    public boolean isBoneShown(int boneIndex) {
        return drawsEditedRig
                ? characterEditorManager.isRigBoneShown(boneRegion, boneIndex)
                : characterEditorManager.isBoneShown(boneRegion, getPosedRig(), boneIndex);
    }

    @Override
    public boolean isFacialPartBone(int boneIndex) {
        return characterEditorManager.isFacialPartBone(getBoneName(boneIndex));
    }

    @Override
    public int getSelectedBone() {
        return drawsEditedRig
                ? characterEditorManager.getSelectedRigBoneIndex()
                : characterEditorManager.getSelectedBoneIndex();
    }

    private RigHandle getPosedRig() {
        return characterViewPoseSystem.getRaceData().getRigHandle();
    }
}
