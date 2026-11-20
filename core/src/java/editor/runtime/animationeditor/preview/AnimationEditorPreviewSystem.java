package editor.runtime.animationeditor.preview;

import java.util.Objects;

import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneKind;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterBoneSource;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class AnimationEditorPreviewSystem extends SystemPackage implements CharacterBoneSource {

    /*
     * Drives this tab's character viewport from the animation editor. The
     * chosen character is put on its own animation tree whenever the clip,
     * its preview or the character changes; each frame it is posed straight
     * from the preview clip at the playhead, or held at rest while the clip
     * has nothing to play or none is open, so the character — the player at
     * first — always stands in view, and every bone is drawn as the
     * Properties tab's Bone Display says, in its kind's colour, the selected
     * bone highlighted.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private EntityManager entityManager;
    private CharacterEditorManager characterEditorManager;
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
        this.characterEditorManager = get(CharacterEditorManager.class);
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
        characterViewRenderSystem.addBones(
                this,
                characterEditorManager.getBoneDisplay(),
                characterEditorManager.getBoneColors());
    }

    // Bones \\

    @Override
    public int getBoneCount() {
        return getRig().getBoneCount();
    }

    @Override
    public int getBoneParent(int boneIndex) {
        return getRig().getBone(boneIndex).getParentIndex();
    }

    @Override
    public String getBoneName(int boneIndex) {
        return getRig().getBone(boneIndex).getName();
    }

    @Override
    public Matrix4 getBoneWorldMatrix(int boneIndex) {
        return characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex);
    }

    // The bone's width, the mean of its size across
    @Override
    public float getBoneSize(int boneIndex) {

        Vector3 size = getRig().getBone(boneIndex).getSize();

        return (size.x + size.z) * 0.5f;
    }

    @Override
    public boolean isBoneShown(int boneIndex) {
        return true;
    }

    @Override
    public boolean isFacialPartBone(int boneIndex) {
        return CharacterBoneKind.isFacialPartBone(getRig(), getBoneName(boneIndex));
    }

    @Override
    public int getSelectedBone() {
        return animationEditorManager.getSelectedBoneIndex();
    }

    private RigHandle getRig() {
        return characterViewPoseSystem.getRaceData().getRigHandle();
    }
}
