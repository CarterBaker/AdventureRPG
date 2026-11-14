package editor.bootstrap.recorderpipeline.recording;

import engine.root.StructPackage;

public class RecordingOverlayTrackStruct extends StructPackage {

    /*
     * The interface through a take: one stream of frames for each overlay a
     * window draws over the world, indexed by the overlay's place in
     * RecorderOverlay.
     */

    // Streams
    private final RecordingOverlayStreamStruct[] streams;

    // Constructor \\

    public RecordingOverlayTrackStruct() {

        // Streams
        this.streams = new RecordingOverlayStreamStruct[RecorderOverlay.VALUES.length];

        for (int i = 0; i < streams.length; i++)
            streams[i] = new RecordingOverlayStreamStruct(RecorderOverlay.VALUES[i]);
    }

    // Management \\

    public void shiftTimes(float seconds) {
        for (int i = 0; i < streams.length; i++)
            streams[i].shiftTimes(seconds);
    }

    // Accessible \\

    public RecordingOverlayStreamStruct getStream(RecorderOverlay overlay) {
        return streams[overlay.ordinal()];
    }

    public long getByteCount() {

        long byteCount = 0L;

        for (int i = 0; i < streams.length; i++)
            byteCount += streams[i].getByteCount();

        return byteCount;
    }
}
