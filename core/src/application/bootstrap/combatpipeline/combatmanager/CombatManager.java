package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.combatpipeline.combat.BlowOutcome;
import application.bootstrap.combatpipeline.combat.BlowStruct;
import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import application.bootstrap.combatpipeline.weaponmanager.WeaponManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.physicspipeline.util.RayBoxUtility;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CombatManager extends ManagerPackage {

    /*
     * Entity-agnostic combat. Every spawned entity is a combatant, and each
     * frame CombatManager advances every combatant's action — none while the
     * game is paused — and resolves it the moment its effect lands: AttackBranch lands a weapon's attack
     * through StrikeBranch, and ThrowBranch lets one held item fly. Everything
     * fights as a weapon: a held item by the weapon it names, or the default
     * item weapon when it names none, and empty hands by the entity's own;
     * a held weapon not made for the entity's rig falls back to the entity's
     * own as well, and resolveWeapon() is the one place that decides which.
     * control() reads an entity's input each frame it is driven, starting the
     * attack its weapon makes from the side the input asks for and raising a
     * guard toward the side it holds it, which GuardBranch swings from side
     * to side for as long as it stays up; a side the weapon lacks plays the
     * one it falls back to. A guard catches only a blow from the side it is
     * held toward — the attack's mirror — and turns the attacker back into
     * recoil. Every attack is quickened by the entity's attack speed; any
     * item can be thrown from a raised aim. gesture() and control() are the
     * only ways an action begins, and only while the hands are free. Every
     * blow, swung or thrown, is a BlowStruct its deliverer fills and lands
     * through land() on whatever it met first — an entity through
     * DamageBranch, a tree through TreeManager, a block through
     * PlacementManager — so mining and felling are blows like any other,
     * telling only for the right tool; damage() is the path for anything
     * that hurts without a blow. resolveDriveSpeed() is the one rule for how
     * fast an arm drives a mass, the swing and the throw alike.
     * findTarget() is the one place that decides which entity a ray meets,
     * and getAttackReach() the one measure of how far a blow reaches. The
     * player passes mouse input and an NPC the input its mind writes; the
     * code path is the same.
     */

    // Internal
    private ClockManager clockManager;
    private WeaponManager weaponManager;
    private StatManager statManager;
    private PlacementManager placementManager;
    private TreeManager treeManager;

    // Branches
    private AttackBranch attackBranch;
    private GuardBranch guardBranch;
    private ThrowBranch throwBranch;
    private DamageBranch damageBranch;

    // Combatants
    private ObjectArrayList<EntityInstance> combatants;

    // Weapons
    private short defaultItemWeaponID;

    // Targeting
    private float targetDistance;

    // Base \\

    @Override
    protected void create() {

        // Branches
        create(StrikeBranch.class);
        this.attackBranch = create(AttackBranch.class);
        this.guardBranch = create(GuardBranch.class);
        this.throwBranch = create(ThrowBranch.class);
        this.damageBranch = create(DamageBranch.class);

        // Combatants
        this.combatants = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.weaponManager = get(WeaponManager.class);
        this.statManager = get(StatManager.class);
        this.placementManager = get(PlacementManager.class);
        this.treeManager = get(TreeManager.class);
    }

    @Override
    protected void awake() {
        this.defaultItemWeaponID = weaponManager.getWeaponIDFromWeaponName(EngineSetting.DEFAULT_ITEM_WEAPON);
    }

    // Update \\

    @Override
    protected void update() {

        if (clockManager.isPaused())
            return;

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

    public boolean isCombatant(EntityInstance entity) {
        return combatants.contains(entity);
    }

    public ObjectArrayList<EntityInstance> getCombatants() {
        return combatants;
    }

    // Actions \\

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

    // Weapons \\

    // The weapon an entity fights with — what it wields, else its own
    short resolveWeapon(EntityInstance entity) {
        return isWielding(entity)
                ? resolveItemWeapon(entity.getInventoryHandle().getMainHand())
                : entity.getEntityData().getWeaponID();
    }

    // The item an entity's blow is struck with — its main hand's, null when it fights with its own weapon
    ItemInstance resolveWielded(EntityInstance entity) {
        return isWielding(entity) ? entity.getInventoryHandle().getMainHand() : null;
    }

    // True while the main hand holds something whose weapon is made for the entity's rig
    private boolean isWielding(EntityInstance entity) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        return held != null && weaponManager.getWeaponHandleFromWeaponID(resolveItemWeapon(held))
                .hasRig(entity.getEntityData().getRigHandle());
    }

    // The weapon an item names, the default item weapon when it names none
    private short resolveItemWeapon(ItemInstance item) {

        short weaponID = item.getItemDefinitionHandle().getWeaponID();

        return weaponID != EngineSetting.WEAPON_NONE ? weaponID : defaultItemWeaponID;
    }

    // The mass an entity's blow drives in — the weight of what it wields, else its own weapon's
    float resolveBlowMass(EntityInstance entity) {

        ItemInstance wielded = resolveWielded(entity);

        return wielded != null
                ? wielded.getTotalWeight()
                : weaponManager.getWeaponHandleFromWeaponID(resolveWeapon(entity)).getMass();
    }

    // How fast an arm drives a mass by an impulse at a power — the swing and the throw alike, slower the heavier it is
    float resolveDriveSpeed(float mass, float impulse, float power) {
        return impulse * Math.max(0f, power) / (mass + EngineSetting.ARM_MASS);
    }

    // The item a guard is raised with — the off hand's, else the main hand's, null when it guards bare-handed
    ItemInstance resolveGuardItem(EntityInstance entity) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        return inventoryHandle.hasOffHand() ? inventoryHandle.getOffHand() : inventoryHandle.getMainHand();
    }

    // How much faster than its plain pace an entity attacks
    float resolveAttackSpeed(EntityInstance entity) {
        return Math.max(EngineSetting.DIVISION_EPSILON, entity.getStat(statManager.getAttackSpeedStatID()));
    }

    // How far from its eye an entity's blow from a side reaches — the attack its weapon makes when asked for that side
    public float getAttackReach(EntityInstance entity, CombatDirection side) {

        WeaponHandle weaponHandle = weaponManager.getWeaponHandleFromWeaponID(resolveWeapon(entity));

        return resolvePlainReach(entity) * weaponHandle.getAttack(weaponHandle.resolveAttackSide(side)).getReach();
    }

    // How far from its eye an entity's plain strike reaches
    float resolvePlainReach(EntityInstance entity) {
        return entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;
    }

    // State \\

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

    // Landing \\

    // A blow landing on an entity — one from a side meets only a guard held toward its mirror
    public void land(BlowStruct blow, EntityInstance target) {
        damageBranch.apply(
                blow.getSource(),
                target,
                blow.getDamage(),
                blow.getDamageTypeID(),
                blow.getDirection(),
                blow.getSide());
    }

    // A blow landing on the tree a cast met
    public BlowOutcome land(BlowStruct blow, TreeCastStruct cast) {
        return treeManager.strikeTree(blow, cast);
    }

    // A blow landing on the block a cast met
    public BlowOutcome land(BlowStruct blow, BlockCastStruct cast) {
        return placementManager.strikeBlock(blow, cast);
    }

    // Damage \\

    // Anything that hurts without a blow, by a source or by nothing at all — any raised guard facing it catches its
    // share when its type can be guarded
    public void damage(
            EntityInstance source,
            EntityInstance target,
            float amount,
            short damageTypeID,
            Vector3 direction) {
        damageBranch.apply(source, target, amount, damageTypeID, direction, null);
    }
}
