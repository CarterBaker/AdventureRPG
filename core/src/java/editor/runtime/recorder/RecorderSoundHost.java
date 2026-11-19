package editor.runtime.recorder;

public interface RecorderSoundHost {

    /*
     * A Recorder tab that carries a session's sound itself, so a media file
     * with no picture plays while no Preview tab is open. Its context creates
     * the same sound system a Dev window does; the recorder binds the first
     * such tab open when it finds no Dev window to play music in.
     */
}
