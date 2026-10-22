package editor.bootstrap.recorderpipeline.recording;

import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingInstance extends InstancePackage {

    /*
     * One recording open in the recorder: its name, the world it was made in,
     * its layers bottom to top, its markers in time order, and how it
     * exports. It runs as long as its longest layer reaches. For each channel
     * the topmost unmuted layer covering a moment is the one that plays it.
     */

    // Identity
    private String recordingName;
    private String worldName;

    // Timeline
    private ObjectArrayList<RecordingLayerStruct> layers;
    private ObjectArrayList<RecordingMarkerStruct> markers;

    // Export
    private RecordingExportStruct exportStruct;

    // Internal \\

    @Override
    protected void create() {

        // Timeline
        this.layers = new ObjectArrayList<>();
        this.markers = new ObjectArrayList<>();
    }

    // Constructor \\

    public void constructor(String recordingName, String worldName, RecordingExportStruct exportStruct) {

        // Identity
        this.recordingName = recordingName;
        this.worldName = worldName;

        // Export
        this.exportStruct = exportStruct;
    }

    // Layers \\

    public void addLayer(RecordingLayerStruct layer) {
        layers.add(layer);
    }

    public void removeLayer(int index) {
        layers.remove(index);
    }

    // The topmost layer playing the channel at the time, leaving out the one given, null when none does
    public RecordingLayerStruct findPlayingLayer(RecorderChannel channel, float time, RecordingLayerStruct excluded) {

        for (int i = layers.size() - 1; i >= 0; i--) {

            RecordingLayerStruct layer = layers.get(i);

            if (layer != excluded && layer.covers(channel, time))
                return layer;
        }

        return null;
    }

    // Markers \\

    // Kept in time order, a marker at the same moment as another going after it
    public int addMarker(RecordingMarkerStruct marker) {

        int index = 0;

        while (index < markers.size() && markers.get(index).getTime() <= marker.getTime())
            index++;

        markers.add(index, marker);
        return index;
    }

    public void removeMarker(int index) {
        markers.remove(index);
    }

    // Accessible \\

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public String getWorldName() {
        return worldName;
    }

    public ObjectArrayList<RecordingLayerStruct> getLayers() {
        return layers;
    }

    public ObjectArrayList<RecordingMarkerStruct> getMarkers() {
        return markers;
    }

    public RecordingExportStruct getExportStruct() {
        return exportStruct;
    }

    public float getDuration() {

        float duration = 0f;

        for (int i = 0; i < layers.size(); i++)
            duration = Math.max(duration, layers.get(i).getEndTime());

        return duration;
    }
}
