package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.util.Map;

public interface ShaderData {

    Map<String, UniformAttribute> getUniforms();

    default void setUniform(String name, Object value) {
        UniformAttribute ua = getUniforms().get(name);
        if (ua != null)
            ua.value = value;
    }
}
