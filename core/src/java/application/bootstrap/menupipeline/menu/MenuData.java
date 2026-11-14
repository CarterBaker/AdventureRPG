package application.bootstrap.menupipeline.menu;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuData extends DataPackage {

    /*
     * Persistent menu definition. Holds identity, input lock flags, named
     * entry point IDs, whether this menu defines a canvas_area element, and
     * the sound cues it plays as it opens, closes and is clicked, each
     * INDEX_NOT_FOUND for a menu silent at that moment.
     * Owned by MenuHandle, created with new during bootstrap.
     */

    // Identity
    private final String name;
    private final boolean lockInput;
    private final boolean raycastInput;
    private final boolean hasCanvasArea;

    // Entry Points
    private final ObjectArrayList<String> entryPoints;

    // Sound
    private final int openCueID;
    private final int closeCueID;
    private final int clickCueID;

    // Constructor \\

    public MenuData(
            String name,
            boolean lockInput,
            boolean raycastInput,
            boolean hasCanvasArea,
            ObjectArrayList<String> entryPoints,
            int openCueID,
            int closeCueID,
            int clickCueID) {
        this.name = name;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;
        this.hasCanvasArea = hasCanvasArea;
        this.entryPoints = entryPoints;
        this.openCueID = openCueID;
        this.closeCueID = closeCueID;
        this.clickCueID = clickCueID;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public boolean isLockInput() {
        return lockInput;
    }

    public boolean isRaycastInput() {
        return raycastInput;
    }

    public boolean hasCanvasArea() {
        return hasCanvasArea;
    }

    public ObjectArrayList<String> getEntryPoints() {
        return entryPoints;
    }

    public int getOpenCueID() {
        return openCueID;
    }

    public int getCloseCueID() {
        return closeCueID;
    }

    public int getClickCueID() {
        return clickCueID;
    }
}