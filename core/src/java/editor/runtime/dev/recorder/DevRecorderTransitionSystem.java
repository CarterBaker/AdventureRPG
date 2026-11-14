package editor.runtime.dev.recorder;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTransitionStruct;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.runtime.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;

public class DevRecorderTransitionSystem extends SystemPackage {

    /*
     * Shows the recorder's transitions in this Dev window. While the
     * timeline's picture passes through a transition, the window draws the
     * timeline with the transition's clip on one frame and without it on the
     * next, each gathered into a target of its own in place of being shown,
     * and what is shown — and captured, so an export holds it too — is the
     * two blended by the transition's shader at how far the picture has gone
     * over. A side the timeline shows no picture on is never drawn and blends
     * as the transition's colour. A side not yet drawn since the transition
     * began stands in with the other, so the first frame of a transition
     * never shows anything stale. An export picks the side it renders each
     * frame itself, so both sides of one moment are rendered before it is
     * captured. Updates after DevRecorderExportSystem and before
     * DevRecorderSystem, so the side it sets is the one played that frame.
     */

    // Internal
    private RecorderManager recorderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private MaterialManager materialManager;
    private MeshManager meshManager;

    // Targets — made the first time a transition shows here, and resized with the window
    private FBOInstance[] sides;
    private FBOInstance blended;
    private ModelInstance blendModel;

    // Transition
    private RecordingClipStruct shownClip;
    private RecorderClipEdge shownEdge;
    private boolean[] sidesDrawn;
    private boolean withoutNext;
    private boolean standing;

    // Uniforms
    private Vector4 mixParams;
    private Vector4 sideParams;
    private Vector4 destRect;
    private Vector2 resolution;

    // Base \\

    @Override
    protected void create() {

        // Transition
        this.sidesDrawn = new boolean[EditorSetting.RECORDER_TRANSITION_SIDES];

        // Uniforms
        this.mixParams = new Vector4();
        this.sideParams = new Vector4();
        this.destRect = new Vector4(
                EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                EngineSetting.BLIT_FULLSCREEN_SENTINEL);
        this.resolution = new Vector2();
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.materialManager = get(MaterialManager.class);
        this.meshManager = get(MeshManager.class);
    }

    @Override
    protected void dispose() {
        releaseStandIn();
    }

    // Update \\

    // The transition at the playhead read again, and the side played this frame flipped while both sides show a
    // picture; an export sets the side itself
    @Override
    protected void update() {

        RecordingTransitionStruct transition = recorderManager.isSessionFor((DevContext) context)
                ? recorderManager.resolveTransition()
                : null;

        if (transition == null || !transition.isActive()) {
            this.shownClip = null;
            this.shownEdge = null;
            return;
        }

        if (transition.getClip() != shownClip || transition.getEdge() != shownEdge) {
            this.shownClip = transition.getClip();
            this.shownEdge = transition.getEdge();
            this.sidesDrawn[EditorSetting.RECORDER_TRANSITION_SIDE_WITH] = false;
            this.sidesDrawn[EditorSetting.RECORDER_TRANSITION_SIDE_WITHOUT] = false;
        }

        if (recorderManager.getState() == RecorderState.EXPORTING)
            return;

        this.withoutNext = transition.isWithoutShown() && !withoutNext;
        recorderManager.setTransitionWithout(withoutNext);
    }

    // Render \\

    // The side drawn this frame gathered into its own target, and the blend of both shown in place of every layer
    @Override
    protected void render() {

        RecordingTransitionStruct transition = recorderManager.getTransition();

        if (shownClip == null || !transition.isActive()) {
            releaseStandIn();
            return;
        }

        WindowInstance window = context.getWindow();

        ensureTargets(window);

        int drawn = transition.isWithoutPlayed()
                ? EditorSetting.RECORDER_TRANSITION_SIDE_WITHOUT
                : EditorSetting.RECORDER_TRANSITION_SIDE_WITH;

        this.sidesDrawn[drawn] = true;
        bindBlend(transition, window, drawn);
        fboRenderSystem.setStandIn(window, sides[drawn], blended, blendModel);
        this.standing = true;
    }

    private void ensureTargets(WindowInstance window) {

        if (sides == null) {

            this.sides = new FBOInstance[EditorSetting.RECORDER_TRANSITION_SIDES];

            for (int i = 0; i < sides.length; i++)
                sides[i] = fboManager.cloneFbo(EditorSetting.RECORDER_CAPTURE_FBO, window);

            this.blended = fboManager.cloneFbo(EditorSetting.RECORDER_CAPTURE_FBO, window);

            MaterialInstance material = materialManager.cloneMaterial(EditorSetting.RECORDER_TRANSITION_MATERIAL);

            this.blendModel = create(ModelInstance.class);
            blendModel.constructor(
                    meshManager.getMeshHandleFromMeshName(EngineSetting.DEFAULT_BLIT_MESH).getMeshData(), material);
        }

        for (int i = 0; i < sides.length; i++)
            fboManager.resize(sides[i], window.getWidth(), window.getHeight());

        fboManager.resize(blended, window.getWidth(), window.getHeight());
    }

    // The transition's look and how far over it is, each side read from its target — or, not yet drawn, from the
    // side that is — and blank where the timeline shows no picture on it
    private void bindBlend(RecordingTransitionStruct transition, WindowInstance window, int drawn) {

        TransitionHandle transitionHandle = transition.getTransitionHandle();
        MaterialInstance material = blendModel.getMaterial();
        int from = transition.isFromWithout()
                ? EditorSetting.RECORDER_TRANSITION_SIDE_WITHOUT
                : EditorSetting.RECORDER_TRANSITION_SIDE_WITH;
        int to = EditorSetting.RECORDER_TRANSITION_SIDES - 1 - from;

        mixParams.set(
                transition.getMix(),
                transitionHandle.getKind().ordinal(),
                transitionHandle.getSoftness(),
                (float) window.getWidth() / Math.max(1, window.getHeight()));
        sideParams.set(
                transitionHandle.getDirection().getX(),
                transitionHandle.getDirection().getY(),
                isBlank(transition, from) ? 1f : 0f,
                isBlank(transition, to) ? 1f : 0f);
        resolution.set(window.getWidth(), window.getHeight());

        material.setUniform(EditorSetting.RECORDER_TRANSITION_UNIFORM_FROM, resolveTexture(from, drawn));
        material.setUniform(EditorSetting.RECORDER_TRANSITION_UNIFORM_TO, resolveTexture(to, drawn));
        material.setUniform(EditorSetting.RECORDER_TRANSITION_UNIFORM_MIX_PARAMS, mixParams);
        material.setUniform(EditorSetting.RECORDER_TRANSITION_UNIFORM_SIDE_PARAMS, sideParams);
        material.setUniform(EditorSetting.RECORDER_TRANSITION_UNIFORM_COLOR, transitionHandle.getColor());
        material.setUniform(EngineSetting.UNIFORM_DEST_RECT, destRect);
        material.setUniform(EngineSetting.UNIFORM_RESOLUTION, resolution);
    }

    private boolean isBlank(RecordingTransitionStruct transition, int side) {
        return side == EditorSetting.RECORDER_TRANSITION_SIDE_WITHOUT && !transition.isWithoutShown();
    }

    private int resolveTexture(int side, int drawn) {
        return sides[sidesDrawn[side] ? side : drawn].getTextureId();
    }

    // The window shows its own layers again
    private void releaseStandIn() {

        if (!standing)
            return;

        fboRenderSystem.clearStandIn(context.getWindow());
        this.standing = false;
    }
}
