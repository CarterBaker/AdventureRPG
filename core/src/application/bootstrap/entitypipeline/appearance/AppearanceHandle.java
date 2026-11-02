package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class AppearanceHandle extends HandlePackage {

    /*
     * Per-entity appearance: its colors, detail amounts, shape slider values,
     * worn features, body build and head shape. Seeded from the template and
     * edited in place. Slider values are kept as the weights of the character
     * mesh's morphs, beside the expression weights the face plays — a blink
     * at an interval of its own, passing through the half-closed shape, while
     * the face is animated.
     * applyProportions() is the one path that writes bone proportions and
     * re-poses the entity, writeAppearance() the one path that hands the lot
     * to a skinned draw, and resetToDefaults() returns everything to the
     * template. Lives on EntityInstance.
     */

    // Internal
    private AppearanceData appearanceData;
    private AnimationStateHandle animationStateHandle;

    // Colors — indexed by SkinnedColor ordinal
    private Color[] colors;

    // Details — indexed by SkinnedDetail ordinal
    private float[] details;

    // Shapes — slider values, and the weight of every morph of the character mesh
    private float[] shapeValues;
    private float[] morphWeights;

    // Gender
    private short genderID;

    // Face
    private float blinkSeed;
    private boolean faceAnimated;

    // Features — indexed by FeatureSlot ordinal
    private FeatureHandle[] features;

    // Proportions
    private float weightRatio;
    private Vector3 headProportion;

    // Constructor \\

    public void constructor(
            AppearanceData appearanceData,
            AnimationStateHandle animationStateHandle,
            float weightRatio) {

        // Internal
        this.appearanceData = appearanceData;
        this.animationStateHandle = animationStateHandle;

        // Colors
        this.colors = new Color[SkinnedColor.VALUES.length];

        for (int i = 0; i < colors.length; i++)
            colors[i] = new Color();

        // Details
        this.details = new float[SkinnedDetail.VALUES.length];

        // Shapes
        this.shapeValues = new float[appearanceData.getShapeCount()];
        this.morphWeights = new float[appearanceData.getCharacterMesh().getMorphCount()];

        // Face
        this.blinkSeed = (float) Math.random();
        this.faceAnimated = true;

        // Features
        this.features = new FeatureHandle[FeatureSlot.VALUES.length];

        // Proportions
        this.weightRatio = weightRatio;
        this.headProportion = new Vector3();

        resetToDefaults();
    }

    // Defaults \\

    public void resetToDefaults() {

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            colors[skinnedColor.ordinal()].set(appearanceData.getColor(skinnedColor));

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            details[skinnedDetail.ordinal()] = appearanceData.getDetail(skinnedDetail);

        for (int i = 0; i < shapeValues.length; i++)
            setShapeValue(i, appearanceData.getShape(i).getDefaultValue());

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            features[featureSlot.ordinal()] = appearanceData.getDefaultFeature(featureSlot);

        applyGenderPreset();
        headProportion.set(1f, 1f, 1f);

        applyProportions();
    }

    // Gender \\

    // Takes on a gender and resets to the look it starts with
    public void setGender(short genderID) {
        this.genderID = genderID;
        resetToDefaults();
    }

    private void applyGenderPreset() {

        AppearanceGenderStruct preset = appearanceData.getGenderPreset(genderID);

        if (preset == null)
            return;

        for (int i = 0; i < shapeValues.length; i++)
            if (!Float.isNaN(preset.getShapeValue(i)))
                setShapeValue(i, preset.getShapeValue(i));

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (preset.getFeature(featureSlot.ordinal()) != null)
                features[featureSlot.ordinal()] = preset.getFeature(featureSlot.ordinal());
    }

    // Colors \\

    public Color getColor(SkinnedColor skinnedColor) {
        return colors[skinnedColor.ordinal()];
    }

    public void setColor(SkinnedColor skinnedColor, float r, float g, float b) {
        colors[skinnedColor.ordinal()].set(r, g, b, 1f);
    }

    // Details \\

    public float getDetail(SkinnedDetail skinnedDetail) {
        return details[skinnedDetail.ordinal()];
    }

    public void setDetail(SkinnedDetail skinnedDetail, float amount) {
        details[skinnedDetail.ordinal()] = amount;
    }

    // Shapes \\

    public float getShapeValue(int shapeIndex) {
        return shapeValues[shapeIndex];
    }

    // Sets a slider within its range, weighing its morph by the value
    public void setShapeValue(int shapeIndex, float value) {

        AppearanceShapeStruct shape = appearanceData.getShape(shapeIndex);
        float clamped = shape.clamp(value);

        shapeValues[shapeIndex] = clamped;
        morphWeights[shape.getMorphIndex()] = clamped;
    }

    // Face \\

    // Plays the face at a moment in seconds: each blink falls at a pseudo-random point in an interval of this
    // character's own, closing through the half shape and opening back through it
    public void updateFace(double seconds) {

        if (!faceAnimated)
            return;

        float interval = EngineSetting.FACE_BLINK_INTERVAL_MIN
                + (EngineSetting.FACE_BLINK_INTERVAL_MAX - EngineSetting.FACE_BLINK_INTERVAL_MIN) * blinkSeed;
        double cycle = Math.floor(seconds / interval);
        float start = hash(cycle) * (interval - EngineSetting.FACE_BLINK_DURATION);
        float local = (float) (seconds - cycle * interval) - start;
        float closed = local < 0f || local > EngineSetting.FACE_BLINK_DURATION
                ? 0f
                : 1f - Math.abs(2f * local / EngineSetting.FACE_BLINK_DURATION - 1f);

        setBlink(closed);
    }

    // Whether updateFace() plays the face; an editor holding a pose of the face turns it off
    public void setFaceAnimated(boolean faceAnimated) {
        this.faceAnimated = faceAnimated;
    }

    // Closes both eyes by an amount: the half shape carries the first half of the way and hands over to the full one
    public void setBlink(float closed) {

        float half = closed < EngineSetting.FACE_BLINK_HALF
                ? closed / EngineSetting.FACE_BLINK_HALF
                : (1f - closed) / (1f - EngineSetting.FACE_BLINK_HALF);
        float full = closed < EngineSetting.FACE_BLINK_HALF
                ? 0f
                : (closed - EngineSetting.FACE_BLINK_HALF) / (1f - EngineSetting.FACE_BLINK_HALF);

        setExpression(AppearanceExpression.BLINK_LEFT, full);
        setExpression(AppearanceExpression.BLINK_RIGHT, full);
        setExpression(AppearanceExpression.BLINK_LEFT_HALF, half);
        setExpression(AppearanceExpression.BLINK_RIGHT_HALF, half);
    }

    private void setExpression(AppearanceExpression expression, float weight) {

        int morphIndex = appearanceData.getExpressionMorph(expression);

        if (morphIndex != EngineSetting.INDEX_NOT_FOUND)
            morphWeights[morphIndex] = weight;
    }

    private float hash(double cycle) {

        double value = Math.sin(cycle * 12.9898 + blinkSeed * 78.233) * 43758.5453;
        return (float) (value - Math.floor(value));
    }

    // Features \\

    public FeatureHandle getFeature(FeatureSlot featureSlot) {
        return features[featureSlot.ordinal()];
    }

    public boolean hasFeature(FeatureSlot featureSlot) {
        return features[featureSlot.ordinal()] != null;
    }

    public void setFeature(FeatureHandle featureHandle) {

        if (!appearanceData.isCompatible(featureHandle))
            throwException("Feature \"" + featureHandle.getFeatureName()
                    + "\" does not fit this character — its mesh must use the character's rig.");

        features[featureHandle.getFeatureSlot().ordinal()] = featureHandle;
    }

    public void clearFeature(FeatureSlot featureSlot) {
        features[featureSlot.ordinal()] = null;
    }

    // Draw \\

    // Hands every color, amount and morph weight to a skinned draw's appearance
    public void writeAppearance(SkinnedAppearanceStruct appearance) {

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            appearance.setColor(skinnedColor, colors[skinnedColor.ordinal()]);

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            appearance.setDetail(skinnedDetail, details[skinnedDetail.ordinal()]);

        appearance.setMorphWeights(appearanceData.getCharacterMesh(), morphWeights);
    }

    // Proportions \\

    public float getWeightRatio() {
        return weightRatio;
    }

    public void setWeightRatio(float weightRatio) {
        this.weightRatio = weightRatio;
        applyProportions();
    }

    public Vector3 getHeadProportion() {
        return headProportion;
    }

    public void setHeadProportion(float width, float height, float depth) {
        headProportion.set(width, height, depth);
        applyProportions();
    }

    private void applyProportions() {

        float buildFactor = appearanceData.getBuildFactor(weightRatio);
        int headBoneIndex = appearanceData.getHeadBoneIndex();
        int boneCount = appearanceData.getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++) {

            float girth = 1f + (buildFactor - 1f) * appearanceData.getGirthInfluence(i);

            if (i == headBoneIndex)
                animationStateHandle.setBoneProportion(
                        i,
                        girth * headProportion.x,
                        headProportion.y,
                        girth * headProportion.z);
            else
                animationStateHandle.setBoneProportion(i, girth, 1f, girth);
        }

        animationStateHandle.refreshPose();
    }
}
