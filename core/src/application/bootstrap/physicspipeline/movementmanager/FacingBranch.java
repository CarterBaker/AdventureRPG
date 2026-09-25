package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class FacingBranch extends BranchPackage {

    /*
     * Turns an entity's body smoothly each frame and resolves where it looks
     * relative to it. The body heads toward the way the entity is actually
     * travelling while it has horizontal input, holds its heading while it
     * has none, and follows the facing direction instead whenever the input
     * asks to strafe. It closes on that heading exponentially at the
     * behavior's turn responsiveness, and the turn rate it records is how far
     * that step moved per second, eased by TURN_RATE_SMOOTHING so a sudden
     * change of heading never snaps anything that leans on it. Look pitch
     * and yaw are the facing direction measured against the body, in
     * degrees, the yaw wrapped to half a turn either way. Yaw follows the
     * render convention — degrees of atan2(x, z), increasing as the body
     * turns left.
     */

    // Update \\

    void update(EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        EntityInputHandle input = entity.getEntityInputHandle();
        Vector3 facing = input.getFacingDirection();
        float delta = internal.getDeltaTime();

        float facingYaw = toYaw(facing.x, facing.z);
        float bodyYaw = state.getBodyYaw();
        float targetYaw = resolveTargetYaw(state, input, facingYaw, bodyYaw);
        float smoothing = Math.min(1f, delta * entity.getBehaviorHandle().getTurnResponsiveness());
        float step = wrapDegrees(targetYaw - bodyYaw) * smoothing;
        float nextYaw = wrapDegrees(bodyYaw + step);

        state.setBodyYaw(nextYaw, resolveTurnRate(state, step, delta));
        state.setLook(toPitch(facing), wrapDegrees(facingYaw - nextYaw));
    }

    private float resolveTargetYaw(
            EntityStateHandle state,
            EntityInputHandle input,
            float facingYaw,
            float bodyYaw) {

        if (input.isStrafe())
            return facingYaw;

        Vector2 velocity = state.getHorizontalVelocity();
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);

        if (!input.hasHorizontalInput() || speed < EngineSetting.FACING_VELOCITY_EPSILON)
            return bodyYaw;

        return toYaw(velocity.x, velocity.y);
    }

    private float resolveTurnRate(EntityStateHandle state, float step, float delta) {

        if (delta <= 0f)
            return state.getBodyYawRate();

        float previous = state.getBodyYawRate();
        float smoothing = Math.min(1f, delta * EngineSetting.TURN_RATE_SMOOTHING);

        return previous + (step / delta - previous) * smoothing;
    }

    // Utility \\

    private static float toYaw(float x, float z) {
        return (float) Math.toDegrees(Math.atan2(x, z));
    }

    private static float toPitch(Vector3 facing) {

        float length = facing.length();

        if (length <= 0f)
            return 0f;

        return (float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, facing.y / length))));
    }

    private static float wrapDegrees(float degrees) {

        float turn = (float) EngineSetting.DEGREES_PER_FULL_ROTATION;
        float halfTurn = turn / 2f;
        float wrapped = degrees % turn;

        if (wrapped > halfTurn)
            return wrapped - turn;

        if (wrapped < -halfTurn)
            return wrapped + turn;

        return wrapped;
    }
}
