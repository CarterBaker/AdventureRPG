package editor.runtime.charactereditor.preview;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorPreviewSystem extends SystemPackage {

    /*
     * Drives this tab's character viewport from the character editor. A new
     * preview of the active race is put on the animation tree of the gender
     * it is made as; that gender — the model it requires, moving by its own
     * tree, and the features it wears — the sliders,
     * the tone worn, the build and the head shape are applied whenever the
     * editor's state moves; and each frame the preview is posed — in the
     * bind pose, one of the poses the rig names, or played in the chosen
     * movement, held in the exact bind pose while its rig is edited, and
     * played with its springs simulated while Preview Springs is on — and
     * given the markers of the mode: the joints in
     * Build, Mesh and Weights, the rig's bones as they are being edited in
     * Rig, every equipment anchor in Equipment, the selected one highlighted.
     * The tab's own mode decides all of it, so the Character Appearance,
     * Build, Equipment and Mesh Editor tabs each show the character their own
     * way side by side.
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

    // Scratch
    private Vector3 heldSize;

    // Base \\

    @Override
    protected void create() {

        // Bound
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        float size = CharacterEditorSetting.HELD_ANCHOR_MARKER_SIZE;
        this.heldSize = new Vector3(size, size, size);
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
    // the bind pose alone, and Preview Springs plays the movement chosen in a modelling tab, idle at Bind, with its
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

        switch (characterEditorModeSystem.getMode()) {
            case BUILD, MESH, WEIGHTS -> pushJointMarkers();
            case RIG -> pushRigMarkers();
            case EQUIPMENT -> pushAnchorMarkers();
            default -> {
            }
        }
    }

    private void pushJointMarkers() {

        RigHandle rigHandle = characterViewPoseSystem.getRaceData().getRigHandle();
        CharacterEditorRegion region = characterEditorModeSystem.getRegion();

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            if (characterEditorManager.isBoneShown(region, rigHandle, i))
                characterViewRenderSystem.addJointMarker(i, i == characterEditorManager.getSelectedBoneIndex());
    }

    // Every bone of the rig as it is being edited, at its bind joint, and the selected one's size about its joint
    private void pushRigMarkers() {

        int boneCount = characterEditorManager.getRigBoneCount();
        int selected = characterEditorManager.getSelectedRigBoneIndex();

        for (int i = 0; i < boneCount; i++)
            if (characterEditorManager.isRigBoneShown(characterEditorModeSystem.getRegion(), i))
                characterViewRenderSystem.addWorldMarker(
                        characterEditorManager.getRigBoneWorldMatrix(i),
                        i == selected ? EditorSetting.CHARACTER_VIEW_JOINT_SELECTED_SIZE
                                : EditorSetting.CHARACTER_VIEW_JOINT_SIZE,
                        i == selected);

        if (selected != EngineSetting.INDEX_NOT_FOUND)
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
}
