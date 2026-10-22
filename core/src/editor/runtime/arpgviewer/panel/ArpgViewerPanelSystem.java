package editor.runtime.arpgviewer.panel;

import java.io.File;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.arpgviewer.ArpgViewerSetting;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArpgViewerPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the selected file in
     * the viewer menu. Whenever InfoManager's revision moves it reads the
     * selected file from disk, formats the decoded tree as text, and injects
     * one line per row, or the reason the file could not be read.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private InfoManager infoManager;

    // Menus
    private MenuInstance viewerMenu;
    private ElementInstance lineList;

    // Lines
    private ObjectArrayList<ElementInstance> lineElements;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Lines
        this.lineElements = new ObjectArrayList<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.viewerMenu = menuManager.openMenu(ArpgViewerSetting.MENU_ARPG_VIEWER, window);
        this.lineList = viewerMenu.getEntryPoint(ArpgViewerSetting.ENTRY_LINES);

        if (lineList == null)
            throwException("ARPG viewer menu '" + ArpgViewerSetting.MENU_ARPG_VIEWER
                    + "' has no line list entry point.");
    }

    // Update \\

    @Override
    protected void update() {

        if (infoManager.getRevision() == shownRevision)
            return;

        this.shownRevision = infoManager.getRevision();
        showFile(infoManager.getSelectedFile());
    }

    private void showFile(File file) {

        clearLines();

        if (file == null) {
            setTitleText(ArpgViewerSetting.TITLE_NO_SELECTION);
            injectLine(ArpgViewerSetting.MESSAGE_NO_SELECTION, false);
            return;
        }

        setTitleText(file.getPath());

        if (!file.isFile()) {
            injectLine(ArpgViewerSetting.MESSAGE_NOT_ON_DISK, false);
            return;
        }

        try {
            injectLines(ArpgUtility.formatText(ArpgUtility.readObject(file)));
        } catch (InternalException e) {
            injectLine(e.getMessage(), true);
        }
    }

    // Lines \\

    private void injectLines(String text) {

        String[] lines = text.split(ArpgViewerSetting.LINE_SEPARATOR);

        lineElements.ensureCapacity(lines.length);

        for (int i = 0; i < lines.length; i++)
            injectLine(lines[i], false);
    }

    private void injectLine(String text, boolean error) {

        String template = error
                ? ArpgViewerSetting.MENU_LINE_ERROR
                : ArpgViewerSetting.MENU_LINE;

        lineElements.add(menuManager.inject(
                viewerMenu,
                ArpgViewerSetting.ENTRY_LINES,
                template,
                element -> element.setFontText(text)));
    }

    private void clearLines() {

        for (int i = 0; i < lineElements.size(); i++)
            menuManager.eject(viewerMenu, ArpgViewerSetting.ENTRY_LINES, lineElements.get(i));

        lineElements.clear();
        lineList.setScrollY(0f);
    }

    // Utility \\

    private void setTitleText(String text) {

        ElementInstance label = viewerMenu.getEntryPoint(ArpgViewerSetting.ENTRY_TITLE);

        if (label != null)
            label.setFontText(text);
    }
}
