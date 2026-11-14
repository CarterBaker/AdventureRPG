package application.bootstrap.entitypipeline.progression;

import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class ProgressionHandle extends HandlePackage {

    /*
     * Per-being level and attributes. Experience is held as progress through
     * the current level, from 0 to 1, so it never overflows however high the
     * level climbs. Attribute values are indexed by attribute ID; points
     * placed but not yet confirmed are staged beside them and count for
     * nothing until confirmed, and confirmed points are never taken back. The
     * level-up count rises once per level gained, so anything showing a
     * level-up can tell a new one from one it already showed. The revision
     * counts every change that moves a stat. Only ProgressionManager changes
     * it. Lives on EntityInstance.
     */

    // Internal
    private AttributeManager attributeManager;

    // Level
    private int level;
    private double progress;
    private int levelUps;

    // Attributes
    private int[] attributes;
    private int[] staged;
    private int unspentPoints;

    // Revision
    private int revision;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.attributeManager = get(AttributeManager.class);
    }

    @Override
    protected void awake() {

        // Attributes
        this.attributes = new int[attributeManager.getAttributeIDCount()];
        this.staged = new int[attributeManager.getAttributeIDCount()];

        reset();
    }

    // Management \\

    public void reset() {

        // Level
        this.level = EngineSetting.LEVEL_START;
        this.progress = 0.0;

        // Attributes
        for (int attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1; attributeID < attributes.length; attributeID++) {
            attributes[attributeID] = EngineSetting.ATTRIBUTE_START;
            staged[attributeID] = 0;
        }

        this.unspentPoints = 0;

        markChanged();
    }

    public void markChanged() {
        revision++;
    }

    public void countLevelUp() {
        levelUps++;
    }

    // Accessible \\

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public int getLevelUps() {
        return levelUps;
    }

    public int getAttributeCount() {
        return attributes.length;
    }

    public int getAttribute(short attributeID) {
        return attributes[attributeID];
    }

    public void setAttribute(short attributeID, int value) {
        attributes[attributeID] = value;
    }

    public int getStaged(short attributeID) {
        return staged[attributeID];
    }

    public void setStaged(short attributeID, int points) {
        staged[attributeID] = points;
    }

    public int getStagedTotal() {

        int total = 0;

        for (int attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1; attributeID < staged.length; attributeID++)
            total += staged[attributeID];

        return total;
    }

    public int getUnspentPoints() {
        return unspentPoints;
    }

    public void setUnspentPoints(int unspentPoints) {
        this.unspentPoints = unspentPoints;
    }

    public int getRevision() {
        return revision;
    }
}
