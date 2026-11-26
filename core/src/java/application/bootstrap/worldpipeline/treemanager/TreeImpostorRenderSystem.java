package application.bootstrap.worldpipeline.treemanager;

import java.nio.ByteBuffer;

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
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class TreeImpostorRenderSystem extends SystemPackage {

    /*
     * Draws every tree at an impostor level as one GPU instance of a single
     * camera-facing quad, all species in one instanced draw, each instance
     * naming its species' layer of the impostor arrays. Instances live apart
     * from every chunk, mega and macro tile: the registered trees at impostor
     * levels and the sites the macro terrain hands over, those whose root
     * chunk the grid holds left to the registry, so no tree shows twice. The
     * instance data is laid again only when the registry, a tree's LOD, a
     * macro tile's sites or a species' impostor changed, so a tree passing
     * between impostor levels rebuilds nothing else. A tree leaving its
     * impostor keeps it for a hold, long enough for the mesh its chunk or
     * mega draws instead to land, so it never blinks out between them. Each
     * species' impostor is written into its layer the frame it is first seen
     * or baked anew.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeDetailBranch treeDetailBranch;
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
    private ObjectArrayList<FloatArrayList> siteListPool;
    private int macroRevision;

    // Hold — the trees drawn by their impostor at the last laying, and those keeping it a while after leaving
    private ReferenceOpenHashSet<TreeInstance> shownTrees;
    private ReferenceOpenHashSet<TreeInstance> nowShown;
    private ObjectArrayList<TreeInstance> holdTrees;
    private FloatArrayList holdSeconds;

    // Laying — the revisions the instances were last laid at, and the scratch they are laid through
    private int laidRegistryRevision;
    private int laidDetailRevision;
    private int laidMacroRevision;
    private int laidImpostorRevision;
    private int impostorRevision;
    private ObjectArrayList<TreeInstance> trees;
    private float[] instance;

    // Base \\

    @Override
    protected void create() {

        // Macro
        this.macroCoordinate2Sites = new Long2ObjectOpenHashMap<>();
        this.siteListPool = new ObjectArrayList<>();

        // Hold
        this.shownTrees = new ReferenceOpenHashSet<>();
        this.nowShown = new ReferenceOpenHashSet<>();
        this.holdTrees = new ObjectArrayList<>();
        this.holdSeconds = new FloatArrayList();

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
        macroRevision++;
    }

    public void removeMacroSites(long macroCoordinate) {

        FloatArrayList stored = macroCoordinate2Sites.remove(macroCoordinate);

        if (stored == null)
            return;

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
            impostorRevision++;
        }
    }

    // Laying \\

    private boolean isStale() {
        return treeRegistryBranch.getRevision() != laidRegistryRevision
                || treeDetailBranch.getRevision() != laidDetailRevision
                || macroRevision != laidMacroRevision
                || impostorRevision != laidImpostorRevision;
    }

    // Every instance laid afresh: the registered trees at impostor levels and those held, then every macro site
    // whose root chunk the grid does not hold
    private void layInstances() {

        laidRegistryRevision = treeRegistryBranch.getRevision();
        laidDetailRevision = treeDetailBranch.getRevision();
        laidMacroRevision = macroRevision;
        laidImpostorRevision = impostorRevision;

        buffer.clear();
        nowShown.clear();
        treeRegistryBranch.collectTrees(trees);

        for (int i = 0; i < trees.size(); i++) {

            TreeInstance tree = trees.get(i);

            if (TreeLodUtility.isImpostor(tree.getTreeHandle(), tree.getLod(), tree.isAltered())) {
                nowShown.add(tree);
                addTree(tree);
                continue;
            }

            if (shownTrees.contains(tree) && !holdTrees.contains(tree)) {
                holdTrees.add(tree);
                holdSeconds.add(EngineSetting.TREE_IMPOSTOR_HOLD_SECONDS);
            }
        }

        for (int i = 0; i < holdTrees.size(); i++)
            if (!nowShown.contains(holdTrees.get(i)))
                addTree(holdTrees.get(i));

        trees.clear();

        ReferenceOpenHashSet<TreeInstance> swap = shownTrees;
        shownTrees = nowShown;
        nowShown = swap;

        ObjectIterator<FloatArrayList> sites = macroCoordinate2Sites.values().iterator();

        while (sites.hasNext())
            addSites(sites.next());
    }

    private void addTree(TreeInstance tree) {
        if (TreeLodUtility.writeImpostorInstance(
                tree.getTreeHandle(), tree.getSeed(), tree.getMatureHeight(), tree.getAge(),
                tree.getAnchorX(), tree.getAnchorZ(), tree.getBaseY(), instance, 0))
            buffer.addInstance(instance);
    }

    private void addSites(FloatArrayList sites) {

        float[] elements = sites.elements();
        int floats = EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS;

        for (int offset = 0; offset + floats <= sites.size(); offset += floats) {

            long rootChunk = Coordinate2Long.pack(
                    Float.floatToRawIntBits(elements[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_X_FLOAT]),
                    Float.floatToRawIntBits(elements[offset + EngineSetting.TREE_IMPOSTOR_CHUNK_Z_FLOAT]));

            if (worldStreamManager.getChunkInstance(rootChunk) != null)
                continue;

            System.arraycopy(elements, offset, instance, 0, floats);
            buffer.addInstance(instance);
        }
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
