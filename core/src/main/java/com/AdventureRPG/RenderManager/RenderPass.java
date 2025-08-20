package com.AdventureRPG.RenderManager;

import java.util.HashMap;
import java.util.Map;

public class RenderPass {

    // Pass
    public final int id;
    public final String name;
    public final int shaderID;
    public final Map<String, String> texturePaths;
    public final Map<String, Object> defaultUniforms;
    private final RenderAction action;
    public float lifetime;

    // Base \\

    public RenderPass(
            int id,
            String name,
            int shaderID,
            Map<String, String> texturePaths,
            Map<String, Object> defaultUniforms,
            RenderAction action) {

        // Pass
        this.id = id;
        this.name = name;
        this.shaderID = shaderID;
        this.texturePaths = texturePaths;
        this.defaultUniforms = defaultUniforms;
        this.action = action;
        this.lifetime = 0f;
    }

    public RenderPass(RenderPass other) {
        this.id = other.id;
        this.name = other.name;
        this.shaderID = other.shaderID;
        this.texturePaths = new HashMap<>(other.texturePaths);
        this.defaultUniforms = new HashMap<>(other.defaultUniforms);
        this.action = other.action;
        this.lifetime = other.lifetime;
    }

    public RenderPass(RenderPass other, float lifetime) {
        this.id = other.id;
        this.name = other.name;
        this.shaderID = other.shaderID;
        this.texturePaths = new HashMap<>(other.texturePaths);
        this.defaultUniforms = new HashMap<>(other.defaultUniforms);
        this.action = other.action;
        this.lifetime = lifetime;
    }

    public void render(RenderContext context) {
        if (action != null) {
            action.render(context);
        }
    }
}
