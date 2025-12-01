package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Shaders.Shader;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Uniform;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Samplers.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors.*;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private ShaderManager shaderManager;
    private InternalBuildSystem internalBuildSystem;

    private ObjectArrayList<ShaderDataInstance> glslFiles;
    private ObjectArrayList<File> jsonFiles;

    private Set<String> jsonExtensions;
    private Set<String> vertExtensions;
    private Set<String> fragExtensions;
    private Set<String> includeExtensions;

    private Object2ObjectOpenHashMap<String, ShaderDataInstance> lookup;
    private int shaderCount = 0;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.glslFiles = new ObjectArrayList<>();
        this.jsonFiles = new ObjectArrayList<>();

        this.lookup = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {

        // Internal
        this.shaderManager = gameEngine.get(ShaderManager.class);
    }

    @Override
    protected void freeMemory() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) release(new InternalBuildSystem());
    }

    // Shader Management \\

    void loadShaders() {

        // First load all files and organize them
        loadAllFiles();
        parseShaderFiles();
        compileShaders();
    }

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throw new FileException.FileNotFoundException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> categorizeFile(path.toFile()));
        }

        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void categorizeFile(File file) {

        String extension = FileUtility.getExtension(file);

        if (extension == null)
            return;

        ShaderType shaderType = null;

        if (jsonExtensions.contains(extension)) {
            jsonFiles.add(file);
            return;
        }

        else if (vertExtensions.contains(extension))
            shaderType = ShaderType.VERT;
        else if (fragExtensions.contains(extension))
            shaderType = ShaderType.FRAG;
        else if (includeExtensions.contains(extension))
            shaderType = ShaderType.INCLUDE;

        if (shaderType == null)
            throw new GraphicException.ShaderProgramException(
                    "Shader: " + file.getName() + ", Has an unrecognized exception");

        ShaderDataInstance shaderDataInstance = (ShaderDataInstance) create(
                new ShaderDataInstance(
                        shaderType,
                        FileUtility.getFileName(file),
                        file));

        glslFiles.add(shaderDataInstance);

        String filePath = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
        lookup.put(filePath, shaderDataInstance);
    }

    private void parseShaderFiles() {
        for (int i = 0; i < glslFiles.size(); i++)
            internalBuildSystem.parseShaderFile(glslFiles.get(i));
    }

    private void compileShaders() {
        for (int i = 0; i < jsonFiles.size(); i++)
            shaderManager.addShader(
                    assembleShader(
                            internalBuildSystem.compileShader(jsonFiles.get(i))));
    }

    private Shader assembleShader(ShaderDefinitionInstance shaderDefinition) {

        String shaderName = shaderDefinition.shaderName;
        int shaderID = shaderCount++;
        int shaderHandle = GLSLUtility.createShaderProgram(shaderDefinition);

        return assembleUniforms(
                new Shader(
                        shaderName,
                        shaderID,
                        shaderHandle),
                shaderDefinition);
    }

    private Shader assembleUniforms(
            Shader shader,
            ShaderDefinitionInstance shaderDefinition) {

        addUniformsFromShaderData(
                shaderDefinition.vert,
                shader);

        addUniformsFromShaderData(
                shaderDefinition.frag,
                shader);

        ObjectArrayList<ShaderDataInstance> includes = shaderDefinition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addUniformsFromShaderData(includes.get(i), shader);

        return shader;
    }

    private void addUniformsFromShaderData(
            ShaderDataInstance shaderData,
            Shader shader) {

        ObjectArrayList<UniformDataInstance> uniforms = shaderData.getUniforms();

        for (int i = 0; i < uniforms.size(); i++)
            addUniformFromUniformData(uniforms.get(i), shader);
    }

    private void addUniformFromUniformData(
            UniformDataInstance uniformData,
            Shader shader) {

        UniformAttribute<?> uniformAttribute = createUniformAttribute(uniformData);
        Uniform<?> uniform = new Uniform<>(
                GLSLUtility.getUniformHandle(shader.shaderHandle, uniformData.uniformName),
                uniformAttribute);

        shader.addUniform(uniformData.uniformName, uniform);
    }

    private UniformAttribute<?> createUniformAttribute(UniformDataInstance uniformData) {

        return switch (uniformData.uniformType) {

            // Scalars
            case FLOAT -> new FloatUniform();
            case DOUBLE -> new DoubleUniform();
            case INT -> new IntegerUniform();
            case BOOL -> new BooleanUniform();

            // Vectors
            case VECTOR2 -> new Vector2Uniform();
            case VECTOR3 -> new Vector3Uniform();
            case VECTOR4 -> new Vector4Uniform();
            case VECTOR2_DOUBLE -> new Vector2DoubleUniform();
            case VECTOR3_DOUBLE -> new Vector3DoubleUniform();
            case VECTOR4_DOUBLE -> new Vector4DoubleUniform();
            case VECTOR2_INT -> new Vector2IntUniform();
            case VECTOR3_INT -> new Vector3IntUniform();
            case VECTOR4_INT -> new Vector4IntUniform();
            case VECTOR2_BOOLEAN -> new Vector2BooleanUniform();
            case VECTOR3_BOOLEAN -> new Vector3BooleanUniform();
            case VECTOR4_BOOLEAN -> new Vector4BooleanUniform();

            // Matrices
            case MATRIX2 -> new Matrix2Uniform();
            case MATRIX3 -> new Matrix3Uniform();
            case MATRIX4 -> new Matrix4Uniform();
            case MATRIX2_DOUBLE -> new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> new Matrix4DoubleUniform();

            // Samplers
            case SAMPLE_IMAGE_2D -> new SampleImage2DUniform();
            case SAMPLE_IMAGE_2D_ARRAY -> new SampleImage2DArrayUniform();

            default -> throw new GraphicException.ShaderProgramException(
                    "Unsupported uniform type: " + uniformData.uniformType);
        };
    }

    // Utility \\

    ShaderDataInstance getShaderData(String key) {

        // First try full path lookup
        ShaderDataInstance result = lookup.get(key);
        if (result != null)
            return result;

        // Fallback try short stripped name
        for (int i = 0; i < glslFiles.size(); i++) {

            ShaderDataInstance inst = glslFiles.get(i);
            if (inst.shaderName().equals(key))
                return inst;
        }

        throw new GraphicException.ShaderProgramException(
                "Shader data for key: " + key + ", Could not be found");
    }

}
