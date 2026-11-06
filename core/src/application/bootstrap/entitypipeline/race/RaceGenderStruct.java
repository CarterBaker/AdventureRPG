package application.bootstrap.entitypipeline.race;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.StructPackage;

public class RaceGenderStruct extends StructPackage {

    /*
     * One gender a race may be made as and the character model it requires:
     * the rigged body mesh it wears, the rig that mesh is skinned to, the
     * material it is drawn with and the animation tree it moves by, the tree
     * on the same rig. Two genders naming one material share one clone, so
     * their characters batch together.
     */

    // Gender
    private final short genderID;

    // Model
    private final MeshHandle meshHandle;
    private final RigHandle rigHandle;
    private final MaterialInstance material;
    private final AnimationTreeHandle animationTreeHandle;

    // Constructor \\

    public RaceGenderStruct(
            short genderID,
            MeshHandle meshHandle,
            RigHandle rigHandle,
            MaterialInstance material,
            AnimationTreeHandle animationTreeHandle) {

        // Gender
        this.genderID = genderID;

        // Model
        this.meshHandle = meshHandle;
        this.rigHandle = rigHandle;
        this.material = material;
        this.animationTreeHandle = animationTreeHandle;
    }

    // Accessible \\

    public short getGenderID() {
        return genderID;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public AnimationTreeHandle getAnimationTreeHandle() {
        return animationTreeHandle;
    }
}
