package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingMarkerStruct extends StructPackage {

    /*
     * A named bookmark on a recording's timeline — dropped by hand with Mark
     * at the playhead, or by the recorder itself wherever a resident fell
     * during a take. Markers show as ticks on the ruler, the playhead jumps
     * between them, and they can set the export range; they never change what
     * plays.
     */

    // Marker
    private final float time;
    private String name;

    // Constructor \\

    public RecordingMarkerStruct(float time, String name) {

        // Marker
        this.time = time;
        this.name = name;
    }

    // Accessible \\

    public float getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
