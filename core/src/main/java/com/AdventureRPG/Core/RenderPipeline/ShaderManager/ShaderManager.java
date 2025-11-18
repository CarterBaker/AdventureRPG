package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialSystem;
import com.AdventureRPG.Core.Util.GlobalConstant;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;

import java.util.*;

//TODO: I need to refactor how the includes work so they are directly in the vert and frag shaders instead
public class ShaderManager extends ManagerFrame implements ShaderProvider {

    // Root
    private Gson gson;
    private MaterialSystem materialSystem;

    // Settings
    private String SHADER_JSON_PATH;

    // Shader Manager
    public UniversalUniformSystem universalUniformSystem;

    // Shader maps
    private Map<String, ShaderProgram> nameToProgram;
    private Map<Integer, ShaderProgram> idToProgram;
    private Map<String, Integer> nameToID;
    private int nextShaderID;

    private Mesh fullScreenQuad;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.SHADER_JSON_PATH = GlobalConstant.SHADER_JSON_PATH;

        // Shader Manager
        this.universalUniformSystem = (UniversalUniformSystem) register(new UniversalUniformSystem());

        // Shader maps
        this.nameToProgram = new LinkedHashMap<>();
        this.idToProgram = new LinkedHashMap<>();
        this.nameToID = new HashMap<>();
        this.nextShaderID = 0;
    }

    @Override
    protected void init() {

        // Root
        this.gson = gameEngine.gson;
        this.materialSystem = gameEngine.get(MaterialSystem.class);
    }

    @Override
    protected void awake() {

        // Shader Manager \\

        createFullScreenQuad();
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

    // Shader Provider \\

    @Override
    public Shader getShader(Renderable renderable) {

        ShaderProgram shader = materialSystem.getShaderForMaterial(renderable.material);

        return new DefaultShader(renderable, new DefaultShader.Config(), shader);
    }

    @Override
    public void dispose() {

        for (ShaderProgram program : nameToProgram.values())
            program.dispose();

        if (fullScreenQuad != null)
            fullScreenQuad.dispose();
    }

    // Shader Manager \\

    // Assemble each shader
    private void compileShaders() {

        FileHandle directory = Gdx.files.internal(SHADER_JSON_PATH);

        if (!directory.exists() || !directory.isDirectory())
            throw new FileException.FileNotFoundException(directory.file());

        // Iterate JSON files in folder
        for (FileHandle fileHandle : directory.list("json")) {

            try {

                ShaderDefinition definition = gson.fromJson(fileHandle.readString(), ShaderDefinition.class);
                ShaderProgram program = compileShader(definition);

                // Name = JSON filename without .json
                String name = stripExtension(fileHandle.name());

                nameToProgram.put(name, program);
                idToProgram.put(nextShaderID, program);
                nameToID.put(name, nextShaderID);

                nextShaderID++;
            }

            catch (Exception e) {
                throw new FileException.FileNotFoundException(fileHandle.file());
            }
        }
    }

    // Compile ShaderProgram from includes and main source
    private ShaderProgram compileShader(ShaderDefinition def) {

        if (def.vertex == null || def.fragment == null)
            throw new GraphicException.ShaderDefinitionException("Unknown Shader JSON");

        FileHandle vertFile = Gdx.files.internal(SHADER_JSON_PATH + "/" + def.vertex);

        if (!vertFile.exists())
            throw new GraphicException.ShaderFileNotFoundException(def.vertex);

        String vertexSource = vertFile.readString();

        FileHandle fragFile = Gdx.files.internal(SHADER_JSON_PATH + "/" + def.fragment);

        if (!fragFile.exists())
            throw new GraphicException.ShaderFileNotFoundException(def.fragment);

        String fragmentSource = fragFile.readString();

        // Assemble with includes + fix #version
        vertexSource = assembleShaderSource(def.vertexIncludes, vertexSource);
        fragmentSource = assembleShaderSource(def.fragmentIncludes, fragmentSource);

        ShaderProgram program = new ShaderProgram(vertexSource, fragmentSource);

        if (!program.isCompiled())
            throw new GraphicException.ShaderCompilationException("Unnamed Shader", program.getLog());

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
                    throw new GraphicException.ShaderFileNotFoundException(fileHandle.path());

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

    private String stripExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

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

    // Used for post processing effects
    public void renderFullScreenQuad(ShaderProgram shader) {
        fullScreenQuad.render(shader, GL30.GL_TRIANGLES);
    }
}
