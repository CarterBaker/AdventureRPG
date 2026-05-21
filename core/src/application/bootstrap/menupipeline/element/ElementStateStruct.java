package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import engine.graphics.color.Color;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementStateStruct extends StructPackage {

    /*
     * Defines a visual state override for an element. Used by click_state,
     * on_hover_enter, on_hover, and on_hover_exit. All four are identical in
     * capability — sprite, layout, color, text, children, master reference,
     * and an optional method callback.
     */

    // Master
    private final ElementHandle master;

    // Visuals
    private final String spriteOverride;
    private final String textOverride;
    private final Color colorOverride;
    private final LayoutStruct layoutOverride;

    // Callback
    private final String actionClass;
    private final String actionMethod;
    private final String actionArg;

    // Children
    private final ObjectArrayList<MenuNodeStruct> children;

    public ElementStateStruct(
            ElementHandle master,
            String spriteOverride,
            String textOverride,
            Color colorOverride,
            LayoutStruct layoutOverride,
            String actionClass,
            String actionMethod,
            String actionArg,
            ObjectArrayList<MenuNodeStruct> children) {
        this.master = master;
        this.spriteOverride = spriteOverride;
        this.textOverride = textOverride;
        this.colorOverride = colorOverride;
        this.layoutOverride = layoutOverride;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    public ElementHandle getMaster() {
        return master;
    }

    public boolean hasMaster() {
        return master != null;
    }

    public String getSpriteOverride() {
        return spriteOverride;
    }

    public boolean hasSpriteOverride() {
        return spriteOverride != null;
    }

    public String getTextOverride() {
        return textOverride;
    }

    public boolean hasTextOverride() {
        return textOverride != null;
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

    public boolean hasLayoutOverride() {
        return layoutOverride != null;
    }

    public String getActionClass() {
        return actionClass;
    }

    public String getActionMethod() {
        return actionMethod;
    }

    public String getActionArg() {
        return actionArg;
    }

    public boolean hasAction() {
        return actionClass != null && actionMethod != null;
    }

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}