package editor.runtime.animationtreeeditor.preview;

import java.util.Objects;

import application.bootstrap.entitypipeline.animationtree.AnimationLayerStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.bootstrap.animationtreeeditorpipeline.util.AnimationTreeEditorView;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import engine.root.SystemPackage;

public class AnimationTreeEditorPreviewSystem extends SystemPackage {

    /*
     * Drives this tab's character viewport from the animation tree editor.
     * The chosen character is put on the preview tree whenever the tree, its
     * preview or the character changes, and each frame the tree is played
     * exactly as an entity plays it, fed the simulated movement state, every
     * simulated parameter and the action playing. While the mask is shown,
     * every joint the selected layer moves is highlighted.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private EntityManager entityManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;

    // Bound
    private TreeDocumentInstance boundDocument;
    private int boundRevision;
    private String boundCharacterName;

    // Base \\

    @Override
    protected void get() {
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
        this.entityManager = get(EntityManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        TreeDocumentInstance document = animationTreeEditorManager.getActiveDocument();
        String characterName = animationTreeEditorManager.getCharacterName();

        if (document == null || !document.hasPreviewTree() || characterName == null) {
            characterViewPoseSystem.clear();
            this.boundDocument = null;
            return;
        }

        if (document != boundDocument
                || document.getPreviewRevision() != boundRevision
                || !Objects.equals(characterName, boundCharacterName))
            bind(document, characterName);

        for (AnimationParameter parameter : AnimationParameter.VALUES)
            characterViewPoseSystem.setParameter(parameter, animationTreeEditorManager.getParameter(parameter));

        characterViewPoseSystem.play(
                animationTreeEditorManager.getMovementState(),
                animationTreeEditorManager.getPlayingAction(),
                animationTreeEditorManager.getActionPhase(),
                animationTreeEditorManager.getActionSequence(),
                internal.getDeltaTime());

        if (animationTreeEditorManager.getView() == AnimationTreeEditorView.MASK)
            pushMaskMarkers(document.getPreviewTree());
    }

    private void bind(TreeDocumentInstance document, String characterName) {

        EntityData entityData = entityManager.getEntityHandleFromTemplateName(characterName).getEntityData();

        characterViewPoseSystem.bind(entityData, document.getPreviewTree());
        this.boundDocument = document;
        this.boundRevision = document.getPreviewRevision();
        this.boundCharacterName = characterName;
    }

    // Markers \\

    private void pushMaskMarkers(AnimationTreeHandle tree) {

        int layerIndex = animationTreeEditorManager.getSelectedLayerIndex();

        if (layerIndex >= tree.getLayerCount())
            return;

        AnimationLayerStruct layer = tree.getLayer(layerIndex);
        int boneCount = tree.getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++)
            characterViewRenderSystem.addJointMarker(i, layer.getBoneMask(i) > 0f);
    }
}
