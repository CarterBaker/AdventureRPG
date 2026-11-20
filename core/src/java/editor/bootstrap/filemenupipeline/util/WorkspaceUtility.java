package editor.bootstrap.filemenupipeline.util;

import java.io.File;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

public class WorkspaceUtility extends EngineUtility {

    /*
     * The workspace: the folders beside the player's saves where new work is
     * made and kept until it is imported into the game, one folder to each
     * kind of file, and the system's own file picker that opens a file from
     * one, the way a painting or modelling tool keeps its project files apart
     * from the engine that imports their exports. A file is known by its path
     * below its folder without the extension, as the editor names it.
     */

    // Folders \\

    // The workspace itself, under the player's game folder
    public static File resolveRoot(File userRoot) {
        return new File(userRoot, EngineSetting.USER_ASSETS_DIRECTORY);
    }

    // A kind's folder in the workspace under the player's game folder
    public static File resolveFolder(File userRoot, String folderName) {
        return new File(resolveRoot(userRoot), folderName);
    }

    // Picker \\

    // The file chosen in the system's open dialog, which starts in the folder and lists only the files the patterns
    // take; null when the dialog is cancelled
    public static File chooseFile(String title, File folder, String[] filePatterns, String description) {

        folder.mkdirs();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer patterns = stack.mallocPointer(filePatterns.length);

            for (int i = 0; i < filePatterns.length; i++)
                patterns.put(stack.UTF8(filePatterns[i]));

            patterns.flip();

            String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                    title,
                    folder.getAbsolutePath() + File.separator,
                    patterns,
                    description,
                    false);

            return chosen != null ? new File(chosen) : null;
        }
    }

    // Names \\

    // The name a file in the folder goes by, its path below the folder without the extension; null for a file that is
    // not an .arpg file inside the folder
    public static String toName(File folder, File file) {

        if (!ArpgUtility.isArpgFile(file))
            return null;

        String folderPath = folder.getAbsoluteFile().toPath().normalize().toString();
        String filePath = file.getAbsoluteFile().toPath().normalize().toString();

        return filePath.startsWith(folderPath + File.separator)
                ? FileUtility.getPathWithFileNameWithoutExtension(folder, file)
                : null;
    }
}
