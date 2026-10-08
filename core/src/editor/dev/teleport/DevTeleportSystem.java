package editor.dev.teleport;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class DevTeleportSystem extends SystemPackage {

    /*
     * Moves this Dev window's player to a chunk for testing. Driven by the
     * command console's teleport command, which names the chunk by its X and
     * Y coordinate, wrapped around the world. The player is set down in the
     * middle of the chunk and settles onto its ground once it streams in.
     */

    // Internal
    private PlayerManager playerManager;

    // Base \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
    }

    // Management \\

    public void teleport(String chunkXText, String chunkYText) {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TELEPORT_NO_PLAYER);
            return;
        }

        if (!isWholeNumber(chunkXText) || !isWholeNumber(chunkYText)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TELEPORT_INVALID
                    + (isWholeNumber(chunkXText) ? chunkYText : chunkXText));
            return;
        }

        long chunkCoordinate = Coordinate2Long.pack(Integer.parseInt(chunkXText), Integer.parseInt(chunkYText));
        int center = EngineSetting.CHUNK_SIZE / 2;

        playerManager.teleportPlayerForWindow(windowID, chunkCoordinate, center, center);
        chunkCoordinate = playerManager.getPlayerPositionForWindow(windowID).getChunkCoordinate();

        log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_TELEPORT_MOVED
                + Coordinate2Long.unpackX(chunkCoordinate)
                + EditorSetting.COMMAND_MESSAGE_TELEPORT_SEPARATOR
                + Coordinate2Long.unpackY(chunkCoordinate));
    }

    // Utility \\

    private boolean isWholeNumber(String text) {

        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
