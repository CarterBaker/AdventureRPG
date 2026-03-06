package com.internal.bootstrap.shaderpipeline.ubo;

import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Bootstrap transfer container describing a uniform buffer block.
 * Created by shader parsers and JSON loaders during bootstrap; consumed by UBOManager
 * and discarded once bootstrap completes.
 *
 * A binding of UNSPECIFIED_BINDING instructs UBOManager to auto-assign a free binding point.
 * A non-negative binding is an explicit request from the GLSL layout qualifier;
 * UBOManager validates it against the registry and honors it.
 */
public class UBOData extends DataPackage {

    public static final int UNSPECIFIED_BINDING = -1;

    // Internal
    private String blockName;
    private int binding;
    private ObjectArrayList<UniformData> uniforms;

    // Internal \\

    @Override
    protected void get() {
        this.uniforms = new ObjectArrayList<>();
    }

    public void constructor(String blockName, int binding) {
        this.blockName = blockName;
        this.binding = binding;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public int getBinding() {
        return binding;
    }

    public void addUniform(UniformData uniform) {
        uniforms.add(uniform);
    }

    public ObjectArrayList<UniformData> getUniforms() {
        return uniforms;
    }
}