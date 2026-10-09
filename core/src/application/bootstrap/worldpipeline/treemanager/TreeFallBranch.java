package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.tree.TreeFallInstance;
import application.bootstrap.worldpipeline.tree.TreeFallState;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeLogStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TreeMeshUtility;
import application.bootstrap.worldpipeline.util.TreeRasterUtility;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class TreeFallBranch extends BranchPackage {

    /*
     * Owns every piece of tree falling free. fell() takes the piece a cut
     * freed and builds its geometry on a worker, exactly as a chunk would
     * draw it; until that is done the tree still stands whole on screen, and
     * only once the piece is ready is the stump redrawn and the piece let
     * fall, so the tree never blinks. A piece standing upright topples about
     * its cut, away from whoever cut it, its swing gathering speed as a rod
     * hinged at its foot does under gravity, until it lies down; any other
     * piece drops straight down. It rests a moment where it landed, then
     * breaks into its drops: logs for its wood, each stretch of wood the
     * thickest log it is thick enough for, so a trunk gives great logs and a
     * limb small ones, and the odd seed for its leaves, every drop thrown out
     * from where it lay.
     */

    // Internal
    private TreeRebuildBranch treeRebuildBranch;
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;
    private ChunkStreamManager chunkStreamManager;
    private ItemManager itemManager;
    private ProjectileManager projectileManager;
    private ThreadHandle threadHandle;

    // Falls
    private ObjectArrayList<TreeFallInstance> falls;

    // Settings
    private int chunkSize;
    private int resolution;

    // Scratch — main thread
    private float[] pointScratch;
    private float[] logLength;
    private Vector3 positionScratch;
    private Vector3 velocityScratch;
    private Vector3 spinScratch;
    private Quaternion orientationScratch;

    // Base \\

    @Override
    protected void create() {

        // Falls
        this.falls = new ObjectArrayList<>();

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Scratch
        this.pointScratch = new float[EngineSetting.AXIS_COUNT];
        this.logLength = new float[EngineSetting.TREE_MAX_LOG_KINDS];
        this.positionScratch = new Vector3();
        this.velocityScratch = new Vector3();
        this.spinScratch = new Vector3();
        this.orientationScratch = new Quaternion();
    }

    @Override
    protected void get() {
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.itemManager = get(ItemManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
    }

    // Fell \\

    // A piece freed by a cut set falling — pivot and heading in blocks from the centre of the tree's root
    void fell(
            TreeInstance tree,
            TreeShapeStruct piece,
            TreeShapeStruct before,
            EntityInstance feller,
            float pivotX,
            float pivotY,
            float pivotZ,
            float headingX,
            float headingZ,
            boolean toppling) {

        if (piece.isEmpty()) {
            treeRebuildBranch.rebuildTree(tree, before, tree.getShape());
            return;
        }

        TreeFallInstance fall = create(TreeFallInstance.class);
        fall.constructor(tree, piece, before, feller, pivotX, pivotY, pivotZ, headingX, headingZ, toppling);
        falls.add(fall);

        executeAsync(threadHandle, () -> buildGeometry(fall));
    }

    // Worker — the piece drawn whole, in blocks from the centre of its tree's root
    private void buildGeometry(TreeFallInstance fall) {

        TreeShapeStruct piece = fall.getPiece();
        SubVoxelGridStruct grid = new SubVoxelGridStruct();
        SubVoxelQuadListStruct quads = new SubVoxelQuadListStruct();
        ObjectArrayList<TreeHandle> partSpecies = new ObjectArrayList<>();
        FloatArrayList bark = new FloatArrayList();
        FloatArrayList leaves = new FloatArrayList();
        FloatArrayList hiders = new FloatArrayList();
        int rootX = (int) Math.ceil(-piece.getMinX()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int rootY = (int) Math.ceil(-piece.getMinY()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int rootZ = (int) Math.ceil(-piece.getMinZ()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int sizeX = (rootX + (int) Math.ceil(piece.getMaxX()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;
        int sizeY = (rootY + (int) Math.ceil(piece.getMaxY()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;
        int sizeZ = (rootZ + (int) Math.ceil(piece.getMaxZ()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;

        partSpecies.add(piece.getTreeHandle());

        if (!piece.getNodes().isEmpty()) {

            TreeRasterUtility.rasterize(piece, rootX, rootY, rootZ, 0, 0, 0, sizeX, sizeY, sizeZ, 0, grid);
            subVoxelManager.meshGrid(grid, null, quads);
            TreeMeshUtility.collectHiders(
                    piece,
                    (rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution, rootY * resolution,
                    (rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution, hiders);
            TreeMeshUtility.emitWood(
                    grid, quads, partSpecies, hiders,
                    -(rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS), -rootY,
                    -(rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS), bark);
        }

        TreeMeshUtility.emitLeaves(
                piece, 0f, 0f, 0f,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, leaves);

        fall.publishGeometry(bark, leaves);
    }

    // Update \\

    void update(float deltaTime) {

        for (int i = falls.size() - 1; i >= 0; i--) {

            TreeFallInstance fall = falls.get(i);

            switch (fall.getState()) {
                case BUILDING -> beginFall(fall);
                case FALLING -> advanceFall(fall, deltaTime);
                case RESTING -> rest(fall, deltaTime);
                case DONE -> falls.remove(i);
            }
        }
    }

    // Once the piece's geometry is ready, it is uploaded, the stump redrawn, and the piece let go
    private void beginFall(TreeFallInstance fall) {

        if (!fall.isBuilt())
            return;

        uploadMeshes(fall.getBuiltBark(), fall.getBarkMeshes());
        uploadMeshes(fall.getBuiltLeaves(), fall.getLeafMeshes());
        treeRebuildBranch.rebuildTree(fall.getTree(), fall.getBefore(), fall.getTree().getShape());

        fall.setAngle(EngineSetting.TREE_FALL_START_RADIANS, 0f);
        fall.setState(TreeFallState.FALLING);
    }

    // A rod hinged at its foot swings faster the further it leans, by three halves of gravity over its length
    private void advanceFall(TreeFallInstance fall, float deltaTime) {

        if (!fall.isToppling()) {

            float dropSpeed = fall.getDropSpeed() + EngineSetting.GRAVITY_FORCE * deltaTime;

            fall.setDrop(fall.getDrop() + dropSpeed * deltaTime, dropSpeed);

            if (fall.getDrop() >= EngineSetting.TREE_FALL_DROP_BLOCKS)
                settle(fall);

            return;
        }

        float length = Math.max(
                fall.getPiece().getMaxY() - fall.getPivotY(),
                EngineSetting.TREE_FALL_MIN_LENGTH_BLOCKS);
        float acceleration = EngineSetting.TREE_FALL_HINGE_ACCELERATION * EngineSetting.GRAVITY_FORCE / length
                * (float) Math.sin(fall.getAngle());
        float angularSpeed = fall.getAngularSpeed() + acceleration * deltaTime;
        float angle = Math.min(fall.getAngle() + angularSpeed * deltaTime, EngineSetting.TREE_FALL_REST_RADIANS);

        fall.setAngle(angle, angularSpeed);

        if (angle >= EngineSetting.TREE_FALL_REST_RADIANS)
            settle(fall);
    }

    private void settle(TreeFallInstance fall) {
        fall.setRestSeconds(EngineSetting.TREE_FALL_REST_SECONDS);
        fall.setState(TreeFallState.RESTING);
    }

    private void rest(TreeFallInstance fall, float deltaTime) {

        fall.setRestSeconds(fall.getRestSeconds() - deltaTime);

        if (fall.getRestSeconds() > 0f)
            return;

        shatter(fall);
        disposeMeshes(fall.getBarkMeshes());
        disposeMeshes(fall.getLeafMeshes());
        fall.setState(TreeFallState.DONE);
    }

    // Drops \\

    // The piece broken into its logs and seeds, every one thrown out from where it lies
    private void shatter(TreeFallInstance fall) {

        TreeShapeStruct piece = fall.getPiece();
        TreeHandle treeHandle = piece.getTreeHandle();
        ObjectArrayList<TreeLogStruct> logs = treeHandle.getWood().getLogs();
        int kinds = Math.min(logs.size(), EngineSetting.TREE_MAX_LOG_KINDS);
        int dropped = 0;

        for (int kind = 0; kind < kinds; kind++)
            logLength[kind] = 0f;

        for (int segment = 0; segment < piece.getSegmentCount() && dropped < EngineSetting.TREE_MAX_LOG_DROPS;
                segment++) {

            int kind = resolveLogKind(logs, kinds, piece, segment);

            if (kind == EngineSetting.INDEX_NOT_FOUND)
                continue;

            TreeLogStruct log = logs.get(kind);

            logLength[kind] += segmentLength(piece, segment);

            while (logLength[kind] >= log.getLengthBlocks() && dropped < EngineSetting.TREE_MAX_LOG_DROPS) {
                logLength[kind] -= log.getLengthBlocks();
                throwFromPiece(fall, log.getItemName(), segmentMiddle(piece, segment, 0),
                        segmentMiddle(piece, segment, 1), segmentMiddle(piece, segment, 2), dropped++);
            }
        }

        if (!treeHandle.getGrowth().hasSeed())
            return;

        int seeds = 0;

        for (int leaf = 0; leaf < piece.getLeafCount() && seeds < EngineSetting.TREE_MAX_SEED_DROPS; leaf++) {

            float roll = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                    piece.getSeed() ^ EngineSetting.TREE_SEED_DROP_SALT, piece.getLeafSource(leaf), 0));

            if (roll >= treeHandle.getGrowth().getSeedChance())
                continue;

            throwFromPiece(fall, treeHandle.getGrowth().getSeedItemName(),
                    piece.getLeafX(leaf), piece.getLeafY(leaf), piece.getLeafZ(leaf), dropped + seeds++);
        }
    }

    // The thickest log a stretch of wood is thick enough for, INDEX_NOT_FOUND for twigs thinner than every log
    private int resolveLogKind(ObjectArrayList<TreeLogStruct> logs, int kinds, TreeShapeStruct piece, int segment) {

        float radius = (piece.getStartRadius(segment) + piece.getEndRadius(segment)) * 0.5f * resolution;

        for (int kind = 0; kind < kinds; kind++)
            if (radius >= logs.get(kind).getMinRadiusSubVoxels())
                return kind;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private float segmentLength(TreeShapeStruct piece, int segment) {

        float dx = piece.getEndX(segment) - piece.getStartX(segment);
        float dy = piece.getEndY(segment) - piece.getStartY(segment);
        float dz = piece.getEndZ(segment) - piece.getStartZ(segment);

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float segmentMiddle(TreeShapeStruct piece, int segment, int axis) {

        if (axis == EngineSetting.AXIS_X)
            return (piece.getStartX(segment) + piece.getEndX(segment)) * 0.5f;

        if (axis == EngineSetting.AXIS_Y)
            return (piece.getStartY(segment) + piece.getEndY(segment)) * 0.5f;

        return (piece.getStartZ(segment) + piece.getEndZ(segment)) * 0.5f;
    }

    // A point of the piece where it lies now, in blocks from the centre of its tree's root, thrown out as a drop
    private void throwFromPiece(TreeFallInstance fall, String itemName, float x, float y, float z, int index) {

        fall.transformPoint(x, y, z, 0f, 0f, 0f, pointScratch);
        throwDrop(fall.getTree(), fall.getFeller(), itemName, pointScratch[0], pointScratch[1], pointScratch[2], index);
    }

    // One item thrown up and out from a point in blocks from the centre of a tree's root, in the frame of the
    // root's chunk; whoever brought it down is the one it is thrown for, and is never struck by it
    void throwDrop(TreeInstance tree, EntityInstance thrower, String itemName, float x, float y, float z, int index) {

        if (thrower == null)
            return;

        int chunkX = (int) Math.floorDiv(tree.getAnchorX(), chunkSize);
        int chunkZ = (int) Math.floorDiv(tree.getAnchorZ(), chunkSize);
        float rootX = tree.getAnchorX() - (long) chunkX * chunkSize + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float rootZ = tree.getAnchorZ() - (long) chunkZ * chunkSize + EngineSetting.TREE_ROOT_CENTER_BLOCKS;
        float angle = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                tree.getSeed() ^ EngineSetting.TREE_DROP_ANGLE_SALT, index, 0)) * (float) (Math.PI * 2.0);

        positionScratch.set(rootX + x, tree.getBaseY() + y + EngineSetting.TREE_DROP_LIFT_BLOCKS, rootZ + z);
        velocityScratch.set(
                (float) Math.cos(angle) * EngineSetting.TREE_DROP_SPREAD_SPEED,
                EngineSetting.TREE_DROP_LIFT_SPEED,
                (float) Math.sin(angle) * EngineSetting.TREE_DROP_SPREAD_SPEED);
        spinScratch.set((float) Math.sin(angle), 0f, (float) Math.cos(angle));
        orientationScratch.set(1f, 0f, 0f, 0f);

        projectileManager.launch(
                thrower,
                itemManager.createItem(itemName),
                Coordinate2Long.pack(chunkX, chunkZ),
                positionScratch,
                velocityScratch,
                spinScratch,
                EngineSetting.TREE_DROP_SPIN_RATE,
                orientationScratch);
    }

    // Meshes \\

    // Geometry in the chunk vertex layout uploaded as meshes, each cut before the mesh vertex limit
    private void uploadMeshes(FloatArrayList vertices, ObjectArrayList<MeshInstance> out) {

        int stride = EngineSetting.CHUNK_VERTEX_FLOAT_COUNT;
        int quadFloats = stride * EngineSetting.QUAD_VERTEX_COUNT;
        int quadsPerMesh = EngineSetting.MESH_VERT_LIMIT / EngineSetting.QUAD_VERTEX_COUNT;
        int quadCount = vertices.size() / quadFloats;

        for (int first = 0; first < quadCount; first += quadsPerMesh) {

            int count = Math.min(quadsPerMesh, quadCount - first);
            FloatArrayList meshVertices = new FloatArrayList(count * quadFloats);
            ShortArrayList indices = new ShortArrayList(count * EngineSetting.QUAD_INDEX_COUNT);

            meshVertices.addElements(0, vertices.elements(), first * quadFloats, count * quadFloats);

            for (int quad = 0; quad < count; quad++) {

                int base = quad * EngineSetting.QUAD_VERTEX_COUNT;

                indices.add((short) base);
                indices.add((short) (base + 1));
                indices.add((short) (base + 2));
                indices.add((short) base);
                indices.add((short) (base + 2));
                indices.add((short) (base + 3));
            }

            out.add(meshManager.createMesh(chunkStreamManager.getChunkVAO(), meshVertices, indices));
        }
    }

    private void disposeMeshes(ObjectArrayList<MeshInstance> meshes) {

        for (int i = 0; i < meshes.size(); i++)
            meshManager.removeMesh(meshes.get(i));

        meshes.clear();
    }

    // Accessible \\

    ObjectArrayList<TreeFallInstance> getFalls() {
        return falls;
    }
}
