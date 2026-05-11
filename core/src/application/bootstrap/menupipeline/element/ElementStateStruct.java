package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import engine.graphics.color.Color;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementStateStruct extends StructPackage {

    /*
     * Immutable visual state override applied when an element is hovered or
     * clicked. Null master means apply overrides on top of the owning element
     * itself. Non-null master swaps the base entirely to the referenced handle.
     * Empty children list means inherit from the base at runtime.
     */

    private final ElementHandle master;
    private final String spriteOverride;
    private final String textOverride;
    private final Color colorOverride;
    private final LayoutStruct layoutOverride;
    private final String actionClassOverride;
    private final String actionMethodOverride;
    private final String actionArgOverride;
    private final ObjectArrayList<MenuNodeStruct> children;

    public ElementStateStruct(
            ElementHandle master,
            String spriteOverride,
            String textOverride,
            Color colorOverride,
            LayoutStruct layoutOverride,
            String actionClassOverride,
            String actionMethodOverride,
            String actionArgOverride,
            ObjectArrayList<MenuNodeStruct> children) {

        // Base
        this.master = master;

        // Overrides
        this.spriteOverride = spriteOverride;
        this.textOverride = textOverride;
        this.colorOverride = colorOverride;
        this.layoutOverride = layoutOverride;
        this.actionClassOverride = actionClassOverride;
        this.actionMethodOverride = actionMethodOverride;
        this.actionArgOverride = actionArgOverride;

        // Tree
        this.children = children;
    }

    // Accessible \\

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

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}