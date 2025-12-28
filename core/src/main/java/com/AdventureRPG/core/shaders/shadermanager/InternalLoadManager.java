package com.AdventureRPG.core.shaders.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.engine.EngineSetting;
import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.shaders.shaders.Shader;
import com.AdventureRPG.core.shaders.ubomanager.UBOData;
import com.AdventureRPG.core.shaders.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaders.ubomanager.UBOManager;
import com.AdventureRPG.core.shaders.uniforms.Uniform;
import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.shaders.uniforms.UniformData;
import com.AdventureRPG.core.shaders.uniforms.matrices.*;
import com.AdventureRPG.core.shaders.uniforms.matrixArrays.*;
import com.AdventureRPG.core.shaders.uniforms.samplers.*;
import com.AdventureRPG.core.shaders.uniforms.scalarArrays.*;
import com.AdventureRPG.core.shaders.uniforms.scalars.*;
import com.AdventureRPG.core.shaders.uniforms.vectorarrays.*;
import com.AdventureRPG.core.shaders.uniforms.vectors.*;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private ShaderManager shaderManager;
    private UBOManager uboManager;
    private InternalBuildSystem internalBuildSystem;

    private ObjectArrayList<ShaderData> glslFiles;
    private ObjectArrayList<File> jsonFiles;

    private Object2ObjectOpenHashMap<String, ShaderData> lookup;

    private int shaderCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.SHADER_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.glslFiles = new ObjectArrayList<>();
        this.jsonFiles = new ObjectArrayList<>();

        this.lookup = new Object2ObjectOpenHashMap<>();

        this.shaderCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.shaderManager = gameEngine.get(ShaderManager.class);
        this.uboManager = gameEngine.get(UBOManager.class);
    }

    @Override
    protected void freeMemory() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // Shader Management \\

    void loadShaders() {

        // First load all files and organize them
        loadAllFiles();
        buildShaderData();
        compileShaders();
    }

    // Load \\

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
            throw new FileException.FileReadException("ShaderManager failed to load one or more files: ", e);
        }
    }

    private void categorizeFile(File file) {

        String extension = FileUtility.getExtension(file);

        if (extension == null)
            return;

        ShaderType shaderType = null;

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(extension)) {
            jsonFiles.add(file);
            return;
        }

        else if (EngineSetting.VERT_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.VERT;
        else if (EngineSetting.FRAG_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.FRAG;
        else if (EngineSetting.INCLUDE_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.INCLUDE;

        if (shaderType == null)
            throw new GraphicException.ShaderProgramException(
                    "Shader: " + file.getName() + ", Has an unrecognized extension");

        ShaderData shaderDataInstance = new ShaderData(
                shaderType,
                FileUtility.getFileName(file),
                file);

        glslFiles.add(shaderDataInstance);

        String filePath = FileUtility.getPathWithFileNameWithExtension(root, file);
        lookup.put(filePath, shaderDataInstance);
    }

    // Build \\

    private void buildShaderData() {
        for (int i = 0; i < glslFiles.size(); i++)
            internalBuildSystem.parseShaderFile(glslFiles.get(i));
    }

    // Compile \\

    private void compileShaders() {
        for (int i = 0; i < jsonFiles.size(); i++)
            shaderManager.addShader(
                    assembleShader(
                            internalBuildSystem.compileShader(jsonFiles.get(i))));
    }

    // Shader
    private Shader assembleShader(ShaderDefinitionInstance shaderDefinition) {

        String shaderName = shaderDefinition.shaderName;
        int shaderID = shaderCount++;
        int shaderHandle = GLSLUtility.createShaderProgram(shaderDefinition);

        Shader shader = new Shader(
                shaderName,
                shaderID,
                shaderHandle);

        assembleBuffers(
                shader,
                shaderDefinition);

        assembleUniforms(
                shader,
                shaderDefinition);

        return shader;
    }

    // Buffers
    private void assembleBuffers(
            Shader shader,
            ShaderDefinitionInstance shaderDefinition) {

        addBuffersFromShaderData(
                shaderDefinition.vert,
                shader);

        addBuffersFromShaderData(
                shaderDefinition.frag,
                shader);

        ObjectArrayList<ShaderData> includes = shaderDefinition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addBuffersFromShaderData(includes.get(i), shader);
    }

    private void addBuffersFromShaderData(
            ShaderData shaderData,
            Shader shader) {

        ObjectArrayList<UBOData> bufferBlocks = shaderData.getBufferBlocks();

        for (int i = 0; i < bufferBlocks.size(); i++)
            addBufferFromBufferData(bufferBlocks.get(i), shader);
    }

    private void addBufferFromBufferData(
            UBOData bufferData,
            Shader shader) {

        UBOHandle ubo = uboManager.buildBuffer(bufferData);
        shaderManager.bindShaderToUBO(shader, ubo);
        shader.addBuffer(bufferData.blockName(), ubo);
    }

    // Uniforms
    private void assembleUniforms(
            Shader shader,
            ShaderDefinitionInstance shaderDefinition) {

        addUniformsFromShaderData(
                shaderDefinition.vert,
                shader);

        addUniformsFromShaderData(
                shaderDefinition.frag,
                shader);

        ObjectArrayList<ShaderData> includes = shaderDefinition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addUniformsFromShaderData(includes.get(i), shader);
    }

    private void addUniformsFromShaderData(
            ShaderData shaderData,
            Shader shader) {

        ObjectArrayList<UniformData> uniforms = shaderData.getUniforms();

        for (int i = 0; i < uniforms.size(); i++)
            addUniformFromUniformData(uniforms.get(i), shader);
    }

    private void addUniformFromUniformData(
            UniformData uniformData,
            Shader shader) {

        UniformAttribute<?> uniformAttribute = createUniformAttribute(uniformData);
        Uniform<?> uniform = new Uniform<>(
                GLSLUtility.getUniformHandle(shader.shaderHandle, uniformData.uniformName()),
                uniformAttribute);

        shader.addUniform(uniformData.uniformName(), uniform);
    }

    private UniformAttribute<?> createUniformAttribute(UniformData uniformData) {

        int count = uniformData.count();
        boolean isArray = count > 1;

        return switch (uniformData.uniformType()) {

            // Scalars
            case FLOAT -> isArray ? new FloatArrayUniform(count) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(count) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(count) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(count) : new BooleanUniform();

            // Vectors
            case VECTOR2 -> isArray ? new Vector2ArrayUniform(count) : new Vector2Uniform();
            case VECTOR3 -> isArray ? new Vector3ArrayUniform(count) : new Vector3Uniform();
            case VECTOR4 -> isArray ? new Vector4ArrayUniform(count) : new Vector4Uniform();
            case VECTOR2_DOUBLE -> isArray ? new Vector2DoubleArrayUniform(count) : new Vector2DoubleUniform();
            case VECTOR3_DOUBLE -> isArray ? new Vector3DoubleArrayUniform(count) : new Vector3DoubleUniform();
            case VECTOR4_DOUBLE -> isArray ? new Vector4DoubleArrayUniform(count) : new Vector4DoubleUniform();
            case VECTOR2_INT -> isArray ? new Vector2IntArrayUniform(count) : new Vector2IntUniform();
            case VECTOR3_INT -> isArray ? new Vector3IntArrayUniform(count) : new Vector3IntUniform();
            case VECTOR4_INT -> isArray ? new Vector4IntArrayUniform(count) : new Vector4IntUniform();
            case VECTOR2_BOOLEAN -> isArray ? new Vector2BooleanArrayUniform(count) : new Vector2BooleanUniform();
            case VECTOR3_BOOLEAN -> isArray ? new Vector3BooleanArrayUniform(count) : new Vector3BooleanUniform();
            case VECTOR4_BOOLEAN -> isArray ? new Vector4BooleanArrayUniform(count) : new Vector4BooleanUniform();

            // Matrices
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(count) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(count) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(count) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(count) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(count) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(count) : new Matrix4DoubleUniform();

            // Samplers
            case SAMPLE_IMAGE_2D -> new SampleImage2DUniform();
            case SAMPLE_IMAGE_2D_ARRAY -> new SampleImage2DArrayUniform(); // Already an array type

            default -> throw new GraphicException.ShaderProgramException(
                    "Unsupported uniform type: " + uniformData.uniformType().toString());
        };
    }

    // Utility \\

    ShaderData getShaderData(String key) {

        // First try full path lookup
        ShaderData result = lookup.get(key);
        if (result != null)
            return result;

        // Fallback try short stripped name
        for (int i = 0; i < glslFiles.size(); i++) {

            ShaderData inst = glslFiles.get(i);
            if (inst.shaderName().equals(key))
                return inst;
        }

        throw new GraphicException.ShaderProgramException(
                "Shader data for key: " + key + ", Could not be found");
    }

}
