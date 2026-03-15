package com.internal.bootstrap.menupipeline.element;

import com.internal.bootstrap.menupipeline.util.LayoutStruct;
import com.internal.bootstrap.menupipeline.util.MenuAwareAction;
import com.internal.core.engine.StructPackage;

public class ElementPlacementStruct extends StructPackage {

    /*
     * Links a master ElementHandle to optional per-placement overrides. Override
     * fields are inlined — any null field means "use the handle's value". Master
     * may be null initially for deferred ref placements and resolved via
     * setMaster().
     */

    // Internal
    private ElementHandle master;

    // Overrides
    private final String spriteNameOverride;
    private final String textOverride;
    private final float[] colorOverride;
    private final Runnable clickActionOverride;
    private final MenuAwareAction menuAwareActionOverride;
    private final LayoutStruct layoutOverride;

    // Constructor — no override \\

    public ElementPlacementStruct(ElementHandle master) {

        this.master = master;
        this.spriteNameOverride = null;
        this.textOverride = null;
        this.colorOverride = null;
        this.clickActionOverride = null;
        this.menuAwareActionOverride = null;
        this.layoutOverride = null;
    }

    // Constructor — with overrides \\

    public ElementPlacementStruct(
            ElementHandle master,
            String spriteNameOverride,
            String textOverride,
            float[] colorOverride,
            Runnable clickActionOverride,
            MenuAwareAction menuAwareActionOverride,
            LayoutStruct layoutOverride) {

        this.master = master;
        this.spriteNameOverride = spriteNameOverride;
        this.textOverride = textOverride;
        this.colorOverride = colorOverride;
        this.clickActionOverride = clickActionOverride;
        this.menuAwareActionOverride = menuAwareActionOverride;
        this.layoutOverride = layoutOverride;
    }

    // Deferred Resolution \\

    public void setMaster(ElementHandle master) {
        this.master = master;
    }

    // Accessible \\

    public ElementHandle getMaster() {
        return master;
    }

    public String getSpriteNameOverride() {
        return spriteNameOverride;
    }

    public String getTextOverride() {
        return textOverride;
    }

    public float[] getColorOverride() {
        return colorOverride;
    }

    public Runnable getClickActionOverride() {
        return clickActionOverride;
    }

    public MenuAwareAction getMenuAwareActionOverride() {
        return menuAwareActionOverride;
    }

    public LayoutStruct getLayoutOverride() {
        return layoutOverride;
    }

    public boolean hasColorOverride() {
        return colorOverride != null;
    }
}