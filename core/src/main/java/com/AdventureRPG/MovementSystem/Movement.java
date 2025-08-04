package com.AdventureRPG.MovementSystem;

import com.badlogic.gdx.math.Vector3;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.Util.Vector3Int;

public class Movement {

    private float MovementSpeed = 1f;

    private final GameManager GameManager;

    private final Vector3 forward;
    private final Vector3 right;
    private final Vector3 localMove;
    private final Vector3 move;

    public Movement(GameManager GameManager) {
        this.GameManager = GameManager;

        this.forward = new Vector3();
        this.right = new Vector3();
        this.localMove = new Vector3();
        this.move = new Vector3();
    }

    public void SetSpeed(float input) {
        MovementSpeed = input * GameManager.settings.BASE_SPEED;
    }

    public Vector3 Calculate(Vector3 currentPosition, Vector3Int input, Vector3 cameraDirection) {

        float delta = GameManager.DeltaTime();

        // Calculate horizontal forward vector (XZ plane)
        forward.set(cameraDirection.x, 0f, cameraDirection.z).nor();

        // Right = perpendicular to forward
        right.set(forward.z, 0f, -forward.x);

        // Build movement vector
        localMove.set(0f, 0f, 0f);
        localMove.mulAdd(forward, input.z);
        localMove.mulAdd(right, input.x);
        localMove.y += input.y;

        // Normalize to prevent diagonal speed boost
        if (!localMove.isZero())
            localMove.nor();

        // Scale by movement speed and delta
        move.set(localMove).scl(MovementSpeed * delta);

        // Return result without allocating a new Vector3
        return move.add(currentPosition); // returns a new vector, still
    }
}
