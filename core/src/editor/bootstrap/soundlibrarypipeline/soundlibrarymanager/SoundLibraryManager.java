package editor.bootstrap.soundlibrarypipeline.soundlibrarymanager;

import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class SoundLibraryManager extends ManagerPackage {

    /*
     * Owns the sound library every Sound Library tab shows: every loaded
     * sound, gathered again whenever another loads, grouped by the folder it
     * lies in and in name order within it, each folder opened or closed here; the sound
     * selected; and the one preview that samples it. The transport plays the
     * selected sound from the cue, pauses and resumes it, stops it back to
     * the start, rewinds it, loops it or lets it end, and the cue is set by
     * scrubbing anywhere along its length — a playing preview jumps there. A
     * sound previewed from its row is selected and played from its start.
     * The selected sound is added to the recorder's timeline at its playhead
     * here; dragging it there goes through the recorder itself. A preview
     * that runs out returns its cue to the start. Each change moves the
     * revision the tab redraws on.
     */

    // Internal
    private SoundManager soundManager;
    private RecorderManager recorderManager;

    // Library
    private ObjectArrayList<SoundHandle> sounds;
    private ObjectOpenHashSet<String> closedFolders;
    private int gatheredSoundCount;

    // Selection
    private int selectedSoundID;

    // Preview
    private int previewVoice;
    private int previewSoundID;
    private boolean looping;
    private float cue;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Library
        this.sounds = new ObjectArrayList<>();
        this.closedFolders = new ObjectOpenHashSet<>();
        this.gatheredSoundCount = EngineSetting.INDEX_NOT_FOUND;

        // Selection
        this.selectedSoundID = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.previewVoice = EngineSetting.INDEX_NOT_FOUND;
        this.previewSoundID = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.statusText = EditorSetting.SOUND_LIBRARY_STATUS_READY;
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        gatherSounds();
        releaseEndedPreview();
    }

    // Every loaded sound folder by folder in name order, gathered again whenever another has loaded since
    private void gatherSounds() {

        if (soundManager.getSoundCount() == gatheredSoundCount)
            return;

        this.gatheredSoundCount = soundManager.getSoundCount();
        sounds.clear();
        soundManager.collectSoundHandles(sounds);
        sounds.sort(this::compareSounds);
        revision++;
    }

    // By folder first, so each folder's sounds lie together, then by name
    private int compareSounds(SoundHandle first, SoundHandle second) {

        int byFolder = resolveFolder(first).compareToIgnoreCase(resolveFolder(second));

        return byFolder != 0 ? byFolder : first.getSoundName().compareToIgnoreCase(second.getSoundName());
    }

    // A preview that ran out lets go of its voice, its cue back at the start
    private void releaseEndedPreview() {

        if (previewVoice == EngineSetting.INDEX_NOT_FOUND
                || soundManager.isVoicePlaying(previewVoice) || soundManager.isVoicePaused(previewVoice))
            return;

        this.previewVoice = EngineSetting.INDEX_NOT_FOUND;
        this.cue = 0f;
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_ENDED);
    }

    // Selection \\

    public void selectSound(String soundName) {

        if (!soundManager.hasSound(soundName))
            return;

        int soundID = soundManager.getSoundIDFromSoundName(soundName);

        if (soundID == selectedSoundID)
            return;

        stop();
        this.selectedSoundID = soundID;
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_SELECTED + soundName);
    }

    // The sound selected and sampled from its start
    public void previewSound(String soundName) {

        selectSound(soundName);
        stop();
        play();
    }

    // Folders \\

    public void toggleFolder(String folderName) {

        if (!closedFolders.remove(folderName))
            closedFolders.add(folderName);

        revision++;
    }

    public boolean isFolderOpened(String folderName) {
        return !closedFolders.contains(folderName);
    }

    // The folder a sound lies in below the sound directory, empty for one at its root
    public String resolveFolder(SoundHandle soundHandle) {

        String soundName = soundHandle.getSoundName();
        int separator = soundName.lastIndexOf(EngineSetting.PATH_SEPARATOR);

        return separator < 0 ? "" : soundName.substring(0, separator);
    }

    // Transport \\

    // Plays the selected sound from the cue, or carries a paused preview on
    public void play() {

        if (!hasSelection()) {
            setStatus(EditorSetting.SOUND_LIBRARY_STATUS_NO_SOUND);
            return;
        }

        if (soundManager.isVoicePaused(previewVoice)) {
            soundManager.resumeVoice(previewVoice);
            setStatus(EditorSetting.SOUND_LIBRARY_STATUS_PLAYING + getSelectedSound().getSoundName());
            return;
        }

        if (soundManager.isVoicePlaying(previewVoice))
            return;

        if (!soundManager.isAudible()) {
            setStatus(EditorSetting.SOUND_LIBRARY_STATUS_SILENT);
            return;
        }

        this.previewSoundID = selectedSoundID;
        this.previewVoice = soundManager.playSound(
                selectedSoundID, EngineSetting.DEFAULT_SOUND_VOLUME, EngineSetting.DEFAULT_SOUND_PITCH, cue);
        soundManager.setVoiceLooping(previewVoice, looping);
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_PLAYING + getSelectedSound().getSoundName());
    }

    // Pauses the preview where it stands, or carries a paused one on
    public void pause() {

        if (soundManager.isVoicePaused(previewVoice)) {
            play();
            return;
        }

        if (!soundManager.isVoicePlaying(previewVoice))
            return;

        this.cue = soundManager.getVoiceOffset(previewVoice);
        soundManager.pauseVoice(previewVoice);
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_PAUSED);
    }

    public void togglePlayback() {

        if (soundManager.isVoicePlaying(previewVoice))
            pause();
        else
            play();
    }

    // The one way a preview ends early; its cue goes back to the start
    public void stop() {

        this.cue = 0f;

        if (previewVoice == EngineSetting.INDEX_NOT_FOUND)
            return;

        soundManager.stopVoice(previewVoice);
        this.previewVoice = EngineSetting.INDEX_NOT_FOUND;
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_STOPPED);
    }

    // Back to the start, a playing preview carrying on from there
    public void rewind() {
        seek(0f);
    }

    public void toggleLoop() {

        this.looping = !looping;
        soundManager.setVoiceLooping(previewVoice, looping);
        revision++;
    }

    // The cue set a share of the way along the selected sound, a preview of it jumping there
    public void scrub(float fraction) {
        if (hasSelection())
            seek(fraction * getSelectedSound().getLength());
    }

    private void seek(float offset) {

        this.cue = offset;
        soundManager.setVoiceOffset(previewVoice, offset);
    }

    // Recorder \\

    // The selected sound laid on the recorder's timeline at its playhead
    public void addToRecorder() {

        if (!hasSelection()) {
            setStatus(EditorSetting.SOUND_LIBRARY_STATUS_NO_SOUND);
            return;
        }

        recorderManager.placeSoundAtPlayhead(selectedSoundID);
        setStatus(EditorSetting.SOUND_LIBRARY_STATUS_ADDED + getSelectedSound().getSoundName());
    }

    // Utility \\

    private void setStatus(String statusText) {
        this.statusText = statusText;
        revision++;
    }

    // Accessible \\

    public ObjectArrayList<SoundHandle> getSounds() {
        return sounds;
    }

    public boolean hasSelection() {
        return selectedSoundID != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getSelectedSoundID() {
        return selectedSoundID;
    }

    public SoundHandle getSelectedSound() {
        return soundManager.getSoundHandleFromSoundID(selectedSoundID);
    }

    public boolean isPlaying() {
        return soundManager.isVoicePlaying(previewVoice);
    }

    public boolean isPaused() {
        return soundManager.isVoicePaused(previewVoice);
    }

    public boolean isLooping() {
        return looping;
    }

    // Seconds into the selected sound the preview stands, or the cue while none plays
    public float getPlayhead() {
        return previewVoice != EngineSetting.INDEX_NOT_FOUND && previewSoundID == selectedSoundID
                ? soundManager.getVoiceOffset(previewVoice)
                : cue;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
