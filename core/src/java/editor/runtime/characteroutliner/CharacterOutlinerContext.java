package editor.runtime.characteroutliner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.OutlinerProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterOutlinerContext extends ContextPackage implements CharacterViewRowSource, FileMenuProvider {

    /*
     * The one docker window listing what the character and animation tools
     * work on, for the window worked in last among those that list outliner
     * rows: on a Character Editor layer how the preview stands and the
     * features, sliders, bones, anchors or worn parts to pick, the rig's
     * bones in Rig, the face's controls and bones on the Face layer; on an
     * Animation Editor the open clip and its bones. Picking a row picks it
     * for that tool; the state lives in its manager, so every window of the
     * tool follows. The list is made again whenever the window it follows
     * changes or that window's list may have. The shared tool keys work
     * while this window is focused.
     */

    // Internal
    private PropertiesManager propertiesManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Shown — the outliner followed and its revision when last asked, and this window's own revision over both
    private OutlinerProvider shownOutliner;
    private int shownOutlinerRevision;
    private int revision;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.propertiesManager = get(PropertiesManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    // Moves whenever the window followed changes or its own list moves
    @Override
    public int getRowRevision() {

        OutlinerProvider outliner = propertiesManager.getOutliner();
        int outlinerRevision = outliner != null ? outliner.getOutlinerRevision() : 0;

        if (outliner != shownOutliner || outlinerRevision != shownOutlinerRevision) {
            this.shownOutliner = outliner;
            this.shownOutlinerRevision = outlinerRevision;
            revision++;
        }

        return revision;
    }

    @Override
    public String getRowTitle() {

        OutlinerProvider outliner = propertiesManager.getOutliner();

        return outliner != null ? outliner.getOutlinerTitle() : EditorSetting.CHARACTER_OUTLINER_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        OutlinerProvider outliner = propertiesManager.getOutliner();

        if (outliner != null)
            outliner.buildOutlinerRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {

        OutlinerProvider outliner = propertiesManager.getOutliner();

        return outliner != null ? outliner.getOutlinerTemplates() : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {

        OutlinerProvider outliner = propertiesManager.getOutliner();

        if (outliner != null)
            outliner.stepOutlinerRow(key, steps);
    }

    // File Menu \\

    // The File and Edit menus of the window followed, the character editor's before any
    @Override
    public FileMenuTableStruct getFileMenuTable() {

        OutlinerProvider outliner = propertiesManager.getOutliner();

        return outliner instanceof FileMenuProvider provider
                ? provider.getFileMenuTable()
                : EditorSetting.FILE_MENU_CHARACTER;
    }
}
