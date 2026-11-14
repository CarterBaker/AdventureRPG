package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHandle extends HandlePackage {

    /*
     * Persistent template for one UI element: its default child tree, the four
     * state blocks (hover enter, hover, hover exit, click) and its drag
     * callback. Instances are cloned from it when a menu opens.
     */

    // Data
    private ElementData elementData;

    // Tree
    private ObjectArrayList<MenuNodeStruct> children;

    // States
    private ElementStateStruct hoverEnterState;
    private ElementStateStruct hoverState;
    private ElementStateStruct hoverExitState;
    private ElementStateStruct clickState;

    public void constructor(
            ElementData elementData,
            ObjectArrayList<MenuNodeStruct> children,
            ElementStateStruct hoverEnterState,
            ElementStateStruct hoverState,
            ElementStateStruct hoverExitState,
            ElementStateStruct clickState) {
        this.elementData = elementData;
        this.children = children;
        this.hoverEnterState = hoverEnterState;
        this.hoverState = hoverState;
        this.hoverExitState = hoverExitState;
        this.clickState = clickState;
    }

    // Accessible \\

    public ElementData getElementData() {
        return elementData;
    }

    public ObjectArrayList<MenuNodeStruct> getChildren() {
        return children;
    }

    public ElementStateStruct getHoverEnterState() {
        return hoverEnterState;
    }

    public boolean hasHoverEnterState() {
        return hoverEnterState != null;
    }

    public ElementStateStruct getHoverState() {
        return hoverState;
    }

    public boolean hasHoverState() {
        return hoverState != null;
    }

    public ElementStateStruct getHoverExitState() {
        return hoverExitState;
    }

    public boolean hasHoverExitState() {
        return hoverExitState != null;
    }

    public ElementStateStruct getClickState() {
        return clickState;
    }

    public boolean hasClickState() {
        return clickState != null;
    }

    public boolean isHoverable() {
        return hoverEnterState != null || hoverState != null
                || hoverExitState != null || elementData.hasOnDrag()
                || elementData.hasHoverColor();
    }

    public String getId() {
        return elementData.getId();
    }

    public ElementType getType() {
        return elementData.getType();
    }

    public String getSpriteName() {
        return elementData.getSpriteName();
    }

    public String getText() {
        return elementData.getText();
    }

    public String getFontName() {
        return elementData.getFontName();
    }

    public String getMaterialName() {
        return elementData.getMaterialName();
    }

    public DimensionValueStruct getFontSize() {
        return elementData.getFontSize();
    }

    public boolean hasExplicitFontSize() {
        return elementData.hasExplicitFontSize();
    }

    public MenuColorStruct getColor() {
        return elementData.getColor();
    }

    public MenuColorStruct getHoverColor() {
        return elementData.getHoverColor();
    }

    public MenuColorStruct getParentHoverColor() {
        return elementData.getParentHoverColor();
    }

    public LayoutStruct getLayout() {
        return elementData.getLayout();
    }

    public boolean isMask() {
        return elementData.isMask();
    }

    public StackDirection getStackDirection() {
        return elementData.getStackDirection();
    }

    public DimensionValueStruct getSpacing() {
        return elementData.getSpacing();
    }

    public TextAlign getTextAlign() {
        return elementData.getTextAlign();
    }

    public boolean isStartExpanded() {
        return elementData.isStartExpanded();
    }

    public ElementAnimationStruct getAnimation() {
        return elementData.getAnimation();
    }

    public boolean hasAnimation() {
        return elementData.hasAnimation();
    }

    public String getActionClass() {
        return elementData.getActionClass();
    }

    public String getActionMethod() {
        return elementData.getActionMethod();
    }

    public String getActionArg() {
        return elementData.getActionArg();
    }
}