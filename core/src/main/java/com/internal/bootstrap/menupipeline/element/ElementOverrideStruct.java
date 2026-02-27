package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.StructPackage;

public class ElementOverrideStruct extends StructPackage {

    private final SpriteHandle spriteHandle;
    private final String text;
    private final Runnable clickAction;
    private final MenuAwareAction menuAwareAction;
    private final LayoutStruct layout;

    public ElementOverrideStruct(
            SpriteHandle spriteHandle, String text,
            Runnable clickAction, MenuAwareAction menuAwareAction,
            LayoutStruct layout) {
        this.spriteHandle = spriteHandle;
        this.text = text;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.layout = layout;
    }

    public SpriteHandle getSpriteHandle() {
        return spriteHandle;
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