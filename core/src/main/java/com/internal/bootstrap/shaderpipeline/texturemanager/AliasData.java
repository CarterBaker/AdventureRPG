package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import com.internal.core.engine.DataPackage;

/*
 * Bootstrap-only container for a single alias type, its default fill
 * colour, and the UBO uniform name that receives this alias's layer index
 * during texture UBO seeding. Held in AliasLibrarySystem during the build
 * phase only. Must not be held after bootstrap completes.
 */
public class AliasData extends DataPackage {

    // Internal
    private String aliasType;
    private Color defaultColor;
    private String uniformName;

    // Internal \\

    void constructor(String aliasType, Color defaultColor, String uniformName) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
        this.uniformName = uniformName;
    }

    // Accessible \\

    String getAliasType() {
        return aliasType;
    }

    Color getAliasColor() {
        return defaultColor;
    }

    /*
     * The UBO uniform this alias writes its layer index to.
     * Null if this alias has no associated uniform.
     */
    String getUniformName() {
        return uniformName;
    }
}