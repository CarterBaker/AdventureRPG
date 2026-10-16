package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class AttributeBranch extends BranchPackage {

    /*
     * Attribute points for ProgressionManager. Points are staged first —
     * placed against a being's unspent points, never past them, and never
     * below nothing in any attribute — and count for nothing until confirm()
     * spends them into the attributes for good; clear() lets a staging go
     * without spending anything. Nothing ever takes a confirmed point back. A
     * cap holds every attribute at most so high while staging, as character
     * creation caps them. spendByWeights() spends every unspent point at once
     * the way an NPC grows: each point to whichever weighted attribute lags
     * furthest behind its share, so the spread follows the weights however
     * many points there are, or evenly when no weights are given.
     */

    // Stage \\

    // Moves a staged point into or out of an attribute — false when the move is not allowed
    boolean stage(EntityInstance entity, short attributeID, int points, int cap) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();
        int staged = progressionHandle.getStaged(attributeID) + points;

        if (staged < 0)
            return false;

        if (points > 0 && progressionHandle.getStagedTotal() + points > progressionHandle.getUnspentPoints())
            return false;

        if (progressionHandle.getAttribute(attributeID) + staged > cap)
            return false;

        progressionHandle.setStaged(attributeID, staged);

        return true;
    }

    void clear(EntityInstance entity) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < progressionHandle.getAttributeCount();
                attributeID++)
            progressionHandle.setStaged(attributeID, 0);
    }

    // Spends every staged point for good — false when nothing was staged
    boolean confirm(EntityInstance entity) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();
        int stagedTotal = progressionHandle.getStagedTotal();

        if (stagedTotal == 0)
            return false;

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < progressionHandle.getAttributeCount();
                attributeID++) {

            progressionHandle.setAttribute(
                    attributeID,
                    progressionHandle.getAttribute(attributeID) + progressionHandle.getStaged(attributeID));
            progressionHandle.setStaged(attributeID, 0);
        }

        progressionHandle.setUnspentPoints(progressionHandle.getUnspentPoints() - stagedTotal);
        progressionHandle.markChanged();

        return true;
    }

    // Spend \\

    void spendByWeights(EntityInstance entity, ValueMapStruct weights) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();
        float totalWeight = weights.getTotal();

        clear(entity);

        while (progressionHandle.getUnspentPoints() > 0) {

            short attributeID = totalWeight > 0f
                    ? findLaggingAttribute(progressionHandle, weights, totalWeight)
                    : findLowestAttribute(progressionHandle);

            progressionHandle.setAttribute(attributeID, progressionHandle.getAttribute(attributeID) + 1);
            progressionHandle.setUnspentPoints(progressionHandle.getUnspentPoints() - 1);
        }

        progressionHandle.markChanged();
    }

    // The weighted attribute furthest below its share of all the points spent among the weighted attributes
    private short findLaggingAttribute(ProgressionHandle progressionHandle, ValueMapStruct weights, float totalWeight) {

        int spent = 0;

        for (int i = 0; i < weights.size(); i++)
            spent += progressionHandle.getAttribute(weights.getID(i)) - EngineSetting.ATTRIBUTE_START;

        short lagging = weights.getID(0);
        float largestLag = -Float.MAX_VALUE;

        for (int i = 0; i < weights.size(); i++) {

            float share = (spent + 1) * weights.getValue(i) / totalWeight;
            float lag = share - (progressionHandle.getAttribute(weights.getID(i)) - EngineSetting.ATTRIBUTE_START);

            if (lag > largestLag) {
                largestLag = lag;
                lagging = weights.getID(i);
            }
        }

        return lagging;
    }

    private short findLowestAttribute(ProgressionHandle progressionHandle) {

        short lowest = EngineSetting.REGISTRY_RESERVED_ID + 1;

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < progressionHandle.getAttributeCount();
                attributeID++)
            if (progressionHandle.getAttribute(attributeID) < progressionHandle.getAttribute(lowest))
                lowest = attributeID;

        return lowest;
    }
}
