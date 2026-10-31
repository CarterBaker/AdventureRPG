package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class RecordingSoundBranch extends BranchPackage {

    /*
     * Reads and edits the sounds of a take as the Keys pane's Sounds layer
     * shows them. A sound is found under a moment of its clip, on the layer's
     * own row among them all or on its own sound's row among its kind; it is
     * retimed within its clip, added from the sound library as the library
     * plays it, given another pitch — a sound that plays through running
     * shorter or longer with it — another length no longer than it can run,
     * or another sound heard as loud as the library would play it, and every
     * one of a sound is removed at once. Every edit changes the take, so every
     * clip of it hears the change. Nothing here marks the recording changed;
     * RecorderManager does, once, for each edit.
     */

    // Internal
    private SoundManager soundManager;

    // Base \\

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    // Find \\

    // The sound under the moment within its clip — any sound when no name is given, else a sound of that name — the
    // one begun last when several are, reached this near; -1 for none
    int findSoundAt(
            RecordingClipStruct clip,
            RecordingSoundTrackStruct track,
            int nameIndex,
            float time,
            float tolerance) {

        float takeTime = clip.toTakeTime(time);
        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int i = track.findFirstSounding(takeTime - tolerance);
                i < track.size() && track.getTime(i) <= takeTime + tolerance; i++)
            if ((nameIndex == EngineSetting.INDEX_NOT_FOUND || track.getNameIndex(i) == nameIndex)
                    && takeTime <= track.getTime(i) + track.getDuration(i) + tolerance && isHeardIn(clip, track, i))
                found = i;

        return found;
    }

    // True when any of the sound lies within the span of the take its clip plays
    boolean isHeardIn(RecordingClipStruct clip, RecordingSoundTrackStruct track, int index) {
        return track.getTime(index) < clip.getOutPoint()
                && track.getTime(index) + track.getDuration(index) > clip.getInPoint();
    }

    // How many sounds of the name the track holds
    int countSounds(RecordingSoundTrackStruct track, int nameIndex) {

        int count = 0;

        for (int i = 0; i < track.size(); i++)
            if (track.getNameIndex(i) == nameIndex)
                count++;

        return count;
    }

    // The moment of the timeline a sound carried begins at, kept within its clip
    float resolveLanding(RecordingClipStruct clip, float start) {
        return Math.clamp(start, clip.getStart(), clip.getEnd());
    }

    // Add \\

    // The sound heard from the moment as the sound library plays it, through once; its place comes back
    int addSound(RecordingSoundTrackStruct track, SoundHandle soundHandle, float takeTime) {
        return track.addEvent(
                takeTime,
                soundHandle.getPlayedLength(),
                soundHandle.getSoundName(),
                EngineSetting.DEFAULT_SOUND_VOLUME,
                EngineSetting.DEFAULT_SOUND_PITCH,
                false,
                0L,
                0f,
                0f,
                0f,
                soundHandle.getVolume(),
                0f);
    }

    // Change \\

    // A sound that plays through runs as much shorter or longer as it is played faster or slower
    void setPitch(RecordingSoundTrackStruct track, int index, float pitch) {

        if (!isLooping(track, index))
            track.setDuration(index, track.getDuration(index) * track.getPitch(index) / pitch);

        track.setPitch(index, pitch);
    }

    void setLength(RecordingSoundTrackStruct track, int index, float length) {
        track.setDuration(index, Math.clamp(length, EditorSetting.RECORDER_SOUND_MIN_LENGTH_SECONDS,
                Math.max(EditorSetting.RECORDER_SOUND_MIN_LENGTH_SECONDS, resolveLongestLength(track, index))));
    }

    // Another sound heard in its place, as loud against the library's own volume as the old one was against its
    // own, and through once unless it loops
    void replaceSound(RecordingSoundTrackStruct track, int index, SoundHandle soundHandle) {

        String previousName = track.getSoundName(index);
        float heardGain = soundManager.hasSound(previousName)
                ? track.getHeardGain(index) / soundManager.getSoundHandleFromSoundName(previousName).getVolume()
                        * soundHandle.getVolume()
                : track.getVolume(index) * soundHandle.getVolume();

        track.setSound(index, soundHandle.getSoundName(), heardGain);

        if (!soundHandle.isLoop())
            track.setDuration(index, soundHandle.getPlayedLength() / track.getPitch(index));
    }

    // Every sound of the name gone from the track, the last first so the rest keep their places; how many comes back
    int removeEvery(RecordingSoundTrackStruct track, String soundName) {

        int removed = 0;

        for (int i = track.size() - 1; i >= 0; i--) {

            if (!track.getSoundName(i).equals(soundName))
                continue;

            track.removeEvent(i);
            removed++;
        }

        return removed;
    }

    // Utility \\

    // How long the sound can run at its pitch: through once, or for as long as it is asked when it loops or the game
    // no longer has it
    float resolveLongestLength(RecordingSoundTrackStruct track, int index) {
        return isLooping(track, index) || !soundManager.hasSound(track.getSoundName(index))
                ? Float.MAX_VALUE
                : soundManager.getSoundHandleFromSoundName(track.getSoundName(index)).getPlayedLength()
                        / track.getPitch(index);
    }

    boolean isLooping(RecordingSoundTrackStruct track, int index) {
        return soundManager.hasSound(track.getSoundName(index))
                && soundManager.getSoundHandleFromSoundName(track.getSoundName(index)).isLoop();
    }

    // The number typed, NaN when it is not one or lies outside the bounds
    float parseAmount(String text, float minimum, float maximum) {

        try {
            float value = Float.parseFloat(text.trim());
            return value >= minimum && value <= maximum ? value : Float.NaN;
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }
}
