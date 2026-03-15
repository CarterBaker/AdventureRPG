package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import com.internal.core.engine.StructPackage;

public class AliasStruct extends StructPackage {

    /*
     * Bootstrap container for a single alias type, its default fill colour,
     * and the UBO uniform name that receives this alias's layer index during
     * texture UBO seeding. GCs with the loader when bootstrap completes.
     */

    // Identity
    private final String aliasType;
    private final Color defaultColor;
    private final String uniformName;

    // Constructor \\

    public AliasStruct(String aliasType, Color defaultColor, String uniformName) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
        this.uniformName = uniformName;
    }

    // Accessible \\

    String getAliasType() {
        return aliasType;
    }

    public Color getAliasColor() {
        return defaultColor;
    }

    public String getUniformName() {
        return uniformName;
    }
}