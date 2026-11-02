package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import engine.root.StructPackage;

public class AppearanceGenderStruct extends StructPackage {

    /*
     * The look one gender starts with: a value per shape slider (NaN keeps the
     * template's default) and a feature per slot (null keeps the template's).
     * A woman and a man are one mesh; their presets set the masculine shape
     * and the hair each starts with.
     */

    // Shapes — indexed like the template's shape sliders
    private final float[] shapeValues;

    // Features — indexed by FeatureSlot ordinal
    private final FeatureHandle[] features;

    // Constructor \\

    public AppearanceGenderStruct(float[] shapeValues, FeatureHandle[] features) {
        this.shapeValues = shapeValues;
        this.features = features;
    }

    // Accessible \\

    public float getShapeValue(int shapeIndex) {
        return shapeValues[shapeIndex];
    }

    public FeatureHandle getFeature(int slotIndex) {
        return features[slotIndex];
    }
}
