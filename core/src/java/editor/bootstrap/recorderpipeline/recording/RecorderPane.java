package editor.bootstrap.recorderpipeline.recording;

public enum RecorderPane {

    /*
     * The panes of the recorder, each a tab beside the Recorder's transport:
     * the Keys of the selected clip's take or the graph of the value selected,
     * or the Timeline of clips. The pane worked in last is the one the
     * recorder's deletes act on. Each pane opens the Layers tab on the view it
     * works with: the Keys on the take's layers, the Timeline on the takes of
     * the open recording to lay.
     */

    KEYS(RecorderLayerView.LAYERS),
    TIMELINE(RecorderLayerView.TAKES);

    // Values
    public static final RecorderPane[] VALUES = values();

    // Internal
    private final RecorderLayerView layerView;

    // Constructor \\

    RecorderPane(RecorderLayerView layerView) {
        this.layerView = layerView;
    }

    // Accessible \\

    public RecorderLayerView getLayerView() {
        return layerView;
    }
}
