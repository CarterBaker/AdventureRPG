package com.AdventureRPG.ShaderManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;

import java.util.*;

public class ShaderManager implements ShaderProvider {

    // Debug
    private final boolean debug = false; // TODO: Remove debug line

    // Game Manager
    private final Settings settings;
    private final Gson gson;
    private final ShaderProvider defaultShaderProvider;
    private final MaterialManager materialManager;

    // Settings
    private final String SHADER_JSON_PATH;

    // Shader maps
    private final Map<String, ShaderProgram> nameToProgram;
    private final Map<Integer, ShaderProgram> idToProgram;
    private final Map<String, Integer> nameToID;
    private int nextShaderID;

    private final Texture whitePixelTexture;

    // Base \\

    public ShaderManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;
        this.gson = gameManager.gson;
        this.defaultShaderProvider = gameManager.defaultShaderProvider;
        this.materialManager = gameManager.materialManager;

        // Settings
        this.SHADER_JSON_PATH = settings.SHADER_JSON_PATH;

        // Shader maps
        this.nameToProgram = new LinkedHashMap<>();
        this.idToProgram = new LinkedHashMap<>();
        this.nameToID = new HashMap<>();
        this.nextShaderID = 0;

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f); // white
        pixmap.fill();
        whitePixelTexture = new Texture(pixmap);
        pixmap.dispose();

        // Core Logic \\

        compileShaders();
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

    }

    public void dispose() {

        for (ShaderProgram program : nameToProgram.values())
            program.dispose();
    }

    @Override
    public Shader getShader(Renderable renderable) {

        ShaderProgram shader = materialManager.getShaderForMaterial(renderable.material);

        if (shader == null)
            return defaultShaderProvider.getShader(renderable);

        return new DefaultShader(renderable, new DefaultShader.Config(), shader);
    }

    // Core Logic \\

    // Assemble each shader
    private void compileShaders() {

        FileHandle directory = Gdx.files.internal(SHADER_JSON_PATH);

        if (!directory.exists() || !directory.isDirectory())
            throw new RuntimeException("Shader folder not found: " + SHADER_JSON_PATH);

        // Iterate JSON files in folder
        for (FileHandle file : directory.list("json")) {

            try {

                ShaderDefinition definition = gson.fromJson(file.readString(), ShaderDefinition.class);
                ShaderProgram program = compileShader(definition);

                // Name = JSON filename without .json
                String name = stripExtension(file.name());

                nameToProgram.put(name, program);
                idToProgram.put(nextShaderID, program);
                nameToID.put(name, nextShaderID);

                nextShaderID++;
            }

            catch (Exception exception) {

                // TODO: Remove debug line
                log("Failed to load shader: " + file.name() + " - " + exception.getMessage());
            }
        }
    }

    // Compile ShaderProgram from includes and main source
    private ShaderProgram compileShader(ShaderDefinition def) {

        String vertexSource = assembleShaderSource(def.vertexIncludes, def.vertexCode);
        String fragmentSource = assembleShaderSource(def.fragmentIncludes, def.fragmentCode);

        ShaderProgram program = new ShaderProgram(vertexSource, fragmentSource);

        if (!program.isCompiled())
            throw new RuntimeException("Shader compile error: " + program.getLog());

        return program;
    }

    // Assemble shader code from include files + main code
    private String assembleShaderSource(List<String> includes, String mainCode) {

        StringBuilder stringBuilder = new StringBuilder();

        if (includes != null) {

            for (String include : includes) {

                FileHandle fileHandle = Gdx.files.internal(SHADER_JSON_PATH + "/includes/" + include);

                if (fileHandle.exists())
                    stringBuilder.append(fileHandle.readString()).append("\n");

                else// TODO: Remove debug line
                    log("Include not found: " + include);
            }
        }

        stringBuilder.append(mainCode);

        return stringBuilder.toString();
    }

    // Utility \\

    // Remove .json extension
    private String stripExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private void log(String msg) { // TODO: Remove debug line

        if (debug)
            System.out.println("[ShaderManager] " + msg);
    }

    // Private data types \\

    // Internal representation of a shader for Gson
    private static class ShaderDefinition {

        List<String> vertexIncludes;
        List<String> fragmentIncludes;
        String vertexCode;
        String fragmentCode;
    }

    // Accessible \\

    // Get shader by JSON filename
    public ShaderProgram getShaderByName(String name) {
        return nameToProgram.get(name);
    }

    // Get shader by unique ID
    public ShaderProgram getShaderByID(int id) {
        return idToProgram.get(id);
    }

    // Get unique shader ID from name
    public int getShaderID(String name) {
        return nameToID.getOrDefault(name, -1);
    }

    public void bindShader(int id) {
        ShaderProgram program = getShaderByID(id);
        if (program != null)
            program.bind();
    }

    public void setUniforms(int id, Map<String, Object> uniforms) {
        ShaderProgram program = getShaderByID(id);
        if (program == null || uniforms == null)
            return;

        for (Map.Entry<String, Object> entry : uniforms.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Float f)
                program.setUniformf(name, f);
            else if (value instanceof Integer i)
                program.setUniformi(name, i);
            // add more types as needed
        }
    }

    public void renderFullScreenQuad(SpriteBatch batch) {
        batch.draw(whitePixelTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
}
