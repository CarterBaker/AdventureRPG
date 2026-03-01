package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.StructPackage;

/*
 * Carries per-placement overrides applied on top of an ElementHandle at
 * instantiation time. Sprite is stored as a name — cloning into a
 * SpriteInstance happens inside ElementSystem. Null fields mean no override
 * for that property.
 */
public class ElementOverrideStruct extends StructPackage {

    private final String spriteName;
    private final String text;
    private final Runnable clickAction;
    private final MenuAwareAction menuAwareAction;
    private final LayoutStruct layout;

    public ElementOverrideStruct(
            String spriteName,
            String text,
            Runnable clickAction,
            MenuAwareAction menuAwareAction,
            LayoutStruct layout) {
        this.spriteName = spriteName;
        this.text = text;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.layout = layout;
    }

    // Accessible \\

    public String getSpriteName() {
        return spriteName;
    }

    public String getText() {
        return text;
    }

    public Runnable getClickAction() {
        return clickAction;
    }

    public MenuAwareAction getMenuAwareAction() {
        return menuAwareAction;
    }

    public LayoutStruct getLayout() {
        return layout;
    }
}