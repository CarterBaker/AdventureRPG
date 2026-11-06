package editor.runtime.charactereditor.preview;

import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
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
     * editor's state moves; and each frame the preview is posed — at rest or
     * played in the chosen movement, or held in the exact bind pose while its
     * mesh or rig is edited — and given the markers of the mode: the joints in
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

    private void strikePose() {

        CharacterEditorPose pose = characterEditorManager.getPose();

        if (characterEditorModeSystem.getMode().holdsBindPose()) {
            characterViewPoseSystem.poseBind();
            return;
        }

        if (!pose.isPlayed()) {
            characterViewPoseSystem.poseRest();
            return;
        }

        characterViewPoseSystem.setParameter(AnimationParameter.SPEED, pose.getSpeed());
        characterViewPoseSystem.play(pose.getMovementState(), EntityAction.NONE, 0f, 0, internal.getDeltaTime());
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

        int boneCount = characterViewPoseSystem.getRaceData().getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++)
            characterViewRenderSystem.addJointMarker(i, i == characterEditorManager.getSelectedBoneIndex());
    }

    // Every bone of the rig as it is being edited, at its bind joint, and the selected one's size about its joint
    private void pushRigMarkers() {

        int boneCount = characterEditorManager.getRigBoneCount();
        int selected = characterEditorManager.getSelectedRigBoneIndex();

        for (int i = 0; i < boneCount; i++)
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
