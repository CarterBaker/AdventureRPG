package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import engine.graphics.color.Color;
import engine.root.StructPackage;

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
    private final Color colorOverride;
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
            Color colorOverride,
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

    public Color getColorOverride() {
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