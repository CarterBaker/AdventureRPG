package application.bootstrap.entitypipeline.appearance;

import java.util.Arrays;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationClipUtility;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigSpringChainStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedAppearanceStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedMaskMode;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import application.bootstrap.itempipeline.itemdefinition.ItemWornModelStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
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
     * the weights of the body's morphs, beside the weights of the morphs the
     * face's expressions play — each expression a clip of face bones and
     * morph curves at a phase and a level, fading toward the level asked of
     * it, the blink playing by itself once in an interval of its own while
     * the face is animated, its bones laid on the pose as its face layer —
     * and as the moves, turns
     * and scales of the bones they drive, each bone slider taking every
     * control's push along its curve on top of its own value. applyProportions() is the one path
     * that writes bone proportions and shaping and re-poses the entity,
     * writeAppearance() the one path that hands the lot to a skinned draw,
     * and resetToDefaults() returns everything to the race and the
     * gender's preset. wear() takes on the gear an inventory shows, each body
     * slot's item as the model it is worn as on this rig and gender, the body
     * bones those models cover gathered into one mask cut from the body.
     * resolveSprings() runs each of the rig's spring chains only while the
     * body, a feature or a worn model weighs onto it, so long hair and a cape
     * swing and a bald, bare body simulates nothing. Every array is allocated once
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

    // Face — the face's own clock and this character's blink pattern, and per expression its clip, the level asked
    // of it, the level it stands at and the phase it plays at; the clips, phases and levels are the pose's face layer
    private float faceTime;
    private long blinkSeed;
    private boolean faceAnimated;
    private AnimationClipHandle[] expressionClips;
    private float[] expressionTargets;
    private float[] expressionLevels;
    private float[] expressionPhases;

    // Features — indexed by FeatureSlot ordinal
    private FeatureHandle[] features;

    // Feature Variants — indexed by FeatureSlot ordinal, INDEX_NOT_FOUND for the worn feature's default
    private int[] featureVariants;

    // Overlay Styles — indexed by SkinnedOverlay ordinal, INDEX_NOT_FOUND for the model's default
    private int[] overlayStyles;

    // Worn — indexed by EquipmentSlot ordinal, the model each slot's item is worn as, null where none is; the body
    // bones they cover and whether they cover any; the inventory revision and gender they were taken on for
    private ItemWornModelStruct[] wornModels;
    private SkinnedBoneMaskStruct coveredBones;
    private boolean bodyCovered;
    private boolean[] coveredFeatures;
    private int wornRevision;
    private short wornGenderID;

    // Proportions
    private float weightRatio;
    private Vector3 headProportion;

    // Bone Shaping — per bone, what the sliders driving it add up to, and per slider, its own value with every
    // control's push on it
    private Vector3[] boneMoves;
    private Vector3[] boneTurns;
    private Vector3[] boneScales;
    private float[] sliderTotals;
    private float[] curveScratch;

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
        int expressionCount = appearanceData.getExpressionCount();

        this.blinkSeed = Double.doubleToLongBits(Math.random());
        this.faceAnimated = true;
        this.expressionClips = new AnimationClipHandle[expressionCount];
        this.expressionTargets = new float[expressionCount];
        this.expressionLevels = new float[expressionCount];
        this.expressionPhases = new float[expressionCount];

        for (int i = 0; i < expressionCount; i++)
            expressionClips[i] = appearanceData.getExpression(i).getClip();

        // Features
        this.features = new FeatureHandle[FeatureSlot.VALUES.length];
        this.featureVariants = new int[FeatureSlot.VALUES.length];
        this.overlayStyles = new int[SkinnedOverlay.VALUES.length];

        // Worn
        this.wornModels = new ItemWornModelStruct[EquipmentSlot.VALUES.length];
        this.coveredBones = new SkinnedBoneMaskStruct();
        this.coveredFeatures = new boolean[FeatureSlot.VALUES.length];
        this.wornRevision = EngineSetting.INDEX_NOT_FOUND;

        // Proportions
        this.weightRatio = weightRatio;
        this.headProportion = new Vector3();

        // Bone Shaping
        int boneCount = appearanceData.getBoneCount();

        this.boneMoves = new Vector3[boneCount];
        this.boneTurns = new Vector3[boneCount];
        this.boneScales = new Vector3[boneCount];

        for (int i = 0; i < boneCount; i++) {
            boneMoves[i] = new Vector3();
            boneTurns[i] = new Vector3();
            boneScales[i] = new Vector3(1f, 1f, 1f);
        }

        this.sliderTotals = new float[appearanceData.getShapeCount()];
        this.curveScratch = new float[EngineSetting.BONE_CONTROL_CURVE_SCRATCH_FLOATS];

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
        animationStateHandle.setExpressionLayer(expressionClips, expressionPhases, expressionLevels);
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

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            if (preset.namesFeature(featureSlot.ordinal()))
                features[featureSlot.ordinal()] = preset.getFeature(featureSlot.ordinal());

            featureVariants[featureSlot.ordinal()] = preset.getFeatureVariant(featureSlot.ordinal());
        }
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

        if (appearanceData.getShape(shapeIndex).shapesBones())
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

    // Plays the face on by the time passed: the blink falls once at a pseudo-random moment in every interval of this
    // character's own, and every other expression fades toward the level asked of it, a looping clip playing on and
    // any other held at its end; the morphs they weigh are written after
    public void updateFace(float deltaTime) {

        if (!faceAnimated)
            return;

        this.faceTime += deltaTime;

        int blink = appearanceData.getBlinkExpression();

        for (int i = 0; i < expressionClips.length; i++)
            if (i != blink)
                advanceExpression(i, deltaTime);

        if (blink != EngineSetting.INDEX_NOT_FOUND)
            placeBlink(blink);

        writeExpressions();
    }

    private void advanceExpression(int expressionIndex, float deltaTime) {

        float step = deltaTime / EngineSetting.FACE_EXPRESSION_FADE;
        float level = expressionLevels[expressionIndex];
        float target = expressionTargets[expressionIndex];
        AnimationClipHandle clip = expressionClips[expressionIndex];

        level = level < target ? Math.min(target, level + step) : Math.max(target, level - step);
        expressionLevels[expressionIndex] = level;

        if (level <= 0f) {
            expressionPhases[expressionIndex] = 0f;
            return;
        }

        float phase = expressionPhases[expressionIndex] + deltaTime / clip.getDuration();

        expressionPhases[expressionIndex] = clip.isLooping() ? phase - (float) Math.floor(phase) : Math.min(1f, phase);
    }

    // The blink's phase where the face's clock stands within this interval, wholly on while it plays and off outside
    private void placeBlink(int blink) {

        float range = EngineSetting.FACE_BLINK_INTERVAL_MAX - EngineSetting.FACE_BLINK_INTERVAL_MIN;
        float interval = EngineSetting.FACE_BLINK_INTERVAL_MIN + range * BiomeFieldUtility.hash01(blinkSeed);
        float duration = expressionClips[blink].getDuration();
        long cycle = (long) Math.floor(faceTime / interval);
        float start = BiomeFieldUtility.hash01(blinkSeed ^ EngineSetting.FACE_BLINK_SALT ^ cycle)
                * Math.max(0f, interval - duration);
        float local = faceTime - cycle * interval - start;
        boolean playing = local >= 0f && local <= duration;

        expressionLevels[blink] = playing ? 1f : 0f;
        expressionPhases[blink] = playing ? local / duration : 0f;
    }

    // Whether updateFace() plays the face; an editor holding a pose of the face turns it off
    public void setFaceAnimated(boolean faceAnimated) {
        this.faceAnimated = faceAnimated;
    }

    // Asks an expression for a level, toward which it fades as the face plays
    public void setExpression(int expressionIndex, float level) {
        expressionTargets[expressionIndex] = level;
    }

    // Holds an expression at a phase and a level outright, as an editor strikes it, and weighs its morphs at once
    public void poseExpression(int expressionIndex, float phase, float level) {

        expressionTargets[expressionIndex] = level;
        expressionLevels[expressionIndex] = level;
        expressionPhases[expressionIndex] = Math.max(0f, Math.min(1f, phase));
        writeExpressions();
    }

    // Closes both eyes by an amount, holding the blink where its clip closes them that far: fully at its middle
    public void setBlink(float closed) {

        int blink = appearanceData.getBlinkExpression();

        if (blink != EngineSetting.INDEX_NOT_FOUND)
            poseExpression(blink, closed * EngineSetting.FACE_BLINK_CLOSED_PHASE, 1f);
    }

    public float getExpressionLevel(int expressionIndex) {
        return expressionLevels[expressionIndex];
    }

    public float getExpressionPhase(int expressionIndex) {
        return expressionPhases[expressionIndex];
    }

    // Every morph an expression weighs set to what the expressions playing add up to there
    private void writeExpressions() {

        for (int e = 0; e < expressionClips.length; e++) {

            AppearanceExpressionStruct expression = appearanceData.getExpression(e);

            for (int t = 0; t < expression.getTrackCount(); t++)
                morphWeights[expression.getTrackMorph(bodyIndex, t)] = 0f;
        }

        for (int e = 0; e < expressionClips.length; e++) {

            float level = expressionLevels[e];

            if (level <= 0f)
                continue;

            AppearanceExpressionStruct expression = appearanceData.getExpression(e);

            for (int t = 0; t < expression.getTrackCount(); t++)
                morphWeights[expression.getTrackMorph(bodyIndex, t)] +=
                        AnimationClipUtility.sampleMorph(expressionClips[e], t, expressionPhases[e]) * level;
        }
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

    // Worn \\

    // Takes on the gear the inventory shows, once each time it or the gender changes: each body slot's item worn as
    // its model for this rig, gender and the slot's side where it has one
    public void wear(InventoryHandle inventory) {

        if (inventory.getRevision() == wornRevision && genderID == wornGenderID)
            return;

        this.wornRevision = inventory.getRevision();
        this.wornGenderID = genderID;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            wornModels[equipmentSlot.ordinal()] = !equipmentSlot.isHand() && inventory.isShown(equipmentSlot)
                    ? inventory.getItem(equipmentSlot).getItemDefinitionHandle()
                            .getWornModel(appearanceData.getRigHandle(), genderID, equipmentSlot.getEquipmentSide())
                    : null;

        resolveWorn();
    }

    // Wears a model in a slot outside any inventory, as an editor previews gear; null takes it off. resolveWorn()
    // takes up every change made so
    public void setWornModel(EquipmentSlot equipmentSlot, ItemWornModelStruct wornModel) {
        wornModels[equipmentSlot.ordinal()] = wornModel;
    }

    // The bones and features the models worn cover, gathered once, and the springs run again for whatever now weighs
    // onto them
    public void resolveWorn() {

        this.bodyCovered = false;
        coveredBones.clear();
        coveredBones.setMode(SkinnedMaskMode.HIDE);
        Arrays.fill(coveredFeatures, false);

        for (ItemWornModelStruct wornModel : wornModels) {

            if (wornModel == null)
                continue;

            for (FeatureSlot featureSlot : FeatureSlot.VALUES)
                coveredFeatures[featureSlot.ordinal()] |= wornModel.coversFeature(featureSlot);

            if (!wornModel.covers())
                continue;

            coveredBones.addBones(wornModel.getCoveredBones());
            this.bodyCovered = true;
        }

        resolveSprings();
    }

    // The model a slot's item is worn as, null where it is worn on an anchor or the slot shows nothing
    public ItemWornModelStruct getWornModel(EquipmentSlot equipmentSlot) {
        return wornModels[equipmentSlot.ordinal()];
    }

    public boolean isBodyCovered() {
        return bodyCovered;
    }

    // Every body bone the worn models cover, a mask that hides them
    public SkinnedBoneMaskStruct getCoveredBones() {
        return coveredBones;
    }

    // Whether a worn model leaves the feature worn in the slot off
    public boolean isFeatureCovered(FeatureSlot featureSlot) {
        return coveredFeatures[featureSlot.ordinal()];
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

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (features[featureSlot.ordinal()] != null && !coveredFeatures[featureSlot.ordinal()]
                    && features[featureSlot.ordinal()].getMeshHandle().weighsOnto(boneIndex))
                return true;

        for (ItemWornModelStruct wornModel : wornModels)
            if (wornModel != null && wornModel.getMeshHandle().weighsOnto(boneIndex))
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
        int boneCount = appearanceData.getBoneCount();

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

    // Each bone's move, turn and scale summed over the bone sliders driving it, each at its own value with every
    // control's push added and held to its range; a scale is held above the least a bone may shrink to
    private void accumulateBoneShaping() {

        for (int i = 0; i < boneMoves.length; i++) {
            boneMoves[i].set(0f, 0f, 0f);
            boneTurns[i].set(0f, 0f, 0f);
            boneScales[i].set(1f, 1f, 1f);
        }

        System.arraycopy(shapeValues, 0, sliderTotals, 0, shapeValues.length);

        for (int s = 0; s < shapeValues.length; s++)
            if (appearanceData.getShape(s).isControl())
                pushControl(s);

        for (int s = 0; s < shapeValues.length; s++) {

            AppearanceShapeStruct shape = appearanceData.getShape(s);
            float value = shape.clamp(sliderTotals[s]);

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

    // Every push a control makes, at the value it stands at, on the bone sliders it drives
    private void pushControl(int controlIndex) {

        AppearanceShapeStruct control = appearanceData.getShape(controlIndex);

        for (int d = 0; d < control.getDriveCount(); d++)
            sliderTotals[control.getDrive(d).getSliderIndex()] += appearanceData.resolvePush(
                    controlIndex, d, shapeValues[controlIndex], curveScratch);
    }
}
