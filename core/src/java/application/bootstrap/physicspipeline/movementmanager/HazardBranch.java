package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.progressionpipeline.damagetypemanager.DamageTypeManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class HazardBranch extends BranchPackage {

    /*
     * Hurts an entity for what its movement just led it into, through the one
     * damage path every blow takes, each hurt a share of its health with no
     * source: a landing on anything but water, by how fast it fell against
     * the speed of a fall from the safe and the lethal heights, read from the
     * landing MovementSoundBranch is about to hear; and a breath held while
     * the water closes over its eyes or its head lies buried in a solid
     * block, which once spent drowns or smothers it a share each tick. Breath
     * comes back once the head is clear. Every entity moves through here,
     * players and NPCs alike.
     */

    // Internal
    private CombatManager combatManager;
    private DamageTypeManager damageTypeManager;
    private StatManager statManager;
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Cached Vectors
    private Vector3 still;

    // Internal \\

    @Override
    protected void create() {

        // Cached Vectors
        this.still = new Vector3();
    }

    @Override
    protected void get() {
        this.combatManager = get(CombatManager.class);
        this.damageTypeManager = get(DamageTypeManager.class);
        this.statManager = get(StatManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Update \\

    void update(
            EntityInstance entity,
            boolean inWater,
            boolean swimming,
            boolean climbing,
            boolean eyeSubmerged) {

        EntityStateHandle state = entity.getEntityStateHandle();
        boolean airborne = !state.isGrounded() && !swimming && !climbing;

        if (!airborne && state.isHeardAirborne() && !inWater)
            land(entity, state.getHeardFallSpeed());

        breathe(entity, state, eyeSubmerged);
    }

    // Fall \\

    // A landing hurts by the share of the way from the safe height's speed to the lethal height's its speed reached,
    // measured in the energy of the fall so the share grows with the height dropped
    private void land(EntityInstance entity, float speed) {

        float pull = 2f * EngineSetting.GRAVITY_FORCE * entity.getWorldHandle().getGravityMultiplier();
        float safe = pull * EngineSetting.FALL_SAFE_BLOCKS;
        float lethal = pull * EngineSetting.FALL_LETHAL_BLOCKS;
        float energy = speed * speed;

        if (energy <= safe)
            return;

        hurt(entity, (energy - safe) / (lethal - safe), damageTypeManager.getFallDamageTypeID());
    }

    // Breath \\

    private void breathe(EntityInstance entity, EntityStateHandle state, boolean eyeSubmerged) {

        float deltaTime = internal.getDeltaTime();
        boolean buried = !eyeSubmerged && isHeadBuried(entity);

        if (!eyeSubmerged && !buried) {
            state.setBreath(Math.min(1f, state.getBreath() + deltaTime / EngineSetting.BREATH_RECOVER_SECONDS));
            state.setBreathTickTime(0f);
            return;
        }

        float breath = Math.max(0f, state.getBreath() - deltaTime / EngineSetting.BREATH_HOLD_SECONDS);

        state.setBreath(breath);

        if (breath > 0f)
            return;

        float held = state.getBreathTickTime() + deltaTime;

        if (held >= EngineSetting.BREATH_TICK_SECONDS) {
            held -= EngineSetting.BREATH_TICK_SECONDS;
            hurt(
                    entity,
                    EngineSetting.BREATH_SHARE_PER_TICK,
                    eyeSubmerged
                            ? damageTypeManager.getDrowningDamageTypeID()
                            : damageTypeManager.getAsphyxiationDamageTypeID());
        }

        state.setBreathTickTime(held);
    }

    // Whether the sub-cell holding the entity's eyes is solid
    private boolean isHeadBuried(EntityInstance entity) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();

        return SubBlockSampleUtility.isSolid(
                worldStreamManager,
                blockManager,
                entity.getWorldPositionStruct().getChunkCoordinate(),
                SubBlockSampleUtility.toSub(position.x + size.x * 0.5f),
                SubBlockSampleUtility.toSub(position.y + entity.getEyeHeight()),
                SubBlockSampleUtility.toSub(position.z + size.z * 0.5f));
    }

    // Hurt \\

    // A share of the entity's health, given to the damage path as the amount that comes to it
    private void hurt(EntityInstance entity, float share, short damageTypeID) {
        combatManager.damage(
                null,
                entity,
                share * entity.getStat(statManager.getHealthStatID()),
                damageTypeID,
                still);
    }
}
