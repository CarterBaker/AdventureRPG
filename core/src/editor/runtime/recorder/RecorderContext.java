package editor.runtime.recorder;

import editor.runtime.recorder.panel.RecorderKeysSystem;
import editor.runtime.recorder.panel.RecorderPanelSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage {

    /*
     * Editor tab for the recorder: the recordings saved in the game's
     * directory, the transport that records, plays and stops in the Dev
     * window, the bin of takes to drag onto the timeline, the timeline's
     * lanes of clips to move, split, trim, stamp, mute and strip of channels,
     * zoomed and scrolled, each clip showing the nodes of what the player
     * does along it, the markers, the channels armed for the next take, the
     * export settings, and the Keys tab in place of the bin, where every
     * value the selected clip's take recorded is a curve of keys to retime,
     * move and bend, and every character's movements and gestures are nodes.
     * All state lives in RecorderManager; this tab only shows it and hands
     * clicks, drags and the wheel to RecorderBranch.
     */

    // Internal
    private RecorderPanelSystem recorderPanelSystem;
    private RecorderKeysSystem recorderKeysSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderPanelSystem = create(RecorderPanelSystem.class);
        this.recorderKeysSystem = create(RecorderKeysSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
