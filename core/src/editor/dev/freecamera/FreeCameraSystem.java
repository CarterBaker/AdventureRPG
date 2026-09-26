package editor.dev.freecamera;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.editor.EditorSetting;
import engine.root.SystemPackage;

public class FreeCameraSystem extends SystemPackage {

    /*
     * Switches this Dev window's player between playing and free flying — the
     * DevContext counterpart of PlayerSystem. While free flying, the window
     * streams the world around a camera that flies with no physics and no
     * character; switching back lands the character on safe ground beneath
     * wherever the camera flew. Driven by the command console's fly command.
     */

    // Internal
    private PlayerManager playerManager;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
    }

    // Management \\

    public void toggleFreeCamera() {

        WindowInstance window = context.getWindow();
        boolean freeCamera = !playerManager.isFreeCameraForWindow(window.getWindowID());

        playerManager.setFreeCameraForWindow(window.getWindowID(), freeCamera);
        log(window.getTitle() + (freeCamera
                ? EditorSetting.COMMAND_MESSAGE_FREE_CAMERA_ON
                : EditorSetting.COMMAND_MESSAGE_FREE_CAMERA_OFF));
    }
}
