package application.bootstrap.shaderpipeline.materialmanager;

import java.io.File;

import application.bootstrap.shaderpipeline.material.MaterialData;
import application.bootstrap.shaderpipeline.material.MaterialHandle;
import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.bootstrap.shaderpipeline.uniforms.UniformUtility;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class MaterialBuilder extends BuilderPackage {

    /*
     * Parses material ARPG files into MaterialHandles during bootstrap.
     * Uniforms are cloned from the compiled shader — the optional "uniforms"
     * block applies default values to named uniforms. Sampler uniforms with
     * string values are resolved through TextureManager to GPU handles.
     */

    // Internal
    private ShaderManager shaderManager;
    private UBOManager uboManager;
    private MaterialManager materialManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
        this.materialManager = get(MaterialManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    void build(File file, String materialName) {

        if (materialManager.hasMaterial(materialName))
            return;

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        String shaderName = ArpgUtility.validateString(arpg, "shader");
        ShaderHandle shaderHandle = shaderManager.getShaderHandleFromShaderName(shaderName);

        // UBOs
        Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs = new Object2ObjectOpenHashMap<>();

        if (arpg.has("ubos")) {
            ArpgObjectStruct ubosArpg = arpg.getAsObject("ubos");
            for (String uboName : ubosArpg.keySet())
                sourceUBOs.put(uboName, uboManager.getUBOHandleFromUBOName(uboName));
        }

        // Uniforms — clone all from shader, apply ARPG overrides where declared
        Object2ObjectOpenHashMap<String, UniformStruct<?>> shaderUniforms = shaderHandle.getCompiledUniforms();
        Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms = new Object2ObjectOpenHashMap<>();

        for (String uniformName : shaderUniforms.keySet())
            uniforms.put(uniformName, shaderUniforms.get(uniformName).clone());

        if (arpg.has("uniforms")) {

            ArpgObjectStruct uniformsArpg = arpg.getAsObject("uniforms");

            for (String uniformName : uniformsArpg.keySet()) {

                UniformStruct<?> uniform = uniforms.get(uniformName);

                if (uniform == null)
                    throwException("Material '" + materialName
                            + "' references unknown uniform: " + uniformName);

                ArpgObjectStruct uniformArpg = uniformsArpg.getAsObject(uniformName);
                UniformAttributeStruct<?> attribute = uniform.attribute();

                if (isSamplerType(attribute.getUniformType())
                        && uniformArpg.has("value")
                        && uniformArpg.get("value").isValue()
                        && !uniformArpg.get("value").getAsValue().isNumber()) {

                    String textureName = uniformArpg.get("value").getAsString();
                    int gpuHandle = resolveTextureHandle(textureName, uniformName, materialName);

                    @SuppressWarnings("unchecked")
                    UniformAttributeStruct<Integer> samplerAttr = (UniformAttributeStruct<Integer>) attribute;
                    samplerAttr.set(gpuHandle);
                } else {
                    UniformUtility.applyFromArpgObject(attribute, uniformName, uniformArpg);
                }
            }
        }

        // Construct
        int materialID = materialManager.registerMaterialName(materialName);
        MaterialData data = new MaterialData(
                materialName,
                materialID,
                shaderHandle,
                sourceUBOs,
                uniforms);

        MaterialHandle handle = create(MaterialHandle.class);
        handle.constructor(data);
        materialManager.addMaterial(handle);
    }

    // Sampler Resolution \\

    private boolean isSamplerType(UniformType type) {
        return type == UniformType.SAMPLE_IMAGE_2D
                || type == UniformType.SAMPLE_IMAGE_2D_ARRAY;
    }

    private int resolveTextureHandle(
            String textureName,
            String uniformName,
            String materialName) {

        if (textureManager.hasTexture(textureName))
            return textureManager.getTextureHandleFromTextureName(textureName).getGpuHandle();

        return textureManager.getTextureHandleFromArrayName(textureName).getGpuHandle();
    }
}