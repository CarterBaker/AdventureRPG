package editor.runtime.animationeditor.preview;

import java.util.Objects;

import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.race.RaceData;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class AnimationEditorPreviewSystem extends SystemPackage {

    /*
     * Drives this tab's character viewport from the animation editor. The
     * chosen character is put on its own animation tree whenever the clip,
     * its preview or the character changes; each frame it is posed straight
     * from the preview clip at the playhead, or held at rest while the clip
     * has nothing to play or none is open, so the character — the player at
     * first — always stands in view, and every joint is marked, the selected
     * bone highlighted.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private EntityManager entityManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private AnimationDocumentInstance boundDocument;
    private int boundRevision;
    private String boundCharacterName;

    // Base \\

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.entityManager = get(EntityManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        AnimationDocumentInstance document = animationEditorManager.getActiveDocument();
        String characterName = animationEditorManager.getCharacterName();

        if (characterName == null) {
            characterViewPoseSystem.clear();
            this.boundDocument = null;
            this.boundCharacterName = null;
            return;
        }

        if (document != boundDocument
                || document != null && document.getPreviewRevision() != boundRevision
                || !Objects.equals(characterName, boundCharacterName))
            bind(document, characterName);

        if (document != null && document.hasPreviewClip())
            characterViewPoseSystem.poseClip(document.getPreviewClip(), animationEditorManager.getTime());
        else
            characterViewPoseSystem.poseRest();

        pushMarkers();
    }

    private void bind(AnimationDocumentInstance document, String characterName) {

        RaceData raceData = entityManager.getEntityHandleFromTemplateName(characterName).getEntityData().getRaceData();

        characterViewPoseSystem.bind(raceData, raceData.getDefaultGenderID());
        this.boundDocument = document;
        this.boundRevision = document != null ? document.getPreviewRevision() : EngineSetting.INDEX_NOT_FOUND;
        this.boundCharacterName = characterName;
    }

    // Markers \\

    private void pushMarkers() {

        int boneCount = characterViewPoseSystem.getRaceData().getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++)
            characterViewRenderSystem.addJointMarker(i, i == animationEditorManager.getSelectedBoneIndex());
    }
}
