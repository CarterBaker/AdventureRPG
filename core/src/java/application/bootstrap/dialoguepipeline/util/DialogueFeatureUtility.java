package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class DialogueFeatureUtility extends EngineUtility {

    /*
     * The one way the dialogue models name what they weigh. A feature is a
     * 32-bit number: hashName() gives every word, tag, trait, mood and line
     * the stable seed of its name, so the same content trains the same model
     * on every machine whatever IDs a run assigns, and mix() joins a kind's
     * salt with one or two seeds into the feature a weight table buckets.
     */

    // Hash \\

    public static int hashName(CharSequence name) {
        return hashText(name, 0, name.length());
    }

    public static int hashText(CharSequence text, int start, int end) {

        int hash = EngineSetting.FNV_OFFSET_BASIS;

        for (int i = start; i < end; i++) {
            hash ^= text.charAt(i);
            hash *= EngineSetting.FNV_PRIME;
        }

        return hash;
    }

    // Mix \\

    public static int mix(int salt, int value) {

        int hash = salt ^ (value * EngineSetting.DIALOGUE_HASH_MULTIPLIER);

        hash ^= hash >>> 16;
        hash *= EngineSetting.DIALOGUE_HASH_MIX_FIRST;
        hash ^= hash >>> 13;
        hash *= EngineSetting.DIALOGUE_HASH_MIX_SECOND;
        hash ^= hash >>> 16;

        return hash;
    }

    public static int mix(int salt, int first, int second) {
        return mix(mix(salt, first), second);
    }
}
