package application.bootstrap.entitypipeline.animationtree;

public enum AnimationBlendMode {

    /*
     * How an animation layer combines with the layers beneath it. OVERRIDE
     * cross-fades the pose toward the layer's own by its weight; ADDITIVE
     * adds the layer's keyframe offsets on top of the pose, scaled by its
     * weight, so a layer can bend a bone without replacing its motion.
     */

    OVERRIDE,
    ADDITIVE
}
