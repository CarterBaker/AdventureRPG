package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

class FirstPersonBranch extends BranchPackage {

    /*
     * Seats a player's camera at its eyes and poses its body around the view.
     * As the camera closes in on the eye, the chest — the bone the head hangs
     * from — bends forward with the look downward and twists after the look
     * sideways, over the tree's own look layers, so looking down shows the
     * player's own chest, legs and feet, and looking around turns the
     * shoulders before the feet. The camera then sits where the eyes are on
     * the posed head: forward of the head's middle at eye level, carried by
     * the head bone through every bend, twist and stride, so the hidden head's
     * neck always stays behind the view. With head bob off the gait's lift
     * and drop of the hips is taken back out, and the camera stays level while
     * the body still sways beneath it. Farther out the camera's origin eases
     * back to the eye gameplay aims from.
     */

    // Scratch
    private Vector3 headEye;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.headEye = new Vector3();
    }

    // Blend \\

    // How far the camera has closed in on the eyes, 0 at the blend distance or beyond to 1 at the eye
    float resolveEyeBlend(float zoomDistance) {
        return 1f - Math.min(1f, Math.max(0f, zoomDistance / EngineSetting.CAMERA_EYE_BLEND_DISTANCE));
    }

    // Pose \\

    // Sets the chest's posture for this frame's look, ahead of the animation update; none at a blend of 0
    void pose(EntityInstance player, float eyeBlend) {

        int chestBone = resolveChestBone(player);

        if (chestBone == EngineSetting.INDEX_NOT_FOUND)
            return;

        EntityStateHandle state = player.getEntityStateHandle();
        float lookDown = Math.max(0f, -state.getLookPitch());

        player.getAnimationStateHandle().setBonePosture(
                chestBone,
                lookDown * EngineSetting.CAMERA_FIRST_PERSON_BEND_SHARE * eyeBlend,
                state.getLookYaw() * EngineSetting.CAMERA_FIRST_PERSON_TWIST_SHARE * eyeBlend,
                0f);
    }

    // Camera \\

    // The point the camera looks out from, eased from the aiming eye to the posed head's eyes by the blend
    Vector3 resolveCameraOrigin(EntityInstance player, float eyeBlend, Vector3 aimEye, Vector3 out) {

        out.set(aimEye);

        if (eyeBlend <= 0f || resolveHeadBone(player) == EngineSetting.INDEX_NOT_FOUND)
            return out;

        resolveHeadEye(player, headEye);

        return out.add(
                (headEye.x - aimEye.x) * eyeBlend,
                (headEye.y - aimEye.y) * eyeBlend,
                (headEye.z - aimEye.z) * eyeBlend);
    }

    // The eyes in the bind pose, carried through the head bone's skinning into the posed model, then into the world
    private void resolveHeadEye(EntityInstance player, Vector3 out) {

        EntityData entityData = player.getEntityData();
        AnimationStateHandle animation = player.getAnimationStateHandle();
        Matrix4 head = animation.getSkinningMatrix(resolveHeadBone(player));

        float y = entityData.getModelHeight() * entityData.getEyeLevel();
        float z = EngineSetting.CAMERA_EYE_FORWARD;

        float modelX = head.getM01() * y + head.getM02() * z + head.getM03();
        float modelY = head.getM11() * y + head.getM12() * z + head.getM13();
        float modelZ = head.getM21() * y + head.getM22() * z + head.getM23();

        if (!settings.headBob) {

            int rootBone = resolveRootBone(animation.getRigHandle(), resolveHeadBone(player));
            Matrix4 root = animation.getWorldMatrix(rootBone);
            Vector3 rest = animation.getRigHandle().getBone(rootBone).getPosition();

            modelX -= root.getM03() - rest.x;
            modelY -= root.getM13() - rest.y;
            modelZ -= root.getM23() - rest.z;
        }

        player.getModelPoint(modelX, modelY, modelZ, out);
    }

    // Bones \\

    private int resolveHeadBone(EntityInstance player) {

        if (!player.hasAnimationState() || !player.hasAppearance())
            return EngineSetting.INDEX_NOT_FOUND;

        return player.getEntityData().getAppearanceData().getHeadBoneIndex();
    }

    private int resolveChestBone(EntityInstance player) {

        int headBone = resolveHeadBone(player);

        if (headBone == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        RigBoneStruct head = player.getAnimationStateHandle().getRigHandle().getBone(headBone);

        return head.isRoot() ? EngineSetting.INDEX_NOT_FOUND : head.getParentIndex();
    }

    private static int resolveRootBone(RigHandle rigHandle, int boneIndex) {

        int bone = boneIndex;

        while (!rigHandle.getBone(bone).isRoot())
            bone = rigHandle.getBone(bone).getParentIndex();

        return bone;
    }
}
