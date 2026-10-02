package application.bootstrap.combatpipeline.projectilemanager;

import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class ProjectileImpactBranch extends BranchPackage {

    /*
     * Lets a projectile break the block it strikes. The momentum it drives
     * into the face must reach the block's durability times the momentum
     * each point of durability takes; an unbreakable block never gives. A
     * block in one piece breaks whole and throws its eight pieces, a
     * subdivided one loses only the sub-block struck and throws that one, and
     * every piece is launched from where it stood through ProjectileManager,
     * carrying a share of the projectile's speed and a scatter of its own, to
     * bounce, land and be built back in as a sub-block like any thrown piece.
     * The projectile keeps the momentum the block did not take and flies on.
     */

    // Internal
    private ProjectileManager projectileManager;
    private BlockManager blockManager;
    private BlockPlacementSystem blockPlacementSystem;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;

    // Block IDs
    private short airBlockID;

    // Scratch
    private Vector3 innerScratch;
    private Vector3 pieceScratch;
    private Vector3 velocityScratch;
    private Vector3 spinAxisScratch;
    private Quaternion orientationScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.innerScratch = new Vector3();
        this.pieceScratch = new Vector3();
        this.velocityScratch = new Vector3();
        this.spinAxisScratch = new Vector3();
        this.orientationScratch = new Quaternion();
    }

    @Override
    protected void get() {

        // Internal
        this.projectileManager = get(ProjectileManager.class);
        this.blockManager = get(BlockManager.class);
        this.blockPlacementSystem = get(BlockPlacementSystem.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
    }

    @Override
    protected void awake() {

        // Block IDs
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Impact \\

    // Breaks the block the cast met when the projectile drives enough momentum into it, its pieces thrown from where
    // they stood and the projectile slowed by what the break took — true when the block broke
    boolean tryBreak(ProjectileInstance projectile, BlockCastStruct castStruct, Vector3 hitPoint) {

        BlockHandle block = castStruct.getBlock();
        Direction3Vector face = castStruct.getHitFace();
        Vector3 velocity = projectile.getVelocity();
        float impactSpeed = -(velocity.x * face.x + velocity.y * face.y + velocity.z * face.z);

        if (block == null || block.isUnbreakable() || impactSpeed <= 0f)
            return false;

        float momentum = projectile.getItemInstance().getTotalWeight() * impactSpeed;
        float required = block.getDurability() * EngineSetting.PROJECTILE_BREAK_MOMENTUM_PER_DURABILITY;

        if (momentum < required)
            return false;

        boolean subdivided = SubBlockUtility.isSubdivided(blockPlacementSystem.getSubBlockMask(castStruct));
        boolean broken = subdivided
                ? blockPlacementSystem.removeSubBlock(castStruct)
                : blockPlacementSystem.replaceBlock(castStruct, airBlockID);

        if (!broken)
            return false;

        innerScratch.set(hitPoint).subtract(
                face.x * SubBlockUtility.SIZE * 0.5f,
                face.y * SubBlockUtility.SIZE * 0.5f,
                face.z * SubBlockUtility.SIZE * 0.5f);

        if (subdivided)
            throwSubBlockPiece(projectile, block);
        else
            throwBlockPieces(projectile, block);

        velocity.multiply(1f - required / momentum);

        return true;
    }

    // Pieces \\

    // The one sub-block struck, thrown from its own centre
    private void throwSubBlockPiece(ProjectileInstance projectile, BlockHandle block) {

        float size = SubBlockUtility.SIZE;

        pieceScratch.set(
                ((float) Math.floor(innerScratch.x / size) + 0.5f) * size,
                ((float) Math.floor(innerScratch.y / size) + 0.5f) * size,
                ((float) Math.floor(innerScratch.z / size) + 0.5f) * size);

        throwPiece(projectile, block, pieceScratch);
    }

    // Every sub-block of the whole block, each thrown from its own centre
    private void throwBlockPieces(ProjectileInstance projectile, BlockHandle block) {

        float size = SubBlockUtility.SIZE;
        float blockX = (float) Math.floor(innerScratch.x);
        float blockY = (float) Math.floor(innerScratch.y);
        float blockZ = (float) Math.floor(innerScratch.z);

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            pieceScratch.set(
                    blockX + (SubBlockUtility.getOctantX(octant) + 0.5f) * size,
                    blockY + (SubBlockUtility.getOctantY(octant) + 0.5f) * size,
                    blockZ + (SubBlockUtility.getOctantZ(octant) + 0.5f) * size);

            throwPiece(projectile, block, pieceScratch);
        }
    }

    // One piece of the block launched from a point in the projectile's chunk, with a share of its speed and a
    // scatter and tumble of its own
    private void throwPiece(ProjectileInstance projectile, BlockHandle block, Vector3 position) {

        if (!block.hasPiece())
            return;

        ItemDefinitionHandle piece = itemDefinitionManager.getBlockPieceHandle(block.getBlockID());
        Vector3 velocity = projectile.getVelocity();
        float share = EngineSetting.PROJECTILE_DEBRIS_SPEED_SHARE;
        float scatter = EngineSetting.PROJECTILE_DEBRIS_SCATTER;

        velocityScratch.set(
                velocity.x * share + resolveScatter(scatter),
                velocity.y * share + Math.abs(resolveScatter(scatter)),
                velocity.z * share + resolveScatter(scatter));
        spinAxisScratch.set(resolveScatter(1f), resolveScatter(1f), resolveScatter(1f));

        if (spinAxisScratch.lengthSquared() <= EngineSetting.DIVISION_EPSILON)
            spinAxisScratch.set(1f, 0f, 0f);

        spinAxisScratch.normalize();
        orientationScratch.set(projectile.getOrientation());

        projectileManager.launch(
                projectile.getThrower(),
                itemManager.createItem(piece),
                projectile.getWorldPositionStruct().getChunkCoordinate(),
                position,
                velocityScratch,
                spinAxisScratch,
                EngineSetting.PROJECTILE_DEBRIS_SPIN_RATE,
                orientationScratch);
    }

    private float resolveScatter(float scatter) {
        return (float) (Math.random() * 2.0 - 1.0) * scatter;
    }
}
