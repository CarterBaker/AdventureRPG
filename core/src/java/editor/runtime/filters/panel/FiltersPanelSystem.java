package editor.runtime.filters.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.photofilter.PhotoFilterHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.runtime.EditorSetting;
import editor.runtime.filters.FiltersSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FiltersPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the filters. When the
     * recorder's revision moves it refills the list — a row for every filter
     * a clip may hold by its display name, the one picked marked — and the
     * picked filter's details: its name, its contrast, vignette and grain,
     * and how it is laid. The status line follows the recorder's, so laying a
     * filter says what it did.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private RecorderManager recorderManager;

    // Menus
    private MenuInstance filtersMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.filtersMenu = menuManager.openMenu(EditorSetting.FILTERS_MENU, window);

        this.statusLabel = requireEntryPoint(FiltersSetting.ENTRY_STATUS);
        this.listTitle = requireEntryPoint(FiltersSetting.ENTRY_LIST_TITLE);

        requireEntryPoint(FiltersSetting.ENTRY_FILTERS);
        requireEntryPoint(FiltersSetting.ENTRY_DETAILS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = filtersMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Filters menu '" + EditorSetting.FILTERS_MENU + "' has no entry point " + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() == shownRevision)
            return;

        this.shownRevision = recorderManager.getRevision();
        statusLabel.setFontText(recorderManager.getStatusText());
        refreshFilters();
        refreshDetails();
    }

    // Filters \\

    // A row for every filter in palette order, each picked up to drag it onto a clip
    private void refreshFilters() {

        menuManager.ejectAll(filtersMenu, FiltersSetting.ENTRY_FILTERS);

        ObjectArrayList<PhotoFilterHandle> filters = recorderManager.getFilters();
        String selected = recorderManager.getSelectedFilterName();

        listTitle.setFontText(String.format(FiltersSetting.FORMAT_LIST_TITLE, filters.size()));

        if (filters.isEmpty()) {
            injectText(FiltersSetting.ENTRY_FILTERS, FiltersSetting.TEXT_NO_FILTERS);
            return;
        }

        for (int i = 0; i < filters.size(); i++) {

            PhotoFilterHandle filter = filters.get(i);
            ElementInstance row = menuManager.inject(
                    filtersMenu,
                    FiltersSetting.ENTRY_FILTERS,
                    filter.getPhotoFilterName().equals(selected)
                            ? FiltersSetting.TEMPLATE_FILTER_ROW_SELECTED
                            : FiltersSetting.TEMPLATE_FILTER_ROW);

            row.setOnDragArgOverride(filter.getPhotoFilterName());
            row.findChildById(FiltersSetting.ELEMENT_ROW_LABEL).setFontText(filter.getDisplayName());
            row.findChildById(FiltersSetting.ELEMENT_ROW_VALUE).setFontText(filter.getPhotoFilterName());
        }
    }

    // Details \\

    // What the picked filter does to the picture, as its file sets it, and how it is laid
    private void refreshDetails() {

        menuManager.ejectAll(filtersMenu, FiltersSetting.ENTRY_DETAILS);

        PhotoFilterHandle filter = recorderManager.resolveFilterHandle(recorderManager.getSelectedFilterName());

        if (filter != null) {
            injectHeader(filter.getDisplayName());
            injectDetail(FiltersSetting.TEXT_NAME, filter.getPhotoFilterName());
            injectDetail(FiltersSetting.TEXT_CONTRAST,
                    String.format(FiltersSetting.FORMAT_DECIMAL, filter.getContrast()));
            injectDetail(FiltersSetting.TEXT_VIGNETTE,
                    String.format(FiltersSetting.FORMAT_DECIMAL, filter.getVignette()));
            injectDetail(FiltersSetting.TEXT_GRAIN, String.format(FiltersSetting.FORMAT_DECIMAL, filter.getGrain()));
        }

        injectText(FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEXT_DRAG_HINT);
        injectText(FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEXT_FADE_HINT);
        injectText(FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEXT_APPLY_HINT);
        injectText(FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEXT_CHANGE_HINT);
        injectText(FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEXT_EDIT_HINT);
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                filtersMenu, FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(FiltersSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(FiltersSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Utility \\

    private void injectHeader(String text) {
        menuManager.inject(filtersMenu, FiltersSetting.ENTRY_DETAILS, FiltersSetting.TEMPLATE_HEADER_ROW)
                .findChildById(FiltersSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(filtersMenu, entryPoint, FiltersSetting.TEMPLATE_TEXT_ROW)
                .findChildById(FiltersSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }
}
