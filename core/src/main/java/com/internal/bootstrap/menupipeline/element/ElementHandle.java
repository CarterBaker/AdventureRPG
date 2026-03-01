package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Immutable master definition for a UI element. Owns the sprite name rather
 * than a sprite handle — cloning into a SpriteInstance happens at instantiation
 * time inside ElementSystem. Shared across all placements that reference it.
 */
public class ElementHandle extends HandlePackage {

    // Internal
    private String id;
    private ElementType type;
    private String spriteName;
    private String text;
    private LayoutStruct layout;
    private Runnable clickAction;
    private MenuAwareAction menuAwareAction;
    private ObjectArrayList<ElementPlacementHandle> children;

    // Internal \\

    public void constructor(
            String id,
            ElementType type,
            String spriteName,
            String text,
            LayoutStruct layout,
            Runnable clickAction,
            MenuAwareAction menuAwareAction,
            ObjectArrayList<ElementPlacementHandle> children) {
        this.id = id;
        this.type = type;
        this.spriteName = spriteName;
        this.text = text;
        this.layout = layout;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.children = children;
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