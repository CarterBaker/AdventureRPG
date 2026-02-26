package com.internal.bootstrap.menupipeline.menumanager;

import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuElementData extends DataPackage {

    // Set when this is a reference — all other fields null
    private String refPath;

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
    private ObjectArrayList<MenuElementData> children;

    // Real element
    public void constructor(
            String id,
            ElementType type,
            String spritePath,
            String text,
            LayoutStruct layout,
            String actionClass,
            String actionMethod,
            String actionArg,
            ObjectArrayList<MenuElementData> children) {
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
    }

    // Reference element
    public void constructorRef(String id, String refPath) {
        this.id = id;
        this.refPath = refPath;
        this.children = new ObjectArrayList<>();
    }

    public boolean isRef() {
        return refPath != null;
    }

    public String getId() {
        return id;
    }

    public String getRefPath() {
        return refPath;
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

    public ObjectArrayList<MenuElementData> getChildren() {
        return children;
    }
}