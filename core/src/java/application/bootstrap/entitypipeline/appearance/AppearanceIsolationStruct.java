package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import engine.root.StructPackage;

public class AppearanceIsolationStruct extends StructPackage {

    /*
     * What of a character is drawn: whether the body is drawn, which worn
     * features are, and whether each is drawn through the bone mask the body
     * is, so a part editor shows its part alone and a first-person view hides
     * the head with its hair and every other feature it carries. Cleared, it
     * draws everything whole. An editor owns and rebuilds its own only when
     * what it shows changes; the game's are fixed.
     */

    // Body
    private boolean bodyShown;

    // Features — indexed by FeatureSlot ordinal
    private final boolean[] featureShown;
    private final boolean[] featureMasked;

    // Mask
    private final SkinnedBoneMaskStruct boneMask;

    // Constructor \\

    public AppearanceIsolationStruct() {

        // Features
        this.featureShown = new boolean[FeatureSlot.VALUES.length];
        this.featureMasked = new boolean[FeatureSlot.VALUES.length];

        // Mask
        this.boneMask = new SkinnedBoneMaskStruct();

        clear();
    }

    // Management \\

    public void clear() {

        this.bodyShown = true;

        for (int i = 0; i < featureShown.length; i++) {
            featureShown[i] = true;
            featureMasked[i] = false;
        }

        boneMask.clear();
    }

    public void setBodyShown(boolean bodyShown) {
        this.bodyShown = bodyShown;
    }

    public void setFeature(FeatureSlot featureSlot, boolean shown, boolean masked) {
        featureShown[featureSlot.ordinal()] = shown;
        featureMasked[featureSlot.ordinal()] = masked;
    }

    // Accessible \\

    public boolean isBodyShown() {
        return bodyShown;
    }

    public boolean isFeatureShown(FeatureSlot featureSlot) {
        return featureShown[featureSlot.ordinal()];
    }

    public boolean isFeatureMasked(FeatureSlot featureSlot) {
        return featureMasked[featureSlot.ordinal()];
    }

    public SkinnedBoneMaskStruct getBoneMask() {
        return boneMask;
    }
}
