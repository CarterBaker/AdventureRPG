package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingMarkerStruct extends StructPackage {

    /*
     * A named moment on a recording's timeline — dropped by hand while
     * recording or playing, or by the recorder itself where something fell.
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
