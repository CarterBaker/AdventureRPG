package com.internal.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOData;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.matrices.*;
import com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.samplers.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalars.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectors.*;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Drives the full shader bootstrap sequence: file discovery, GLSL parsing,
 * JSON-driven compilation, and final handle assembly. Released after bootstrap
 * so no parsing state persists into the runtime loop.
 */
class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private ShaderManager shaderManager;
    private UBOManager uboManager;
    private InternalBuildSystem internalBuildSystem;

    private ObjectArrayList<ShaderData> glslFiles;
    private ObjectArrayList<File> jsonFiles;
    private Object2ObjectOpenHashMap<String, ShaderData> lookup;

    private int shaderCount;

    // Internal \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.SHADER_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);

        this.glslFiles = new ObjectArrayList<>();
        this.jsonFiles = new ObjectArrayList<>();
        this.lookup = new Object2ObjectOpenHashMap<>();

        this.shaderCount = 0;
    }

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Bootstrap \\

    void loadShaders() {
        loadAllFiles();
        buildShaderData();
        compileShaders();
    }

    // Load \\

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throwException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> categorizeFile(path.toFile()));
        } catch (IOException e) {
            throwException("ShaderManager failed to load files: ", e);
        }
    }

    private void categorizeFile(File file) {

        String extension = FileUtility.getExtension(file);

        if (extension == null || extension.isEmpty())
            return;

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(extension)) {
            jsonFiles.add(file);
            return;
        }

        ShaderType shaderType = null;

        if (EngineSetting.VERT_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.VERT;
        else if (EngineSetting.FRAG_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.FRAG;
        else if (EngineSetting.INCLUDE_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.INCLUDE;

        if (shaderType == null)
            throwException("Unrecognized shader extension: " + file.getName());

        ShaderData shaderData = create(ShaderData.class);
        shaderData.constructor(shaderType, FileUtility.getFileName(file), file);

        glslFiles.add(shaderData);
        lookup.put(FileUtility.getPathWithFileNameWithExtension(root, file), shaderData);
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

    // Assemble \\

    private ShaderHandle assembleShader(ShaderDefinitionData definition) {

        ShaderHandle shader = create(ShaderHandle.class);
        shader.constructor(
                definition.getShaderName(),
                shaderCount++,
                GLSLUtility.createShaderProgram(definition));

        assembleBuffers(shader, definition);
        assembleUniforms(shader, definition);

        return shader;
    }

    // Buffers
    private void assembleBuffers(ShaderHandle shader, ShaderDefinitionData definition) {

        addBuffersFromShaderData(shader, definition.getVert());
        addBuffersFromShaderData(shader, definition.getFrag());

        ObjectArrayList<ShaderData> includes = definition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addBuffersFromShaderData(shader, includes.get(i));
    }

    private void addBuffersFromShaderData(ShaderHandle shader, ShaderData shaderData) {

        ObjectArrayList<UBOData> blocks = shaderData.getBufferBlocks();

        for (int i = 0; i < blocks.size(); i++)
            addBuffer(shader, blocks.get(i));
    }

    private void addBuffer(ShaderHandle shader, UBOData bufferData) {

        // Register or retrieve the handle — UBOManager owns binding assignment
        uboManager.buildBuffer(bufferData);

        // Bind the shader program's block index to the UBO's binding point
        shaderManager.bindShaderToUBO(shader, bufferData.getBlockName());

        // Track by name only — no handle reference crosses into ShaderHandle
        shader.addUBOBlock(bufferData.getBlockName());
    }

    // Uniforms
    private void assembleUniforms(ShaderHandle shader, ShaderDefinitionData definition) {

        addUniformsFromShaderData(shader, definition.getVert());
        addUniformsFromShaderData(shader, definition.getFrag());

        ObjectArrayList<ShaderData> includes = definition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addUniformsFromShaderData(shader, includes.get(i));
    }

    private void addUniformsFromShaderData(ShaderHandle shader, ShaderData shaderData) {

        ObjectArrayList<UniformData> uniforms = shaderData.getUniforms();

        for (int i = 0; i < uniforms.size(); i++)
            addUniform(shader, uniforms.get(i));
    }

    private void addUniform(ShaderHandle shader, UniformData uniformData) {

        // Query as "name[0]" for arrays; plain name for singles.
        String queryName = uniformData.getCount() > 1
                ? uniformData.getUniformName() + "[0]"
                : uniformData.getUniformName();

        // A location of -1 means the compiler removed the uniform as unused.
        // We store the uniform with location -1 and rely on the upload path to
        // no-op silently. This prevents crashes from shared include files where
        // a uniform is declared but not used by every shader variant.
        int location = GLSLUtility.getUniformLocation(shader.getShaderHandle(), queryName);

        UniformAttribute<?> attribute = createUniformAttribute(uniformData);
        Uniform<?> uniform = new Uniform<>(location, uniformData.getCount(), attribute);

        shader.addUniform(uniformData.getUniformName(), uniform);
    }

    private UniformAttribute<?> createUniformAttribute(UniformData uniformData) {

        int count = uniformData.getCount();
        boolean isArray = count > 1;

        return switch (uniformData.getUniformType()) {
            case FLOAT -> isArray ? new FloatArrayUniform(count) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(count) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(count) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(count) : new BooleanUniform();
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
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(count) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(count) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(count) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(count) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(count) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(count) : new Matrix4DoubleUniform();
            case SAMPLE_IMAGE_2D -> new SampleImage2DUniform();
            case SAMPLE_IMAGE_2D_ARRAY -> new SampleImage2DArrayUniform();
            default -> throwException("Unsupported uniform type: " + uniformData.getUniformType());
        };
    }

    // Utility \\

    ShaderData getShaderData(String key) {

        ShaderData result = lookup.get(key);

        if (result != null)
            return result;

        for (int i = 0; i < glslFiles.size(); i++) {
            ShaderData data = glslFiles.get(i);
            if (data.getShaderName().equals(key))
                return data;
        }

        return throwException("Shader data not found for key: " + key);
    }
}