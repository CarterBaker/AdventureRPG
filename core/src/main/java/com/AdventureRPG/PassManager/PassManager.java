package com.AdventureRPG.PassManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.RenderManager.RenderManager;
import com.AdventureRPG.RenderManager.RenderPass;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PassManager {

    // Game Manager
    private final Settings settings;
    private final Gson gson;
    private final ShaderManager shaderManager;
    private final RenderManager renderManager;

    // Settings
    private final String PASS_JSON_PATH;

    // Data
    private final Map<Integer, RenderPass> idToPass = new LinkedHashMap<>();
    private final Map<String, Integer> nameToID = new HashMap<>();
    private int nextPassID = 0;

    // Base \\

    public PassManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;
        this.gson = gameManager.gson;
        this.shaderManager = gameManager.shaderManager;
        this.renderManager = gameManager.renderManager;

        // Settings
        this.PASS_JSON_PATH = settings.PASS_JSON_PATH;
    }

    public void awake() {
        compilePasses();
    }

    public void start() {

    }

    public void update() {

    }

    // Core Logic \\

    private void compilePasses() {

        FileHandle dir = Gdx.files.internal(PASS_JSON_PATH);

        if (!dir.exists() || !dir.isDirectory())
            throw new RuntimeException("Pass folder not found: " + PASS_JSON_PATH);

        for (FileHandle file : dir.list("json")) {

            try {

                PassJson def = gson.fromJson(file.readString(), PassJson.class);

                // Shader reference as int ID
                int shaderID = shaderManager.getShaderID(def.shader);

                // Name from filename (strip .json)
                String name = stripExtension(file.name());

                RenderPass pass = new RenderPass(
                        nextPassID,
                        name,
                        shaderID,
                        def.textures != null ? def.textures : new HashMap<>(),
                        def.uniforms != null ? def.uniforms : new HashMap<>(),
                        context -> {

                            ShaderProgram shader = shaderManager.getShaderByID(shaderID);

                            if (shader != null) {

                                shader.bind();

                                // set uniforms
                                if (def.uniforms != null) {

                                    for (var entry : def.uniforms.entrySet()) {

                                        Object value = entry.getValue();

                                        if (value instanceof Float f)
                                            shader.setUniformf(entry.getKey(), f);

                                        else if (value instanceof Integer i)
                                            shader.setUniformi(entry.getKey(), i);
                                    }
                                }

                                // draw fullscreen quad
                                context.spriteBatch.begin();
                                shaderManager.renderFullScreenQuad(context.spriteBatch);
                                context.spriteBatch.end();
                            }
                        });

                idToPass.put(nextPassID, pass);
                nameToID.put(name, nextPassID);

                nextPassID++;
            }

            catch (Exception exception) {
                System.err.println("Failed to load pass: " + file.name() + " - " + exception.getMessage());
            }
        }
    }

    // Utility \\

    private String stripExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private static class PassJson {
        String shader;
        Map<String, String> textures;
        Map<String, Object> uniforms;
    }

    // Accessible \\S

    public RenderPass getPassByID(int id) {
        return idToPass.get(id);
    }

    public int getPassID(String name) {
        return nameToID.getOrDefault(name, -1);
    }

    public RenderPass createPassInstance(String name) {

        Integer id = nameToID.get(name);

        if (id == null)
            return null;

        return createPassInstance(id, 0f);
    }

    public RenderPass createPassInstance(String name, float lifeTime) {

        Integer id = nameToID.get(name);

        if (id == null)
            return null;

        return createPassInstance(id, lifeTime);
    }

    public RenderPass createPassInstance(int id) {

        RenderPass renderPass = createPassInstance(id, 0f);
        return renderPass;
    }

    public RenderPass createPassInstance(int id, float lifeTime) {

        RenderPass template = idToPass.get(id);

        if (template == null)
            return null;

        RenderPass renderPass = new RenderPass(template);

        renderManager.enqueue(renderPass);

        return renderPass;
    }

}