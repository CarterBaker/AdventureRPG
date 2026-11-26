package application.bootstrap.worldpipeline.treemanager;

import java.nio.ByteBuffer;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionUtility;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.TreeLodUtility;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class TreeImpostorRenderSystem extends SystemPackage {

    /*
     * Draws every tree at its impostor stage as one GPU instance of a single
     * camera-facing quad, all species in one instanced draw, each instance
     * naming its species' layer of the impostor arrays and the three frames
     * nearest the view blended. Instances live apart from every chunk, mega
     * and macro tile: the registered trees at their impostor stage, and the
     * sites the macro terrain hands over, those the grid shows itself left out
     * by their registry key, so no tree shows twice and none goes missing. The
     * instance data is laid again only when the registry, a tree's LOD, the
     * trees the grid has on the GPU, a macro tile's sites or a species'
     * impostor changed, at most once an interval, so a tree at its impostor
     * stage rebuilds nothing else. Every instance carries the time its fade
     * starts on the shaders' clock: a tree whose LOD has just brought it to
     * its impostor rises in through the trees' dither while its mesh stands,
     * and one leaving it keeps it for a hold, falling away only once the mesh
     * taking its place has risen; trees arriving already there stand whole.
     * Each species' impostor is written into its layer the frame it is first
     * seen or baked anew.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeDetailBranch treeDetailBranch;
    private WorldRenderManager worldRenderManager;
    private ClockManager clockManager;
    private WorldStreamManager worldStreamManager;
    private VAOManager vaoManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private CompositeBufferManager compositeBufferManager;
    private RenderManager renderManager;

    // Draw
    private CompositeBufferInstance buffer;
    private MaterialInstance material;

    // Arrays — one layer to each species, and the impostor each layer last took
    private int albedoArray;
    private int normalArray;
    private TreeImpostorStruct[] layer2Impostor;
    private ByteBuffer chainStaging;

    // Macro — every tile's sites, laid as instances, and the revision moving with them
    private Long2ObjectOpenHashMap<FloatArrayList> macroCoordinate2Sites;
    private ObjectArrayList<FloatArrayList> siteLists;
    private ObjectArrayList<FloatArrayList> siteListPool;
    private int macroRevision;

    // Hold — the trees drawn by their impostor at the last laying, and those keeping it a while after leaving
    private ReferenceOpenHashSet<TreeInstance> shownTrees;
    private ReferenceOpenHashSet<TreeInstance> nowShown;
    private ObjectArrayList<TreeInstance> holdTrees;
    private FloatArrayList holdSeconds;
    private FloatArrayList holdFallStarts;

    // Fades — the trees in the registry at the last laying, and when each tree shown began rising
    private ReferenceOpenHashSet<TreeInstance> registeredTrees;
    private ReferenceOpenHashSet<TreeInstance> nowRegistered;
    private Reference2FloatOpenHashMap<TreeInstance> riseStarts;
    private Reference2FloatOpenHashMap<TreeInstance> nowRiseStarts;

    // Laying — the revisions the instances were last laid at, and the scratch they are laid through
    private int laidRegistryRevision;
    private int laidDetailRevision;
    private int laidMacroRevision;
    private int laidImpostorRevision;
    private int laidTreeDrawnRevision;
    private int impostorRevision;
    private float sinceLaid;
    private ObjectArrayList<TreeInstance> trees;
    private float[] instance;

    // Base \\

    @Override
    protected void create() {

        // Macro
        this.macroCoordinate2Sites = new Long2ObjectOpenHashMap<>();
        this.siteLists = new ObjectArrayList<>();
        this.siteListPool = new ObjectArrayList<>();

        // Hold
        this.shownTrees = new ReferenceOpenHashSet<>();
        this.nowShown = new ReferenceOpenHashSet<>();
        this.holdTrees = new ObjectArrayList<>();
        this.holdSeconds = new FloatArrayList();
        this.holdFallStarts = new FloatArrayList();

        // Fades
        this.registeredTrees = new ReferenceOpenHashSet<>();
        this.nowRegistered = new ReferenceOpenHashSet<>();
        this.riseStarts = new Reference2FloatOpenHashMap<>();
        this.nowRiseStarts = new Reference2FloatOpenHashMap<>();

        // Laying
        this.laidRegistryRevision = EngineSetting.INDEX_NOT_FOUND;
        this.trees = new ObjectArrayList<>();
        this.instance = new float[EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS];
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.treeDetailBranch = get(TreeDetailBranch.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.clockManager = get(ClockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.vaoManager = get(VAOManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void start() {

        // Draw
        this.buffer = create(CompositeBufferInstance.class);
        compositeBufferManager.constructor(buffer, createQuad(), EngineSetting.TREE_IMPOSTOR_INSTANCE_ATTR_SIZES);
        this.material = materialManager.cloneMaterial(
                materialManager.getMaterialIDFromMaterialName(EngineSetting.TREE_IMPOSTOR_MATERIAL));

        // Arrays
        int layers = treeManager.getTreeSlotCount();

        this.albedoArray = TreeImpostorGLSLUtility.createArray(layers);
        this.normalArray = TreeImpostorGLSLUtility.createArray(layers);
        this.layer2Impostor = new TreeImpostorStruct[layers];
        this.chainStaging = ByteBuffer.allocateDirect(
                TextureCompressionUtility.getChainBytes(EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS));

        material.setUniform(EngineSetting.UNIFORM_TREE_IMPOSTOR_ALBEDO, albedoArray);
        material.setUniform(EngineSetting.UNIFORM_TREE_IMPOSTOR_NORMAL, normalArray);
    }

    // The one quad every impostor stands on, its corners from minus one to one across and up, in the chunk layout
    private MeshHandle createQuad() {

        int stride = EngineSetting.CHUNK_VERTEX_FLOAT_COUNT;
        FloatArrayList vertices = new FloatArrayList(stride * EngineSetting.QUAD_VERTEX_COUNT);
        ShortArrayList indices = new ShortArrayList(EngineSetting.QUAD_INDEX_COUNT);
        float[] cornersX = { -1f, 1f, 1f, -1f };
        float[] cornersY = { -1f, -1f, 1f, 1f };

        for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {

            vertices.add(cornersX[corner]);
            vertices.add(cornersY[corner]);

            for (int pad = 2; pad < stride; pad++)
                vertices.add(0f);
        }

        indices.add((short) 0);
        indices.add((short) 1);
        indices.add((short) 2);
        indices.add((short) 0);
        indices.add((short) 2);
        indices.add((short) 3);

        return meshManager.createMeshHandle(
                EngineSetting.TREE_IMPOSTOR_MESH, vaoManager.getVAOHandleFromVAOName(EngineSetting.CHUNK_VAO),
                vertices, indices);
    }

    @Override
    protected void dispose() {
        TreeImpostorGLSLUtility.deleteArray(albedoArray);
        TreeImpostorGLSLUtility.deleteArray(normalArray);
    }

    // Update \\

    @Override
    protected void update() {

        uploadLayers();
        advanceHolds(internal.getDeltaTime());
        sinceLaid += internal.getDeltaTime();

        if (isStale())
            layInstances();

        if (buffer.isEmpty() || !worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        for (int i = 0; i < grids.size(); i++) {

            WindowInstance window = grids.get(i).getWindowInstance();
            FBOInstance worldFbo = grids.get(i).getRenderTargetFbo();

            if (window == null || worldFbo == null)
                continue;

            renderManager.ensureFboRendered(worldFbo, window);
            renderManager.pushCompositeCall(material, buffer, worldFbo, window);
        }
    }

    // Layers \\

    // Every species whose impostor is new since its layer was last written, written now
    private void uploadLayers() {

        for (int layer = 1; layer < layer2Impostor.length; layer++) {

            TreeHandle treeHandle = treeManager.findTreeHandleByID((short) layer);
            TreeImpostorStruct impostor = treeHandle != null ? treeHandle.getImpostor() : null;

            if (impostor == null || impostor == layer2Impostor[layer])
                continue;

            uploadChain(albedoArray, layer, impostor.getAlbedo());
            uploadChain(normalArray, layer, impostor.getNormal());
            layer2Impostor[layer] = impostor;
            impostorRevision++;
        }
    }

    private void uploadChain(int array, int layer, byte[] chain) {

        chainStaging.clear();
        chainStaging.put(chain);
        chainStaging.flip();
        TreeImpostorGLSLUtility.uploadLayer(array, layer, chainStaging);
    }

    // Macro \\

    // A macro tile's sites, laid as impostor instances, taking the place of any it handed over before
    public void setMacroSites(long macroCoordinate, FloatArrayList sites) {

        removeMacroSites(macroCoordinate);

        if (sites.isEmpty())
            return;

        FloatArrayList stored = siteListPool.isEmpty() ? new FloatArrayList(sites.size()) : siteListPool.pop();

        stored.addAll(sites);
        macroCoordinate2Sites.put(macroCoordinate, stored);
        siteLists.add(stored);
        macroRevision++;
    }

    public void removeMacroSites(long macroCoordinate) {

        FloatArrayList stored = macroCoordinate2Sites.remove(macroCoordinate);

        if (stored == null)
            return;

        siteLists.remove(stored);
        stored.clear();
        siteListPool.push(stored);
        macroRevision++;
    }

    // Hold \\

    // Every held tree's hold run down by a frame, the instances laid again once one ends
    private void advanceHolds(float deltaTime) {

        for (int i = holdTrees.size() - 1; i >= 0; i--) {

            float seconds = holdSeconds.getFloat(i) - deltaTime;

            if (seconds > 0f) {
                holdSeconds.set(i, seconds);
                continue;
            }

            holdTrees.remove(i);
            holdSeconds.removeFloat(i);
            holdFallStarts.removeFloat(i);
            impostorRevision++;
        }
    }

    // Laying \\

    // Due when what the instances are laid from moved since, and no sooner than an interval after the last laying
    private boolean isStale() {
        return sinceLaid >= EngineSetting.TREE_IMPOSTOR_LAY_SECONDS
                && (treeRegistryBranch.getRevision() != laidRegistryRevision
                        || treeDetailBranch.getRevision() != laidDetailRevision
                        || worldRenderManager.getTreeDrawnRevision() != laidTreeDrawnRevision
                        || macroRevision != laidMacroRevision
                        || impostorRevision != laidImpostorRevision);
    }

    // Every instance laid afresh: the registered trees at their impostor stage and those held, then every macro site
    // the grid does not show itself
    private void layInstances() {

        float now = clockManager.getShaderTime();

        sinceLaid = 0f;
        laidRegistryRevision = treeRegistryBranch.getRevision();
        laidDetailRevision = treeDetailBranch.getRevision();
        laidTreeDrawnRevision = worldRenderManager.getTreeDrawnRevision();
        laidMacroRevision = macroRevision;
        laidImpostorRevision = impostorRevision;

        buffer.clear();
        nowShown.clear();
        nowRegistered.clear();
        nowRiseStarts.clear();
        treeRegistryBranch.collectTrees(trees);

        for (int i = 0; i < trees.size(); i++)
            layTree(trees.get(i), now);

        for (int i = 0; i < holdTrees.size(); i++)
            if (!nowShown.contains(holdTrees.get(i)))
                addTree(holdTrees.get(i), holdFallStarts.getFloat(i), EngineSetting.TREE_IMPOSTOR_FALLING);

        trees.clear();
        swapLaying();

        for (int i = 0; i < siteLists.size(); i++)
            addSites(siteLists.get(i));
    }

    // A registered tree at its impostor stage laid, rising when its LOD has just brought it there and whole when it
    // came into the registry already there; one leaving its impostor held, falling away at its hold's end
    private void layTree(TreeInstance tree, float now) {

        nowRegistered.add(tree);

        if (!TreeLodUtility.isImpostor(tree.getTreeHandle(), tree.getLod(), tree.isAltered())) {

            if (shownTrees.contains(tree) && !holdTrees.contains(tree)) {
                holdTrees.add(tree);
                holdSeconds.add(EngineSetting.TREE_IMPOSTOR_HOLD_SECONDS);
                holdFallStarts.add(now + EngineSetting.TREE_IMPOSTOR_HOLD_SECONDS
                        - EngineSetting.TREE_IMPOSTOR_FADE_SECONDS);
            }

            return;
        }

        float start = shownTrees.contains(tree)
                ? riseStarts.getFloat(tree)
                : registeredTrees.contains(tree) ? now : EngineSetting.TREE_IMPOSTOR_WHOLE_START;

        nowShown.add(tree);
        nowRiseStarts.put(tree, start);
        addTree(tree, start, EngineSetting.TREE_IMPOSTOR_RISING);
    }

    // This laying's sets taken as the last, the last ones emptied for the next
    private void swapLaying() {

        ReferenceOpenHashSet<TreeInstance> shown = shownTrees;
        ReferenceOpenHashSet<TreeInstance> registered = registeredTrees;
        Reference2FloatOpenHashMap<TreeInstance> starts = riseStarts;

        shownTrees = nowShown;
        nowShown = shown;
        registeredTrees = nowRegistered;
        nowRegistered = registered;
        riseStarts = nowRiseStarts;
        nowRiseStarts = starts;
    }

    private void addTree(TreeInstance tree, float fadeStart, float fadeDirection) {

        if (!TreeLodUtility.writeImpostorInstance(
                tree.getTreeHandle(), tree.getSeed(), tree.getMatureHeight(), tree.getAge(),
                tree.getAnchorX(), tree.getAnchorZ(), tree.getBaseY(), instance, 0))
            return;

        instance[EngineSetting.TREE_IMPOSTOR_FADE_START_FLOAT] = fadeStart;
        instance[EngineSetting.TREE_IMPOSTOR_FADE_DIRECTION_FLOAT] = fadeDirection;
        buffer.addInstance(instance);
    }

    // Every site of a macro tile laid, but for those the grid shows itself: a tree registered there drawn by its
    // impostor, held, or standing in trees the grid has on the GPU
    private void addSites(FloatArrayList sites) {

        float[] elements = sites.elements();
        int floats = EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS;

        for (int offset = 0; offset + floats <= sites.size(); offset += floats) {

            TreeInstance tree = treeRegistryBranch.find(resolveSiteKey(elements, offset));

            if (tree != null && (shownTrees.contains(tree) || holdTrees.contains(tree)
                    || worldRenderManager.isChunkTreesOnGpu(tree.getRootChunkCoordinate())))
                continue;

            System.arraycopy(elements, offset, instance, 0, floats);
            buffer.addInstance(instance);
        }
    }

    // The registry key of the tree a site's instance stands for, from its root chunk, its place in it and its layer
    private long resolveSiteKey(float[] elements, int offset) {

        int chunkSize = EngineSetting.CHUNK_SIZE;
        long anchorX = (long) Float.floatToRawIntBits(elements[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_X_FLOAT])
                * chunkSize + (long) Math.floor(elements[offset + EngineSetting.TREE_IMPOSTOR_LOCAL_X_FLOAT]);
        long anchorZ = (long) Float.floatToRawIntBits(elements[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_Z_FLOAT])
                * chunkSize + (long) Math.floor(elements[offset + EngineSetting.TREE_IMPOSTOR_LOCAL_Z_FLOAT]);

        return TreeInstance.toRegistryKey(
                anchorX, anchorZ, (short) elements[offset + EngineSetting.TREE_IMPOSTOR_LAYER_FLOAT]);
    }

    // Accessible \\

    public MeshHandle getQuad() {
        return buffer.getMeshHandle();
    }

    public int getAlbedoArray() {
        return albedoArray;
    }

    public int getNormalArray() {
        return normalArray;
    }
}
