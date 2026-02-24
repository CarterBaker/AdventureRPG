package com.internal.bootstrap.worldpipeline.blockmanager;

import com.internal.bootstrap.shaderpipeline.texturemanager.AliasLibrarySystem;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector2;

public class InternalBufferSystem extends SystemPackage {

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
    private TextureManager textureManager;

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.aliasLibrarySystem = get(AliasLibrarySystem.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        pushAtlasLayers();
        pushBlockOrientationMap();
    }

    // Atlas \\

    private void pushAtlasLayers() {
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName("StandardTextureLayoutData");

        for (String type : ATLAS_LAYER_TYPES) {
            if (!aliasLibrarySystem.hasAlias(type))
                throwException("Atlas layer alias not found: " + type);
            ubo.updateUniform("u_layer_" + type, aliasLibrarySystem.get(type));
        }

        int atlasSize = textureManager.getAtlasSizeFromTextureArrayName("surface/standard");
        float uvPerBlock = 1.0f / atlasSize;
        ubo.updateUniform("u_uvPerBlock", new Vector2(uvPerBlock, uvPerBlock));
        ubo.push();
    }

    // Block Orientation Map \\

    private void pushBlockOrientationMap() {
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName("BlockOrientationMapData");

        // 24 entries — one per textureFace * 4 + spin (0-23)
        // x = axisMode: 0=XZ (UP/DOWN), 1=ZY (EAST/WEST), 2=XY (NORTH/SOUTH)
        // y = spin: 0-3
        Vector2[] faceOrientations = new Vector2[24];

        for (Direction3Vector face : Direction3Vector.VALUES) {

            int axisMode;
            if (face == Direction3Vector.UP || face == Direction3Vector.DOWN)
                axisMode = 0; // horizontal — XZ
            else if (face == Direction3Vector.EAST || face == Direction3Vector.WEST)
                axisMode = 1; // vertical — ZY
            else
                axisMode = 2; // vertical — XY (NORTH/SOUTH)

            for (int spin = 0; spin < 4; spin++) {
                int index = face.ordinal() * 4 + spin;
                faceOrientations[index] = new Vector2(axisMode, spin);
            }
        }

        ubo.updateUniform("u_faceOrientations", faceOrientations);
        ubo.push();
    }
}