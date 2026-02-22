package com.internal.bootstrap.worldpipeline.blockmanager;

import com.internal.bootstrap.shaderpipeline.texturemanager.AliasLibrarySystem;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;

public class InternalBufferSystem extends SystemPackage {

    // The alias types we expect — names match alias JSON filenames exactly
    private static final String[] ATLAS_LAYER_TYPES = {
            "albedo",
            "ao",
            "emission",
            "height",
            "metallic",
            "normal",
            "specular"
    };

    // Internal
    private UBOManager uboManager;
    private AliasLibrarySystem aliasLibrarySystem;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.aliasLibrarySystem = get(AliasLibrarySystem.class);
    }

    @Override
    protected void awake() {
        pushAtlasLayers();
    }

    // Push \\

    private void pushAtlasLayers() {

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName("TextureLayerData");

        for (String type : ATLAS_LAYER_TYPES) {

            if (!aliasLibrarySystem.hasAlias(type))
                throwException("Atlas layer alias not found: " + type);

            ubo.updateUniform("u_layer_" + type, aliasLibrarySystem.get(type));
        }

        ubo.push();
    }
}