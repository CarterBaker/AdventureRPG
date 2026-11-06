package editor.runtime.characterview.pose;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import editor.runtime.EditorSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterViewPoseSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Holds the character one character viewport shows: the race it is built
     * from and the gender it is made as, its pose on an animation tree, and
     * its appearance at the race's defaults. bind() puts a race's character
     * on the tree its gender moves by, or on a tree given that keeps it
     * whatever the gender; setGender() takes on another gender's model,
     * moving onto its tree when the tree follows the gender. The tool driving
     * the viewport strikes the pose every frame — at rest, in the exact bind
     * pose a mesh is authored in, from one clip at a time, or played through
     * the tree. It is what the viewport's camera frames: a sphere about a
     * share of the model's height, reaching down to its feet.
     */

    // Subject
    private RaceData raceData;
    private RaceGenderStruct raceGender;
    private AnimationTreeHandle fixedTreeHandle;
    private AnimationStateHandle animationStateHandle;
    private AppearanceHandle appearanceHandle;

    // Subject \\

    // The race made as a gender, posed on that gender's own tree
    public void bind(RaceData raceData, short genderID) {
        bind(raceData, genderID, null);
    }

    // The race made as a gender, posed on the tree given whatever its gender, or on its gender's own without one
    public void bind(RaceData raceData, short genderID, AnimationTreeHandle fixedTreeHandle) {

        if (fixedTreeHandle != null && fixedTreeHandle.getRigHandle() != raceData.getRigHandle())
            throwException("Animation tree \"" + fixedTreeHandle.getTreeName()
                    + "\" targets a different rig than the race \"" + raceData.getRaceName() + "\" it would pose.");

        this.raceData = raceData;
        this.raceGender = raceData.getGender(genderID);
        this.fixedTreeHandle = fixedTreeHandle;
        this.animationStateHandle = create(AnimationStateHandle.class);
        animationStateHandle.constructor(resolveTree(), EntityState.IDLE);
        this.appearanceHandle = raceData.hasAppearance() ? createAppearance() : null;
    }

    private AppearanceHandle createAppearance() {

        AppearanceHandle appearance = create(AppearanceHandle.class);
        appearance.constructor(
                raceData.getAppearanceData(),
                animationStateHandle,
                EditorSetting.CHARACTER_VIEW_WEIGHT_RATIO);
        appearance.setGender(raceGender.getGenderID());

        return appearance;
    }

    // Takes on a gender's model, moving onto its tree when the tree follows the gender; the look is put on after
    public void setGender(short genderID) {

        if (raceData == null || raceGender.getGenderID() == genderID)
            return;

        AnimationTreeHandle previousTree = resolveTree();
        this.raceGender = raceData.getGender(genderID);

        if (resolveTree() != previousTree)
            animationStateHandle.constructor(resolveTree(), EntityState.IDLE);
    }

    private AnimationTreeHandle resolveTree() {
        return fixedTreeHandle != null ? fixedTreeHandle : raceGender.getAnimationTreeHandle();
    }

    public void clear() {
        this.raceData = null;
        this.raceGender = null;
        this.fixedTreeHandle = null;
        this.animationStateHandle = null;
        this.appearanceHandle = null;
    }

    // Pose \\

    public void poseRest() {

        if (animationStateHandle != null)
            animationStateHandle.poseRest();
    }

    // The bind pose at unit proportions and unshaped, so every skinned vertex stands exactly where its mesh file puts
    // it; the next appearance change applies the build, the head shape and the sliders' bones again
    public void poseBind() {

        if (animationStateHandle == null)
            return;

        for (int i = 0; i < raceData.getRigHandle().getBoneCount(); i++) {
            animationStateHandle.setBoneProportion(i, 1f, 1f, 1f);
            animationStateHandle.clearBoneShape(i);
        }

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
        return raceData != null;
    }

    public RaceData getRaceData() {
        return raceData;
    }

    public RaceGenderStruct getRaceGender() {
        return raceGender;
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

    // Frame \\

    @Override
    public boolean isShown() {
        return raceData != null && raceData.getModelHeight() > 0f;
    }

    @Override
    public float getCenter(int axis) {
        return axis == EngineSetting.AXIS_Y ? getBoundingRadius() : 0f;
    }

    @Override
    public float getBoundingRadius() {
        return raceData.getModelHeight() * EditorSetting.CHARACTER_VIEW_FRAME_HEIGHT_SHARE;
    }
}
