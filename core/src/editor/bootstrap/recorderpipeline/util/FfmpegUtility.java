package editor.bootstrap.recorderpipeline.util;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class FfmpegUtility extends EngineUtility {

    /*
     * Finds an installed ffmpeg for exports that need one: first in the bin
     * folder of the game's own directory, where one can be dropped in beside
     * the editor's files, then in every folder on the system path. Null when
     * there is none, so only the built-in AVI export is open.
     */

    public static File findExecutable(File gameDirectory) {

        File bundled = findIn(new File(gameDirectory, EngineSetting.BIN_DIRECTORY));

        if (bundled != null)
            return bundled;

        String path = System.getenv(EditorSetting.RECORDER_PATH_VARIABLE);

        if (path == null)
            return null;

        for (String directory : path.split(File.pathSeparator)) {

            File found = findIn(new File(directory));

            if (found != null)
                return found;
        }

        return null;
    }

    private static File findIn(File directory) {

        for (String executableName : EditorSetting.RECORDER_FFMPEG_EXECUTABLES) {

            File executable = new File(directory, executableName);

            if (executable.isFile() && executable.canExecute())
                return executable;
        }

        return null;
    }
}
