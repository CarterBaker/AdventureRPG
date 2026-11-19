package application.bootstrap.entitypipeline.entity;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.entitypipeline.progression.ConductHandle;
import application.bootstrap.entitypipeline.progression.DispositionHandle;
import application.bootstrap.entitypipeline.progression.ProficiencyHandle;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.shaderpipeline.texture.TextureTileSetStruct;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class EntityInstance extends InstancePackage {

    /*
     * Runtime entity spawned by EntityManager, holding its template and all
     * per-instance state. setWeight() keeps weight and body build in step,
     * updateAnimation() is the one path that advances its animation tree,
     * playing whatever action its hands are in over its movement — a weapon's
     * own clip where the action carries one — getStat() reads a stat's total,
     * resolved through ProgressionManager only once something it is built from
     * has changed, getEyePosition() is the one place its eye — where it aims
     * from — is placed, and getModelPoint() sets a point of its body in the
     * world just where it is drawn; before each update its springs are told
     * how it stands turned and sized and how far it travelled, across chunks
     * as well, so its hair keeps its momentum. Its level, attributes, trades, skills, conduct
     * and professions live in their own handles, the same for a player as for
     * any NPC, and it carries one of the genders its race allows, wearing the
     * model that gender requires and moving by its animation tree. Its race is
     * its template's until setRaceData() makes it another, as the creator
     * does for a player choosing among the playable races.
     */

    // Internal
    private ProgressionManager progressionManager;
    private WindManager windManager;
    private EntityData entityData;
    private WorldHandle worldHandle;
    private BehaviorHandle behaviorHandle;

    // State
    private EntityStateHandle entityStateHandle;
    private EntityActionHandle entityActionHandle;
    private StatisticsHandle statisticsHandle;
    private InventoryHandle inventoryHandle;

    // Progression
    private ProgressionHandle progressionHandle;
    private ProficiencyHandle proficiencyHandle;
    private ConductHandle conductHandle;
    private CareerHandle careerHandle;
    private DispositionHandle dispositionHandle;

    // Input
    private EntityInputHandle EntityInputHandle;

    // Animation — on the animation tree of the gender worn
    private AnimationStateHandle animationStateHandle;

    // Appearance — created only if the race has one
    private AppearanceHandle appearanceHandle;

    // Physics
    private WorldPositionStruct worldPositionStruct;

    // Springs — where the body stood at the last update, and how it stands turned and sized now
    private long springChunkCoordinate;
    private Vector3 springPosition;
    private Matrix4 springFrame;
    private Vector3 springOrigin;
    private Vector3 springTurn;
    private Vector3 springScale;
    private Matrix4 springScratchA;
    private Matrix4 springScratchB;

    // Runtime
    private Vector3 size;
    private float weight;
    private RaceData raceData;
    private short genderID;
    private RaceGenderStruct raceGender;

    // Texture — the texture user holding the tiles the entity draws while it is in the world, and those tiles
    private int textureUser;
    private TextureTileSetStruct textureTiles;

    // Internal \\

    @Override
    protected void create() {

        // State
        this.entityStateHandle = create(EntityStateHandle.class);
        this.entityActionHandle = create(EntityActionHandle.class);
        this.statisticsHandle = create(StatisticsHandle.class);
        this.inventoryHandle = create(InventoryHandle.class);

        // Progression
        this.progressionHandle = create(ProgressionHandle.class);
        this.proficiencyHandle = create(ProficiencyHandle.class);
        this.conductHandle = create(ConductHandle.class);
        this.careerHandle = create(CareerHandle.class);
        this.dispositionHandle = create(DispositionHandle.class);

        // Input
        this.EntityInputHandle = create(EntityInputHandle.class);

        // Physics
        this.worldPositionStruct = new WorldPositionStruct();

        // Springs
        this.springPosition = new Vector3();
        this.springFrame = new Matrix4();
        this.springOrigin = new Vector3();
        this.springTurn = new Vector3();
        this.springScale = new Vector3();
        this.springScratchA = new Matrix4();
        this.springScratchB = new Matrix4();

        // Texture
        this.textureUser = EngineSetting.INDEX_NOT_FOUND;
        this.textureTiles = new TextureTileSetStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.progressionManager = get(ProgressionManager.class);
        this.windManager = get(WindManager.class);
    }

    // Constructor \\

    public void constructor(
            EntityData entityData,
            WorldHandle worldHandle,
            BehaviorHandle behaviorHandle,
            Vector3 position,
            long chunkCoordinate,
            Vector3 size,
            float weight) {

        // Internal
        this.entityData = entityData;
        this.worldHandle = worldHandle;
        this.behaviorHandle = behaviorHandle;

        // Physics
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);

        // Springs
        this.springChunkCoordinate = chunkCoordinate;
        this.springPosition.set(position);

        // Runtime
        this.size = size;
        this.weight = weight;

        // Animation
        this.animationStateHandle = create(AnimationStateHandle.class);

        applyRace(entityData.getRaceData());
    }

    // Race \\

    // Makes the entity of another race, its size carried over in proportion to the two races' model heights, in the
    // race's default gender and look
    public void setRaceData(RaceData raceData) {

        if (raceData == this.raceData)
            return;

        size.multiply(raceData.getModelHeight() / this.raceData.getModelHeight());
        applyRace(raceData);
    }

    // The one way an entity takes on a race: its default gender's model and tree, and an appearance when it has one
    private void applyRace(RaceData raceData) {

        this.raceData = raceData;
        this.genderID = raceData.getDefaultGenderID();
        this.raceGender = raceData.getGender(genderID);

        // Animation
        animationStateHandle.constructor(
                raceGender.getAnimationTreeHandle(),
                entityStateHandle.getMovementState());

        // Appearance
        if (!raceData.hasAppearance()) {
            this.appearanceHandle = null;
            return;
        }

        if (appearanceHandle == null)
            this.appearanceHandle = create(AppearanceHandle.class);

        appearanceHandle.constructor(
                raceData.getAppearanceData(),
                animationStateHandle,
                entityData.getWeightRatio(weight));
        appearanceHandle.setGender(genderID);
    }

    public RaceData getRaceData() {
        return raceData;
    }

    // How far the race's model stands against the template's own, the factor the template's sizes take for it
    public float getRaceScale() {
        return raceData.getModelHeight() / entityData.getRaceData().getModelHeight();
    }

    // Animation \\

    public void updateAnimation(float deltaTime) {
        updateAnimation(
                entityActionHandle.getAction(),
                entityActionHandle.getClip(),
                entityActionHandle.getPhase(),
                entityActionHandle.getSequence(),
                deltaTime);
    }

    // Animates the entity's movement under an action given from outside — a recording replaying what it did
    public void updateAnimation(
            EntityAction action,
            AnimationClipHandle clip,
            float phase,
            int sequence,
            float deltaTime) {

        animationStateHandle.setParameter(AnimationParameter.SPEED, entityStateHandle.getHorizontalSpeed());
        animationStateHandle.setParameter(AnimationParameter.VERTICAL_SPEED, entityStateHandle.getVerticalSpeed());
        animationStateHandle.setParameter(AnimationParameter.TURN_RATE, entityStateHandle.getBodyYawRate());
        animationStateHandle.setParameter(AnimationParameter.LOOK_PITCH, entityStateHandle.getLookPitch());
        animationStateHandle.setParameter(AnimationParameter.LOOK_YAW, entityStateHandle.getLookYaw());
        moveSprings();

        if (appearanceHandle != null) {
            appearanceHandle.wear(inventoryHandle);
            appearanceHandle.updateFace(deltaTime);
        }

        animationStateHandle.update(
                entityStateHandle.getMovementState(),
                action,
                clip,
                phase,
                sequence,
                deltaTime);
    }

    // How the body stands turned and sized for its springs, and how far it moved since the last update, measured
    // across chunk edges and the world's wrap
    private void moveSprings() {

        Vector3 position = worldPositionStruct.getPosition();
        long chunkCoordinate = worldPositionStruct.getChunkCoordinate();
        long delta = WorldWrapUtility.unwrapToGridCoordinate(worldHandle, springChunkCoordinate, chunkCoordinate);
        float scale = size.y / raceData.getModelHeight();

        springTurn.set(0f, entityStateHandle.getBodyYaw(), 0f);
        springScale.set(scale, scale, scale);
        RigMathUtility.composeLocal(springOrigin, springTurn, springScale, springFrame, springScratchA, springScratchB);
        animationStateHandle.setSpringFrame(
                springFrame,
                Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x - springPosition.x,
                position.y - springPosition.y,
                Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z - springPosition.z);

        this.springChunkCoordinate = chunkCoordinate;
        springPosition.set(position);
        blowSprings();
    }

    // The wind the springs' drag works against: the global wind's share that moves past a body, swelling and easing
    // in gusts on the wind's own clock
    private void blowSprings() {

        WindHandle windHandle = windManager.getWindHandle();
        Vector3 direction = windHandle.getGlobalWindDirection();
        float time = windManager.getGustTime();
        float swell = 0.5f + 0.5f * (float) (Math.sin(time * EngineSetting.RIG_SPRING_GUST_SPEED)
                * Math.sin(time * EngineSetting.RIG_SPRING_GUST_SWELL + EngineSetting.RIG_SPRING_GUST_PHASE));
        float gust = EngineSetting.RIG_SPRING_GUST_FLOOR + (1f - EngineSetting.RIG_SPRING_GUST_FLOOR) * swell;
        float speed = windHandle.getGlobalWindSpeed() * EngineSetting.RIG_SPRING_WIND_SHARE * gust;

        animationStateHandle.setSpringWind(direction.x * speed, direction.y * speed, direction.z * speed);
    }

    // Statistics \\

    public float getStat(short statID) {
        return progressionManager.resolveStats(this)[statID];
    }

    public int getLevel() {
        return progressionHandle.getLevel();
    }

    // Accessible \\

    public EntityData getEntityData() {
        return entityData;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public BehaviorHandle getBehaviorHandle() {
        return behaviorHandle;
    }

    public void setBehaviorHandle(BehaviorHandle behaviorHandle) {
        this.behaviorHandle = behaviorHandle;
    }

    public EntityStateHandle getEntityStateHandle() {
        return entityStateHandle;
    }

    public EntityActionHandle getEntityActionHandle() {
        return entityActionHandle;
    }

    public StatisticsHandle getStatisticsHandle() {
        return statisticsHandle;
    }

    public InventoryHandle getInventoryHandle() {
        return inventoryHandle;
    }

    public ProgressionHandle getProgressionHandle() {
        return progressionHandle;
    }

    public ProficiencyHandle getProficiencyHandle() {
        return proficiencyHandle;
    }

    public ConductHandle getConductHandle() {
        return conductHandle;
    }

    public CareerHandle getCareerHandle() {
        return careerHandle;
    }

    public DispositionHandle getDispositionHandle() {
        return dispositionHandle;
    }

    public EntityInputHandle getEntityInputHandle() {
        return EntityInputHandle;
    }

    public AnimationStateHandle getAnimationStateHandle() {
        return animationStateHandle;
    }

    public boolean hasAppearance() {
        return appearanceHandle != null;
    }

    public AppearanceHandle getAppearanceHandle() {
        return appearanceHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public int getTextureUser() {
        return textureUser;
    }

    public void setTextureUser(int textureUser) {
        this.textureUser = textureUser;
    }

    public TextureTileSetStruct getTextureTiles() {
        return textureTiles;
    }

    public void setLocation(Vector3 position, long chunkCoordinate) {

        worldPositionStruct.setPosition(position);
        worldPositionStruct.setChunkCoordinate(chunkCoordinate);
    }

    public Vector3 getSize() {
        return size;
    }

    public void setSize(Vector3 size) {
        this.size = size;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {

        this.weight = weight;

        if (appearanceHandle != null)
            appearanceHandle.setWeightRatio(entityData.getWeightRatio(weight));
    }

    public short getGenderID() {
        return genderID;
    }

    // The one way a being changes gender — it takes on the gender's model, moving by its tree from where it stands
    public void setGenderID(short genderID) {

        if (!raceData.allowsGender(genderID))
            throwException("Race \"" + raceData.getRaceName() + "\" does not allow gender ID " + genderID + ".");

        RaceGenderStruct previousGender = raceGender;

        this.genderID = genderID;
        this.raceGender = raceData.getGender(genderID);

        if (raceGender.getAnimationTreeHandle() != previousGender.getAnimationTreeHandle())
            animationStateHandle.constructor(
                    raceGender.getAnimationTreeHandle(),
                    entityStateHandle.getMovementState());

        if (appearanceHandle != null)
            appearanceHandle.setGender(genderID);
    }

    public RaceGenderStruct getRaceGender() {
        return raceGender;
    }

    public float getEyeHeight() {
        return size.y * entityData.getEyeLevel();
    }

    // The chunk-local point the entity looks and aims from — the centre of its footprint, at eye height above
    // the ground it is drawn standing on
    public Vector3 getEyePosition(Vector3 out) {

        Vector3 position = worldPositionStruct.getPosition();

        return out.set(
                position.x + size.x * 0.5f,
                position.y + entityStateHandle.getGroundOffset() + getEyeHeight(),
                position.z + size.z * 0.5f);
    }

    // The chunk-local point a point of its model stands at — turned to its body yaw and scaled to its size about
    // the centre of its footprint, on the ground it is drawn standing on, exactly as EntityRenderSystem draws it
    public Vector3 getModelPoint(float modelX, float modelY, float modelZ, Vector3 out) {

        Vector3 position = worldPositionStruct.getPosition();
        float scale = size.y / raceData.getModelHeight();
        double yaw = Math.toRadians(entityStateHandle.getBodyYaw());
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);

        return out.set(
                position.x + size.x * 0.5f + (modelX * cos + modelZ * sin) * scale,
                position.y + entityStateHandle.getGroundOffset() + modelY * scale,
                position.z + size.z * 0.5f + (modelZ * cos - modelX * sin) * scale);
    }
}