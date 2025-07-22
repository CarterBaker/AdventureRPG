package com.AdventureRPG.MovementSystem;

import com.badlogic.gdx.math.Vector3;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.Util.Vector3Int;

public class Movement {

    private float MovementSpeed = 1f;

    private final GameManager GameManager;

    public Movement(GameManager GameManager) {
        this.GameManager = GameManager;
    }

    public void SetSpeed(float input) {
        MovementSpeed = input * GameManager.settings.BASE_SPEED;
    }

    public Vector3 Calculate(Vector3 currentPosition, Vector3Int input) {
        float delta = GameManager.DeltaTime();

        float X = input.x * MovementSpeed * delta;
        float Y = input.y * MovementSpeed * delta;
        float Z = input.z * MovementSpeed * delta;

        return new Vector3(
                currentPosition.x + X,
                currentPosition.y + Y,
                currentPosition.z + Z);
    }

}
