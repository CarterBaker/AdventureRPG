package application.bootstrap.renderpipeline.rendermanager;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FBODestinationStruct;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbo.FBOStandInStruct;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FBORenderSystem extends SystemPackage {

    /*
     * Composites every window's FBOs onto its screen. Collects pushFbo()
     * submissions with their layer and destination rect, redirects logical
     * windows onto their OS window's region, and blits them in sort order
     * through the blit material. pushFboToTarget() blits an FBO into another
     * FBO through the same material instead, its layer as the draw depth, so
     * a target can gather several FBOs in order before reaching the screen.
     * A window given a capture target also has every layer it pushes
     * gathered into that target, in layer order, just before its OS window
     * draws, so a recording can read back exactly what the window
     * shows at whatever size it renders. A window given a stand-in has every
     * layer it pushes gathered into a target of its own instead of being
     * shown, and the stand-in — drawn from its model once every layer is
     * gathered — shown in their place, and captured in their place too, so
     * what the window shows can be built from what it drew this frame and
     * before. findPushedFbo() answers which target a window has pushed on a
     * layer so far this frame, and withdrawPushedFbos() takes back every
     * target a window has pushed from a layer up.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;

    // Blit
    private Object2ObjectOpenHashMap<FBOInstance, ModelInstance> fbo2BlitModel;
    private Object2ObjectOpenHashMap<WindowInstance, ObjectArrayList<FBOInstance>> window2BlitQueue;

    // Capture
    private Object2ObjectOpenHashMap<WindowInstance, FBOInstance> window2CaptureTarget;

    // Stand-In
    private Object2ObjectOpenHashMap<WindowInstance, FBOStandInStruct> window2StandIn;
    private ObjectArrayList<FBOStandInStruct> standIns;

    // Scratch
    private final Vector4 destRectScratch = new Vector4();
    private final Vector2 resolutionScratch = new Vector2();

    // Base \\

    @Override
    protected void create() {
        fbo2BlitModel = new Object2ObjectOpenHashMap<>();
        window2BlitQueue = new Object2ObjectOpenHashMap<>();
        window2CaptureTarget = new Object2ObjectOpenHashMap<>();
        window2StandIn = new Object2ObjectOpenHashMap<>();
        standIns = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        meshManager = get(MeshManager.class);
        materialManager = get(MaterialManager.class);
        renderManager = get(RenderManager.class);
    }

    // Accessible \\

    public void pushFbo(FBOInstance fbo, int layer, WindowInstance window) {
        pushFbo(fbo, layer, window, null, EngineSetting.SCREEN_ORDER_FOREGROUND);
    }

    public void pushFbo(FBOInstance fbo, int layer, WindowInstance window, FBODestinationStruct destRect) {
        pushFbo(fbo, layer, window, destRect, EngineSetting.SCREEN_ORDER_FOREGROUND);
    }

    public void pushFbo(FBOInstance fbo, int layer, WindowInstance window, int screenOrder) {
        pushFbo(fbo, layer, window, null, screenOrder);
    }

    public void pushFbo(FBOInstance fbo, int layer, WindowInstance window, FBODestinationStruct destRect,
            int screenOrder) {

        if (fbo == null || window == null)
            return;

        if (window.isWithdrawn())
            return;

        WindowInstance target = window.hasCompositeTarget() ? window.getCompositeTarget() : window;

        ObjectArrayList<FBOInstance> queue = window2BlitQueue.get(target);
        if (queue == null) {
            queue = new ObjectArrayList<>();
            window2BlitQueue.put(target, queue);
        }

        if (queue.size() >= EngineSetting.MAX_RENDER_CALLS_PER_FRAME)
            return;

        int sortKey = window.getZOrder() * EngineSetting.FBO_LAYER_STRIDE + layer;

        fbo.setPushData(window, layer, sortKey, screenOrder, resolveDestRect(window, destRect));
        queue.add(fbo);
    }

    public void setBlitOverride(FBOInstance fbo, MeshData mesh, MaterialInstance material) {
        if (fbo == null)
            return;

        fbo.setBlitOverride(mesh, material);
        fbo2BlitModel.remove(fbo);
    }

    public void pushBlits(WindowInstance window) {
        ObjectArrayList<FBOInstance> queue = window2BlitQueue.get(window);

        if (queue == null || queue.isEmpty())
            return;

        for (int i = 1; i < queue.size(); i++) {
            FBOInstance entry = queue.get(i);
            int entryKey = entry.getPushSortKey();
            int j = i - 1;

            while (j >= 0 && queue.get(j).getPushSortKey() > entryKey) {
                queue.set(j + 1, queue.get(j));
                j--;
            }

            queue.set(j + 1, entry);
        }

        for (int i = 0; i < queue.size(); i++) {
            FBOInstance fbo = queue.get(i);

            if (isStoodIn(fbo))
                continue;

            ModelInstance model = bindBlitModel(fbo, fbo.getPushDestRect(), window);

            renderManager.pushScreenCall(model, window, fbo.getPushScreenOrder());
        }

        queue.clear();
    }

    public void pushFboToTarget(FBOInstance fbo, FBOInstance target, int layer, WindowInstance window) {

        if (fbo == null || target == null || window == null)
            return;

        renderManager.pushRenderCall(bindBlitModel(fbo, null, window), target, layer, window);
    }

    public void removeWindowResources(WindowInstance window) {
        window2BlitQueue.remove(window);
        clearCaptureTarget(window);
        clearStandIn(window);
    }

    // The target the window pushed on the layer this frame, null when it pushed none there
    public FBOInstance findPushedFbo(WindowInstance window, int layer) {

        WindowInstance target = window.hasCompositeTarget() ? window.getCompositeTarget() : window;
        ObjectArrayList<FBOInstance> queue = window2BlitQueue.get(target);

        if (queue == null)
            return null;

        for (int i = 0; i < queue.size(); i++) {

            FBOInstance fbo = queue.get(i);

            if (fbo.getPushWindow() == window && fbo.getPushLayer() == layer)
                return fbo;
        }

        return null;
    }

    // Every target the window pushed this frame on the layer or above is taken back, so none of it is shown
    public void withdrawPushedFbos(WindowInstance window, int fromLayer) {

        WindowInstance target = window.hasCompositeTarget() ? window.getCompositeTarget() : window;
        ObjectArrayList<FBOInstance> queue = window2BlitQueue.get(target);

        if (queue == null)
            return;

        for (int i = queue.size() - 1; i >= 0; i--) {

            FBOInstance fbo = queue.get(i);

            if (fbo.getPushWindow() == window && fbo.getPushLayer() >= fromLayer)
                queue.remove(i);
        }
    }

    // Capture \\

    // Every layer the window pushes from now on is also gathered into the target
    public void setCaptureTarget(WindowInstance window, FBOInstance target) {
        window2CaptureTarget.put(window, target);
    }

    public void clearCaptureTarget(WindowInstance window) {
        window2CaptureTarget.remove(window);
    }

    // Queued just before the OS window's targets draw, so each capture target follows every layer it gathers, and
    // each stand-in follows every layer its window gathered
    public void pushCaptures(WindowInstance window) {

        if (window2CaptureTarget.isEmpty() && standIns.isEmpty())
            return;

        ObjectArrayList<FBOInstance> queue = window2BlitQueue.get(window);

        if (queue == null || queue.isEmpty())
            return;

        for (int i = 0; i < queue.size(); i++) {

            FBOInstance fbo = queue.get(i);
            WindowInstance source = fbo.getPushWindow();
            FBOStandInStruct standIn = window2StandIn.get(source);
            FBOInstance target = standIn != null ? standIn.getGatherTarget() : window2CaptureTarget.get(source);

            if (target == null)
                continue;

            if (standIn != null)
                standIn.setGathered(true);

            renderManager.pushRenderCall(bindBlitModel(fbo, null, source), target, fbo.getPushLayer(), source);
        }

        pushStandIns(window);
    }

    // Stand-In \\

    // From now on every layer the window pushes is gathered into the gather target instead of being shown, and the
    // stand-in, drawn from its model once every layer is gathered, is shown and captured in their place
    public void setStandIn(
            WindowInstance window,
            FBOInstance gatherTarget,
            FBOInstance standIn,
            ModelInstance standInModel) {

        FBOStandInStruct standInStruct = window2StandIn.get(window);

        if (standInStruct == null) {
            standInStruct = new FBOStandInStruct(window);
            window2StandIn.put(window, standInStruct);
            standIns.add(standInStruct);
        }

        standInStruct.set(gatherTarget, standIn, standInModel);
    }

    public void clearStandIn(WindowInstance window) {

        FBOStandInStruct standInStruct = window2StandIn.remove(window);

        if (standInStruct != null)
            standIns.remove(standInStruct);
    }

    // Each stand-in of a window drawing into this OS window that gathered anything is drawn, captured when its
    // window has a capture target, and shown on the window's lowest layer
    private void pushStandIns(WindowInstance osWindow) {

        for (int i = 0; i < standIns.size(); i++) {

            FBOStandInStruct standIn = standIns.get(i);
            WindowInstance source = standIn.getWindow();
            WindowInstance target = source.hasCompositeTarget() ? source.getCompositeTarget() : source;

            if (target != osWindow || !standIn.isGathered())
                continue;

            standIn.setGathered(false);
            renderManager.pushRenderCall(
                    standIn.getStandInModel(), standIn.getStandIn(), EngineSetting.FBO_STAND_IN_LAYER, source);

            FBOInstance capture = window2CaptureTarget.get(source);

            if (capture != null)
                renderManager.pushRenderCall(bindBlitModel(standIn.getStandIn(), null, source), capture,
                        EngineSetting.FBO_STAND_IN_LAYER, source);

            pushFbo(standIn.getStandIn(), EngineSetting.FBO_STAND_IN_LAYER, source);
        }
    }

    // True for a layer pushed by a window something stands in for, which is gathered rather than shown
    private boolean isStoodIn(FBOInstance fbo) {

        FBOStandInStruct standIn = window2StandIn.get(fbo.getPushWindow());

        return standIn != null && standIn.getStandIn() != fbo;
    }

    public void removeBlitModel(FBOInstance fbo) {
        fbo2BlitModel.remove(fbo);
    }

    // Internal \\

    private FBODestinationStruct resolveDestRect(WindowInstance window, FBODestinationStruct destRect) {
        if (destRect != null)
            return destRect;

        if (window.hasCompositeTarget() && window.hasCompositeRect()) {
            return new FBODestinationStruct(
                    window.getCompositeX(), window.getCompositeY(),
                    window.getCompositeW(), window.getCompositeH());
        }

        return null;
    }

    private ModelInstance bindBlitModel(FBOInstance fbo, FBODestinationStruct destRect, WindowInstance window) {

        ModelInstance model = resolveBlitModel(fbo);
        MaterialInstance material = model.getMaterial();

        if (destRect != null)
            destRectScratch.set(destRect.x, destRect.y, destRect.width, destRect.height);
        else
            destRectScratch.set(
                    EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                    EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                    EngineSetting.BLIT_FULLSCREEN_SENTINEL,
                    EngineSetting.BLIT_FULLSCREEN_SENTINEL);

        resolutionScratch.set(window.getWidth(), window.getHeight());

        material.setUniform(EngineSetting.UNIFORM_SOURCE, fbo.getTextureId());
        material.setUniform(EngineSetting.UNIFORM_DEST_RECT, destRectScratch);
        material.setUniform(EngineSetting.UNIFORM_RESOLUTION, resolutionScratch);

        return model;
    }

    private ModelInstance resolveBlitModel(FBOInstance fbo) {
        ModelInstance model = fbo2BlitModel.get(fbo);

        if (model != null)
            return model;

        MeshData meshData = fbo.getBlitMeshOverride();
        if (meshData == null)
            meshData = meshManager.getMeshHandleFromMeshName(EngineSetting.DEFAULT_BLIT_MESH).getMeshData();

        MaterialInstance material = fbo.getBlitMaterialOverride();
        if (material == null) {
            material = materialManager.cloneMaterial(EngineSetting.DEFAULT_BLIT_MATERIAL);
            material.setUniform(EngineSetting.BLIT_PREMULTIPLIED_UNIFORM, fbo.getFboData().isPremultipliedBlit());
            material.setUniform(EngineSetting.BLIT_RESOLVE_UNIFORM, fbo.getFboData().isResolveBlit());
        }

        model = create(ModelInstance.class);
        model.constructor(meshData, material);

        fbo2BlitModel.put(fbo, model);

        return model;
    }
}