package com.internal.bootstrap.menupipeline.element;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHandle extends com.internal.core.engine.HandlePackage {

    private String id;
    private ElementType type;
    private String spriteName;
    private String text;
    private String fontName;
    private float[] color;
    private LayoutStruct layout;
    private boolean mask;
    private StackDirection stackDirection;
    private DimensionValue spacing;
    private TextAlign textAlign;
    private Runnable clickAction;
    private MenuAwareAction menuAwareAction;
    private ObjectArrayList<ElementPlacementHandle> children;

    public void constructor(
            String id, ElementType type,
            String spriteName, String text, String fontName, float[] color,
            LayoutStruct layout,
            boolean mask, StackDirection stackDirection, DimensionValue spacing,
            TextAlign textAlign,
            Runnable clickAction, MenuAwareAction menuAwareAction,
            ObjectArrayList<ElementPlacementHandle> children) {
        this.id = id;
        this.type = type;
        this.spriteName = spriteName;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.mask = mask;
        this.stackDirection = stackDirection != null ? stackDirection : StackDirection.NONE;
        this.spacing = spacing;
        this.textAlign = textAlign != null ? textAlign : TextAlign.CENTER;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.children = children;
    }

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

    public Runnable getClickAction() {
        return clickAction;
    }

    public MenuAwareAction getMenuAwareAction() {
        return menuAwareAction;
    }

    public ObjectArrayList<ElementPlacementHandle> getChildren() {
        return children;
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

    public boolean hasColor() {
        return color != null;
    }

    public boolean hasClickAction() {
        return clickAction != null;
    }

    public boolean hasMenuAwareAction() {
        return menuAwareAction != null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}