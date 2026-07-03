package application.bootstrap.geometrypipeline.rig;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class RigData extends DataPackage {

    /*
     * Immutable bone hierarchy for one rig template. Bones are stored in
     * strict parent-before-child order — index 0 is always the root, and
     * every bone's parentIndex is guaranteed to be less than its own index.
     * This lets pose evaluation walk the array once, front to back, with
     * every parent already resolved before its children are reached.
     */

    // Bones
    private final RigBoneStruct[] bones;
    private final Object2IntOpenHashMap<String> boneName2Index;

    // Constructor \\

    public RigData(RigBoneStruct[] bones, Object2IntOpenHashMap<String> boneName2Index) {

        // Bones
        this.bones = bones;
        this.boneName2Index = boneName2Index;
    }

    // Accessible \\

    public RigBoneStruct[] getBones() {
        return bones;
    }

    public int getBoneCount() {
        return bones.length;
    }

    public RigBoneStruct getBone(int boneIndex) {
        return bones[boneIndex];
    }

    public boolean hasBone(String boneName) {
        return boneName2Index.containsKey(boneName);
    }

    public int getBoneIndex(String boneName) {

        if (!boneName2Index.containsKey(boneName))
            throwException("Bone not found in rig: \"" + boneName + "\"");

        return boneName2Index.getInt(boneName);
    }
}