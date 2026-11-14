package editor.runtime.faceeditor.preview;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.EditorSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class FaceEditorPreviewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Drives this tab's character viewport from the face editor. The
     * character the Character Editor has open is put on its own animation
     * tree whenever its preview is rebuilt; its look — the Character
     * Editor's preview gender, tones and sliders, so a control shows at its
     * preview value — is applied whenever either editor's state moves, the
     * face held still; and each frame it stands at rest, keeping the shape its
     * sliders give its bones, with every face bone's joint marked and the
     * picked one highlighted. It is what the viewport's camera frames: a
     * sphere about the head bone, a share of its height above its joint.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private CharacterDocumentInstance boundDocument;
    private int boundRevision;
    private int lookRevision;

    // Scratch
    private Vector3 jointScratch;

    // Base \\

    @Override
    protected void create() {

        // Bound
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.jointScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();

        if (!faceEditorManager.hasFace()) {
            characterViewPoseSystem.clear();
            this.boundDocument = null;
            return;
        }

        if (document != boundDocument || document.getPreviewRevision() != boundRevision)
            bind(document);

        if (faceEditorManager.getRevision() != lookRevision)
            applyLook();

        characterViewPoseSystem.poseRest();
        pushMarkers();
    }

    private void bind(CharacterDocumentInstance document) {

        characterViewPoseSystem.bind(document.getPreviewData(), characterEditorManager.getPreviewGenderID());
        this.boundDocument = document;
        this.boundRevision = document.getPreviewRevision();
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    private void applyLook() {

        this.lookRevision = faceEditorManager.getRevision();
        characterViewPoseSystem.setGender(characterEditorManager.getPreviewGenderID());

        characterEditorManager.applyLook(
                characterViewPoseSystem.getAppearanceHandle(),
                characterViewPoseSystem.getRaceData().getAppearanceData(),
                false,
                true);
    }

    // Markers \\

    private void pushMarkers() {

        int boneCount = characterViewPoseSystem.getRaceData().getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++)
            if (faceEditorManager.isFaceBone(i))
                characterViewRenderSystem.addJointMarker(i, i == faceEditorManager.getSelectedBoneIndex());
    }

    // Frame \\

    @Override
    public boolean isShown() {
        return characterViewPoseSystem.hasSubject() && characterViewPoseSystem.hasAppearance();
    }

    @Override
    public float getCenter(int axis) {

        int headBoneIndex = characterViewPoseSystem.getRaceData().getAppearanceData().getHeadBoneIndex();
        RigBoneStruct headBone = characterViewPoseSystem.getRaceData().getRigHandle().getBone(headBoneIndex);

        CharacterPoseUtility.getTranslation(
                characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(headBoneIndex),
                jointScratch);

        return switch (axis) {
            case EngineSetting.AXIS_X -> jointScratch.x;
            case EngineSetting.AXIS_Y -> jointScratch.y
                    + headBone.getSize().y * EditorSetting.FACE_EDITOR_FRAME_CENTRE_SHARE;
            default -> jointScratch.z;
        };
    }

    @Override
    public float getBoundingRadius() {

        int headBoneIndex = characterViewPoseSystem.getRaceData().getAppearanceData().getHeadBoneIndex();
        Vector3 size = characterViewPoseSystem.getRaceData().getRigHandle().getBone(headBoneIndex).getSize();

        return Math.max(size.x, Math.max(size.y, size.z)) * EditorSetting.FACE_EDITOR_FRAME_RADIUS_SHARE;
    }
}
