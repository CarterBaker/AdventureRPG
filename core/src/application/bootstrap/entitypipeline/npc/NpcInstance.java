package application.bootstrap.entitypipeline.npc;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.worldspawn.WorldSpawnPaletteHandle;
import application.bootstrap.worldpipeline.worldspawn.WorldSpawnStruct;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3;

public class NpcInstance extends InstancePackage {

    /*
     * One NPC living in the world, spawned by NpcManager: its definition, the
     * entity it walks as, the chunk it lives in and the world spawn it came
     * from, which its fall takes out of that spawn's palette so the spot stays
     * empty. Home is the middle of the block its spot names, local to its home
     * chunk. It walks to targets about home, measured in the same frame, idles
     * between walks, and counts how long a walk has gone on so one it cannot
     * finish is given up. A fallen NPC remembers what felled it, whose luck
     * its loot is drawn by.
     */

    // Internal
    private NpcHandle npcHandle;
    private EntityInstance entityInstance;

    // Home
    private long homeChunkCoordinate;
    private WorldSpawnPaletteHandle worldSpawnPaletteHandle;
    private WorldSpawnStruct worldSpawnStruct;
    private Vector3 homePosition;

    // Wander
    private Vector3 wanderTarget;
    private boolean walking;
    private float idleSeconds;
    private float walkSeconds;

    // State
    private boolean fallen;
    private EntityInstance killer;

    // Internal \\

    @Override
    protected void create() {

        // Home
        this.homePosition = new Vector3();

        // Wander
        this.wanderTarget = new Vector3();
    }

    // Constructor \\

    public void constructor(
            NpcHandle npcHandle,
            EntityInstance entityInstance,
            long homeChunkCoordinate,
            WorldSpawnPaletteHandle worldSpawnPaletteHandle,
            WorldSpawnStruct worldSpawnStruct) {

        // Internal
        this.npcHandle = npcHandle;
        this.entityInstance = entityInstance;

        // Home
        this.homeChunkCoordinate = homeChunkCoordinate;
        this.worldSpawnPaletteHandle = worldSpawnPaletteHandle;
        this.worldSpawnStruct = worldSpawnStruct;
        this.homePosition.set(
                worldSpawnStruct.getBlockX() + 0.5f,
                worldSpawnStruct.getBlockY(),
                worldSpawnStruct.getBlockZ() + 0.5f);

        // Wander
        this.wanderTarget.set(homePosition);
        this.walking = false;
        this.idleSeconds = 0f;
        this.walkSeconds = 0f;

        // State
        this.fallen = false;
    }

    // Wander \\

    // A walk toward a target, its clock started afresh
    public void beginWalk(float targetX, float targetZ) {

        wanderTarget.set(targetX, homePosition.y, targetZ);
        walking = true;
        walkSeconds = 0f;
    }

    // A walk ended, and the time to stand before the next
    public void beginIdle(float seconds) {

        walking = false;
        idleSeconds = seconds;
    }

    public void advanceWalk(float deltaTime) {
        walkSeconds += deltaTime;
    }

    public void advanceIdle(float deltaTime) {
        idleSeconds -= deltaTime;
    }

    // Accessible \\

    public NpcHandle getNpcHandle() {
        return npcHandle;
    }

    public EntityInstance getEntityInstance() {
        return entityInstance;
    }

    public long getHomeChunkCoordinate() {
        return homeChunkCoordinate;
    }

    public WorldSpawnPaletteHandle getWorldSpawnPaletteHandle() {
        return worldSpawnPaletteHandle;
    }

    public WorldSpawnStruct getWorldSpawnStruct() {
        return worldSpawnStruct;
    }

    public Vector3 getHomePosition() {
        return homePosition;
    }

    public Vector3 getWanderTarget() {
        return wanderTarget;
    }

    public boolean isWalking() {
        return walking;
    }

    public float getIdleSeconds() {
        return idleSeconds;
    }

    public float getWalkSeconds() {
        return walkSeconds;
    }

    public boolean isFallen() {
        return fallen;
    }

    public EntityInstance getKiller() {
        return killer;
    }

    public void setKiller(EntityInstance killer) {
        this.killer = killer;
    }

    public void setFallen(boolean fallen) {
        this.fallen = fallen;
    }
}
