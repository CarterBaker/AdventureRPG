package application.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import application.bootstrap.shaderpipeline.shader.ShaderData;
import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.shader.ShaderSourceStruct;
import application.bootstrap.shaderpipeline.shader.ShaderType;
import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformUtility;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShaderLoader extends LoaderPackage {

    /*
     * Scans the shader directory, builds ShaderSourceStructs for all GLSL files,
     * parses their content, then batch-assembles compiled ShaderHandles from JSON
     * definitions each frame. ShaderSourceStructs GC with this loader when the
     * queue empties — nothing from the parse phase survives bootstrap.
     */

    // Internal
    private File root;
    private ShaderManager shaderManager;
    private ShaderBuilder internalBuilder;

    // Source Registry — bootstrap only, GCs with loader
    private ObjectArrayList<ShaderSourceStruct> glslSources;
    private Object2ObjectOpenHashMap<String, ShaderSourceStruct> glslKey2Source;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> shaderName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SHADER_PATH);
        this.glslSources = new ObjectArrayList<>();
        this.glslKey2Source = new Object2ObjectOpenHashMap<>();
        this.shaderName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Shader directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> categorizeFile(path.toFile()));
        } catch (IOException e) {
            throwException("Failed to walk shader directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ShaderBuilder.class);
    }

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
    }

    @Override
    protected void awake() {
        for (int i = 0; i < glslSources.size(); i++)
            internalBuilder.parseShaderFile(glslSources.get(i));
    }

    // Categorize \\

    private void categorizeFile(File file) {

        String extension = FileUtility.getExtension(file);

        if (extension == null || extension.isEmpty())
            return;

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(extension)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            shaderName2File.put(resourceName, file);
            fileQueue.offer(file);
            return;
        }

        ShaderType shaderType = null;

        if (EngineSetting.VERT_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.VERT;
        else if (EngineSetting.FRAG_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.FRAG;
        else if (EngineSetting.TCS_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.TCS;
        else if (EngineSetting.TES_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.TES;
        else if (EngineSetting.INCLUDE_FILE_EXTENSIONS.contains(extension))
            shaderType = ShaderType.INCLUDE;

        if (shaderType == null)
            throwException("Unrecognized shader extension: " + file.getName());

        String key = FileUtility.getPathWithFileNameWithExtension(root, file);
        ShaderSourceStruct source = new ShaderSourceStruct(
                shaderType,
                FileUtility.getFileName(file),
                file);

        glslSources.add(source);
        glslKey2Source.put(key, source);
    }

    // Load \\

    @Override
    protected void load(File file) {
        shaderManager.addShaderHandle(
                assembleShader(internalBuilder.buildAssembly(file)));
    }

    // On-Demand \\

    void request(String shaderName) {

        File file = shaderName2File.get(shaderName);

        if (file == null)
            throwException("On-demand shader load failed — not found in scan registry: \"" + shaderName + "\"");

        request(file);
    }

    // Assemble \\

    private ShaderHandle assembleShader(ShaderSourceStruct assembly) {

        int shaderID = RegistryUtility.toIntID(assembly.getShaderName());
        int gpuHandle = ShaderGLSLUtility.createShaderProgram(assembly);

        boolean usesTessellation = assembly.getTCS() != null && assembly.getTES() != null;
        int patchVertexCount = usesTessellation ? assembly.getPatchVertexCount() : 0;

        if (usesTessellation && patchVertexCount <= 0)
            throwException("Tessellated shader '" + assembly.getShaderName()
                    + "' — TCS file is missing 'layout(vertices = N) out;'");

        ShaderData data = new ShaderData(
                assembly.getShaderName(),
                shaderID,
                gpuHandle,
                usesTessellation,
                patchVertexCount);

        ShaderHandle shader = create(ShaderHandle.class);
        shader.constructor(data);

        assembleBuffers(shader, assembly);
        assembleUniforms(shader, assembly);

        return shader;
    }

    // Buffers \\

    private void assembleBuffers(ShaderHandle shader, ShaderSourceStruct assembly) {

        addBuffersFromSource(shader, assembly.getVert());

        if (assembly.getTCS() != null)
            addBuffersFromSource(shader, assembly.getTCS());
        if (assembly.getTES() != null)
            addBuffersFromSource(shader, assembly.getTES());

        addBuffersFromSource(shader, assembly.getFrag());
        ObjectArrayList<ShaderSourceStruct> includes = assembly.getFlattenedIncludes();

        for (int i = 0; i < includes.size(); i++)
            addBuffersFromSource(shader, includes.get(i));
    }

    private void addBuffersFromSource(ShaderHandle shader, ShaderSourceStruct source) {

        ObjectArrayList<String> blockNames = source.getBufferBlockNames();

        for (int i = 0; i < blockNames.size(); i++) {
            shaderManager.bindShaderToUBO(shader, blockNames.get(i));
            shader.addCompiledUBOBlockName(blockNames.get(i));
        }
    }

    // Uniforms \\

    private void assembleUniforms(ShaderHandle shader, ShaderSourceStruct assembly) {

        addUniformsFromSource(shader, assembly.getVert());

        if (assembly.getTCS() != null)
            addUniformsFromSource(shader, assembly.getTCS());
        if (assembly.getTES() != null)
            addUniformsFromSource(shader, assembly.getTES());
        addUniformsFromSource(shader, assembly.getFrag());

        ObjectArrayList<ShaderSourceStruct> includes = assembly.getFlattenedIncludes();

        for (int i = 0; i < includes.size(); i++)
            addUniformsFromSource(shader, includes.get(i));
    }

    private void addUniformsFromSource(ShaderHandle shader, ShaderSourceStruct source) {

        ObjectArrayList<UniformData> declarations = source.getUniformDeclarations();

        for (int i = 0; i < declarations.size(); i++)
            addUniform(shader, declarations.get(i));
    }

    private void addUniform(ShaderHandle shader, UniformData uniformData) {

        String queryName = uniformData.getCount() > 1
                ? uniformData.getUniformName() + "[0]"
                : uniformData.getUniformName();

        int location = ShaderGLSLUtility.getUniformLocation(shader.getGpuHandle(), queryName);
        UniformAttributeStruct<?> attribute = UniformUtility.createUniformAttribute(uniformData);
        UniformStruct<?> uniform = new UniformStruct<>(location, attribute);

        shader.addCompiledUniform(uniformData.getUniformName(), uniform);
    }

    // Utility \\

    ShaderSourceStruct getSourceStruct(String key) {

        ShaderSourceStruct result = glslKey2Source.get(key);

        if (result != null)
            return result;

        for (int i = 0; i < glslSources.size(); i++)
            if (glslSources.get(i).getShaderName().equals(key))
                return glslSources.get(i);

        return throwException("Shader source not found for key: " + key);
    }
}