package editor.runtime.recorderassets;

import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderAssetsSystem;
import engine.root.ContextPackage;

public class RecorderAssetsContext extends ContextPackage implements RecorderTabProvider {

    /*
     * Editor tab listing every asset the open recording uses across its
     * takes, section by section, each marked missing when the game can no
     * longer find it; the list is gathered again while this tab is worked
     * in. All state lives in RecorderManager; clicks go to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderAssetsSystem recorderAssetsSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderAssetsSystem = create(RecorderAssetsSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return RecorderSetting.MENU_ASSETS;
    }

    @Override
    public RecorderPane getRecorderPane() {
        return RecorderPane.ASSETS;
    }
}
