package editor.runtime.brushes.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushMode;
import editor.bootstrap.brushpipeline.util.BrushShape;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.brushes.BrushesSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BrushesMenuSystem extends SystemPackage {

    /*
     * Opens the Brushes menu: one row per preset with its name and its shape
     * and mode, the one in use highlighted and every row carrying its
     * preset's name; below it a button per shape and per mode, the preset's
     * own highlighted and each carrying its shape's or mode's name, and one
     * row per number with its value between buttons carrying the field's
     * name; and the brush size of the canvas the tools target, or a dash
     * where it takes no brush. The rows are rebuilt only when the presets
     * move, and the size only when the tools do.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private BrushManager brushManager;
    private ToolManager toolManager;

    // Menus
    private MenuInstance brushesMenu;

    // Rows
    private ObjectArrayList<ElementInstance> presetElements;
    private ObjectArrayList<ElementInstance> shapeElements;
    private ObjectArrayList<ElementInstance> modeElements;
    private ObjectArrayList<ElementInstance> fieldElements;

    // Status
    private int shownBrushRevision;
    private int shownToolRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.presetElements = new ObjectArrayList<>();
        this.shapeElements = new ObjectArrayList<>();
        this.modeElements = new ObjectArrayList<>();
        this.fieldElements = new ObjectArrayList<>();

        // Status
        this.shownBrushRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownToolRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.brushManager = get(BrushManager.class);
        this.toolManager = get(ToolManager.class);
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

        if (brushManager.getRevision() != shownBrushRevision) {
            shownBrushRevision = brushManager.getRevision();
            refreshPresets();
        }

        if (toolManager.getRevision() != shownToolRevision) {
            shownToolRevision = toolManager.getRevision();
            refreshSize();
        }
    }

    // Presets \\

    private void refreshPresets() {

        BrushHandle active = brushManager.getActiveBrush();
        ObjectArrayList<BrushHandle> brushes = brushManager.getBrushes();

        clear(presetElements, BrushesSetting.ENTRY_PRESETS);
        clear(shapeElements, BrushesSetting.ENTRY_SHAPES);
        clear(modeElements, BrushesSetting.ENTRY_MODES);
        clear(fieldElements, BrushesSetting.ENTRY_FIELDS);

        for (int i = 0; i < brushes.size(); i++)
            presetElements.add(injectPreset(brushes.get(i), brushes.get(i) == active));

        for (int i = 0; i < BrushShape.VALUES.length; i++)
            shapeElements.add(injectChoice(
                    BrushesSetting.ENTRY_SHAPES,
                    BrushShape.VALUES[i] == active.getShape()
                            ? BrushesSetting.TEMPLATE_SHAPE_BUTTON_SELECTED
                            : BrushesSetting.TEMPLATE_SHAPE_BUTTON,
                    ArpgUtility.toEnumName(BrushShape.VALUES[i]),
                    BrushShape.VALUES[i].getLabel()));

        for (int i = 0; i < BrushMode.VALUES.length; i++)
            modeElements.add(injectChoice(
                    BrushesSetting.ENTRY_MODES,
                    BrushMode.VALUES[i] == active.getMode()
                            ? BrushesSetting.TEMPLATE_MODE_BUTTON_SELECTED
                            : BrushesSetting.TEMPLATE_MODE_BUTTON,
                    ArpgUtility.toEnumName(BrushMode.VALUES[i]),
                    BrushMode.VALUES[i].getLabel()));

        for (int i = 0; i < BrushField.VALUES.length; i++)
            fieldElements.add(injectField(BrushField.VALUES[i], active.getValue(BrushField.VALUES[i])));
    }

    private ElementInstance injectPreset(BrushHandle brush, boolean selected) {

        String template = selected
                ? BrushesSetting.TEMPLATE_PRESET_ROW_SELECTED
                : BrushesSetting.TEMPLATE_PRESET_ROW;
        String detail = brush.getShape().getLabel() + BrushesSetting.DETAIL_SEPARATOR + brush.getMode().getLabel();

        return menuManager.inject(
                brushesMenu, BrushesSetting.ENTRY_PRESETS, template,
                element -> {
                    element.setActionArgOverride(brush.getBrushName());
                    setChildText(element, BrushesSetting.ELEMENT_PRESET_LABEL, brush.getDisplayName());
                    setChildText(element, BrushesSetting.ELEMENT_PRESET_DETAIL, detail);
                });
    }

    private ElementInstance injectChoice(int entry, String template, String arg, String text) {
        return menuManager.inject(
                brushesMenu, entry, template,
                element -> {
                    element.setActionArgOverride(arg);
                    setChildText(element, BrushesSetting.ELEMENT_CHOICE_LABEL, text);
                });
    }

    private ElementInstance injectField(BrushField field, float value) {

        String arg = ArpgUtility.toEnumName(field);

        return menuManager.inject(
                brushesMenu, BrushesSetting.ENTRY_FIELDS, BrushesSetting.TEMPLATE_FIELD_ROW,
                element -> {
                    setChildText(element, BrushesSetting.ELEMENT_FIELD_LABEL, field.getLabel());
                    setChildText(element, BrushesSetting.ELEMENT_FIELD_VALUE, formatValue(value));
                    setChildArg(element, BrushesSetting.ELEMENT_FIELD_LOWER, arg);
                    setChildArg(element, BrushesSetting.ELEMENT_FIELD_RAISE, arg);
                });
    }

    // Size \\

    private void refreshSize() {

        ToolCanvas canvas = toolManager.getCanvas();
        ElementInstance label = brushesMenu.getEntryPoint(BrushesSetting.ENTRY_SIZE);

        if (label != null)
            label.setFontText(canvas != null && canvas.takesBrush()
                    ? BrushesSetting.SIZE_PREFIX + Math.round(toolManager.getSize(canvas))
                    : BrushesSetting.NO_VALUE);
    }

    // Utility \\

    private void clear(ObjectArrayList<ElementInstance> elements, int entry) {

        for (int i = 0; i < elements.size(); i++)
            menuManager.eject(brushesMenu, entry, elements.get(i));

        elements.clear();
    }

    private void setChildText(ElementInstance element, String childID, String text) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setFontText(text);
    }

    private void setChildArg(ElementInstance element, String childID, String arg) {

        ElementInstance child = element.findChildById(childID);

        if (child != null)
            child.setActionArgOverride(arg);
    }

    // Whole numbers as they are, fractions to the precision the panel steps them by
    private String formatValue(float value) {

        float precision = (float) EditorSetting.BRUSH_VALUE_PRECISION;

        return value == Math.round(value)
                ? Integer.toString(Math.round(value))
                : Float.toString(Math.round(value * precision) / precision);
    }
}
