package editor.runtime.faceeditor.preview;

import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class FaceEditorPreviewSystem extends SystemPackage {

    /*
     * Drives the Character Editor window's viewport from the face editor
     * while its Face layer is worked on; on any other layer
     * CharacterEditorPreviewSystem drives it, and coming back the preview is
     * bound afresh. The character the Character Editor has open is put on
     * its own animation tree whenever its preview is rebuilt; its look — the
     * Character Editor's preview gender, tones and sliders, so a control
     * shows at its preview value — is applied whenever either editor's state
     * moves, the face held still; and each frame it stands at rest, keeping
     * the shape its sliders give its bones, with every face bone's joint
     * marked and the picked one highlighted.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private CharacterDocumentInstance boundDocument;
    private int boundRevision;
    private int lookRevision;

    // Base \\

    @Override
    protected void create() {

        // Bound
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (!characterEditorModeSystem.getMode().shapesFace()) {
            unbind();
            return;
        }

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

    // Let go while another system drives the viewport, so the preview is bound and dressed again on return
    private void unbind() {
        this.boundDocument = null;
        this.lookRevision = EngineSetting.INDEX_NOT_FOUND;
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
}
