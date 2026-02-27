package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHandle extends HandlePackage {

    private String id;
    private ElementType type;
    private SpriteHandle spriteHandle;
    private String text;
    private LayoutStruct layout;
    private Runnable clickAction;
    private MenuAwareAction menuAwareAction;
    private ObjectArrayList<ElementPlacementHandle> children;

    public void constructor(
            String id,
            ElementType type,
            SpriteHandle spriteHandle,
            String text,
            LayoutStruct layout,
            Runnable clickAction,
            MenuAwareAction menuAwareAction,
            ObjectArrayList<ElementPlacementHandle> children) {
        this.id = id;
        this.type = type;
        this.spriteHandle = spriteHandle;
        this.text = text;
        this.layout = layout;
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

    public SpriteHandle getSpriteHandle() {
        return spriteHandle;
    }

    public String getText() {
        return text;
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
        return spriteHandle != null;
    }

    public boolean hasText() {
        return text != null;
    }

    public boolean hasClickAction() {
        return clickAction != null;
    }

    public boolean hasMenuAwareAction() {
        return menuAwareAction != null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}