package editor.bootstrap.treeimpostorpipeline.treeimpostormanager;

import java.nio.ByteBuffer;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.pbo.PBOInstance;
import application.bootstrap.renderpipeline.pbomanager.PBOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.treemanager.TreeImpostorArpgUtility;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeLodUtility;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.treeimpostorpipeline.util.TreeImpostorAtlasUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeImpostorBakeBranch extends BranchPackage {

    /*
     * Bakes one species' impostor at a time. Its shared tree, grown in full
     * from its LOD seed at full age, is drawn into the bake target from every
     * frame's direction, once for its albedo and once for its normal, one view
     * a frame through the bake materials, which shade the wood and leaves
     * exactly as the game's G-buffer receives them. Each view is read back
     * through a double-buffered pack buffer two frames after it was drawn, so
     * the main thread never waits on the GPU, and copied into its frame of the
     * atlas. Once every view is in, a worker dilates and compresses both maps,
     * writes the impostor file and hands it to TreeManager, which shows it at
     * once. A species asked for while another bakes waits its turn.
     */

    // Internal
    private TreeManager treeManager;
    private ItemModelManager itemModelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private PBOManager pboManager;
    private WindowManager windowManager;
    private ThreadHandle threadHandle;

    // Target
    private FBOInstance bakeFbo;
    private PBOInstance bakePbo;
    private ByteBuffer readback;
    private int barkMaterialID;
    private int leafMaterialID;

    // Job — the species baking, the source it carries, its shared tree's meshes and frame
    private String treeName;
    private long source;
    private ObjectArrayList<MeshInstance> barkMeshes;
    private ObjectArrayList<MeshInstance> leafMeshes;
    private FloatArrayList builtBark;
    private FloatArrayList builtLeaves;
    private float height;
    private float centerY;
    private float radius;

    // Views — the next to draw, the one drawn last, those read back but not yet retrieved, and how many are in
    private int nextView;
    private int lastView;
    private IntArrayList pendingViews;
    private int retrievedViews;

    // Atlases
    private byte[] albedo;
    private byte[] normal;
    private boolean[] covered;
    private boolean[] coveredScratch;
    private TextureCompressionScratchStruct compressionScratch;
    private volatile boolean compressing;

    // Scratch
    private float[] direction;
    private Matrix4 viewProjection;

    // Base \\

    @Override
    protected void create() {

        int atlasTexels = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS;

        // Job
        this.barkMeshes = new ObjectArrayList<>();
        this.leafMeshes = new ObjectArrayList<>();
        this.builtBark = new FloatArrayList();
        this.builtLeaves = new FloatArrayList();

        // Views
        this.pendingViews = new IntArrayList();

        // Atlases
        this.albedo = new byte[atlasTexels * atlasTexels * EngineSetting.COLOR_CHANNEL_COUNT];
        this.normal = new byte[albedo.length];
        this.covered = new boolean[atlasTexels * atlasTexels];
        this.coveredScratch = new boolean[covered.length];
        this.compressionScratch = new TextureCompressionScratchStruct();

        // Scratch
        this.direction = new float[EngineSetting.AXIS_COUNT];
        this.viewProjection = new Matrix4();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.pboManager = get(PBOManager.class);
        this.windowManager = get(WindowManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.GENERAL_THREAD_NAME);
    }

    @Override
    protected void start() {

        int texels = EngineSetting.TREE_IMPOSTOR_FRAME_TEXELS;

        // Target
        this.bakeFbo = fboManager.getFbo(EditorSetting.FBO_TREE_IMPOSTOR_BAKE);
        this.bakePbo = pboManager.createPbo(windowManager.getMainWindow(), texels, texels);
        this.readback = ByteBuffer.allocateDirect(texels * texels * EngineSetting.COLOR_CHANNEL_COUNT);
        this.barkMaterialID = materialManager.getMaterialIDFromMaterialName(
                EditorSetting.TREE_IMPOSTOR_BAKE_BARK_MATERIAL);
        this.leafMaterialID = materialManager.getMaterialIDFromMaterialName(
                EditorSetting.TREE_IMPOSTOR_BAKE_LEAF_MATERIAL);
    }

    // Job \\

    boolean isIdle() {
        return treeName == null;
    }

    String getTreeName() {
        return treeName;
    }

    // Main thread — a species' shared tree grown and put on the GPU, its views drawn from the next frame; false when
    // the species is not loaded
    boolean begin(String bakedName, long bakedSource) {

        TreeHandle treeHandle = treeManager.findTreeHandle(bakedName);

        if (treeHandle == null)
            return false;

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(
                treeHandle, null, 0L, 0L, 0, TreeLodUtility.resolveSharedSeed(treeHandle),
                -treeHandle.getGrowth().getDays(), false, 0.0);

        TreeShapeStruct shape = tree.getShape();

        builtBark.clear();
        builtLeaves.clear();
        treeManager.buildShapeGeometry(shape, builtBark, builtLeaves);
        treeManager.uploadTreeMeshes(builtBark, barkMeshes);
        treeManager.uploadTreeMeshes(builtLeaves, leafMeshes);
        builtBark.clear();
        builtBark.trim();
        builtLeaves.clear();
        builtLeaves.trim();

        float spanX = Math.max(Math.abs(shape.getMinX()), Math.abs(shape.getMaxX()));
        float spanZ = Math.max(Math.abs(shape.getMinZ()), Math.abs(shape.getMaxZ()));
        float halfHeight = (shape.getMaxY() - shape.getMinY()) * 0.5f;

        this.treeName = bakedName;
        this.source = bakedSource;
        this.height = tree.getMatureHeight();
        this.centerY = (shape.getMinY() + shape.getMaxY()) * 0.5f;
        this.radius = Math.max(
                (float) Math.sqrt(spanX * spanX + spanZ * spanZ + halfHeight * halfHeight),
                EditorSetting.TREE_IMPOSTOR_MIN_RADIUS_BLOCKS);
        this.nextView = 0;
        this.lastView = EngineSetting.INDEX_NOT_FOUND;
        this.retrievedViews = 0;
        pendingViews.clear();

        return true;
    }

    // Main thread — one frame of the bake: the view drawn two frames ago taken in, the last one's read queued and the
    // next one drawn, then the atlases handed to the worker once every view is in; true the frame the bake ends
    boolean step() {

        if (compressing)
            return false;

        if (retrievedViews == EditorSetting.TREE_IMPOSTOR_BAKE_VIEWS) {
            finish();
            return true;
        }

        WindowInstance window = windowManager.getMainWindow();

        if (pboManager.captureFbo(bakePbo, window, bakeFbo, readback) && !pendingViews.isEmpty())
            takeView(pendingViews.removeInt(0));

        pendingViews.add(lastView);

        if (nextView < EditorSetting.TREE_IMPOSTOR_BAKE_VIEWS) {
            drawView(nextView, window);
            lastView = nextView++;
        } else
            lastView = EngineSetting.INDEX_NOT_FOUND;

        if (retrievedViews == EditorSetting.TREE_IMPOSTOR_BAKE_VIEWS)
            beginCompression();

        return false;
    }

    // Views \\

    // A view drawn into the bake target: its frame's camera, the map it writes, every mesh of the shared tree
    private void drawView(int view, WindowInstance window) {

        int frames = EngineSetting.TREE_IMPOSTOR_FRAMES;
        int frame = view % (frames * frames);
        int map = view / (frames * frames);

        TreeImpostorAtlasUtility.resolveDirection(frame % frames, frame / frames, direction);
        TreeImpostorAtlasUtility.writeViewProjection(direction, centerY, radius, viewProjection);

        pushMeshes(barkMeshes, barkMaterialID, map, window);
        pushMeshes(leafMeshes, leafMaterialID, map, window);
        renderManager.ensureFboRendered(bakeFbo, window);
    }

    private void pushMeshes(ObjectArrayList<MeshInstance> meshes, int materialID, int map, WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), materialID);

            model.getMaterial().setUniform(EditorSetting.UNIFORM_TREE_IMPOSTOR_BAKE_VIEW_PROJECTION, viewProjection);
            model.getMaterial().setUniform(EditorSetting.UNIFORM_TREE_IMPOSTOR_BAKE_MAP, map);
            renderManager.pushRenderCall(model, bakeFbo, EditorSetting.TREE_IMPOSTOR_BAKE_DEPTH, window);
        }
    }

    // A view read back laid into its frame of its map; a frame read before any view was drawn is passed over
    private void takeView(int view) {

        if (view == EngineSetting.INDEX_NOT_FOUND)
            return;

        int frames = EngineSetting.TREE_IMPOSTOR_FRAMES;
        int frame = view % (frames * frames);
        byte[] atlas = view / (frames * frames) == EditorSetting.TREE_IMPOSTOR_BAKE_MAP_ALBEDO ? albedo : normal;

        TreeImpostorAtlasUtility.copyFrame(readback, atlas, frame % frames, frame / frames);
        retrievedViews++;
    }

    // Compression \\

    // Every view in: the shared tree's meshes let go and the atlases compressed and written on the worker
    private void beginCompression() {

        treeManager.disposeTreeMeshes(barkMeshes);
        treeManager.disposeTreeMeshes(leafMeshes);

        String bakedName = treeName;
        TreeImpostorStruct frame = new TreeImpostorStruct(source, height, centerY, radius, null, null);

        compressing = true;
        executeAsync(threadHandle, () -> compress(bakedName, frame));
    }

    // Worker — both maps dilated and compressed, the impostor written and handed over
    private void compress(String bakedName, TreeImpostorStruct frame) {

        try {

            TreeImpostorAtlasUtility.dilate(albedo, normal, covered, coveredScratch);

            TreeImpostorStruct impostor = new TreeImpostorStruct(
                    frame.getSource(), frame.getHeight(), frame.getCenterY(), frame.getRadius(),
                    TreeImpostorAtlasUtility.compress(albedo, albedo, true, compressionScratch),
                    TreeImpostorAtlasUtility.compress(normal, albedo, false, compressionScratch));

            TreeImpostorArpgUtility.tryWrite(bakedName, impostor);
            treeManager.applyImpostor(bakedName, impostor);
        } finally {
            compressing = false;
        }
    }

    // The bake ended, the branch free for the next species; every frame of the atlases is drawn again by the next
    private void finish() {
        this.treeName = null;
    }
}
