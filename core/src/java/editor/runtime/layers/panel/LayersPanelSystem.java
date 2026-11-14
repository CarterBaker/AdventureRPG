package editor.runtime.layers.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import editor.runtime.layers.LayersSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayersPanelSystem extends SystemPackage {

    /*
     * Binds the Layers tab to a UI render target and opens its menu, then
     * lists the layer stack of the window worked in last: one row per layer
     * and group shown, top first, indented by depth, a group's marker folding
     * or opening it, an eye on every layer that can be hidden, drawn faint
     * while it is, and the layer the window's tools work on highlighted. A
     * folded group holding that layer has its name drawn in the accent
     * color, so the active layer is always found. Every row, marker and eye
     * carries its layer's key. The rows are laid out again only when the
     * window, its stack's revision or the groups folded change.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private LayersManager layersManager;

    // Menus
    private MenuInstance layersMenu;

    // Colors
    private MenuColorStruct holdingColor;
    private MenuColorStruct hiddenColor;

    // Rows
    private ObjectArrayList<LayerNodeStruct> roots;
    private ObjectArrayList<ElementInstance> rowElements;

    // Shown
    private LayersProvider shownProvider;
    private int shownRevision;
    private int shownFoldRevision;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.holdingColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.COLOR_CHANNEL_MAX);
        this.hiddenColor = new MenuColorStruct(ThemeColor.TEXT, LayersSetting.EYE_HIDDEN_ALPHA);

        // Rows
        this.roots = new ObjectArrayList<>();
        this.rowElements = new ObjectArrayList<>();

        // Shown
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownFoldRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.layersManager = get(LayersManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.layersMenu = menuManager.openMenu(LayersSetting.MENU_LAYERS, window);
    }

    // Update \\

    @Override
    protected void update() {

        LayersProvider provider = layersManager.getProvider();
        int revision = provider != null ? provider.getLayerRevision() : EngineSetting.INDEX_NOT_FOUND;

        if (provider == shownProvider && revision == shownRevision
                && layersManager.getRevision() == shownFoldRevision)
            return;

        this.shownProvider = provider;
        this.shownRevision = revision;
        this.shownFoldRevision = layersManager.getRevision();
        layout(provider);
    }

    // Layout \\

    private void layout(LayersProvider provider) {

        for (int i = 0; i < rowElements.size(); i++)
            menuManager.eject(layersMenu, LayersSetting.ENTRY_ROWS, rowElements.get(i));

        rowElements.clear();
        roots.clear();

        setEntryText(LayersSetting.ENTRY_TITLE,
                provider != null ? provider.getLayerTitle() : LayersSetting.TITLE_NO_LAYERS);

        if (provider == null)
            return;

        provider.buildLayers(roots);
        injectRows(roots, 0);
    }

    private void injectRows(ObjectArrayList<LayerNodeStruct> nodes, int depth) {

        for (int i = 0; i < nodes.size(); i++) {

            LayerNodeStruct node = nodes.get(i);
            boolean expanded = node.isGroup() && layersManager.isExpanded(node);

            rowElements.add(injectRow(node, depth, expanded));

            if (expanded)
                injectRows(node.getChildren(), depth + 1);
        }
    }

    // A layer the window's tools work on is highlighted; a group holding it is named in the accent color while folded
    private ElementInstance injectRow(LayerNodeStruct node, int depth, boolean expanded) {

        String template = node.isActive() && !node.isGroup()
                ? LayersSetting.TEMPLATE_LAYER_ROW_ACTIVE
                : LayersSetting.TEMPLATE_LAYER_ROW;
        float indent = depth * LayersSetting.INDENT_PIXELS;

        return menuManager.inject(
                layersMenu, LayersSetting.ENTRY_ROWS, template,
                element -> {
                    element.setActionArgOverride(node.getLayerKey());

                    bindChild(element, LayersSetting.ELEMENT_TOGGLE, node, indent, node.isGroup());
                    bindChild(
                            element, LayersSetting.ELEMENT_EYE, node,
                            indent + LayersSetting.TOGGLE_WIDTH_PIXELS, node.isHideable());
                    setChildText(element, LayersSetting.ELEMENT_TOGGLE_LABEL, resolveMarker(node, expanded));

                    ElementInstance eyeIcon = element.findChildById(LayersSetting.ELEMENT_EYE_ICON);

                    if (eyeIcon != null && !node.isVisible())
                        eyeIcon.setColorOverride(hiddenColor);

                    ElementInstance label = element.findChildById(LayersSetting.ELEMENT_LABEL);

                    if (label == null)
                        return;

                    label.setPositionOverride(toOffset(
                            indent + LayersSetting.TOGGLE_WIDTH_PIXELS + LayersSetting.EYE_WIDTH_PIXELS));
                    label.setFontText(node.getLabel());

                    if (node.isGroup() && node.isActive() && !expanded)
                        label.setColorOverride(holdingColor);
                });
    }

    private String resolveMarker(LayerNodeStruct node, boolean expanded) {

        if (!node.isGroup())
            return EngineSetting.HIERARCHY_LEAF_MARKER;

        return expanded ? EngineSetting.HIERARCHY_EXPANDED_MARKER : EngineSetting.HIERARCHY_COLLAPSED_MARKER;
    }

    // Utility \\

    // A child kept where it applies, carrying the layer's key and placed at its offset, taken away otherwise
    private void bindChild(ElementInstance element, String childId, LayerNodeStruct node, float x, boolean kept) {

        ElementInstance child = element.findChildById(childId);

        if (child == null)
            return;

        if (!kept) {
            menuManager.eject(element, child);
            return;
        }

        child.setActionArgOverride(node.getLayerKey());
        child.setPositionOverride(toOffset(x));
    }

    private void setChildText(ElementInstance element, String childId, String text) {

        ElementInstance child = element.findChildById(childId);

        if (child != null)
            child.setFontText(text);
    }

    private void setEntryText(int entryPoint, String text) {

        ElementInstance label = layersMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(text);
    }

    private DimensionVector2Struct toOffset(float x) {
        return new DimensionVector2Struct(DimensionValueStruct.ofAbsolute(x), DimensionValueStruct.ofAbsolute(0f));
    }
}
