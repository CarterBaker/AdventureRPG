package com.internal.bootstrap.menupipeline.element;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Bootstrap-only data carrier parsed from JSON. Consumed by InternalBuilder
 * to produce ElementHandles. Does not outlive bootstrap.
 * color is RGBA float[4], null means inherit default (white).
 */
public class ElementData extends com.internal.core.engine.DataPackage {

    private String id;
    private ElementType type;
    private String spritePath;
    private String text;
    private String fontName;
    private float[] color;
    private LayoutStruct layout;
    private String actionClass;
    private String actionMethod;
    private String actionArg;
    private ObjectArrayList<ElementData> children;

    // Ref / Use flags
    private String refPath;
    private String usePath;

    // Constructors \\

    // Inline element
    public void constructor(
            String id, ElementType type,
            String spritePath, String text, String fontName, float[] color,
            LayoutStruct layout,
            String actionClass, String actionMethod, String actionArg,
            ObjectArrayList<ElementData> children) {
        this.id = id;
        this.type = type;
        this.spritePath = spritePath;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    // Ref element — no font/color/sprite/action, only layout override
    public void constructorRef(String id, String refPath, LayoutStruct layout) {
        this.id = id;
        this.refPath = refPath;
        this.layout = layout;
    }

    // Use element
    public void constructorUse(
            String id, String usePath,
            String spritePath, String text, String fontName, float[] color,
            LayoutStruct layout,
            String actionClass, String actionMethod, String actionArg,
            ObjectArrayList<ElementData> children) {
        this.id = id;
        this.usePath = usePath;
        this.spritePath = spritePath;
        this.text = text;
        this.fontName = fontName;
        this.color = color;
        this.layout = layout;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
    }

    // Accessors \\

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

    public String getActionClass() {
        return actionClass;
    }

    public String getActionMethod() {
        return actionMethod;
    }

    public String getActionArg() {
        return actionArg;
    }

    public ObjectArrayList<ElementData> getChildren() {
        return children != null ? children : new ObjectArrayList<>();
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
}