package com.internal.bootstrap.menupipeline.element;

/*
 * Optional overrides applied on top of a master ElementHandle at placement
 * time. Any field that is null means "use the handle's value".
 * color override is RGBA float[4], null means use handle color.
 */
public class ElementOverrideStruct {

    private final String spriteName;
    private final String text;
    private final float[] color;
    private final Runnable clickAction;
    private final MenuAwareAction menuAwareAction;
    private final LayoutStruct layout;

    public ElementOverrideStruct(
            String spriteName,
            String text,
            float[] color,
            Runnable clickAction,
            MenuAwareAction menuAwareAction,
            LayoutStruct layout) {
        this.spriteName = spriteName;
        this.text = text;
        this.color = color;
        this.clickAction = clickAction;
        this.menuAwareAction = menuAwareAction;
        this.layout = layout;
    }

    public String getSpriteName() {
        return spriteName;
    }

    public String getText() {
        return text;
    }

    public float[] getColor() {
        return color;
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

    public boolean hasColor() {
        return color != null;
    }
}