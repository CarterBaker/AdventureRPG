package editor.runtime.dev.sceneassets;

import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.world.WorldSystem;
import editor.bootstrap.sceneassetspipeline.sceneassetsmanager.SceneAssetsManager;
import editor.runtime.EditorSetting;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class DevSceneAssetsSystem extends SystemPackage {

    /*
     * Feeds the Scene Assets tab from this Dev window while the tab watches
     * it and this window holds the scene. A few times a second it casts the
     * ray through the middle of the view, the crosshair, and hands the block
     * it lands on to SceneAssetsManager; about once a second it casts a grid
     * of SCENE_ASSETS_SCAN_COLUMNS by SCENE_ASSETS_SCAN_ROWS rays evenly
     * across the view and hands every block they land on, so what is seen is
     * what the window shows. Every ray reuses one cast and two vectors, so
     * watching allocates nothing.
     */

    // Internal
    private SceneAssetsManager sceneAssetsManager;
    private RaycastManager raycastManager;
    private WorldSystem worldSystem;

    // Rays
    private BlockCastStruct castStruct;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Timing — the time since the crosshair was cast and since the view was scanned
    private float pickElapsed;
    private float scanElapsed;

    // Base \\

    @Override
    protected void create() {

        // Rays
        this.castStruct = new BlockCastStruct();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();

        // Timing
        this.pickElapsed = EditorSetting.SCENE_ASSETS_PICK_SECONDS;
        this.scanElapsed = EditorSetting.SCENE_ASSETS_SCAN_SECONDS;
    }

    @Override
    protected void get() {
        this.sceneAssetsManager = get(SceneAssetsManager.class);
        this.raycastManager = get(RaycastManager.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        GridInstance grid = worldSystem.getGridInstance();
        CameraInstance camera = window.getActiveCamera();

        if (grid == null || camera == null || !sceneAssetsManager.claim(window.getWindowID()))
            return;

        float deltaTime = internal.getDeltaTime();

        this.pickElapsed += deltaTime;
        this.scanElapsed += deltaTime;

        if (pickElapsed >= EditorSetting.SCENE_ASSETS_PICK_SECONDS) {
            this.pickElapsed = 0f;
            castThrough(grid, camera, 0f, 0f);
            sceneAssetsManager.showPointed(grid.getWorldHandle(), castStruct);
        }

        if (scanElapsed >= EditorSetting.SCENE_ASSETS_SCAN_SECONDS) {
            this.scanElapsed = 0f;
            scan(grid, camera);
        }
    }

    // A ray through the middle of every cell of the grid laid over the view
    private void scan(GridInstance grid, CameraInstance camera) {

        sceneAssetsManager.beginScan(grid.getWorldHandle());

        for (int row = 0; row < EditorSetting.SCENE_ASSETS_SCAN_ROWS; row++)
            for (int column = 0; column < EditorSetting.SCENE_ASSETS_SCAN_COLUMNS; column++) {

                castThrough(grid, camera,
                        toDeviceCoordinate(column, EditorSetting.SCENE_ASSETS_SCAN_COLUMNS),
                        toDeviceCoordinate(row, EditorSetting.SCENE_ASSETS_SCAN_ROWS));
                sceneAssetsManager.addSeen(castStruct);
            }

        sceneAssetsManager.endScan();
    }

    // The middle of the cell across the view, from -1 at one edge to 1 at the other
    private float toDeviceCoordinate(int cell, int cells) {
        return (2f * cell + 1f) / cells - 1f;
    }

    private void castThrough(GridInstance grid, CameraInstance camera, float deviceX, float deviceY) {
        camera.getPickRay(deviceX, deviceY, rayOrigin, rayDirection);
        raycastManager.castBlock(grid.getActiveChunkCoordinate(), rayOrigin, rayDirection,
                EditorSetting.SCENE_ASSETS_SCAN_DISTANCE, castStruct);
    }
}
