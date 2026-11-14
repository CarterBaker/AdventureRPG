package application.runtime.sound;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SoundListenerSystem extends SystemPackage {

    /*
     * Hears the world from this window's camera. Every frame, once the camera
     * has moved, the listener is placed at it — in the chunk the window's
     * player stands in, facing the way the camera faces — while this window
     * has focus, or while no window does, so in the editor the sounds of the
     * world are heard from the game window last clicked into; a window pinned
     * as the one heard is heard instead whatever has focus. Its own ambience,
     * music and heartbeat play while it is heard, unless it is pinned quiet. A
     * window with no player yet, as at the main menu, places nothing.
     */

    // Internal
    private SoundManager soundManager;
    private PlayerManager playerManager;
    private WindowManager windowManager;

    // Base \\

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.playerManager = get(PlayerManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        int windowID = context.getWindow().getWindowID();

        if (!isHeardHere() || !playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        soundManager.placeListener(
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                camera.getPosition(),
                camera.getDirection(),
                camera.getUp());
    }

    // Accessible \\

    // True while this window is the one heard: pinned as it, or with none pinned it has focus, or no window does
    public boolean isHeardHere() {

        int pinnedWindowID = soundManager.getPinnedWindowID();

        if (pinnedWindowID != EngineSetting.INDEX_NOT_FOUND)
            return pinnedWindowID == context.getWindow().getWindowID();

        WindowInstance focused = windowManager.getFocusedWindow();

        return focused == null || focused == context.getWindow();
    }

    // True while this window's own ambience, music and heartbeat are heard: it is the one heard, and not pinned quiet
    public boolean isAmbientHere() {
        return isHeardHere() && !(soundManager.isPinnedQuiet()
                && soundManager.getPinnedWindowID() == context.getWindow().getWindowID());
    }
}
