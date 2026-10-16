package application.bootstrap.entitypipeline.progression;

import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class CareerHandle extends HandlePackage {

    /*
     * Per-being professions: the one active, every one unlocked, and every one
     * granted by an event in the world, each indexed by profession ID. A being
     * only ever switches among professions it has unlocked. The pending flag
     * marks that something a requirement reads has changed, so locked
     * professions are checked again — once, on the next frame — instead of
     * every frame. The revision counts every change of the active profession.
     * Only ProgressionManager changes it. Lives on EntityInstance.
     */

    // Internal
    private ProfessionManager professionManager;

    // Active
    private short activeProfessionID;

    // Access
    private boolean[] unlocked;
    private boolean[] granted;

    // Evaluation
    private boolean pending;

    // Revision
    private int revision;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.professionManager = get(ProfessionManager.class);
    }

    @Override
    protected void awake() {

        // Access
        this.unlocked = new boolean[professionManager.getProfessionIDCount()];
        this.granted = new boolean[professionManager.getProfessionIDCount()];
    }

    // Management \\

    public void reset() {

        this.activeProfessionID = EngineSetting.REGISTRY_RESERVED_ID;

        for (int professionID = 0; professionID < unlocked.length; professionID++) {
            unlocked[professionID] = false;
            granted[professionID] = false;
        }

        this.pending = false;

        markChanged();
    }

    public void markChanged() {
        revision++;
    }

    // Accessible \\

    public boolean hasActiveProfession() {
        return activeProfessionID != EngineSetting.REGISTRY_RESERVED_ID;
    }

    public short getActiveProfessionID() {
        return activeProfessionID;
    }

    public void setActiveProfessionID(short activeProfessionID) {
        this.activeProfessionID = activeProfessionID;
    }

    public boolean isUnlocked(short professionID) {
        return unlocked[professionID];
    }

    public void setUnlocked(short professionID, boolean value) {
        unlocked[professionID] = value;
    }

    public boolean isGranted(short professionID) {
        return granted[professionID];
    }

    public void setGranted(short professionID, boolean value) {
        granted[professionID] = value;
    }

    public int getProfessionCount() {
        return unlocked.length;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public int getRevision() {
        return revision;
    }
}
