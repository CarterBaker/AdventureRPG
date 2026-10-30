package editor.runtime.recorder;

import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.recorder.panel.RecorderAssetsSystem;
import editor.runtime.recorder.panel.RecorderKeysSystem;
import editor.runtime.recorder.panel.RecorderPanelSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage implements ToolCanvasProvider {

    /*
     * Editor tab for the recorder: the recordings saved in the game's
     * directory, the transport that records, plays and stops in the Dev
     * window, the timeline's lanes of clips to move, split, trim, stamp, mute
     * and strip of channels, zoomed and scrolled, sounds dropped onto them from
     * the sound library, each clip showing the nodes
     * of what the player does along it, the channels armed for the next take,
     * the export settings, and below the timeline one of three panes: the
     * Keys, where every value the selected clip's take recorded is a curve of
     * keys to retime, move and bend on a graph, and every character's
     * movements and gestures are nodes; the Bin of takes to drag onto the
     * timeline beside the selected clip's edits; and the Assets the whole
     * recording uses, section by section. All state lives in RecorderManager;
     * this tab only shows it and hands clicks, drags and the wheel to
     * RecorderBranch. The tab is a tool canvas, the timeline, for as long as
     * it is open.
     */

    // Internal
    private RecorderPanelSystem recorderPanelSystem;
    private RecorderKeysSystem recorderKeysSystem;
    private RecorderAssetsSystem recorderAssetsSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderPanelSystem = create(RecorderPanelSystem.class);
        this.recorderKeysSystem = create(RecorderKeysSystem.class);
        this.recorderAssetsSystem = create(RecorderAssetsSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.RECORDER;
    }
}
