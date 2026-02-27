package com.internal.bootstrap.menupipeline.element;

import com.internal.core.engine.HandlePackage;

public class ElementPlacementHandle extends HandlePackage {

    private ElementHandle master;
    private ElementOverrideStruct override; // null for inline/ref placements

    public void constructor(ElementHandle master, ElementOverrideStruct override) {
        this.master = master;
        this.override = override;
    }

    // Deferred ref resolution — override stays null for refs
    public void setMaster(ElementHandle master) {
        this.master = master;
    }

    public ElementHandle getMaster() {
        return master;
    }

    public ElementOverrideStruct getOverride() {
        return override;
    }
}