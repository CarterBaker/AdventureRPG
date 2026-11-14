package editor.bootstrap.recorderpipeline.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;

public class FfmpegUtility extends EngineUtility {

    /*
     * The editor's one way to ffmpeg, run only on a thread of the thread
     * pipeline. ffmpeg is the one on the PATH, else the portable copy
     * Delivery.bat keeps in the user's local application data; without
     * either nothing runs and the caller tells the owner to run Delivery.bat
     * once. Sound is encoded with the very settings Delivery.bat converts
     * sounds with — Ogg Vorbis at quality 5, 48 kHz, no metadata — so a
     * render is the engine's ordinary sound. Never held — all methods static.
     */

    // Find \\

    // The ffmpeg the editor runs, null when there is none
    public static File findFfmpeg() {

        String path = System.getenv(EditorSetting.FFMPEG_PATH_VARIABLE);

        if (path != null)
            for (String directory : path.split(File.pathSeparator)) {

                File found = findIn(new File(directory));

                if (found != null)
                    return found;
            }

        String localAppData = System.getenv(EditorSetting.FFMPEG_LOCAL_APP_DATA_VARIABLE);

        return localAppData != null
                ? findIn(new File(localAppData, EditorSetting.FFMPEG_PORTABLE_DIRECTORY))
                : null;
    }

    private static File findIn(File directory) {

        for (int i = 0; i < EditorSetting.FFMPEG_FILE_NAMES.length; i++) {

            File candidate = new File(directory, EditorSetting.FFMPEG_FILE_NAMES[i]);

            if (candidate.isFile() && candidate.canExecute())
                return candidate;
        }

        return null;
    }

    // Encode \\

    // Raw 16-bit little-endian PCM of the rate and channels given encoded to an Ogg Vorbis file; throws a catchable
    // InternalException carrying what ffmpeg said when it fails
    public static void encodeVorbis(File ffmpeg, File pcmFile, int sampleRate, int channels, File target) {

        ProcessBuilder builder = new ProcessBuilder(
                ffmpeg.getAbsolutePath(),
                EditorSetting.FFMPEG_ARG_NO_STDIN,
                EditorSetting.FFMPEG_ARG_LOG_LEVEL, EditorSetting.FFMPEG_LOG_ERRORS,
                EditorSetting.FFMPEG_ARG_OVERWRITE,
                EditorSetting.FFMPEG_ARG_FORMAT, EditorSetting.FFMPEG_FORMAT_PCM,
                EditorSetting.FFMPEG_ARG_RATE, Integer.toString(sampleRate),
                EditorSetting.FFMPEG_ARG_CHANNELS, Integer.toString(channels),
                EditorSetting.FFMPEG_ARG_INPUT, pcmFile.getAbsolutePath(),
                EditorSetting.FFMPEG_ARG_MAP, EditorSetting.FFMPEG_MAP_FIRST_AUDIO,
                EditorSetting.FFMPEG_ARG_MAP_METADATA, EditorSetting.FFMPEG_MAP_METADATA_NONE,
                EditorSetting.FFMPEG_ARG_AUDIO_CODEC, EditorSetting.FFMPEG_CODEC_VORBIS,
                EditorSetting.FFMPEG_ARG_AUDIO_QUALITY, EditorSetting.FFMPEG_VORBIS_QUALITY,
                EditorSetting.FFMPEG_ARG_RATE, Integer.toString(EditorSetting.FFMPEG_SOUND_RATE),
                target.getAbsolutePath());

        builder.redirectErrorStream(true);

        try {

            Process process = builder.start();
            String output;

            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes()).trim();
            }

            int exit = process.waitFor();

            if (exit != 0)
                throw new InternalException(
                        "ffmpeg failed (" + exit + ") encoding " + target.getName() + ": " + output);
        } catch (IOException e) {
            throw new InternalException("ffmpeg could not be run: " + ffmpeg.getAbsolutePath(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("ffmpeg was interrupted encoding " + target.getName(), e);
        }
    }
}
