package application.bootstrap.soundpipeline.sound;

public enum SoundCategory {

    /*
     * What a sound is, so each kind is turned up or down together: music, the
     * effects of what happens in the world, the ambience beneath them, the
     * interface's own clicks and chimes, and voices. A sound's companion file
     * names its category by the lower-case constant name.
     */

    MUSIC,
    EFFECT,
    AMBIENT,
    INTERFACE,
    VOICE;

    // Values
    public static final SoundCategory[] VALUES = values();
}
