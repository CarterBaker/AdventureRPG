package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.movesetmanager.MovesetManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.tooltype.ToolSwing;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.physicspipeline.util.RayBoxUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CombatManager extends ManagerPackage {

    /*
     * Entity-agnostic combat. Every spawned entity is a combatant, and each
     * frame CombatManager advances every combatant's action and resolves it
     * the moment its effect lands: SwingBranch strikes with whatever is held,
     * AttackBranch lands a weapon's attack, and ThrowBranch lets one held
     * item fly. A weapon fights by the moveset its item names, and bare hands
     * by the unarmed one: control() reads an entity's input each frame it is
     * driven, starting the attack its moveset makes from the side the input
     * asks for and raising a guard toward the side it holds it, which
     * GuardBranch swings from side to side for as long as it stays up. A
     * guard catches only a blow from the side it is held toward — the
     * attack's mirror — and turns the attacker back into recoil. An item that
     * is no weapon swings plainly through swing() instead, overhead or level
     * as its tool type swings it, and a heavier one swings slower; any item
     * can be thrown from a raised aim. swing(), gesture() and control() are
     * the only ways an action begins, and only while the hands are free.
     * findTarget() is the one place that decides which entity a ray meets,
     * damage() and strike() the only paths health is lost, through
     * DamageBranch. The player passes mouse input and an enemy would pass its
     * own; the code path is the same.
     */

    // Internal
    private ToolTypeManager toolTypeManager;
    private MovesetManager movesetManager;

    // Branches
    private SwingBranch swingBranch;
    private AttackBranch attackBranch;
    private GuardBranch guardBranch;
    private ThrowBranch throwBranch;
    private DamageBranch damageBranch;

    // Combatants
    private ObjectArrayList<EntityInstance> combatants;

    // Movesets
    private short unarmedMovesetID;

    // Targeting
    private float targetDistance;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.swingBranch = create(SwingBranch.class);
        this.attackBranch = create(AttackBranch.class);
        this.guardBranch = create(GuardBranch.class);
        this.throwBranch = create(ThrowBranch.class);
        this.damageBranch = create(DamageBranch.class);

        // Combatants
        this.combatants = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.toolTypeManager = get(ToolTypeManager.class);
        this.movesetManager = get(MovesetManager.class);
    }

    @Override
    protected void awake() {
        this.unarmedMovesetID = movesetManager.getMovesetIDFromMovesetName(EngineSetting.UNARMED_MOVESET);
    }

    // Update \\

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();

        for (int i = 0; i < combatants.size(); i++) {

            EntityInstance combatant = combatants.get(i);
            EntityActionHandle actionHandle = combatant.getEntityActionHandle();
            EntityAction action = actionHandle.getAction();

            if (actionHandle.advance(deltaTime))
                resolve(combatant, action);
        }
    }

    private void resolve(EntityInstance entity, EntityAction action) {

        switch (action) {
            case SWING, CHOP -> swingBranch.strike(entity);
            case ATTACK -> attackBranch.strike(entity);
            case THROW -> throwBranch.release(entity);
            default -> {
            }
        }
    }

    // Management \\

    public void addCombatant(EntityInstance entity) {

        if (!combatants.contains(entity))
            combatants.add(entity);
    }

    public void removeCombatant(EntityInstance entity) {
        combatants.remove(entity);
    }

    // Actions \\

    // A plain swing with an item that is no weapon — a weapon or bare hands attack through control() instead
    public boolean swing(EntityInstance entity) {

        if (!isReady(entity) || isArmed(entity))
            return false;

        float duration = Math.min(
                EngineSetting.SWING_MAX_SECONDS,
                EngineSetting.SWING_BASE_SECONDS + resolveHeldWeight(entity) * EngineSetting.SWING_SECONDS_PER_WEIGHT);

        entity.getEntityActionHandle().begin(resolveSwingAction(entity), duration, EngineSetting.SWING_IMPACT);
        return true;
    }

    // A tool its type swings level sweeps across from the side; anything else comes down overhead
    private EntityAction resolveSwingAction(EntityInstance entity) {

        short toolTypeID = entity.getInventoryHandle().getMainHand().getItemDefinitionHandle().getToolTypeID();

        if (toolTypeID == EngineSetting.TOOL_NONE)
            return EntityAction.SWING;

        return toolTypeManager.getToolTypeHandleFromToolTypeID(toolTypeID).getSwing() == ToolSwing.LEVEL
                ? EntityAction.CHOP
                : EntityAction.SWING;
    }

    // A short gesture that only animates — setting something down or taking it up
    public boolean gesture(EntityInstance entity, EntityAction action) {

        float duration = switch (action) {
            case PLACE -> EngineSetting.PLACE_GESTURE_SECONDS;
            case PICK_UP -> EngineSetting.PICK_UP_GESTURE_SECONDS;
            default -> throwException("Action " + action + " is not a gesture.");
        };

        if (!isReady(entity))
            return false;

        entity.getEntityActionHandle().begin(action, duration, EngineSetting.GESTURE_IMPACT);
        return true;
    }

    private boolean isReady(EntityInstance entity) {
        return !entity.getEntityActionHandle().isActing();
    }

    // Control \\

    public void control(EntityInstance entity) {

        EntityInputHandle input = entity.getEntityInputHandle();
        EntityActionHandle actionHandle = entity.getEntityActionHandle();

        switch (actionHandle.getAction()) {
            case NONE -> raiseStance(entity, input, actionHandle);
            case AIM -> controlAim(entity, input, actionHandle);
            case BLOCK -> guardBranch.control(entity, input);
            case ATTACK -> attackBranch.control(entity, input);
            case RECOIL, STAGGER -> attackBranch.queue(entity, input);
            default -> {
            }
        }
    }

    // Free hands raise a guard, start the attack asked for now or still waiting in the queue, or raise an aim
    private void raiseStance(EntityInstance entity, EntityInputHandle input, EntityActionHandle actionHandle) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (input.isBlockAction())
            guardBranch.raise(entity, input.getGuardDirection());
        else if (input.isAttackAction())
            attackBranch.attack(entity, input.getAttackDirection());
        else if (actionHandle.isQueued())
            attackBranch.attack(entity, actionHandle.getQueuedDirection());
        else if (input.isAimAction() && inventoryHandle.hasMainHand())
            actionHandle.hold(EntityAction.AIM, EngineSetting.AIM_RAISE_SECONDS);
    }

    // The throw lets fly; letting the aim go or moving off lowers it and leaves the key to sprint
    private void controlAim(EntityInstance entity, EntityInputHandle input, EntityActionHandle actionHandle) {

        if (!input.isSprint() || input.hasHorizontalInput() || !entity.getInventoryHandle().hasMainHand()) {
            actionHandle.release();
            return;
        }

        if (input.isThrowAction())
            actionHandle.begin(EntityAction.THROW, EngineSetting.THROW_SECONDS, EngineSetting.THROW_RELEASE);
    }

    // Movesets \\

    // The moveset an entity attacks with — its main hand's, bare hands' when that hand is empty, MOVESET_NONE when
    // it holds something that is no weapon
    short resolveAttackMoveset(EntityInstance entity) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        return held != null ? held.getItemDefinitionHandle().getMovesetID() : unarmedMovesetID;
    }

    // The moveset an entity guards with — its weapon's, else bare hands'
    short resolveGuardMoveset(EntityInstance entity) {

        short movesetID = resolveAttackMoveset(entity);

        return movesetID != EngineSetting.MOVESET_NONE ? movesetID : unarmedMovesetID;
    }

    // The item a guard is raised with — the off hand's, else the main hand's, null when it guards bare-handed
    ItemInstance resolveGuardItem(EntityInstance entity) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        return inventoryHandle.hasOffHand() ? inventoryHandle.getOffHand() : inventoryHandle.getMainHand();
    }

    float resolveHeldWeight(EntityInstance entity) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        return held != null ? held.getTotalWeight() : 0f;
    }

    // State \\

    // True while an entity attacks by a moveset — a weapon in hand, or nothing at all
    public boolean isArmed(EntityInstance entity) {
        return resolveAttackMoveset(entity) != EngineSetting.MOVESET_NONE;
    }

    // True while an entity is caught up in a fight — attacking, reeling, or holding a stance — so its hands are not
    // free for the world
    public boolean isEngaged(EntityInstance entity) {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();

        return actionHandle.isHolding() || switch (actionHandle.getAction()) {
            case ATTACK, RECOIL, STAGGER -> true;
            default -> false;
        };
    }

    // True while an entity is held to a walk — behind its guard, or thrown off balance
    public boolean isSlowed(EntityInstance entity) {
        return switch (entity.getEntityActionHandle().getAction()) {
            case BLOCK, RECOIL, STAGGER -> true;
            default -> false;
        };
    }

    public boolean isAttacking(EntityInstance entity) {
        return entity.getEntityActionHandle().getAction() == EntityAction.ATTACK;
    }

    public boolean isGuarding(EntityInstance entity) {
        return entity.getEntityActionHandle().getAction() == EntityAction.BLOCK;
    }

    // Targeting \\

    // The nearest entity other than the source whose box a ray enters within reach — null when it meets none
    public EntityInstance findTarget(
            EntityInstance source,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance) {

        EntityInstance nearest = null;
        float nearestDistance = maxDistance;

        for (int i = 0; i < combatants.size(); i++) {

            EntityInstance combatant = combatants.get(i);

            if (combatant == source || combatant.getWorldHandle() != source.getWorldHandle())
                continue;

            float distance = intersectEntity(combatant, chunkCoordinate, origin, direction);

            if (distance >= nearestDistance)
                continue;

            nearest = combatant;
            nearestDistance = distance;
        }

        targetDistance = nearestDistance;

        return nearest;
    }

    private float intersectEntity(
            EntityInstance entity,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                entity.getWorldHandle(),
                chunkCoordinate,
                entity.getWorldPositionStruct().getChunkCoordinate());
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();

        float minX = Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x;
        float minY = position.y;
        float minZ = Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z;

        return RayBoxUtility.intersect(
                origin,
                direction,
                minX, minY, minZ,
                minX + size.x, minY + size.y, minZ + size.z);
    }

    // How far along its ray the last findTarget() met its entity
    public float getTargetDistance() {
        return targetDistance;
    }

    // Damage \\

    // A blow from no side in particular — any raised guard facing it catches its share
    public void damage(EntityInstance target, float amount, Vector3 direction) {
        damageBranch.apply(target, amount, direction);
    }

    // A weapon's blow from a side of the source — only a guard held toward its mirror catches it
    void strike(
            EntityInstance source,
            EntityInstance target,
            float amount,
            Vector3 direction,
            CombatDirection side) {
        damageBranch.strike(source, target, amount, direction, side);
    }
}
