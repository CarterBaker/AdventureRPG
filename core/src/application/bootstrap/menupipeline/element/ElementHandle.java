package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import engine.graphics.color.Color;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHandle extends HandlePackage {

    /*
     * Immutable master definition for one UI element. Registered in ElementSystem
     * keyed by file path and element ID. Shared across all instances — never
     * mutated after bootstrap completes.
     *
     * children holds the default subtree for this element as defined in its source
     * JSON. Used only during bootstrap when a ref node copies them as its own
     * children. ElementSystem.createInstances never reads these — it always walks
     * the MenuNodeStruct tree passed to it.
     */

    // Internal
    private ElementData data;
    private String actionClass;
    private String actionMethod;
    private String actionArg;
    private ObjectArrayList<MenuNodeStruct> children;

    // Constructor \\

    public void constructor(
            ElementData data,
            String actionClass,
            String actionMethod,
            String actionArg,
            ObjectArrayList<MenuNodeStruct> children) {
        this.data = data;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    // Accessible \\

    public ElementData getElementData() {
        return data;
    }

    public String getId() {
        return data.getId();
    }

    public ElementType getType() {
        return data.getType();
    }

    public String getSpriteName() {
        return data.getSpriteName();
    }

    public String getText() {
        return data.getText();
    }

    public String getFontName() {
        return data.getFontName();
    }

    public String getMaterialName() {
        return data.getMaterialName();
    }

    public DimensionValue getFontSize() {
        return data.getFontSize();
    }

    public boolean hasExplicitFontSize() {
        return data.hasExplicitFontSize();
    }

    public Color getColor() {
        return data.getColor();
    }

    public LayoutStruct getLayout() {
        return data.getLayout();
    }

    public boolean isMask() {
        return data.isMask();
    }

    public StackDirection getStackDirection() {
        return data.getStackDirection();
    }

    public DimensionValue getSpacing() {
        return data.getSpacing();
    }

    public TextAlign getTextAlign() {
        return data.getTextAlign();
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

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public boolean hasSprite() {
        return data.hasSprite();
    }

    public boolean hasText() {
        return data.hasText();
    }

    public boolean hasFont() {
        return data.hasFont();
    }

    public boolean hasMaterial() {
        return data.hasMaterial();
    }

    public boolean hasColor() {
        return data.hasColor();
    }

    public boolean hasAction() {
        return actionClass != null && actionMethod != null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}