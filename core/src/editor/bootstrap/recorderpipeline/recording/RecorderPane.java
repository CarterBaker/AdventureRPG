package editor.bootstrap.recorderpipeline.recording;

public enum RecorderPane {

    /*
     * The panes of the recorder, each a tab beside the Recorder's timeline:
     * the Keys of the selected clip's take with the graph of the value
     * selected, the Bin of takes beside the selected clip's edits, or the
     * Assets the whole recording uses. The pane worked in last is the one the
     * recorder's deletes act on and whose assets it keeps gathered.
     */

    KEYS,
    BIN,
    ASSETS;
}
