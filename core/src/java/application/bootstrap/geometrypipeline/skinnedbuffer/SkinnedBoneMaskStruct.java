package application.bootstrap.geometrypipeline.skinnedbuffer;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SkinnedBoneMaskStruct extends StructPackage {

    /*
     * A set of a rig's bones a skinned draw hides or alone shows, and which
     * of the two. The set is packed SKINNED_BONE_MASK_BITS bones to a float,
     * exact as a whole number, four floats to a palette texel, so the vertex
     * shader tests a bone with one shift. Built once when what it masks
     * changes and copied into each draw's appearance, never per frame.
     */

    // Mode
    private SkinnedMaskMode mode;

    // Bits
    private final float[] bits;

    // Constructor \\

    public SkinnedBoneMaskStruct() {

        // Bits
        this.bits = new float[EngineSetting.SKINNED_BONE_MASK_TEXELS * EngineSetting.COLOR_CHANNEL_COUNT];

        clear();
    }

    // Management \\

    public void clear() {
        this.mode = SkinnedMaskMode.NONE;
        Arrays.fill(bits, 0f);
    }

    public void setMode(SkinnedMaskMode mode) {
        this.mode = mode;
    }

    public void addBone(int boneIndex) {

        int channel = boneIndex / EngineSetting.SKINNED_BONE_MASK_BITS;
        int bit = 1 << (boneIndex % EngineSetting.SKINNED_BONE_MASK_BITS);

        bits[channel] = (int) bits[channel] | bit;
    }

    public void removeBone(int boneIndex) {

        int channel = boneIndex / EngineSetting.SKINNED_BONE_MASK_BITS;
        int bit = 1 << (boneIndex % EngineSetting.SKINNED_BONE_MASK_BITS);

        bits[channel] = (int) bits[channel] & ~bit;
    }

    // A leader's mask laid onto a rig that follows it: every bone they share as the leader's, every bone of the
    // follower's own as its parent, so a worn piece's own bones are hidden and shown with the body bone they hang from
    public void follow(SkinnedBoneMaskStruct leaderMask, RigHandle followerRig) {

        set(leaderMask);

        for (int i = followerRig.getSharedBoneCount(); i < followerRig.getBoneCount(); i++) {

            removeBone(i);

            if (hasBone(followerRig.getBone(i).getParentIndex()))
                addBone(i);
        }
    }

    // A bone and every bone below it, which a rig lists after it
    public void addBranch(RigHandle rigHandle, int boneIndex) {

        addBone(boneIndex);

        for (int i = boneIndex + 1; i < rigHandle.getBoneCount(); i++)
            if (!rigHandle.getBone(i).isRoot() && hasBone(rigHandle.getBone(i).getParentIndex()))
                addBone(i);
    }

    // Every bone of another mask added to this one's, its mode kept
    public void addBones(SkinnedBoneMaskStruct source) {

        for (int i = 0; i < bits.length; i++)
            bits[i] = (int) bits[i] | (int) source.bits[i];
    }

    public void set(SkinnedBoneMaskStruct source) {
        this.mode = source.mode;
        System.arraycopy(source.bits, 0, bits, 0, bits.length);
    }

    // Write \\

    public void write(float[] out, int offset) {
        System.arraycopy(bits, 0, out, offset, bits.length);
    }

    // Accessible \\

    public SkinnedMaskMode getMode() {
        return mode;
    }

    public boolean hasBone(int boneIndex) {

        int channel = boneIndex / EngineSetting.SKINNED_BONE_MASK_BITS;
        int bit = 1 << (boneIndex % EngineSetting.SKINNED_BONE_MASK_BITS);

        return ((int) bits[channel] & bit) != 0;
    }
}
