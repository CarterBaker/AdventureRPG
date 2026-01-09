package com.AdventureRPG.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;

import com.AdventureRPG.core.engine.InstancePackage;

public class AliasInstance extends InstancePackage {

    // Internal
    private String aliasType;
    private Color defaultColor;

    // Internal \\

    void constructor(String aliasType, Color defaultColor) {

        // Internal
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