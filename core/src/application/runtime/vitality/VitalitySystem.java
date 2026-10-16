package application.runtime.vitality;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

public class VitalitySystem extends SystemPackage {

    /*
     * Shows this window's player how close to death it stands, since its
     * health is never shown outright. Its own fullscreen pass, drawn on its
     * own layer above the scene and below every menu, closes a darkness in
     * from the edges of sight once health falls below a share — easing in,
     * half shut deep into a fight, all but black on the brink and wholly black
     * at death — and draws bloodshot veins creeping in from the edges while
     * fresh bleeding lasts, so a cut reads at a glance and a drowning only
     * darkens. A level gained flashes a glow in from the edges, white for the
     * virtuous, grey between and black for the wicked, fading over a few
     * seconds. Each effect follows its target smoothly, and nothing is drawn
     * while all of them are clear.
     */

    // Internal
    private PlayerManager playerManager;
    private ProgressionManager progressionManager;
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;

    // Render Target
    private PassInstance vitalityPass;
    private FBOInstance vitalityFbo;

    // Effects
    private float darkness;
    private float bleed;
    private float glow;
    private float time;

    // Level Up
    private EntityInstance watchedPlayer;
    private int seenLevelUps;

    // Uniforms
    private final Vector4 vitality = new Vector4();
    private final Vector3 glowColor = new Vector3();

    // Base \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
    }

    @Override
    protected void awake() {
        this.vitalityPass = passManager.clonePass(RuntimeSetting.PASS_VITALITY);
        this.vitalityFbo = fboManager.cloneFbo(RuntimeSetting.FBO_VITALITY, context.getWindow());
    }

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();
        int windowID = context.getWindow().getWindowID();
        EntityInstance player = playerManager.hasPlayerForWindow(windowID)
                ? playerManager.getPlayerForWindow(windowID)
                : null;

        time = (time + deltaTime) % RuntimeSetting.VITALITY_TIME_PERIOD;

        if (player == null) {
            approach(0f, 0f, deltaTime);
            glow = Math.max(0f, glow - deltaTime / RuntimeSetting.VITALITY_GLOW_SECONDS);
            return;
        }

        StatisticsHandle statisticsHandle = player.getStatisticsHandle();

        approach(resolveDarkness(statisticsHandle), statisticsHandle.getBleed(), deltaTime);
        updateGlow(player, deltaTime);
    }

    @Override
    protected void lateUpdate() {

        if (darkness < RuntimeSetting.VITALITY_VISIBLE_THRESHOLD
                && bleed < RuntimeSetting.VITALITY_VISIBLE_THRESHOLD
                && glow < RuntimeSetting.VITALITY_VISIBLE_THRESHOLD)
            return;

        MaterialInstance material = vitalityPass.getModelInstance().getMaterial();

        material.setUniform(RuntimeSetting.UNIFORM_VITALITY, vitality.set(darkness, bleed, glow, time));
        material.setUniform(RuntimeSetting.UNIFORM_VITALITY_GLOW_COLOR, glowColor);

        renderManager.pushRenderCall(
                vitalityPass.getModelInstance(),
                vitalityFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        fboRenderSystem.pushFbo(vitalityFbo, RuntimeSetting.LAYER_VITALITY, context.getWindow());
    }

    // Darkness \\

    // How far the darkness should be closed for a share of health left — none above the start, all at death
    private float resolveDarkness(StatisticsHandle statisticsHandle) {

        if (statisticsHandle.isDefeated())
            return 1f;

        float closing = (RuntimeSetting.VITALITY_DARKNESS_START - statisticsHandle.getHealth())
                / RuntimeSetting.VITALITY_DARKNESS_START;

        return (float) Math.pow(Math.max(0f, Math.min(1f, closing)), RuntimeSetting.VITALITY_DARKNESS_EASE);
    }

    private void approach(float targetDarkness, float targetBleed, float deltaTime) {
        darkness += (targetDarkness - darkness)
                * (1f - (float) Math.exp(-RuntimeSetting.VITALITY_DARKNESS_RATE * deltaTime));
        bleed += (targetBleed - bleed) * (1f - (float) Math.exp(-RuntimeSetting.VITALITY_BLEED_RATE * deltaTime));
    }

    // Glow \\

    // A level-up count beyond the one last seen flashes the glow, its shade following the player's morality
    private void updateGlow(EntityInstance player, float deltaTime) {

        int levelUps = player.getProgressionHandle().getLevelUps();

        if (player != watchedPlayer || levelUps < seenLevelUps) {
            watchedPlayer = player;
            seenLevelUps = levelUps;
        }

        glow = Math.max(0f, glow - deltaTime / RuntimeSetting.VITALITY_GLOW_SECONDS);

        if (levelUps == seenLevelUps)
            return;

        float shade = (progressionManager.getMoralityShade(player) + 1f) * 0.5f;

        seenLevelUps = levelUps;
        glow = EngineSetting.VITALITY_FULL;
        glowColor.set(shade, shade, shade);
    }
}
