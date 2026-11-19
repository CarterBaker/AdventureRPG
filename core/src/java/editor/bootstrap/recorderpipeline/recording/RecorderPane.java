package editor.bootstrap.recorderpipeline.recording;

public enum RecorderPane {

    /*
     * The panes of the recorder, each a tab beside the Recorder's transport:
     * the Keys of the selected clip's take or the graph of the value selected,
     * or the Timeline of clips. The pane worked in last is the one the
     * recorder's deletes act on.
     */

    KEYS,
    TIMELINE;
}
