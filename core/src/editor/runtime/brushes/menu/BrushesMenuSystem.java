package editor.runtime.brushes.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.runtime.brushes.BrushesSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BrushesMenuSystem extends SystemPackage {

    /*
     * Opens the Brushes menu: one row per brush with its name and the shape
     * of its tip, the one in use highlighted and every row carrying its
     * brush's name, rebuilt only when the brushes move.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private BrushManager brushManager;

    // Menus
    private MenuInstance brushesMenu;

    // Rows
    private ObjectArrayList<ElementInstance> brushElements;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.brushElements = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.brushManager = get(BrushManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.brushesMenu = menuManager.openMenu(BrushesSetting.MENU_BRUSHES, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (brushManager.getRevision() == shownRevision)
            return;

        shownRevision = brushManager.getRevision();
        refreshBrushes();
    }

    // Brushes \\

    private void refreshBrushes() {

        for (int i = 0; i < brushElements.size(); i++)
            menuManager.eject(brushesMenu, BrushesSetting.ENTRY_BRUSHES, brushElements.get(i));

        brushElements.clear();

        BrushHandle active = brushManager.getActiveBrush();
        ObjectArrayList<BrushHandle> brushes = brushManager.getBrushes();

        for (int i = 0; i < brushes.size(); i++)
            brushElements.add(injectBrush(brushes.get(i), brushes.get(i) == active));
    }

    private ElementInstance injectBrush(BrushHandle brush, boolean selected) {

        String template = selected
                ? BrushesSetting.TEMPLATE_BRUSH_ROW_SELECTED
                : BrushesSetting.TEMPLATE_BRUSH_ROW;

        return menuManager.inject(
                brushesMenu, BrushesSetting.ENTRY_BRUSHES, template,
                element -> {
                    element.setActionArgOverride(brush.getBrushName());
                    setChildText(element, BrushesSetting.ELEMENT_BRUSH_LABEL, brush.getDisplayName());
                    setChildText(element, BrushesSetting.ELEMENT_BRUSH_DETAIL, brush.getShape().getLabel());
                });
    }

    // Utility \\

    private void setChildText(ElementInstance element, String childID, String text) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setFontText(text);
    }
}
