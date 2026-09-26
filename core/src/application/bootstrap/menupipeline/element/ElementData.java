package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import engine.root.DataPackage;

public class ElementData extends DataPackage {

    /*
     * Persistent element definition shared by every instance: layout, visuals,
     * click and drag callbacks, hover and parent hover colors, and an optional
     * animation timeline sampled against the menu's clock. Masked stacked
     * containers scroll along their stack direction.
     */

    // Identity
    private final String id;
    private final ElementType type;

    // Visuals
    private final String spriteName;
    private final String text;
    private final String fontName;
    private final String materialName;
    private final DimensionValueStruct fontSize;
    private final boolean explicitFontSize;
    private final MenuColorStruct color;
    private final MenuColorStruct hoverColor;
    private final MenuColorStruct parentHoverColor;

    // Layout
    private final LayoutStruct layout;
    private final boolean mask;
    private final StackDirection stackDirection;
    private final DimensionValueStruct spacing;
    private final TextAlign textAlign;

    // Expansion
    private final boolean startExpanded;

    // Animation
    private final ElementAnimationStruct animation;

    // on_click
    private final String actionClass;
    private final String actionMethod;
    private final String actionArg;

    // on_drag
    private final String onDragClass;
    private final String onDragMethod;
    private final String onDragArg;

    // Constructor \\

    public ElementData(
            String id,
            ElementType type,
            String spriteName,
            String text,
            String fontName,
            String materialName,
            DimensionValueStruct fontSize,
            boolean explicitFontSize,
            MenuColorStruct color,
            MenuColorStruct hoverColor,
            MenuColorStruct parentHoverColor,
            LayoutStruct layout,
            boolean mask,
            StackDirection stackDirection,
            DimensionValueStruct spacing,
            TextAlign textAlign,
            boolean startExpanded,
            ElementAnimationStruct animation,
            String actionClass,
            String actionMethod,
            String actionArg,
            String onDragClass,
            String onDragMethod,
            String onDragArg) {

        this.id = id;
        this.type = type;
        this.spriteName = spriteName;
        this.text = text;
        this.fontName = fontName;
        this.materialName = materialName;
        this.fontSize = fontSize;
        this.explicitFontSize = explicitFontSize;
        this.color = color;
        this.hoverColor = hoverColor;
        this.parentHoverColor = parentHoverColor;
        this.layout = layout;
        this.mask = mask;
        this.stackDirection = stackDirection;
        this.spacing = spacing;
        this.textAlign = textAlign;
        this.startExpanded = startExpanded;
        this.animation = animation;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.onDragClass = onDragClass;
        this.onDragMethod = onDragMethod;
        this.onDragArg = onDragArg;
    }

    // Accessible \\

    public String getId() {
        return id;
    }

    public ElementType getType() {
        return type;
    }

    public String getSpriteName() {
        return spriteName;
    }

    public String getText() {
        return text;
    }

    public String getFontName() {
        return fontName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public DimensionValueStruct getFontSize() {
        return fontSize;
    }

    public boolean hasExplicitFontSize() {
        return explicitFontSize;
    }

    public MenuColorStruct getColor() {
        return color;
    }

    public MenuColorStruct getHoverColor() {
        return hoverColor;
    }

    public MenuColorStruct getParentHoverColor() {
        return parentHoverColor;
    }

    public LayoutStruct getLayout() {
        return layout;
    }

    public boolean isMask() {
        return mask;
    }

    public StackDirection getStackDirection() {
        return stackDirection;
    }

    public DimensionValueStruct getSpacing() {
        return spacing;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    public boolean isStartExpanded() {
        return startExpanded;
    }

    public ElementAnimationStruct getAnimation() {
        return animation;
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

    public String getOnDragClass() {
        return onDragClass;
    }

    public String getOnDragMethod() {
        return onDragMethod;
    }

    public String getOnDragArg() {
        return onDragArg;
    }

    public boolean hasOnDrag() {
        return onDragClass != null && onDragMethod != null;
    }

    public boolean hasSprite() {
        return spriteName != null;
    }

    public boolean hasText() {
        return text != null;
    }

    public boolean hasFont() {
        return fontName != null;
    }

    public boolean hasMaterial() {
        return materialName != null;
    }

    public boolean hasColor() {
        return color != null;
    }

    public boolean hasHoverColor() {
        return hoverColor != null;
    }

    public boolean hasParentHoverColor() {
        return parentHoverColor != null;
    }

    public boolean hasAnimation() {
        return animation != null;
    }

    public boolean isScrollable() {
        return mask && stackDirection != StackDirection.NONE;
    }
}