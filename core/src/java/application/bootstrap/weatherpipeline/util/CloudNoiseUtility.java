package application.bootstrap.weatherpipeline.util;

import java.util.Arrays;
import java.util.SplittableRandom;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class CloudNoiseUtility extends EngineUtility {

    /*
     * Bakes the cloud noise texture once, so the cloud shaders read their
     * shapes with a few filtered lookups instead of computing noise per sample.
     * Every channel tiles across the texture: x = cloud shape, a periodic fbm of
     * quintic value noise; y = lobes, round sphere bumps 1 on a lobe's crown
     * falling to 0 between lobes; z = towers, a broad periodic value noise that
     * sets how tall each cluster of cloud stands. Shape and towers are
     * equalised onto an even spread over 0 to 1, so a coverage of c covers
     * exactly a share c of the sky.
     */

    // Settings
    private static final int SIZE = EngineSetting.CLOUD_NOISE_SIZE;
    private static final int CHANNELS = EngineSetting.CLOUD_NOISE_CHANNELS;
    private static final int CHANNEL_SHAPE = 0;
    private static final int CHANNEL_LOBES = 1;
    private static final int CHANNEL_TOWERS = 2;

    // Bake \\

    public static float[] bake() {

        SplittableRandom random = new SplittableRandom(EngineSetting.CLOUD_NOISE_SEED);
        float[] pixels = new float[SIZE * SIZE * CHANNELS];

        float[] shape = bakeFbm(
                random, EngineSetting.CLOUD_NOISE_SHAPE_CELLS, EngineSetting.CLOUD_NOISE_SHAPE_OCTAVES);
        float[] lobes = bakeLobes(random, EngineSetting.CLOUD_NOISE_LOBE_CELLS);
        float[] towers = bakeFbm(
                random, EngineSetting.CLOUD_NOISE_TOWER_CELLS, EngineSetting.CLOUD_NOISE_TOWER_OCTAVES);

        equalize(shape);
        equalize(towers);

        for (int i = 0; i < SIZE * SIZE; i++) {
            pixels[i * CHANNELS + CHANNEL_SHAPE] = shape[i];
            pixels[i * CHANNELS + CHANNEL_LOBES] = lobes[i];
            pixels[i * CHANNELS + CHANNEL_TOWERS] = towers[i];
        }

        return pixels;
    }

    // Value Noise \\

    private static float[] bakeFbm(SplittableRandom random, int cells, int octaves) {

        float[] field = new float[SIZE * SIZE];
        float amplitude = 1f;

        for (int octave = 0; octave < octaves; octave++) {

            int octaveCells = cells << octave;
            float[] lattice = bakeLattice(random, octaveCells);

            for (int y = 0; y < SIZE; y++)
                for (int x = 0; x < SIZE; x++)
                    field[y * SIZE + x] += amplitude * sampleValueNoise(lattice, octaveCells, x, y);

            amplitude *= EngineSetting.CLOUD_NOISE_SHAPE_PERSISTENCE;
        }

        return field;
    }

    private static float[] bakeLattice(SplittableRandom random, int cells) {

        float[] lattice = new float[cells * cells];

        for (int i = 0; i < lattice.length; i++)
            lattice[i] = (float) random.nextDouble();

        return lattice;
    }

    // Periodic value noise at a texel, cells lattice cells across the texture.
    private static float sampleValueNoise(float[] lattice, int cells, int x, int y) {

        float u = (x + 0.5f) * cells / SIZE;
        float v = (y + 0.5f) * cells / SIZE;
        int x0 = (int) Math.floor(u);
        int y0 = (int) Math.floor(v);
        float fx = quintic(u - x0);
        float fy = quintic(v - y0);

        float n00 = lattice[wrap(y0, cells) * cells + wrap(x0, cells)];
        float n10 = lattice[wrap(y0, cells) * cells + wrap(x0 + 1, cells)];
        float n01 = lattice[wrap(y0 + 1, cells) * cells + wrap(x0, cells)];
        float n11 = lattice[wrap(y0 + 1, cells) * cells + wrap(x0 + 1, cells)];

        float top = n00 + (n10 - n00) * fx;
        float bottom = n01 + (n11 - n01) * fx;

        return top + (bottom - top) * fy;
    }

    // Lobes \\

    private static float[] bakeLobes(SplittableRandom random, int cells) {

        float[] centers = new float[cells * cells * 2];
        float jitter = EngineSetting.CLOUD_NOISE_LOBE_JITTER;

        for (int i = 0; i < centers.length; i++)
            centers[i] = 0.5f + (float) random.nextDouble(-0.5, 0.5) * jitter;

        float[] field = new float[SIZE * SIZE];
        float radiusSq = EngineSetting.CLOUD_NOISE_LOBE_RADIUS * EngineSetting.CLOUD_NOISE_LOBE_RADIUS;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {

                float u = (x + 0.5f) * cells / SIZE;
                float v = (y + 0.5f) * cells / SIZE;
                int cellX = (int) Math.floor(u);
                int cellY = (int) Math.floor(v);
                float crown = 0f;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {

                        int center = (wrap(cellY + dy, cells) * cells + wrap(cellX + dx, cells)) * 2;
                        float towardX = cellX + dx + centers[center] - u;
                        float towardY = cellY + dy + centers[center + 1] - v;

                        crown = Math.max(crown, 1f - (towardX * towardX + towardY * towardY) / radiusSq);
                    }
                }

                field[y * SIZE + x] = (float) Math.sqrt(crown);
            }
        }

        return field;
    }

    // Utility \\

    // Replaces every value by its rank, so the field spreads evenly over 0 to 1
    // while keeping its shape.
    private static void equalize(float[] field) {

        float[] sorted = field.clone();
        Arrays.sort(sorted);

        float last = sorted.length - 1f;

        for (int i = 0; i < field.length; i++)
            field[i] = Arrays.binarySearch(sorted, field[i]) / last;
    }

    private static float quintic(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static int wrap(int index, int cells) {
        return Math.floorMod(index, cells);
    }
}
