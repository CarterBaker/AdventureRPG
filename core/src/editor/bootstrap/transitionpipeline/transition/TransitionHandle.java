package editor.bootstrap.transitionpipeline.transition;

import editor.bootstrap.transitionpipeline.util.TransitionDirection;
import editor.bootstrap.transitionpipeline.util.TransitionEasing;
import editor.bootstrap.transitionpipeline.util.TransitionKind;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class TransitionHandle extends HandlePackage {

    /*
     * Persistent reference to one transition. Registered and owned by
     * TransitionManager. Delegates all accessors through TransitionData,
     * which a live edit of the transition replaces in place, so every clip
     * that names it plays the edit at once.
     */

    // Internal
    private TransitionData transitionData;

    // Constructor \\

    public void constructor(TransitionData transitionData) {

        // Internal
        this.transitionData = transitionData;
    }

    // Accessible \\

    public String getTransitionName() {
        return transitionData.getTransitionName();
    }

    public int getTransitionID() {
        return transitionData.getTransitionID();
    }

    public String getDisplayName() {
        return transitionData.getDisplayName();
    }

    public TransitionKind getKind() {
        return transitionData.getKind();
    }

    public TransitionDirection getDirection() {
        return transitionData.getDirection();
    }

    public Vector3 getColor() {
        return transitionData.getColor();
    }

    public float getSoftness() {
        return transitionData.getSoftness();
    }

    public TransitionEasing getEasing() {
        return transitionData.getEasing();
    }

    public float getSeconds() {
        return transitionData.getSeconds();
    }
}
