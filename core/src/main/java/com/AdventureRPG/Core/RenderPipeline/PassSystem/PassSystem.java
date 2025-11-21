package com.AdventureRPG.Core.RenderPipeline.PassSystem;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.RenderManager.RenderManager;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.UniformAttribute;
import com.AdventureRPG.Core.RenderPipeline.Util.GlobalConstant;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PassSystem extends SystemFrame {

    // Root
    private Gson gson;
    private ShaderManager shaderManager;
    private RenderManager renderManager;

    // Settings
    private String PASS_JSON_PATH;

    // Data
    private Map<Integer, PassData> idToPass;
    private Map<String, Integer> nameToID;
    private int nextPassID;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.PASS_JSON_PATH = GlobalConstant.PASS_JSON_PATH;

        // Data
        this.idToPass = new LinkedHashMap<>();
        this.nameToID = new HashMap<>();
        this.nextPassID = 0;
    }

    @Override
    protected void init() {

        // Root
        this.gson = gameEngine.gson;
        this.shaderManager = gameEngine.get(ShaderManager.class);
        this.renderManager = gameEngine.get(RenderManager.class);
    }

    @Override
    protected void awake() {

        // Pass ManagerS
        compilePasses();
    }

    // Core Logic \\

    private void compilePasses() {

        FileHandle directory = Gdx.files.internal(PASS_JSON_PATH);

        if (!directory.exists() || !directory.isDirectory())
            throw new FileException.FileNotFoundException(directory.file());

        for (FileHandle file : directory.list("json")) {

            try {

                PassJson json = gson.fromJson(file.readString(), PassJson.class);
                PassData passTemplate = createPassTemplate(file, json);

                registerPass(passTemplate);
            }

            catch (Exception e) {
                System.err.println("Failed to load pass: " + file.name() + " - " + e.getMessage());
            }
        }
    }

    private PassData createPassTemplate(FileHandle file, PassJson json) {

        int shaderID = shaderManager.getShaderID(json.shader);
        Map<String, UniformAttribute> uniforms = parseUniforms(json.uniforms);
        String name = stripExtension(file.name());

        return (PassData) create(
                new PassData(
                        nextPassID,
                        name,
                        shaderID,
                        json.textures,
                        uniforms,
                        shaderManager.universalUniformSystem,
                        null));
    }

    private Map<String, UniformAttribute> parseUniforms(Map<String, Object> rawUniforms) {

        Map<String, UniformAttribute> uniforms = new HashMap<>();

        if (rawUniforms == null)
            return uniforms;

        for (Map.Entry<String, Object> entry : rawUniforms.entrySet()) {

            String name = entry.getKey();
            Object obj = entry.getValue();

            if (obj instanceof Map<?, ?> map) {

                String typeStr = (String) map.get("type");
                Object value = map.get("value");

                UniformAttribute.UniformType type = UniformAttribute.UniformType.valueOf(typeStr);

                // If value is null, assign default
                value = getDefaultValueForType(type, value);

                uniforms.put(name, new UniformAttribute(name, type, value));
            }

            else
                throw new GraphicException.PassDefinitionException(name);

        }

        return uniforms;
    }

    private Object getDefaultValueForType(UniformAttribute.UniformType type, Object value) {

        if (value != null) {

            // Convert JSON numbers to correct types
            switch (type) {

                case FLOAT:
                    if (value instanceof Number n)
                        return n.floatValue();
                    break;

                case INT:
                    if (value instanceof Number n)
                        return n.intValue();
                    break;

                case BOOL:
                    if (value instanceof Boolean b)
                        return b;
                    break;

                default:
                    break; // all other types
            }

            return value; // fallback
        }

        return switch (type) {
            case FLOAT -> 0f;
            case INT -> 0;
            case BOOL -> false;
            case VEC2 -> new com.badlogic.gdx.math.Vector2(0f, 0f);
            case VEC3 -> new com.badlogic.gdx.math.Vector3(0f, 0f, 0f);
            case VEC4, COLOR -> new com.badlogic.gdx.math.Vector4(0f, 0f, 0f, 0f);
            case MATRIX4 -> new com.badlogic.gdx.math.Matrix4().idt();
        };
    }

    private void registerPass(PassData pass) {
        idToPass.put(nextPassID, pass);
        nameToID.put(pass.name, nextPassID);
        nextPassID++;
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

    // Accessible \\

    public PassData getPassByID(int id) {
        return idToPass.get(id);
    }

    public int getPassID(String name) {
        return nameToID.getOrDefault(name, -1);
    }

    public PassData createPassInstance(String name, int sortOrder) {

        Integer id = nameToID.get(name);

        if (id == null)
            return null;

        return createPassInstance(id, sortOrder, 0f);
    }

    public PassData createPassInstance(String name, int sortOrder, float lifeTime) {

        Integer id = nameToID.get(name);

        if (id == null)
            return null;

        return createPassInstance(id, sortOrder, lifeTime);
    }

    public PassData createPassInstance(int id, int sortOrder) {

        PassData renderPass = createPassInstance(id, sortOrder, 0f);
        return renderPass;
    }

    public PassData createPassInstance(int id, int sortOrder, float lifeTime) {

        PassData template = idToPass.get(id);

        if (template == null)
            return null;

        PassData renderPass = (PassData) create(new PassData(template));
        renderManager.enqueue(renderPass, sortOrder);

        return renderPass;
    }
}