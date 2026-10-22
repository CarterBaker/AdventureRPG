package editor.runtime.characterview.pose;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityState;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;

public class CharacterViewPoseSystem extends SystemPackage {

    /*
     * Holds the character one character viewport shows: the template it is
     * built from, its pose on an animation tree, and its appearance at the
     * template's defaults. bind() puts a template's character on a tree; the
     * tool driving the viewport strikes the pose every frame — at rest, in
     * the exact bind pose a mesh is authored in, from one clip at a time, or
     * played through the tree.
     */

    // Subject
    private EntityData entityData;
    private AnimationStateHandle animationStateHandle;
    private AppearanceHandle appearanceHandle;

    // Subject \\

    public void bind(EntityData entityData, AnimationTreeHandle animationTreeHandle) {

        if (!entityData.hasCharacterModel())
            throwException("A character viewport can only show a template with a character model.");

        if (animationTreeHandle.getRigHandle() != entityData.getRigHandle())
            throwException("Animation tree \"" + animationTreeHandle.getTreeName()
                    + "\" targets a different rig than the character it would pose.");

        this.entityData = entityData;
        this.animationStateHandle = create(AnimationStateHandle.class);
        animationStateHandle.constructor(animationTreeHandle, EntityState.IDLE);
        this.appearanceHandle = entityData.hasAppearance() ? createAppearance() : null;
    }

    private AppearanceHandle createAppearance() {

        AppearanceHandle appearance = create(AppearanceHandle.class);
        appearance.constructor(
                entityData.getAppearanceData(),
                animationStateHandle,
                EditorSetting.CHARACTER_VIEW_WEIGHT_RATIO);

        return appearance;
    }

    public void clear() {
        this.entityData = null;
        this.animationStateHandle = null;
        this.appearanceHandle = null;
    }

    // Pose \\

    public void poseRest() {

        if (animationStateHandle != null)
            animationStateHandle.poseRest();
    }

    // The bind pose at unit proportions, so every skinned vertex stands exactly where its mesh file puts it; the next
    // appearance change applies the build and head shape again
    public void poseBind() {

        if (animationStateHandle == null)
            return;

        for (int i = 0; i < entityData.getRigHandle().getBoneCount(); i++)
            animationStateHandle.setBoneProportion(i, 1f, 1f, 1f);

        animationStateHandle.poseRest();
    }

    public void poseClip(AnimationClipHandle clip, float time) {

        if (animationStateHandle != null)
            animationStateHandle.poseClip(clip, time);
    }

    public void setParameter(AnimationParameter parameter, float value) {

        if (animationStateHandle != null)
            animationStateHandle.setParameter(parameter, value);
    }

    public void play(
            EntityState movementState,
            EntityAction action,
            float actionPhase,
            int actionSequence,
            float deltaTime) {

        if (animationStateHandle != null)
            animationStateHandle.update(movementState, action, null, actionPhase, actionSequence, deltaTime);
    }

    // Accessible \\

    public boolean hasSubject() {
        return entityData != null;
    }

    public EntityData getEntityData() {
        return entityData;
    }

    public AnimationStateHandle getAnimationStateHandle() {
        return animationStateHandle;
    }

    public AppearanceHandle getAppearanceHandle() {
        return appearanceHandle;
    }

    public boolean hasAppearance() {
        return appearanceHandle != null;
    }
}
