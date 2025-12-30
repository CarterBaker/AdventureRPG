package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.awt.Color;

import com.AdventureRPG.core.engine.InstancePackage;

public class AliasInstance extends InstancePackage {

    // Internal
    private String aliasType;
    private Color defaultColor;

    // Internal \\

    void init(String aliasType, Color defaultColor) {

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