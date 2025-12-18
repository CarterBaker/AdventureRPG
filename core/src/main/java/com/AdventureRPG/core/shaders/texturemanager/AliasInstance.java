package com.AdventureRPG.core.shaders.texturemanager;

import java.awt.Color;

import com.AdventureRPG.core.engine.InstanceFrame;

class AliasInstance extends InstanceFrame {
    final String aliasType;
    final Color defaultColor;

    AliasInstance(String aliasType, Color defaultColor) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
    }
}