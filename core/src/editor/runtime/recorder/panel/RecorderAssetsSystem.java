package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecordingAssetSectionStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingAssetStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class RecorderAssetsSystem extends SystemPackage {

    /*
     * Fills the Assets pane below the timeline while it shows: every asset the
     * open recording uses across all its takes, one section for each kind
     * that has any, each headed by its name and how many it holds and pressed
     * to open or close it. An opened section lists its assets by name, each
     * beside what brings it in, or marked missing when the game can no longer
     * find it. The pane is drawn again whenever the recorder gathers the
     * assets again or a section is opened or closed.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderPanelSystem recorderPanelSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance assetsTitle;

    // State
    private boolean shown;
    private int shownAssetsRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownAssetsRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderPanelSystem = get(RecorderPanelSystem.class);
    }

    @Override
    protected void awake() {

        this.recorderMenu = recorderPanelSystem.getRecorderMenu();
        this.assetsTitle = requireEntryPoint(RecorderSetting.ENTRY_ASSETS_TITLE);

        requireEntryPoint(RecorderSetting.ENTRY_ASSET_ROWS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = recorderMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recorder menu '" + EditorSetting.RECORDER_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        boolean assets = recorderManager.getPane() == RecorderPane.ASSETS;

        if (assets != shown) {
            this.shown = assets;
            this.shownAssetsRevision = EngineSetting.INDEX_NOT_FOUND;
        }

        if (assets && recorderManager.getAssetsRevision() != shownAssetsRevision)
            refreshAssets();
    }

    // Refresh \\

    private void refreshAssets() {

        this.shownAssetsRevision = recorderManager.getAssetsRevision();

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_ASSET_ROWS);

        if (!recorderManager.hasRecording()) {
            assetsTitle.setFontText(RecorderSetting.TEXT_ASSETS_TITLE);
            recorderPanelSystem.injectText(RecorderSetting.ENTRY_ASSET_ROWS, RecorderSetting.TEXT_NO_RECORDING);
            return;
        }

        assetsTitle.setFontText(String.format(
                RecorderSetting.FORMAT_ASSETS_TITLE, recorderManager.getRecording().getRecordingName()));

        RecordingAssetSectionStruct[] sections = recorderManager.getAssetSections();

        for (int i = 0; i < sections.length; i++)
            if (sections[i].size() > 0)
                injectSection(sections[i], i);
    }

    // A section's head, and with it opened a row for each of its assets
    private void injectSection(RecordingAssetSectionStruct section, int sectionIndex) {

        boolean opened = recorderManager.isAssetSectionOpened(sectionIndex);
        ElementInstance head = menuManager.inject(
                recorderMenu, RecorderSetting.ENTRY_ASSET_ROWS, RecorderSetting.TEMPLATE_ASSET_SECTION_ROW);

        head.setActionArgOverride(Integer.toString(sectionIndex));
        head.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(
                (opened ? RecorderSetting.TEXT_TRACK_OPENED : RecorderSetting.TEXT_TRACK_CLOSED)
                        + section.getKind().getLabel());
        head.findChildById(RecorderSetting.ELEMENT_ROW_VALUE).setFontText(Integer.toString(section.size()));

        if (!opened)
            return;

        for (int i = 0; i < section.size(); i++)
            injectAsset(section.get(i));
    }

    private void injectAsset(RecordingAssetStruct asset) {

        ElementInstance row = menuManager.inject(recorderMenu, RecorderSetting.ENTRY_ASSET_ROWS, asset.isMissing()
                ? RecorderSetting.TEMPLATE_ASSET_ROW_MISSING
                : RecorderSetting.TEMPLATE_ASSET_ROW);

        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(asset.getName());
        row.findChildById(RecorderSetting.ELEMENT_ROW_VALUE).setFontText(describeOwners(asset));
    }

    // What brings the asset in: missing above all, else the first that does and how many more
    private String describeOwners(RecordingAssetStruct asset) {

        if (asset.isMissing())
            return RecorderSetting.TEXT_ASSET_MISSING;

        return asset.getOwnerCount() > 1
                ? String.format(RecorderSetting.FORMAT_ASSET_OWNERS, asset.getFirstOwner(), asset.getOwnerCount() - 1)
                : asset.getFirstOwner();
    }
}
