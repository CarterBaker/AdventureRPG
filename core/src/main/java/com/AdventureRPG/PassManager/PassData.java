package com.AdventureRPG.PassManager;

import com.AdventureRPG.RenderManager.RenderAction;
import com.AdventureRPG.RenderManager.RenderContext;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class PassData {

    // Pass
    public final int id;
    public final String name;
    public final int shaderID;
    public final Map<String, String> texturePaths;
    public final Map<String, UniformAttribute> uniforms = new HashMap<>();
    private final RenderAction action;
    public float lifetime;

    // Base \\

    public PassData(
            int id,
            String name,
            int shaderID,
            Map<String, String> texturePaths,
            Map<String, UniformAttribute> initialUniforms,
            RenderAction action) {
        this.id = id;
        this.name = name;
        this.shaderID = shaderID;
        this.texturePaths = texturePaths != null ? texturePaths : new HashMap<>();
        if (initialUniforms != null)
            this.uniforms.putAll(initialUniforms);
        this.action = action;
        this.lifetime = 0f;
    }

    // Clone constructor
    public PassData(PassData template) {
        this.id = template.id;
        this.name = template.name;
        this.shaderID = template.shaderID;
        this.texturePaths = new HashMap<>(template.texturePaths);
        this.uniforms.putAll(template.uniforms); // copy uniforms
        this.action = template.action;
        this.lifetime = template.lifetime;
    }

    public PassData(PassData template, float lifetime) {
        this(template);
        this.lifetime = lifetime;
    }

    // Dynamically update uniform at runtime
    public void setUniform(String name, Object value) {
        UniformAttribute ua = uniforms.get(name);

        if (ua != null)
            ua.value = value;
    }

    // Render method called by RenderQueue
    public void render(RenderContext context, ShaderManager shaderManager) {

        ShaderProgram shader = shaderManager.getShaderByID(shaderID);

        if (shader == null)
            return;

        shader.bind();

        // Push all uniforms
        for (UniformAttribute ua : uniforms.values()) {

            switch (ua.uniformType) {
                case FLOAT -> shader.setUniformf(ua.name, (float) ua.value);
                case INT -> shader.setUniformi(ua.name, (int) ua.value);
                case BOOL -> shader.setUniformi(ua.name, ((boolean) ua.value) ? 1 : 0);
                case VEC2 -> shader.setUniformf(ua.name, (Vector2) ua.value);
                case VEC3 -> shader.setUniformf(ua.name, (Vector3) ua.value);
                case VEC4 -> shader.setUniformf(ua.name, (Vector4) ua.value);
                case COLOR -> shader.setUniformf(ua.name, (Color) ua.value);
                case MATRIX4 -> shader.setUniformMatrix(ua.name, (Matrix4) ua.value);
            }
        }

        // Call action if exists (optional extra rendering logic)
        if (action != null)
            action.render(context);
        else
            // Default: draw fullscreen quad
            shaderManager.renderFullScreenQuad(shader);
    }
}
