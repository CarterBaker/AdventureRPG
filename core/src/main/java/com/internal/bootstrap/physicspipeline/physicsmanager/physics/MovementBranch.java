package com.internal.bootstrap.physicspipeline.physicsmanager.physics;

import com.internal.bootstrap.entitypipeline.entityManager.StatisticsStruct;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementBranch extends BranchPackage {
    private Vector3 forward;
    private Vector3 right;

    @Override
    public void create() {
        forward = new Vector3();
        right = new Vector3();
    }

    public Vector3 calculate(
            Vector3Int input,
            Vector3 cameraDirection,
            Vector3 movement,
            StatisticsStruct statisticsStruct) {

        // Calculate forward direction (flatten Y to move horizontally)
        forward.x = cameraDirection.x;
        forward.y = 0f;
        forward.z = cameraDirection.z;
        forward.normalize();

        // Calculate right direction (perpendicular to forward)
        right.x = -forward.z;
        right.y = 0f;
        right.z = forward.x;

        // Reset movement vector
        movement.x = 0f;
        movement.y = 0f;
        movement.z = 0f;

        // Apply forward/backward movement (Z input)
        movement.x += forward.x * input.z;
        movement.z += forward.z * input.z;

        // Apply strafe movement (X input)
        movement.x += right.x * input.x;
        movement.z += right.z * input.x;

        // Apply vertical movement (Y input)
        movement.y += input.y;

        // Normalize to prevent faster diagonal movement
        movement.normalize();

        // Scale by movement speed and delta time
        float scale = statisticsStruct.movementSpeed * internal.getDeltaTime();
        movement.x *= scale;
        movement.y *= scale;
        movement.z *= scale;

        return movement;
    }
}