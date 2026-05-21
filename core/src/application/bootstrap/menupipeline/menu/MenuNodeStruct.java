package application.bootstrap.menupipeline.menu;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.util.LayoutStruct;
import engine.graphics.color.Color;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuNodeStruct extends StructPackage {

    /*
     * One node in a resolved menu element tree. References a master ElementHandle,
     * carries optional per-placement overrides for on_click and on_drag (method
     * callbacks only), sprite, text, color, layout, and owns an ordered child list.
     *
     * Hover states are not overridable at the node level — they are defined on
     * the master ElementHandle and apply uniformly wherever it is placed.
     */

    // Master
    private ElementHandle master;

    // Overrides — visuals
    private final String spriteNameOverride;
    private final String textOverride;
    private final Color colorOverride;
    private LayoutStruct layoutOverride;

    // Overrides — on_click
    private final String actionClassOverride;
    private final String actionMethodOverride;
    private final String actionArgOverride;

    // Overrides — on_drag
    private final String onDragClassOverride;
    private final String onDragMethodOverride;
    private final String onDragArgOverride;

    // Tree
    private final ObjectArrayList<MenuNodeStruct> children;

    // Constructor — master + children only \\
    public MenuNodeStruct(ElementHandle master, ObjectArrayList<MenuNodeStruct> children) {
        this.master = master;
        this.spriteNameOverride = null;
        this.textOverride = null;
        this.colorOverride = null;
        this.layoutOverride = null;
        this.actionClassOverride = null;
        this.actionMethodOverride = null;
        this.actionArgOverride = null;
        this.onDragClassOverride = null;
        this.onDragMethodOverride = null;
        this.onDragArgOverride = null;
        this.children = children != null ? children : new ObjectArrayList<>();
    }

    // Constructor — full \\
    public MenuNodeStruct(
            ElementHandle master,
            String spriteNameOverride,
            String textOverride,
            Color colorOverride,
            String actionClassOverride,
            String actionMethodOverride,
            String actionArgOverride,
            String onDragClassOverride,
            String onDragMethodOverride,
            String onDragArgOverride,
            LayoutStruct layoutOverride,
            ObjectArrayList<MenuNodeStruct> children) {
        this.master = master;
        this.spriteNameOverride = spriteNameOverride;
        this.textOverride = textOverride;
        this.colorOverride = colorOverride;
        this.actionClassOverride = actionClassOverride;
        this.actionMethodOverride = actionMethodOverride;
        this.actionArgOverride = actionArgOverride;
        this.onDragClassOverride = onDragClassOverride;
        this.onDragMethodOverride = onDragMethodOverride;
        this.onDragArgOverride = onDragArgOverride;
        this.layoutOverride = layoutOverride;
        this.children = children != null ? children : new ObjectArrayList<>();
    }

    // Deferred Resolution \\
    public void setMaster(ElementHandle master) {
        this.master = master;
    }

    public void setLayoutOverride(LayoutStruct layoutOverride) {
        this.layoutOverride = layoutOverride;
    }

    // Accessible \\
    public ElementHandle getMaster() {
        return master;
    }

    public String getSpriteNameOverride() {
        return spriteNameOverride;
    }

    public String getTextOverride() {
        return textOverride;
    }

    public Color getColorOverride() {
        return colorOverride;
    }

    public boolean hasColorOverride() {
        return colorOverride != null;
    }

    public LayoutStruct getLayoutOverride() {
        return layoutOverride;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public String getActionClassOverride() {
        return actionClassOverride;
    }

    public String getActionMethodOverride() {
        return actionMethodOverride;
    }

    public String getActionArgOverride() {
        return actionArgOverride;
    }

    public String getOnDragClassOverride() {
        return onDragClassOverride;
    }

    public String getOnDragMethodOverride() {
        return onDragMethodOverride;
    }

    public String getOnDragArgOverride() {
        return onDragArgOverride;
    }
}