package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.LayoutBlocks.LayoutBlock;
import com.AdventureRPG.Core.RenderPipeline.Shaders.Shader;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Uniform;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.MatrixArrays.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Samplers.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.ScalarArrays.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars.*;
import com.AdventureRPG.Core.RenderPipeline.Uniforms.VectorArrays.*;
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

    private Object2ObjectOpenHashMap<String, ShaderDataInstance> lookup;

    private int layoutCount;
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

        this.layoutCount = 0;
        this.shaderCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.shaderManager = gameEngine.get(ShaderManager.class);
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

        ShaderDataInstance shaderDataInstance = (ShaderDataInstance) create(
                new ShaderDataInstance(
                        shaderType,
                        FileUtility.getFileName(file),
                        file));

        glslFiles.add(shaderDataInstance);

        String filePath = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
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

        assembleLayouts(
                shader,
                shaderDefinition);

        assembleUniforms(
                shader,
                shaderDefinition);

        return shader;
    }

    // Layouts
    private void assembleLayouts(
            Shader shader,
            ShaderDefinitionInstance shaderDefinition) {

        addLayoutsFromShaderData(
                shaderDefinition.vert,
                shader);

        addLayoutsFromShaderData(
                shaderDefinition.frag,
                shader);

        ObjectArrayList<ShaderDataInstance> includes = shaderDefinition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addLayoutsFromShaderData(includes.get(i), shader);
    }

    private void addLayoutsFromShaderData(
            ShaderDataInstance shaderData,
            Shader shader) {

        ObjectArrayList<LayoutDataInstance> layoutBlocks = shaderData.getLayoutBlocks();

        for (int i = 0; i < layoutBlocks.size(); i++)
            addLayoutFromLayoutData(layoutBlocks.get(i), shader);
    }

    private void addLayoutFromLayoutData(
            LayoutDataInstance layoutData,
            Shader shader) {

        String layoutName = layoutData.blockName();
        int bindingPoint = layoutData.binding();

        // Check if this layout already exists in the manager
        if (shaderManager.hasLayout(layoutName)) {
            // Layout already exists, just reference it in this shader
            LayoutBlock existingLayout = shaderManager.getLayoutFromLayoutName(layoutName);
            shader.addLayout(layoutName, existingLayout);
            return;
        }

        // Layout doesn't exist, create it
        int layoutID = layoutCount++;

        // Create the uniform buffer object on GPU and get its handle
        int layoutHandle = GLSLUtility.createUniformBuffer();

        LayoutBlock layoutBlock = new LayoutBlock(
                layoutName,
                layoutID,
                layoutHandle);

        // Add uniforms to the layout block with proper offsets
        ObjectArrayList<UniformDataInstance> layoutUniforms = layoutData.getUniforms();
        int currentOffset = 0;

        for (int i = 0; i < layoutUniforms.size(); i++) {
            UniformDataInstance uniformData = layoutUniforms.get(i);

            // Calculate offset based on std140 layout rules
            int offset = calculateStd140Offset(currentOffset, uniformData);

            UniformAttribute<?> uniformAttribute = createUniformAttribute(uniformData);
            Uniform<?> uniform = new Uniform<>(
                    offset, // Store the offset within the buffer
                    uniformAttribute);

            layoutBlock.addUniform(uniformData.uniformName(), uniform);

            // Update offset for next uniform
            currentOffset = offset + calculateUniformSize(uniformData);
        }

        // Allocate GPU storage for the buffer (currentOffset now contains total size
        // needed)
        GLSLUtility.allocateUniformBuffer(layoutHandle, currentOffset);

        // Bind it to the binding point
        GLSLUtility.bindUniformBuffer(layoutHandle, bindingPoint);

        // Register the layout in the shader manager (so other shaders can reuse it)
        shaderManager.addLayout(layoutBlock);

        // Also add it to this shader
        shader.addLayout(layoutName, layoutBlock);
    }

    // Calculate offset following std140 layout rules
    private int calculateStd140Offset(int currentOffset, UniformDataInstance uniformData) {
        int alignment = getStd140Alignment(uniformData);

        // Round up to next multiple of alignment
        int offset = ((currentOffset + alignment - 1) / alignment) * alignment;

        return offset;
    }

    // Get alignment requirements for std140 layout
    private int getStd140Alignment(UniformDataInstance uniformData) {
        return switch (uniformData.uniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN -> 16;
            case VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN -> 16;
            case MATRIX2 -> 16; // vec2 columns
            case MATRIX3 -> 16; // vec3 columns aligned as vec4
            case MATRIX4 -> 16; // vec4 columns
            default -> 16; // Safe default
        };
    }

    // Calculate size of uniform in bytes
    private int calculateUniformSize(UniformDataInstance uniformData) {
        int count = uniformData.count();

        // For arrays, calculate stride differently
        if (count > 1) {
            int stride = switch (uniformData.uniformType()) {
                case FLOAT, INT, BOOL -> 16; // Arrays of scalars: 16-byte stride
                case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 16;
                case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN -> 16; // vec3 arrays padded to vec4
                case VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN -> 16;
                case MATRIX2 -> 32; // Each column is vec2, stride 16
                case MATRIX3 -> 48; // Each column is vec3 (as vec4), stride 16
                case MATRIX4 -> 64; // Each column is vec4, stride 16
                default -> 16;
            };
            return stride * count;
        }

        // Single element sizes
        return switch (uniformData.uniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN -> 12;
            case VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN -> 16;
            case MATRIX2 -> 32;
            case MATRIX3 -> 48;
            case MATRIX4 -> 64;
            default -> 16;
        };
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

        ObjectArrayList<ShaderDataInstance> includes = shaderDefinition.getIncludes();

        for (int i = 0; i < includes.size(); i++)
            addUniformsFromShaderData(includes.get(i), shader);
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
                GLSLUtility.getUniformHandle(shader.shaderHandle, uniformData.uniformName()),
                uniformAttribute);

        shader.addUniform(uniformData.uniformName(), uniform);
    }

    private UniformAttribute<?> createUniformAttribute(UniformDataInstance uniformData) {

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
