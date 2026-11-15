package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineUtility;

public class DialogueMathUtility extends EngineUtility {

    /*
     * The arithmetic the three dialogue models share. softmax() turns a list
     * of scores into shares of one whole, sharper as the temperature falls,
     * and pick() draws an index by those shares from one roll, so the
     * understanding model, the choice of reply and the choice of wording all
     * weigh and draw the same way.
     */

    // Shares \\

    // The scores made shares of one whole in place — a score of negative infinity takes no share
    public static void softmax(float[] scores, int count, float temperature) {

        float highest = Float.NEGATIVE_INFINITY;
        float total = 0f;

        for (int i = 0; i < count; i++)
            highest = Math.max(highest, scores[i]);

        for (int i = 0; i < count; i++) {
            scores[i] = (float) Math.exp((scores[i] - highest) / temperature);
            total += scores[i];
        }

        for (int i = 0; i < count; i++)
            scores[i] /= total;
    }

    // The index a roll from 0 to 1 lands on, each index as wide as its share
    public static int pick(float[] shares, int count, float roll) {

        float reached = 0f;

        for (int i = 0; i < count - 1; i++) {

            reached += shares[i];

            if (roll < reached)
                return i;
        }

        return count - 1;
    }

    // The index of the largest share
    public static int findHighest(float[] shares, int count) {

        int highest = 0;

        for (int i = 1; i < count; i++)
            if (shares[i] > shares[highest])
                highest = i;

        return highest;
    }
}
