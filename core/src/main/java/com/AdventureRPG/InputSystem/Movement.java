package com.AdventureRPG.InputSystem;

import com.AdventureRPG.Core.GameSystem;
import com.AdventureRPG.PlayerSystem.Statistics;
import com.AdventureRPG.Util.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

public class Movement extends GameSystem {

    // Game Manager
    private Statistics statistics;

    // Temp
    private Vector3 forward;
    private Vector3 right;
    private Vector3 localMove;

    // Base \\

    public Movement(Statistics statistics) {

        // Game Manager
        this.statistics = statistics;
    }

    @Override
    public void init() {

        // Temp
        this.forward = new Vector3();
        this.right = new Vector3();
        this.localMove = new Vector3();
    }

    // Movement \\

    public Vector3 calculate(Vector3 currentPosition, Vector3Int input, Vector3 cameraDirection) {

        // Calculate horizontal forward vector (XZ plane)
        forward.set(cameraDirection.x, 0f, cameraDirection.z).nor();

        // Right = perpendicular to forward
        right.set(-forward.z, 0f, forward.x);

        // Build movement vector based on input
        localMove.set(0f, 0f, 0f);
        localMove.mulAdd(forward, input.z);
        localMove.mulAdd(right, input.x);
        localMove.y += input.y;

        // Normalize to prevent diagonal speed boost
        if (!localMove.isZero())
            localMove.nor();

        // Scale by movement speed and delta time
        localMove.scl(statistics.movementSpeed * Gdx.graphics.getDeltaTime());

        // Add the movement to current position in place
        currentPosition.add(localMove);

        // Return the mutated currentPosition for chaining or assignment
        return currentPosition;
    }
}
