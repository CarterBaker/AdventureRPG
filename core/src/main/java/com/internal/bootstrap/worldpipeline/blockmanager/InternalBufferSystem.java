package com.internal.bootstrap.worldpipeline.blockmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector2;

/*
 * Seeds GPU-side UBOs with data that cannot be expressed statically in JSON.
 * Atlas layer indices and UV scale are declared via companion ubo.json files
 * inside the texture directory and are handled by TextureManager directly.
 * This system is responsible only for procedurally computed orientation data.
 */
public class InternalBufferSystem extends SystemPackage {

    // Internal
    private UBOManager uboManager;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        pushBlockOrientationMap();
    }

    // Block Orientation Map \\

    private void pushBlockOrientationMap() {

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName("BlockOrientationMapData");

        Vector2[] faceOrientations = new Vector2[24];

        for (Direction3Vector face : Direction3Vector.VALUES) {

            int axisMode;
            if (face == Direction3Vector.UP || face == Direction3Vector.DOWN)
                axisMode = 0;
            else if (face == Direction3Vector.EAST || face == Direction3Vector.WEST)
                axisMode = 1;
            else
                axisMode = 2;

            for (int spin = 0; spin < 4; spin++) {
                int index = face.ordinal() * 4 + spin;
                faceOrientations[index] = new Vector2(axisMode, spin);
            }
        }

        ubo.updateUniform("u_faceOrientations", faceOrientations);
        uboManager.push(ubo);
    }
}