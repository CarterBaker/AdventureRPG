package application.bootstrap.entitypipeline.appearance;

import java.util.Arrays;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigSpringChainStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class AppearanceHandle extends HandlePackage {

    /*
     * Per-entity appearance: the body its gender wears, its colors, detail
     * amounts, shape slider values, worn features, the style each overlay is
     * painted in, body build and head shape. Seeded from the race and edited
     * in place. Slider values are kept as
     * the weights of the body's morphs, beside the expression weights the
     * face plays — a blink at an interval of its own, passing through the
     * half-closed shape, while the face is animated — and as the moves, turns
     * and scales of the bones they drive. applyProportions() is the one path
     * that writes bone proportions and shaping and re-poses the entity,
     * writeAppearance() the one path that hands the lot to a skinned draw,
     * and resetToDefaults() returns everything to the race and the
     * gender's preset. resolveSprings() runs each of the rig's spring chains
     * only while the body or a feature worn weighs onto it, so long hair
     * swings and a bald head simulates nothing. Every array is allocated once
     * in constructor(). Lives on EntityInstance.
     */

    // Internal
    private AppearanceData appearanceData;
    private AnimationStateHandle animationStateHandle;

    // Colors — indexed by SkinnedColor ordinal
    private Color[] colors;

    // Details — indexed by SkinnedDetail ordinal
    private float[] details;

    // Shapes — slider values, and the weight of every morph of the body worn
    private float[] shapeValues;
    private float[] morphWeights;

    // Gender
    private short genderID;
    private int bodyIndex;

    // Face
    private float blinkSeed;
    private boolean faceAnimated;

    // Features — indexed by FeatureSlot ordinal
    private FeatureHandle[] features;

    // Feature Variants — indexed by FeatureSlot ordinal, INDEX_NOT_FOUND for the worn feature's default
    private int[] featureVariants;

    // Overlay Styles — indexed by SkinnedOverlay ordinal, INDEX_NOT_FOUND for the model's default
    private int[] overlayStyles;

    // Proportions
    private float weightRatio;
    private Vector3 headProportion;

    // Bone Shaping — per bone, what the sliders driving it add up to
    private Vector3[] boneMoves;
    private Vector3[] boneTurns;
    private Vector3[] boneScales;

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
        this.morphWeights = new float[appearanceData.getMaxMorphCount()];

        // Face
        this.blinkSeed = (float) Math.random();
        this.faceAnimated = true;

        // Features
        this.features = new FeatureHandle[FeatureSlot.VALUES.length];
        this.featureVariants = new int[FeatureSlot.VALUES.length];
        this.overlayStyles = new int[SkinnedOverlay.VALUES.length];

        // Proportions
        this.weightRatio = weightRatio;
        this.headProportion = new Vector3();

        // Bone Shaping
        int boneCount = appearanceData.getRigHandle().getBoneCount();

        this.boneMoves = new Vector3[boneCount];
        this.boneTurns = new Vector3[boneCount];
        this.boneScales = new Vector3[boneCount];

        for (int i = 0; i < boneCount; i++) {
            boneMoves[i] = new Vector3();
            boneTurns[i] = new Vector3();
            boneScales[i] = new Vector3(1f, 1f, 1f);
        }

        resetToDefaults();
    }

    // Defaults \\

    public void resetToDefaults() {

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            colors[skinnedColor.ordinal()].set(appearanceData.getColor(skinnedColor));

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            details[skinnedDetail.ordinal()] = appearanceData.getDetail(skinnedDetail);

        Arrays.fill(overlayStyles, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(featureVariants, EngineSetting.INDEX_NOT_FOUND);

        Arrays.fill(morphWeights, 0f);

        for (int i = 0; i < shapeValues.length; i++)
            writeShapeValue(i, appearanceData.getShape(i).getDefaultValue());

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            features[featureSlot.ordinal()] = appearanceData.getDefaultFeature(featureSlot);

        applyGenderPreset();
        headProportion.set(1f, 1f, 1f);

        applyProportions();
        resolveSprings();
    }

    // Gender \\

    // Takes on a gender, wearing its body, and resets to the look it starts with
    public void setGender(short genderID) {
        this.genderID = genderID;
        this.bodyIndex = appearanceData.getBodyIndex(genderID);
        resetToDefaults();
    }

    public short getGenderID() {
        return genderID;
    }

    // The mesh of the body worn, the one the sliders' morphs weigh
    public MeshHandle getBodyMesh() {
        return appearanceData.getBodyMesh(bodyIndex);
    }

    private void applyGenderPreset() {

        AppearanceGenderStruct preset = appearanceData.getGenderPreset(genderID);

        if (preset == null)
            return;

        for (int i = 0; i < shapeValues.length; i++)
            if (!Float.isNaN(preset.getShapeValue(i)))
                writeShapeValue(i, preset.getShapeValue(i));

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (preset.namesFeature(featureSlot.ordinal()))
                features[featureSlot.ordinal()] = preset.getFeature(featureSlot.ordinal());
    }

    // Colors \\

    public Color getColor(SkinnedColor skinnedColor) {
        return colors[skinnedColor.ordinal()];
    }

    public void setColor(SkinnedColor skinnedColor, float r, float g, float b) {
        colors[skinnedColor.ordinal()].set(r, g, b, 1f);
    }

    // Overlay Styles \\

    // The style an overlay is painted in, by its place among the styles the worn gender's model offers;
    // INDEX_NOT_FOUND for the model's default
    public int getOverlayStyle(SkinnedOverlay skinnedOverlay) {
        return overlayStyles[skinnedOverlay.ordinal()];
    }

    public void setOverlayStyle(SkinnedOverlay skinnedOverlay, int styleIndex) {
        overlayStyles[skinnedOverlay.ordinal()] = styleIndex;
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

    // Sets a slider within its range, weighing its morph by the value and re-posing the bones it drives
    public void setShapeValue(int shapeIndex, float value) {

        writeShapeValue(shapeIndex, value);

        if (appearanceData.getShape(shapeIndex).hasBoneTargets())
            applyProportions();
    }

    // Sets a slider without re-posing, for a caller setting many at once; the bones it drives take it up at the next
    // proportions pass, the next build or head shape set
    public void stageShapeValue(int shapeIndex, float value) {
        writeShapeValue(shapeIndex, value);
    }

    private void writeShapeValue(int shapeIndex, float value) {

        float clamped = appearanceData.getShape(shapeIndex).clamp(value);
        int morphIndex = appearanceData.getShapeMorph(bodyIndex, shapeIndex);

        shapeValues[shapeIndex] = clamped;

        if (morphIndex != EngineSetting.INDEX_NOT_FOUND)
            morphWeights[morphIndex] = clamped;
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

        int morphIndex = appearanceData.getExpressionMorph(bodyIndex, expression);

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
        featureVariants[featureHandle.getFeatureSlot().ordinal()] = EngineSetting.INDEX_NOT_FOUND;
        resolveSprings();
    }

    public void clearFeature(FeatureSlot featureSlot) {
        features[featureSlot.ordinal()] = null;
        featureVariants[featureSlot.ordinal()] = EngineSetting.INDEX_NOT_FOUND;
        resolveSprings();
    }

    // The variant a worn feature shows, by its place among the variants the feature offers; INDEX_NOT_FOUND for the
    // feature's default
    public int getFeatureVariant(FeatureSlot featureSlot) {
        return featureVariants[featureSlot.ordinal()];
    }

    public void setFeatureVariant(FeatureSlot featureSlot, int variantIndex) {
        featureVariants[featureSlot.ordinal()] = variantIndex;
    }

    // Springs \\

    // Each spring chain of the rig runs only while something worn weighs onto one of its bones
    public void resolveSprings() {

        RigHandle rigHandle = appearanceData.getRigHandle();

        for (int c = 0; c < rigHandle.getSpringChainCount(); c++)
            animationStateHandle.setSpringActive(c, isWorn(rigHandle.getSpringChain(c)));
    }

    private boolean isWorn(RigSpringChainStruct chain) {

        for (int j = 0; j < chain.getBoneCount(); j++)
            if (weighsOnto(chain.getBone(j)))
                return true;

        return false;
    }

    private boolean weighsOnto(int boneIndex) {

        if (getBodyMesh().weighsOnto(boneIndex))
            return true;

        for (FeatureHandle feature : features)
            if (feature != null && feature.getMeshHandle().weighsOnto(boneIndex))
                return true;

        return false;
    }

    // Draw \\

    // Hands every color, amount and morph weight to a skinned draw's appearance
    public void writeAppearance(SkinnedAppearanceStruct appearance) {

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            appearance.setColor(skinnedColor, colors[skinnedColor.ordinal()]);

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            appearance.setDetail(skinnedDetail, details[skinnedDetail.ordinal()]);

        appearance.setMorphWeights(getBodyMesh(), morphWeights);
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

    // Every bone's proportion from the build, the head shape and the sliders scaling it, and its shaping from the
    // sliders moving and turning it, then the pose struck again
    private void applyProportions() {

        float buildFactor = appearanceData.getBuildFactor(weightRatio);
        int headBoneIndex = appearanceData.getHeadBoneIndex();
        int boneCount = appearanceData.getRigHandle().getBoneCount();

        accumulateBoneShaping();

        for (int i = 0; i < boneCount; i++) {

            float girth = 1f + (buildFactor - 1f) * appearanceData.getGirthInfluence(i);
            Vector3 scale = boneScales[i];

            if (i == headBoneIndex)
                animationStateHandle.setBoneProportion(
                        i,
                        girth * headProportion.x * scale.x,
                        headProportion.y * scale.y,
                        girth * headProportion.z * scale.z);
            else
                animationStateHandle.setBoneProportion(i, girth * scale.x, scale.y, girth * scale.z);

            animationStateHandle.setBoneShape(i, boneMoves[i], boneTurns[i]);
        }

        animationStateHandle.refreshPose();
    }

    // Each bone's move, turn and scale summed over the sliders driving it, each at its value; a scale is held above
    // the least a bone may shrink to
    private void accumulateBoneShaping() {

        for (int i = 0; i < boneMoves.length; i++) {
            boneMoves[i].set(0f, 0f, 0f);
            boneTurns[i].set(0f, 0f, 0f);
            boneScales[i].set(1f, 1f, 1f);
        }

        for (int s = 0; s < shapeValues.length; s++) {

            AppearanceShapeStruct shape = appearanceData.getShape(s);
            float value = shapeValues[s];

            for (int t = 0; t < shape.getBoneTargetCount(); t++) {

                AppearanceBoneTargetStruct target = shape.getBoneTarget(t);
                Vector3 move = target.getMove();
                Vector3 turn = target.getTurn();
                Vector3 scale = target.getScale();
                int boneIndex = target.getBoneIndex();

                boneMoves[boneIndex].add(move.x * value, move.y * value, move.z * value);
                boneTurns[boneIndex].add(turn.x * value, turn.y * value, turn.z * value);
                boneScales[boneIndex].add(scale.x * value, scale.y * value, scale.z * value);
            }
        }

        float least = EngineSetting.APPEARANCE_BONE_SCALE_MIN;

        for (int i = 0; i < boneScales.length; i++)
            boneScales[i].set(
                    Math.max(least, boneScales[i].x),
                    Math.max(least, boneScales[i].y),
                    Math.max(least, boneScales[i].z));
    }
}
