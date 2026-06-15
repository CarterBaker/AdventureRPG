package application.runtime.postprocessing;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
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
     * Renders the SSAO occlusion pass into SSAOScene each frame. Reads
     * G-buffer normal and depth from MainScene via named attachment lookup.
     * Kernel and noise texture generated once at awake — stable for the session.
     * SSAO output is consumed by LightingSystem, not composited directly.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private UBOManager uboManager;
    private WorldSystem worldSystem;
    private TextureManager textureManager;

    // Render Target
    private PassHandle ssaoPass;
    private FboInstance ssaoFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void awake() {
        this.ssaoPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_SSAO);
        this.ssaoFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SSAO, context.getWindow());

        FboInstance worldFbo = worldSystem.getWorldFbo();
        MaterialInstance mat = ssaoPass.getModelInstance().getMaterial();

        mat.setUniform("u_gNormal", worldFbo.getColorTexture("normal"));
        mat.setUniform("u_gDepth", worldFbo.getDepthTexture());
        mat.setUniform("u_texNoise", textureManager.createFloatTexture2D(
                generateNoise(), 4, 4,
                EngineSetting.GL_REPEAT,
                EngineSetting.GL_NEAREST));

        UBOHandle ssaoData = uboManager.getUBOHandleFromUBOName(EngineSetting.SSAO_DATA_UBO);
        ssaoData.updateUniform("u_samples", generateKernel());
        ssaoData.updateUniform("u_kernelSize", EngineSetting.SSAO_KERNEL_SIZE);
        ssaoData.updateUniform("u_radius", 0.3f);
        ssaoData.updateUniform("u_bias", 0.025f);
        uboManager.push(ssaoData);
    }

    @Override
    protected void update() {
        renderManager.pushRenderCall(ssaoPass.getModelInstance(), ssaoFbo, 0, context.getWindow());
    }

    // Accessible \\

    public FboInstance getSsaoFbo() {
        return ssaoFbo;
    }

    // Internal \\

    private float[] generateNoise() {
        float[] noise = new float[16 * 3];
        int index = 0;

        for (int i = 0; i < 16; i++) {
            noise[index++] = (float) (Math.random() * 2.0 - 1.0);
            noise[index++] = (float) (Math.random() * 2.0 - 1.0);
            noise[index++] = 0.0f;
        }

        return noise;
    }

    private Vector4[] generateKernel() {
        Vector4[] kernel = new Vector4[EngineSetting.SSAO_KERNEL_SIZE];

        for (int i = 0; i < EngineSetting.SSAO_KERNEL_SIZE; i++) {
            float x = (float) (Math.random() * 2.0 - 1.0);
            float y = (float) (Math.random() * 2.0 - 1.0);
            float z = (float) Math.random();

            float len = (float) Math.sqrt(x * x + y * y + z * z);
            x /= len;
            y /= len;
            z /= len;

            float scale = (float) i / EngineSetting.SSAO_KERNEL_SIZE;
            scale = 0.1f + scale * scale * 0.9f;

            kernel[i] = new Vector4(x * scale, y * scale, z * scale, 0.0f);
        }

        return kernel;
    }
}