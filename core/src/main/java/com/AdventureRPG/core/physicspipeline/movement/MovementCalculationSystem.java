package com.AdventureRPG.core.physicspipeline.movement;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3Int;
import com.AdventureRPG.playermanager.StatisticsInstance;

public class MovementCalculationSystem extends SystemPackage {

    private Vector3 forward;
    private Vector3 right;
    private Vector3 localMove;

    @Override
    public void get() {
        forward = new Vector3();
        right = new Vector3();
        localMove = new Vector3();
    }

    public Vector3 calculate(
            StatisticsInstance statisticsInstance,
            Vector3 currentPosition,
            Vector3Int input,
            Vector3 cameraDirection) {

        forward.x = cameraDirection.x;
        forward.y = 0f;
        forward.z = cameraDirection.z;
        normalize(forward);

        right.x = -forward.z;
        right.y = 0f;
        right.z = forward.x;

        localMove.x = 0f;
        localMove.y = 0f;
        localMove.z = 0f;

        localMove.x += forward.x * input.z;
        localMove.z += forward.z * input.z;

        localMove.x += right.x * input.x;
        localMove.z += right.z * input.x;

        localMove.y += input.y;

        normalize(localMove);

        float scale = statisticsInstance.movementSpeed * internal.getDeltaTime();
        localMove.x *= scale;
        localMove.y *= scale;
        localMove.z *= scale;

        currentPosition.x += localMove.x;
        currentPosition.y += localMove.y;
        currentPosition.z += localMove.z;

        return currentPosition;
    }

    private void normalize(Vector3 v) {
        float lenSq = v.x * v.x + v.y * v.y + v.z * v.z;
        if (lenSq == 0f)
            return;

        float invLen = 1.0f / (float) Math.sqrt(lenSq);
        v.x *= invLen;
        v.y *= invLen;
        v.z *= invLen;
    }
}
