package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementData extends DataPackage {

    // Discriminators
    private String refPath;
    private String usePath;

    // Identity
    private String id;
    private ElementType type;

    // Visuals
    private String spritePath;
    private String text;

    // Layout
    private LayoutStruct layout;

    // Interaction
    private String actionClass;
    private String actionMethod;
    private String actionArg;

    // Hierarchy
    private ObjectArrayList<ElementData> children;

    // Constructors \\

    public void constructor(
            String id, ElementType type, String spritePath, String text,
            LayoutStruct layout, String actionClass, String actionMethod,
            String actionArg, ObjectArrayList<ElementData> children) {
        this.id = id;
        this.type = type;
        this.spritePath = spritePath;
        this.text = text;
        this.layout = layout;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
        this.refPath = null;
        this.usePath = null;
    }

    public void constructorRef(String id, String refPath, LayoutStruct layout) {
        this.id = id;
        this.refPath = refPath;
        this.layout = layout;
        this.children = new ObjectArrayList<>();
        this.usePath = null;
    }

    public void constructorUse(
            String id, String usePath, String spritePath, String text,
            LayoutStruct layout, String actionClass, String actionMethod,
            String actionArg, ObjectArrayList<ElementData> children) {
        this.id = id;
        this.usePath = usePath;
        this.spritePath = spritePath;
        this.text = text;
        this.layout = layout;
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
        this.children = children;
        this.refPath = null;
    }

    // Type Checks \\

    public boolean isRef() {
        return refPath != null;
    }

    public boolean isUse() {
        return usePath != null;
    }

    // Accessors \\

    public String getId() {
        return id;
    }

    public String getRefPath() {
        return refPath;
    }

    public String getUsePath() {
        return usePath;
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
        return children;
    }
}