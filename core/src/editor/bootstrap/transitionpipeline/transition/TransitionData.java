package editor.bootstrap.transitionpipeline.transition;

import editor.bootstrap.transitionpipeline.util.TransitionDirection;
import editor.bootstrap.transitionpipeline.util.TransitionEasing;
import editor.bootstrap.transitionpipeline.util.TransitionKind;
import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class TransitionData extends DataPackage {

    /*
     * Immutable definition of one transition: its name, which is its file's
     * path, and its registry ID; the name it is listed by; how it carries one
     * picture into the next, the way it sweeps, the colour it passes through
     * or opens onto, how soft its edge is and how it paces itself; and how
     * long it runs when first laid.
     */

    // Identity
    private final String transitionName;
    private final int transitionID;
    private final String displayName;

    // Look
    private final TransitionKind kind;
    private final TransitionDirection direction;
    private final Vector3 color;
    private final float softness;
    private final TransitionEasing easing;

    // Length
    private final float seconds;

    // Constructor \\

    public TransitionData(
            String transitionName,
            int transitionID,
            String displayName,
            TransitionKind kind,
            TransitionDirection direction,
            Vector3 color,
            float softness,
            TransitionEasing easing,
            float seconds) {

        // Identity
        this.transitionName = transitionName;
        this.transitionID = transitionID;
        this.displayName = displayName;

        // Look
        this.kind = kind;
        this.direction = direction;
        this.color = color;
        this.softness = softness;
        this.easing = easing;

        // Length
        this.seconds = seconds;
    }

    // Accessible \\

    public String getTransitionName() {
        return transitionName;
    }

    public int getTransitionID() {
        return transitionID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TransitionKind getKind() {
        return kind;
    }

    public TransitionDirection getDirection() {
        return direction;
    }

    public Vector3 getColor() {
        return color;
    }

    public float getSoftness() {
        return softness;
    }

    public TransitionEasing getEasing() {
        return easing;
    }

    public float getSeconds() {
        return seconds;
    }
}
