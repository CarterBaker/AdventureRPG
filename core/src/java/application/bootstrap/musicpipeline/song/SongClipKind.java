package application.bootstrap.musicpipeline.song;

public enum SongClipKind {

    /*
     * What a song clip lays on its lane, as a DAW's arrangement lays its
     * clips. NOTES plays its own packed notes on an instrument; SONG plays
     * another song inside this one, as a nested sequence, by reference, so
     * editing that song changes every place it is laid; SOUND plays a sound
     * through the sound pipeline, as an audio clip. A song names each by its
     * lower-case name.
     */

    NOTES,
    SONG,
    SOUND;

    // Values
    public static final SongClipKind[] VALUES = values();
}
