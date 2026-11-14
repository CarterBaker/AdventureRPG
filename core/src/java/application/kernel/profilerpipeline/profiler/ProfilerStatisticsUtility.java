package application.kernel.profilerpipeline.profiler;

import java.util.Arrays;

import engine.root.EngineUtility;

public final class ProfilerStatisticsUtility extends EngineUtility {

    /*
     * Stateless statistics over profiler samples: nearest-rank percentiles of
     * a sample's rolling history, sorted in caller-owned scratch so reading a
     * percentile allocates nothing.
     */

    // Percentile \\

    public static float percentile(ProfilerSampleStruct sample, float fraction, float[] scratch) {

        int count = Math.min(sample.getHistoryCount(), scratch.length);

        if (count == 0)
            return 0f;

        for (int i = 0; i < count; i++)
            scratch[i] = sample.getHistory(i);

        return percentile(scratch, count, fraction);
    }

    public static float percentile(float[] values, int count, float fraction) {

        if (count == 0)
            return 0f;

        Arrays.sort(values, 0, count);

        int rank = (int) Math.ceil(fraction * count) - 1;

        return values[Math.clamp(rank, 0, count - 1)];
    }
}
