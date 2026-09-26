package application.runtime.postprocessing;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;

public class SSAOSystem extends SystemPackage {

    /*
     * Renders the SSAO occlusion pass into SSAOScene each frame from the
     * world G-buffer's normal and depth. The sample kernel and noise texture
     * are generated once at awake and the noise texture is released with the
     * context. The result is consumed by LightingSystem, never composited.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private UBOManager uboManager;
    private WorldSystem worldSystem;
    private TextureManager textureManager;

    // Render Target
    private PassHandle ssaoPass;
    private FBOInstance ssaoFbo;
    private int noiseTexture;

    // Base \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {

        this.ssaoPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_SSAO);
        this.ssaoFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SSAO, context.getWindow());
        this.noiseTexture = textureManager.createFloatTexture2D(
                generateNoise(),
                RuntimeSetting.SSAO_NOISE_SIZE,
                RuntimeSetting.SSAO_NOISE_SIZE,
                EngineSetting.GL_REPEAT,
                EngineSetting.GL_NEAREST);

        FBOInstance worldFbo = worldSystem.getWorldFbo();
        MaterialInstance material = ssaoPass.getModelInstance().getMaterial();

        material.setUniform(
                RuntimeSetting.UNIFORM_G_NORMAL,
                worldFbo.getColorTexture(RuntimeSetting.ATTACHMENT_NORMAL));
        material.setUniform(RuntimeSetting.UNIFORM_G_DEPTH, worldFbo.getDepthTexture());
        material.setUniform(RuntimeSetting.UNIFORM_SSAO_NOISE, noiseTexture);

        UBOHandle ssaoData = uboManager.getUBOHandleFromUBOName(RuntimeSetting.SSAO_DATA_UBO);
        ssaoData.updateUniform(RuntimeSetting.UNIFORM_SSAO_SAMPLES, generateKernel());
        ssaoData.updateUniform(RuntimeSetting.UNIFORM_SSAO_KERNEL_SIZE, RuntimeSetting.SSAO_KERNEL_SIZE);
        ssaoData.updateUniform(RuntimeSetting.UNIFORM_SSAO_RADIUS, RuntimeSetting.SSAO_RADIUS);
        ssaoData.updateUniform(RuntimeSetting.UNIFORM_SSAO_BIAS, RuntimeSetting.SSAO_BIAS);
        uboManager.push(ssaoData);
    }

    @Override
    protected void update() {
        renderManager.pushRenderCall(
                ssaoPass.getModelInstance(),
                ssaoFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
    }

    @Override
    protected void dispose() {
        textureManager.deleteTexture2D(noiseTexture);
    }

    // Utility \\

    private float[] generateNoise() {

        int sampleCount = RuntimeSetting.SSAO_NOISE_SIZE * RuntimeSetting.SSAO_NOISE_SIZE;
        float[] noise = new float[sampleCount * RuntimeSetting.SSAO_NOISE_CHANNELS];
        int index = 0;

        for (int i = 0; i < sampleCount; i++) {
            noise[index++] = randomSigned();
            noise[index++] = randomSigned();
            noise[index++] = 0f;
        }

        return noise;
    }

    private Vector4[] generateKernel() {

        Vector4[] kernel = new Vector4[RuntimeSetting.SSAO_KERNEL_SIZE];

        for (int i = 0; i < RuntimeSetting.SSAO_KERNEL_SIZE; i++) {

            float x = randomSigned();
            float y = randomSigned();
            float z = (float) Math.random();

            float length = (float) Math.sqrt(x * x + y * y + z * z);
            x /= length;
            y /= length;
            z /= length;

            float t = (float) i / RuntimeSetting.SSAO_KERNEL_SIZE;
            float scale = RuntimeSetting.SSAO_KERNEL_MIN_SCALE + t * t * (1f - RuntimeSetting.SSAO_KERNEL_MIN_SCALE);

            kernel[i] = new Vector4(x * scale, y * scale, z * scale, 0f);
        }

        return kernel;
    }

    private float randomSigned() {
        return (float) (Math.random() * 2.0 - 1.0);
    }

    // Accessible \\

    public FBOInstance getSsaoFbo() {
        return ssaoFbo;
    }
}