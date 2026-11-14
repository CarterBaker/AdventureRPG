package application.bootstrap.soundpipeline.soundcue;

import engine.root.StructPackage;

public class SoundSurfaceStruct extends StructPackage {

    /*
     * The four cues a surface sounds with — stepped on, struck while worked,
     * broken and set down — resolved once by SoundCueManager and shared by
     * every block and covering that names the surface.
     */

    // Cues
    private final int stepCueID;
    private final int digCueID;
    private final int breakCueID;
    private final int placeCueID;

    // Constructor \\

    public SoundSurfaceStruct(int stepCueID, int digCueID, int breakCueID, int placeCueID) {

        // Cues
        this.stepCueID = stepCueID;
        this.digCueID = digCueID;
        this.breakCueID = breakCueID;
        this.placeCueID = placeCueID;
    }

    // Accessible \\

    public int getStepCueID() {
        return stepCueID;
    }

    public int getDigCueID() {
        return digCueID;
    }

    public int getBreakCueID() {
        return breakCueID;
    }

    public int getPlaceCueID() {
        return placeCueID;
    }
}
