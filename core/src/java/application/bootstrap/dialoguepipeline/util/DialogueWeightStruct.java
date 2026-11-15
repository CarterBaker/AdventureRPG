package application.bootstrap.dialoguepipeline.util;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DialogueWeightStruct extends StructPackage {

    /*
     * One learned linear model: a flat table of weights, a row of one weight
     * per class for every feature bucket. A feature lands in the bucket its
     * low bits name, so scoring a set of features against every class is a
     * handful of short contiguous rows added together, and learning is the
     * same rows nudged. Weights are held within a fixed bound, so no amount
     * of feedback lets one feature drown out the rest.
     */

    // Table
    private final float[] weights;
    private final int bucketMask;
    private final int classCount;

    // Constructor \\

    public DialogueWeightStruct(int bucketCount, int classCount) {

        // Table
        this.weights = new float[bucketCount * classCount];
        this.bucketMask = bucketCount - 1;
        this.classCount = classCount;
    }

    // Management \\

    public void clear() {
        Arrays.fill(weights, 0f);
    }

    // Score \\

    // Every class's score for the features, each feature's row scaled and added onto the scores given
    public void score(int[] features, int featureCount, float scale, float[] scores) {

        for (int i = 0; i < featureCount; i++) {

            int row = (features[i] & bucketMask) * classCount;

            for (int classID = 0; classID < classCount; classID++)
                scores[classID] += weights[row + classID] * scale;
        }
    }

    public float score(int[] features, int featureCount, int classID) {

        float score = 0f;

        for (int i = 0; i < featureCount; i++)
            score += weights[(features[i] & bucketMask) * classCount + classID];

        return score;
    }

    public float score(int feature, int classID) {
        return weights[(feature & bucketMask) * classCount + classID];
    }

    // Learn \\

    // Every class's weights for the features moved by that class's amount
    public void shift(int[] features, int featureCount, float[] amounts) {

        for (int i = 0; i < featureCount; i++) {

            int row = (features[i] & bucketMask) * classCount;

            for (int classID = 0; classID < classCount; classID++)
                weights[row + classID] = clamp(weights[row + classID] + amounts[classID]);
        }
    }

    public void shift(int[] features, int featureCount, int classID, float amount) {

        for (int i = 0; i < featureCount; i++) {

            int index = (features[i] & bucketMask) * classCount + classID;

            weights[index] = clamp(weights[index] + amount);
        }
    }

    private float clamp(float weight) {
        return Math.max(-EngineSetting.DIALOGUE_WEIGHT_MAX, Math.min(EngineSetting.DIALOGUE_WEIGHT_MAX, weight));
    }

    // Accessible \\

    public int getClassCount() {
        return classCount;
    }

    public int getWeightCount() {
        return weights.length;
    }
}
