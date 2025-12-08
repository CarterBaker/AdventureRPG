package com.AdventureRPG.core.renderpipeline.texturemanager;

import java.awt.Color;

import com.AdventureRPG.core.kernel.InstanceFrame;

class AliasInstance extends InstanceFrame {
    final String aliasType;
    final Color defaultColor;

    AliasInstance(String aliasType, Color defaultColor) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
    }
}