package editor.runtime.recorder;

import editor.bootstrap.recorderpipeline.recording.RecorderPane;

public interface RecorderTabProvider {

    /*
     * Names what one Recorder tab shows: the menu it opens, and the pane of
     * the recording it is — the Keys, or the Timeline — or null for a tab
     * that is no pane, the transport. A pane tab worked in becomes the pane
     * the recorder's keys and deletes act on.
     */

    String getRecorderMenu();

    RecorderPane getRecorderPane();
}
