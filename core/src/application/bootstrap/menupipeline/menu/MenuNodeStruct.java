package application.bootstrap.menupipeline.menu;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import engine.graphics.color.Color;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuNodeStruct extends StructPackage {

    /*
     * One node in a resolved menu element tree. References a master ElementHandle,
     * carries optional per-placement overrides, and owns an ordered child node
     * list.
     * Built during bootstrap and owned by MenuHandle or parent MenuNodeStructs.
     * Master may be null initially for deferred ref nodes; resolved via
     * setMaster().
     * Children are always fully resolved at build time — ElementSystem never
     * touches
     * master.getChildren() at runtime, only node.getChildren().
     */

    // Master
    private ElementHandle master;

    // Overrides
    private final String spriteNameOverride;
    private final String textOverride;
    private final Color colorOverride;
    private final Runnable clickActionOverride;
    private final MenuAwareAction menuAwareActionOverride;
    private final String actionClassOverride;
    private final String actionMethodOverride;
    private final String actionArgOverride;
    private LayoutStruct layoutOverride;

    // Tree
    private final ObjectArrayList<MenuNodeStruct> children;

    // Constructor — master + children only \\
    public MenuNodeStruct(ElementHandle master, ObjectArrayList<MenuNodeStruct> children) {
        this.master = master;
        this.spriteNameOverride = null;
        this.textOverride = null;
        this.colorOverride = null;
        this.clickActionOverride = null;
        this.menuAwareActionOverride = null;
        this.actionClassOverride = null;
        this.actionMethodOverride = null;
        this.actionArgOverride = null;
        this.layoutOverride = null;
        this.children = children != null ? children : new ObjectArrayList<>();
    }

    // Constructor — full \\
    public MenuNodeStruct(
            ElementHandle master,
            String spriteNameOverride,
            String textOverride,
            Color colorOverride,
            Runnable clickActionOverride,
            MenuAwareAction menuAwareActionOverride,
            String actionClassOverride,
            String actionMethodOverride,
            String actionArgOverride,
            LayoutStruct layoutOverride,
            ObjectArrayList<MenuNodeStruct> children) {
        this.master = master;
        this.spriteNameOverride = spriteNameOverride;
        this.textOverride = textOverride;
        this.colorOverride = colorOverride;
        this.clickActionOverride = clickActionOverride;
        this.menuAwareActionOverride = menuAwareActionOverride;
        this.actionClassOverride = actionClassOverride;
        this.actionMethodOverride = actionMethodOverride;
        this.actionArgOverride = actionArgOverride;
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

    public Runnable getClickActionOverride() {
        return clickActionOverride;
    }

    public MenuAwareAction getMenuAwareActionOverride() {
        return menuAwareActionOverride;
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

    public boolean hasActionOverride() {
        return actionClassOverride != null;
    }

    public LayoutStruct getLayoutOverride() {
        return layoutOverride;
    }

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public boolean hasColorOverride() {
        return colorOverride != null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}