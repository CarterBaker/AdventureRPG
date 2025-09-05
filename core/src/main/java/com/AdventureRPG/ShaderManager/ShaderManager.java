package com.AdventureRPG.ShaderManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;

import java.util.*;

public class ShaderManager implements ShaderProvider {

    // Debug
    private final boolean debug = true; // TODO: Debug line

    // Game Manager
    private final Settings settings;
    private final Gson gson;
    private final ShaderProvider defaultShaderProvider;
    private final MaterialManager materialManager;

    // Settings
    private final String SHADER_JSON_PATH;

    // Shader Manager
    public final UniversalUniform universalUniform;

    // Shader maps
    private final Map<String, ShaderProgram> nameToProgram;
    private final Map<Integer, ShaderProgram> idToProgram;
    private final Map<String, Integer> nameToID;
    private int nextShaderID;

    private Mesh fullScreenQuad;

    // Base \\

    public ShaderManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;
        this.gson = gameManager.gson;
        this.defaultShaderProvider = gameManager.defaultShaderProvider;
        this.materialManager = gameManager.materialManager;

        // Settings
        this.SHADER_JSON_PATH = settings.SHADER_JSON_PATH;

        // Shader Manager
        this.universalUniform = new UniversalUniform(gameManager);

        // Shader maps
        this.nameToProgram = new LinkedHashMap<>();
        this.idToProgram = new LinkedHashMap<>();
        this.nameToID = new HashMap<>();
        this.nextShaderID = 0;

        createFullScreenQuad();

        // Core Logic \\

        compileShaders();
    }

    private void createFullScreenQuad() {
        fullScreenQuad = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

        float[] verts = new float[] {
                -1, -1, 0f, 0f,
                1, -1, 1f, 0f,
                1, 1, 1f, 1f,
                -1, 1, 0f, 1f
        };

        short[] indices = new short[] { 0, 1, 2, 2, 3, 0 };

        fullScreenQuad.setVertices(verts);
        fullScreenQuad.setIndices(indices);
    }

    public void awake() {

        universalUniform.awake();
    }

    public void start() {

    }

    public void update() {

        universalUniform.update();
    }

    @Override
    public Shader getShader(Renderable renderable) {

        ShaderProgram shader = materialManager.getShaderForMaterial(renderable.material);

        if (shader == null)
            return defaultShaderProvider.getShader(renderable);

        return new DefaultShader(renderable, new DefaultShader.Config(), shader);
    }

    public void dispose() {

        for (ShaderProgram program : nameToProgram.values())
            program.dispose();

        if (fullScreenQuad != null)
            fullScreenQuad.dispose();
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

                // TODO: Debug line
                log("Failed to load shader: " + file.name() + " - " + exception.getMessage());
            }
        }
    }

    // Compile ShaderProgram from includes and main source
    private ShaderProgram compileShader(ShaderDefinition def) {

        if (def.vertex == null || def.fragment == null)
            throw new RuntimeException("Shader JSON missing 'vertex' or 'fragment' field.");

        FileHandle vertFile = Gdx.files.internal(SHADER_JSON_PATH + "/" + def.vertex);

        if (!vertFile.exists())
            throw new RuntimeException("Vertex shader file not found: " + def.vertex);

        String vertexSource = vertFile.readString();

        FileHandle fragFile = Gdx.files.internal(SHADER_JSON_PATH + "/" + def.fragment);

        if (!fragFile.exists())
            throw new RuntimeException("Fragment shader file not found: " + def.fragment);

        String fragmentSource = fragFile.readString();

        // Assemble with includes + fix #version
        vertexSource = assembleShaderSource(def.vertexIncludes, vertexSource);
        fragmentSource = assembleShaderSource(def.fragmentIncludes, fragmentSource);

        ShaderProgram program = new ShaderProgram(vertexSource, fragmentSource);

        if (!program.isCompiled())
            throw new RuntimeException("Shader compile error: " + program.getLog());

        return program;
    }

    // Assemble shader code from include files + main code
    private String assembleShaderSource(List<String> includes, String mainCode) {

        StringBuilder sb = new StringBuilder();

        if (includes != null) {

            for (String include : includes) {

                FileHandle fileHandle = Gdx.files.internal(SHADER_JSON_PATH + "/includes/" + include);

                if (fileHandle.exists())
                    sb.append(fileHandle.readString()).append("\n");
                else
                    log("Include not found: " + include);
            }
        }

        sb.append(mainCode);

        // Ensure #version is first line
        String[] lines = sb.toString().split("\\R");
        StringBuilder finalSrc = new StringBuilder();
        StringBuilder rest = new StringBuilder();
        String versionLine = null;

        for (String line : lines) {

            if (line.trim().startsWith("#version"))
                versionLine = line.trim();
            else
                rest.append(line).append("\n");
        }

        if (versionLine != null)
            finalSrc.append(versionLine).append("\n");

        finalSrc.append(rest);

        return finalSrc.toString();
    }

    // Utility \\

    // Remove .json extension
    private String stripExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private void log(String msg) { // TODO: Debug line

        if (debug)
            System.out.println("[ShaderManager] " + msg);
    }

    // Private data types \\

    // Internal representation of a shader for Gson
    private static class ShaderDefinition {

        String vertex;
        String fragment;
        List<String> vertexIncludes;
        List<String> fragmentIncludes;
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

    public void renderFullScreenQuad(ShaderProgram shader) {
        fullScreenQuad.render(shader, GL20.GL_TRIANGLES);
    }

}
