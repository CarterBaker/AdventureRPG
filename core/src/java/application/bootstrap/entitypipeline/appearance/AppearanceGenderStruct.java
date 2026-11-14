package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import engine.root.StructPackage;

public class AppearanceGenderStruct extends StructPackage {

    /*
     * The look one gender starts with: the body it wears, by its index
     * among the race's body meshes, a value per shape slider (NaN keeps the
     * race's default) and, for each slot it names, the feature it wears
     * there (null for none, so a gender can go bald); a slot it does not
     * name keeps the race's. Each gender may be its own mesh, so a man and
     * a woman are modelled apart and share the rig, the sliders and the
     * features.
     */

    // Body
    private final int bodyIndex;

    // Shapes — indexed like the race's shape sliders
    private final float[] shapeValues;

    // Features — indexed by FeatureSlot ordinal
    private final FeatureHandle[] features;
    private final boolean[] namedFeatures;

    // Constructor \\

    public AppearanceGenderStruct(
            int bodyIndex,
            float[] shapeValues,
            FeatureHandle[] features,
            boolean[] namedFeatures) {

        // Body
        this.bodyIndex = bodyIndex;

        // Shapes
        this.shapeValues = shapeValues;

        // Features
        this.features = features;
        this.namedFeatures = namedFeatures;
    }

    // Accessible \\

    public int getBodyIndex() {
        return bodyIndex;
    }

    public float getShapeValue(int shapeIndex) {
        return shapeValues[shapeIndex];
    }

    public FeatureHandle getFeature(int slotIndex) {
        return features[slotIndex];
    }

    // Whether the gender names its own feature for a slot, none included, rather than keeping the race's
    public boolean namesFeature(int slotIndex) {
        return namedFeatures[slotIndex];
    }
}
