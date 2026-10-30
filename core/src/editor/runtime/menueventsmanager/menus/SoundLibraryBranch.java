package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.soundlibrarypipeline.soundlibrarymanager.SoundLibraryManager;
import engine.root.BranchPackage;

public class SoundLibraryBranch extends BranchPackage {

    /*
     * Menu event handlers for the Sound Library tab. Every action works on
     * the one sound library all tabs share. The transport plays, pauses,
     * stops, rewinds and loops the selected sound, and dragging along the
     * scrub bar moves its cue to the moment under the cursor. A folder's head
     * opens or closes it. A row's play button samples its sound from the
     * start; pressing the row itself selects its sound and picks it up, to be
     * laid on the timeline of whichever Recorder tab it is let go over, and
     * Add lays the selected sound at the Recorder's playhead. Rows hand back
     * the name of the sound they show, folder heads their folder.
     */

    // Internal
    private SoundLibraryManager soundLibraryManager;
    private RecorderBranch recorderBranch;
    private InputManager inputManager;

    // Base \\

    @Override
    protected void get() {
        this.soundLibraryManager = get(SoundLibraryManager.class);
        this.recorderBranch = get(RecorderBranch.class);
        this.inputManager = get(InputManager.class);
    }

    // Transport \\

    public void play() {
        soundLibraryManager.play();
    }

    public void pause() {
        soundLibraryManager.pause();
    }

    public void togglePlayback() {
        soundLibraryManager.togglePlayback();
    }

    public void stop() {
        soundLibraryManager.stop();
    }

    public void rewind() {
        soundLibraryManager.rewind();
    }

    public void toggleLoop() {
        soundLibraryManager.toggleLoop();
    }

    public void scrubSound(WindowInstance window, ElementInstance track) {

        if (track.getComputedW() > 0f)
            soundLibraryManager.scrub(Math.clamp(
                    (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW(), 0f, 1f));
    }

    // Sounds \\

    public void toggleFolder(String folderName) {
        soundLibraryManager.toggleFolder(folderName);
    }

    public void previewSound(String soundName) {
        soundLibraryManager.previewSound(soundName);
    }

    // The sound is selected on the press and carried toward the Recorder's lanes until let go
    public void dragSound(String soundName, WindowInstance window) {
        soundLibraryManager.selectSound(soundName);
        recorderBranch.dragSound(soundName, window);
    }

    public void addToRecorder() {
        soundLibraryManager.addToRecorder();
    }
}
