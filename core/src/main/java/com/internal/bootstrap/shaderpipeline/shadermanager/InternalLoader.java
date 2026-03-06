package com.internal.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.shaderpipeline.Shader.ShaderData;
import com.internal.bootstrap.shaderpipeline.Shader.ShaderDefinitionData;
import com.internal.bootstrap.shaderpipeline.Shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.Shader.ShaderType;
import com.internal.bootstrap.shaderpipeline.ubo.UBOData;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformUtility;
import com.internal.bootstrap.shaderpipeline.uniforms.samplers.*;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private ShaderManager shaderManager;
    private InternalBuilder internalBuilder;

    // GLSL state
    private ObjectArrayList<ShaderData> glslFiles;
    private Object2ObjectOpenHashMap<String, ShaderData> lookup;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Counter
    private int shaderCount;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SHADER_PATH);
        this.glslFiles = new ObjectArrayList<>();
        this.lookup = new Object2ObjectOpenHashMap<>();
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Shader directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> categorizeFile(path.toFile()));
        } catch (IOException e) {
            throwException("ShaderManager failed to walk directory: ", e);
        }
    }

    private void categorizeFile(File file) {

        String extension = FileUtility.getExtension(file);

        if (extension == null || extension.isEmpty())
            return;

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(extension)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            fileQueue.offer(file);
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

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
        this.shaderCount = 0;
    }

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
    }

    @Override
    protected void awake() {
        for (int i = 0; i < glslFiles.size(); i++)
            internalBuilder.parseShaderFile(glslFiles.get(i));
    }

    // Load \\

    @Override
    protected void load(File file) {
        shaderManager.addShader(
                assembleShader(
                        internalBuilder.compileShader(file)));
    }

    // On-Demand Loading \\

    void request(String shaderName) {

        File file = resourceName2File.get(shaderName);

        if (file == null)
            throwException(
                    "On-demand shader load failed — resource not found in scan registry: \"" + shaderName + "\"");

        request(file);
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

    // Buffers \\

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
        // UBO is already registered via JSON — just bind and track the block name
        shaderManager.bindShaderToUBO(shader, bufferData.getBlockName());
        shader.addUBOBlock(bufferData.getBlockName());
    }

    // Uniforms \\

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
        String queryName = uniformData.getCount() > 1
                ? uniformData.getUniformName() + "[0]"
                : uniformData.getUniformName();
        int location = GLSLUtility.getUniformLocation(shader.getShaderHandle(), queryName);
        UniformAttribute<?> attribute = UniformUtility.createUniformAttribute(uniformData);
        Uniform<?> uniform = new Uniform<>(location, uniformData.getCount(), attribute);
        shader.addUniform(uniformData.getUniformName(), uniform);
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