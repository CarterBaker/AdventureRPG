package application.bootstrap.shaderpipeline.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;

import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.core.engine.StructPackage;

public class ShaderSourceStruct extends StructPackage {

    /*
     * Bootstrap-only container for a single parsed GLSL source file. Holds all
     * parse and assembly phase fields. Lives in InternalLoader's lists during
     * bootstrap and GCs with the loader when the queue empties. Nothing here
     * survives past compilation.
     */

    // Identity
    private final ShaderType shaderType;
    private final String shaderName;
    private final File shaderFile;

    // Parsed
    private String version;
    private final ObjectArrayList<ShaderSourceStruct> directIncludes;
    private final ObjectArrayList<String> bufferBlockNames;
    private final ObjectArrayList<UniformData> uniformDeclarations;

    // Assembly
    private ShaderSourceStruct vert;
    private ShaderSourceStruct frag;
    private final ObjectArrayList<ShaderSourceStruct> flattenedIncludes;

    // Constructor \\

    public ShaderSourceStruct(ShaderType shaderType, String shaderName, File shaderFile) {

        this.shaderType = shaderType;
        this.shaderName = shaderName;
        this.shaderFile = shaderFile;
        this.directIncludes = new ObjectArrayList<>();
        this.bufferBlockNames = new ObjectArrayList<>();
        this.uniformDeclarations = new ObjectArrayList<>();
        this.flattenedIncludes = new ObjectArrayList<>();
    }

    // Parse Phase \\

    public void setVersion(String version) {
        this.version = version;
    }

    public void addDirectInclude(ShaderSourceStruct include) {
        directIncludes.add(include);
    }

    public void addBufferBlockName(String blockName) {
        bufferBlockNames.add(blockName);
    }

    public void addUniformDeclaration(UniformData uniform) {
        uniformDeclarations.add(uniform);
    }

    // Assembly Phase \\

    public void setVert(ShaderSourceStruct vert) {
        this.vert = vert;
    }

    public void setFrag(ShaderSourceStruct frag) {
        this.frag = frag;
    }

    public void addFlattenedInclude(ShaderSourceStruct include) {
        flattenedIncludes.add(include);
    }

    // Accessible \\

    public ShaderType getShaderType() {
        return shaderType;
    }

    public String getShaderName() {
        return shaderName;
    }

    public File getShaderFile() {
        return shaderFile;
    }

    public String getVersion() {
        return version;
    }

    public ObjectArrayList<ShaderSourceStruct> getDirectIncludes() {
        return directIncludes;
    }

    public ObjectArrayList<String> getBufferBlockNames() {
        return bufferBlockNames;
    }

    public ObjectArrayList<UniformData> getUniformDeclarations() {
        return uniformDeclarations;
    }

    public ShaderSourceStruct getVert() {
        return vert;
    }

    public ShaderSourceStruct getFrag() {
        return frag;
    }

    public ObjectArrayList<ShaderSourceStruct> getFlattenedIncludes() {
        return flattenedIncludes;
    }
}