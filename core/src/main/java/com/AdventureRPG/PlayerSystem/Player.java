package com.AdventureRPG.PlayerSystem;

import com.badlogic.gdx.math.Vector3;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.GameManager;
import com.AdventureRPG.MovementSystem.Movement;

public class Player {

    // Player Position
    private Vector3 BlockPosition = new Vector3();
    private Vector3Int WorldPosition = new Vector3Int();

    // Game Manager
    private final GameManager GameManager;

    // Movement
    private float PlayerSpeed = 1f;
    private final Movement Movement;

    public Player(GameManager GameManager) {

        // Game Manager
        this.GameManager = GameManager;

        // Movement
        this.Movement = new Movement(GameManager);
        Movement.SetSpeed(PlayerSpeed);

    }

    // Movement

    public Vector3 Position() {
        return new Vector3(
                WorldPosition.x + BlockPosition.x,
                WorldPosition.y + BlockPosition.y,
                WorldPosition.z + BlockPosition.z);
    }

    public Vector3 BlockPosition() {
        return BlockPosition;
    }

    public Vector3Int WorldPosition() {
        return WorldPosition;
    }

    public void SetSpeed(float input) {
        PlayerSpeed = input;
        Movement.SetSpeed(PlayerSpeed);
    }

    public void Move(Vector3Int input) {

        // 1. First add up the total world position in tiles and then the float value between those tiles
        Vector3 position = Position();

        // 2. Use the movement engine to calculate how far the player should move based on input
        position = Movement.Calculate(position, input);

        // 3. Wrap how far the player moved between blocks
        BlockPosition = GameManager.WrapAroundBlock(position);

        // 4. Subtract the block position from the total calculated position
        Vector3Int newPosition = new Vector3Int(
                Math.round(position.x - BlockPosition.x),
                Math.round(position.y - BlockPosition.y),
                Math.round(position.z - BlockPosition.z));

        // 5. Wrap the calculated int position around the world
        WorldPosition = GameManager.WrapAroundWorld(newPosition);
    }

}
