package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.InputSystem.Movement;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.math.Vector3;

public class PlayerSystem {

    // Game Manager
    private final Settings settings;
    private final GameManager GameManager;
    private final WorldSystem WorldSystem;

    // Player
    public final PlayerCamera camera;
    private final Movement Movement;

    // Player Position
    private Vector3 BlockPosition = new Vector3();
    private Vector3Int WorldPosition = new Vector3Int();

    // Stats
    private float PlayerSpeed = 1f; // TODO: Move all player statistics to their own class savable by json

    // Base \\

    public PlayerSystem(GameManager GameManager) {

        // Game Manager
        this.settings = GameManager.settings;
        this.GameManager = GameManager;
        this.WorldSystem = GameManager.WorldSystem;

        // Player
        this.camera = new PlayerCamera(settings.FOV, settings.windowWidth, settings.windowHeight);
        this.Movement = new Movement(GameManager);
    }

    public void Awake() {
        camera.Awake();
    }

    public void Start() {
        Movement.SetSpeed(PlayerSpeed);
    }

    public void Update() {

    }

    public void Render() {

    }

    // Movement \\

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

        // 1. First add up the total world position in tiles and then the float value
        // between those tiles
        Vector3 position = Position();

        // 2. Use the movement engine to calculate how far the player should move based
        // on input
        position = Movement.Calculate(position, input, camera.Direction());

        // 3. Wrap how far the player moved between blocks
        BlockPosition = GameManager.WorldSystem.WrapAroundBlock(position);

        // 4. Subtract the block position from the total calculated position
        Vector3Int newPosition = new Vector3Int(
                Math.round(position.x - BlockPosition.x),
                Math.round(position.y - BlockPosition.y),
                Math.round(position.z - BlockPosition.z));

        // 5. Wrap the calculated int position around the world
        WorldPosition = GameManager.WorldSystem.WrapAroundWorld(newPosition);

        // 6. Update the world
        WorldSystem.MoveTo(Position());
    }

}
