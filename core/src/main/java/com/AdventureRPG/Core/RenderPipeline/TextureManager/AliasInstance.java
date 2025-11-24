package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.awt.Color;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

class AliasInstance extends InstanceFrame {
    final String aliasType;
    final Color defaultColor;

    AliasInstance(String aliasType, Color defaultColor) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
    }
}