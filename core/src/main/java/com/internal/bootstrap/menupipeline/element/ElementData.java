package com.internal.bootstrap.menupipeline.element;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementData extends com.internal.core.engine.DataPackage {

    private String id;
    private ElementType type;
    private String spritePath;
    private String text;
    private String fontName;
    private float[] color;
    private LayoutStruct layout;
    private boolean mask;
    private StackDirection stackDirection;
    private DimensionValue spacing;
    private TextAlign textAlign;
    private String actionClass;
    private String actionMethod;
    private String actionArg;
    private ObjectArrayList<ElementData> children;
    private String refPath;
    private String usePath;

    // Inline element
    public void constructor(
            String id, ElementType type,
            String spritePath, String text, String fontName, float[] color,
            LayoutStruct layout,
            boolean mask, StackDirection stackDirection, DimensionValue spacing,
            TextAlign textAlign,
            String actionClass, String actionMethod, String actionArg,
            ObjectArrayList<ElementData> children) {
        this.id = id;
        this.type = type;
        this.spritePath = spritePath;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.mask = mask;
        this.stackDirection = stackDirection != null ? stackDirection : StackDirection.NONE;
        this.spacing = spacing;
        this.textAlign = textAlign != null ? textAlign : TextAlign.CENTER;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    // Ref element
    public void constructorRef(String id, String refPath, LayoutStruct layout) {
        this.id = id;
        this.refPath = refPath;
        this.layout = layout;
        this.stackDirection = StackDirection.NONE;
        this.textAlign = TextAlign.CENTER;
    }

    // Use element
    public void constructorUse(
            String id, String usePath,
            String spritePath, String text, String fontName, float[] color,
            LayoutStruct layout,
            boolean mask, StackDirection stackDirection, DimensionValue spacing,
            TextAlign textAlign,
            String actionClass, String actionMethod, String actionArg,
            ObjectArrayList<ElementData> children) {
        this.id = id;
        this.usePath = usePath;
        this.spritePath = spritePath;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.mask = mask;
        this.stackDirection = stackDirection != null ? stackDirection : StackDirection.NONE;
        this.spacing = spacing;
        this.textAlign = textAlign != null ? textAlign : TextAlign.CENTER;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public ElementType getType() {
        return type;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public String getText() {
        return text;
    }

    public String getFontName() {
        return fontName;
    }

    public float[] getColor() {
        return color;
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

    public DimensionValue getSpacing() {
        return spacing;
    }

    public TextAlign getTextAlign() {
        return textAlign;
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

    public String getRefPath() {
        return refPath;
    }

    public String getUsePath() {
        return usePath;
    }

    public boolean isRef() {
        return refPath != null;
    }

    public boolean isUse() {
        return usePath != null;
    }

    public boolean hasFont() {
        return fontName != null;
    }

    public boolean hasColor() {
        return color != null;
    }

    public ObjectArrayList<ElementData> getChildren() {
        return children != null ? children : new ObjectArrayList<>();
    }
}