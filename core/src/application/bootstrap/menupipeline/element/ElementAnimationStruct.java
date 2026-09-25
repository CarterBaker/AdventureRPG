package application.bootstrap.menupipeline.element;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementAnimationStruct extends StructPackage {

    /*
     * Immutable keyframe animation for one menu element, parsed from the
     * element's "animation" block. Sampled against the owning menu's clock:
     * the first keyframe holds until delay has passed, the last holds once the
     * timeline ends. A looping animation replays its timeline after resting on
     * the last keyframe for repeatDelay seconds.
     */

    // Timing
    private final float delay;
    private final boolean loop;
    private final float repeatDelay;

    // Keyframes — ascending time, never empty
    private final ObjectArrayList<ElementKeyframeStruct> keyframes;

    // Constructor \\

    public ElementAnimationStruct(
            float delay,
            boolean loop,
            float repeatDelay,
            ObjectArrayList<ElementKeyframeStruct> keyframes) {

        this.delay = delay;
        this.loop = loop;
        this.repeatDelay = repeatDelay;
        this.keyframes = keyframes;
    }

    // Sample \\

    public void sample(float elapsed, ElementPoseStruct pose) {

        ElementKeyframeStruct first = keyframes.get(0);
        ElementKeyframeStruct last = keyframes.get(keyframes.size() - 1);
        float time = elapsed - delay;

        if (time <= first.getTime()) {
            pose.set(first);
            return;
        }

        float period = last.getTime() + repeatDelay;

        if (loop && period > 0f)
            time %= period;

        if (time >= last.getTime()) {
            pose.set(last);
            return;
        }

        for (int i = 1; i < keyframes.size(); i++) {

            ElementKeyframeStruct to = keyframes.get(i);

            if (time > to.getTime())
                continue;

            ElementKeyframeStruct from = keyframes.get(i - 1);
            float span = to.getTime() - from.getTime();
            float progress = span > 0f ? (time - from.getTime()) / span : 1f;

            pose.blend(from, to, to.getEase().apply(progress));
            return;
        }

        pose.set(last);
    }

    // Accessible \\

    public float getDelay() {
        return delay;
    }

    public boolean isLoop() {
        return loop;
    }

    public float getRepeatDelay() {
        return repeatDelay;
    }

    public ObjectArrayList<ElementKeyframeStruct> getKeyframes() {
        return keyframes;
    }
}
