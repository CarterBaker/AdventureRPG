package editor.runtime.charactereditor.preview;

import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.inventory.EquipmentAnchorStruct;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorColor;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorPose;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorPreviewSystem extends SystemPackage {

    /*
     * Drives this tab's character viewport from the character editor. A new
     * preview of the active template is put on its own animation tree; the
     * tone worn, the build and the head shape are applied whenever the
     * editor's state moves; and each frame the preview is posed — at rest or
     * played in the chosen movement, or held in the exact bind pose while its
     * mesh or rig is edited — and given the markers of the mode: the joints in
     * Build, Mesh and Weights, the rig's bones as they are being edited in
     * Rig, every equipment anchor in Equipment, the selected one highlighted.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private CharacterDocumentInstance boundDocument;
    private int boundRevision;
    private int lookRevision;

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

        if (characterEditorManager.getRevision() != lookRevision)
            applyLook();

        strikePose();
        pushMarkers();
    }

    private void bind(CharacterDocumentInstance document) {

        EntityData entityData = document.getPreviewData();

        characterViewPoseSystem.bind(entityData, entityData.getAnimationTreeHandle());
        this.boundDocument = document;
        this.boundRevision = document.getPreviewRevision();
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // Look \\

    private void applyLook() {

        this.lookRevision = characterEditorManager.getRevision();

        if (!characterViewPoseSystem.hasAppearance())
            return;

        AppearanceHandle appearance = characterViewPoseSystem.getAppearanceHandle();
        AppearanceData appearanceData = characterViewPoseSystem.getEntityData().getAppearanceData();
        Vector3 headProportion = characterEditorManager.getHeadProportion();
        boolean meshMode = characterEditorManager.getMode().isMeshMode();

        for (CharacterEditorColor color : CharacterEditorColor.VALUES) {

            Color tone = resolveTone(
                    appearanceData.getPalette(color.getSkinnedColor()),
                    appearanceData.getColor(color.getSkinnedColor()),
                    characterEditorManager.getToneIndex(color));

            appearance.setColor(color.getSkinnedColor(), tone.r, tone.g, tone.b);
        }

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            appearance.setShapeValue(i, meshMode ? 0f : characterEditorManager.getShapeValue(i));

        appearance.setFaceAnimated(!meshMode && characterEditorManager.getBlink() <= 0f);
        appearance.setBlink(meshMode ? 0f : characterEditorManager.getBlink());
        appearance.setWeightRatio(characterEditorManager.getWeightRatio());
        appearance.setHeadProportion(headProportion.x, headProportion.y, headProportion.z);
    }

    // The tone worn, or the default color while none is or the palette has not caught up with the edit
    private Color resolveTone(Color[] palette, Color defaultColor, int toneIndex) {
        return toneIndex >= 0 && toneIndex < palette.length ? palette[toneIndex] : defaultColor;
    }

    // Pose \\

    private void strikePose() {

        CharacterEditorPose pose = characterEditorManager.getPose();

        if (characterEditorManager.getMode().holdsBindPose()) {
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

        switch (characterEditorManager.getMode()) {
            case BUILD, MESH, WEIGHTS -> pushJointMarkers();
            case RIG -> pushRigMarkers();
            case EQUIPMENT -> pushAnchorMarkers();
            default -> {
            }
        }
    }

    private void pushJointMarkers() {

        int boneCount = characterViewPoseSystem.getEntityData().getRigHandle().getBoneCount();

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

        ObjectArrayList<EquipmentAnchorStruct> anchors = characterViewPoseSystem.getEntityData().getEquipmentAnchors();

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
