package application.bootstrap.musicpipeline.song;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SongTempoStruct extends StructPackage {

    /*
     * Immutable tempo map of a song: every tempo change in order of its tick,
     * the first at tick 0, each holding its beats a minute until the next.
     * The seconds each change falls at are worked out once, so turning a tick
     * into seconds and back walks no more than a binary search; every tick
     * and second conversion in the music pipeline goes through here. A tick
     * before the song's start, or a second, is timed at the first tempo.
     */

    // Changes — each change's tick, its quarter-note beats a minute and the seconds into the song it falls at
    private final int[] ticks;
    private final float[] bpms;
    private final double[] seconds;

    // Constructor \\

    public SongTempoStruct(int[] ticks, float[] bpms) {

        // Changes
        this.ticks = ticks;
        this.bpms = bpms;
        this.seconds = new double[ticks.length];

        for (int i = 1; i < ticks.length; i++)
            seconds[i] = seconds[i - 1] + toSpanSeconds(ticks[i] - ticks[i - 1], bpms[i - 1]);
    }

    // Conversion \\

    // The seconds into the song the tick falls at
    public double toSeconds(double tick) {

        int change = findChangeAtTick(tick);

        return seconds[change] + toSpanSeconds(tick - ticks[change], bpms[change]);
    }

    // The tick the seconds into the song fall at, with its fraction
    public double toTicks(double secondsIntoSong) {

        int change = findChangeAtSeconds(secondsIntoSong);

        return ticks[change] + (secondsIntoSong - seconds[change]) * bpms[change] * EngineSetting.SONG_TICKS_PER_BEAT
                / EngineSetting.SONG_SECONDS_PER_MINUTE;
    }

    private static double toSpanSeconds(double tickSpan, float bpm) {
        return tickSpan * EngineSetting.SONG_SECONDS_PER_MINUTE / (bpm * EngineSetting.SONG_TICKS_PER_BEAT);
    }

    // Lookup \\

    // The last change at or before the tick, the first for a tick before it
    private int findChangeAtTick(double tick) {

        int low = 0;
        int high = ticks.length - 1;

        while (low < high) {

            int middle = (low + high + 1) >>> 1;

            if (ticks[middle] <= tick)
                low = middle;
            else
                high = middle - 1;
        }

        return low;
    }

    // The last change at or before the seconds, the first for a time before it
    private int findChangeAtSeconds(double secondsIntoSong) {

        int low = 0;
        int high = seconds.length - 1;

        while (low < high) {

            int middle = (low + high + 1) >>> 1;

            if (seconds[middle] <= secondsIntoSong)
                low = middle;
            else
                high = middle - 1;
        }

        return low;
    }

    // Accessible \\

    public int getChangeCount() {
        return ticks.length;
    }

    public int getTick(int change) {
        return ticks[change];
    }

    public float getBpm(int change) {
        return bpms[change];
    }

    // The beats a minute the song plays at on the tick
    public float getBpmAt(double tick) {
        return bpms[findChangeAtTick(tick)];
    }
}
