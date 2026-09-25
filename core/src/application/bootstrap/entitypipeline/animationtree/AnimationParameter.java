package application.bootstrap.entitypipeline.animationtree;

public enum AnimationParameter {

    /*
     * Values the engine feeds an entity's animation tree every frame. A
     * blend node names one of these in JSON by its lower-case name and
     * places its clips along it. SPEED and VERTICAL_SPEED are the entity's
     * real displacement in blocks per second, TURN_RATE is how fast its
     * body is turning in degrees per second (positive turning left), and
     * LOOK_PITCH and LOOK_YAW are where it is looking relative to the way
     * its body faces, in degrees (positive up and left).
     */

    SPEED,
    VERTICAL_SPEED,
    TURN_RATE,
    LOOK_PITCH,
    LOOK_YAW
}
