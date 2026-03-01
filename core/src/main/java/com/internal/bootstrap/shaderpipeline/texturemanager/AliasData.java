package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;

import com.internal.core.engine.DataPackage;

/*
 * Bootstrap-only container for a single alias type and its default fill
 * colour. Held in AliasLibrarySystem during the build phase only. Must not
 * be held after bootstrap completes.
 */
public class AliasData extends DataPackage {

    // Internal
    private String aliasType;
    private Color defaultColor;

    // Internal \\

    void constructor(String aliasType, Color defaultColor) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
    }

    // Accessible \\

    String getAliasType() {
        return aliasType;
    }

    Color getAliasColor() {
        return defaultColor;
    }
}