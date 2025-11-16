package com.AdventureRPG.Core.RenderPipeline.RenderableInstance;

import com.AdventureRPG.Core.RenderPipeline.ShaderManager.UniformAttribute;
import java.util.HashMap;
import java.util.Map;

public class UniformPacket {

    private final Map<String, UniformAttribute> uniforms = new HashMap<>();

    public void setUniform(String name, UniformAttribute.UniformType type, Object value) {
        uniforms.put(name, new UniformAttribute(name, type, value));
    }

    public UniformAttribute getUniform(String name) {
        return uniforms.get(name);
    }

    public Map<String, UniformAttribute> getAll() {
        return uniforms;
    }

    public void clear() {
        uniforms.clear();
    }
}
