package editor.runtime.recorder;

import editor.bootstrap.recorderpipeline.recording.RecorderPane;

public interface RecorderTabProvider {

    /*
     * Names what one Recorder tab shows: the menu it opens, and the pane of
     * the recording it is — the Keys, the Bin or the Assets — or null for a
     * tab that is no pane, the timeline and the settings. A pane tab worked
     * in becomes the pane the recorder's keys and deletes act on.
     */

    String getRecorderMenu();

    RecorderPane getRecorderPane();
}
