package com.internal.bootstrap.menupipeline.element;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Immutable master definition of an element. Produced at bootstrap, never
 * modified at runtime. Instances are cloned from handles at menu open time.
 * color is RGBA float[4], null means white default.
 */
public class ElementHandle extends com.internal.core.engine.HandlePackage {

    private String id;
    private ElementType type;
    private String spriteName;
    private String text;
    private String fontName;
    private float[] color;
    private LayoutStruct layout;
    private Runnable clickAction;
    private MenuAwareAction menuAwareAction;
    private ObjectArrayList<ElementPlacementHandle> children;

    // Constructor \\

    public void constructor(
            String id, ElementType type,
            String spriteName, String text, String fontName, float[] color,
            LayoutStruct layout,
            Runnable clickAction, MenuAwareAction menuAwareAction,
            ObjectArrayList<ElementPlacementHandle> children) {
        this.id = id;
        this.type = type;
        this.spriteName = spriteName;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.children = children;
    }

    // Accessors \\

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