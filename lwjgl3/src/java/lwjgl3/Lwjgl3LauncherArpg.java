package lwjgl3;

import java.io.File;

import engine.root.EngineSetting;

public class Lwjgl3LauncherArpg {

    /*
     * Entry point for the ARPG data tool, run from the project root through
     * the lwjgl3:arpg Gradle task. Seals text into .arpg files, opens them
     * back into text, converts or exports whole folders, and verifies that
     * files open. Exits non-zero when any file fails.
     */

    // Entry \\

    public static void main(String[] args) {

        boolean complete = args.length >= 2 && isKnownCommand(args[0]);
        boolean passed = complete && run(args[0], new File(args[1]), args.length > 2 ? new File(args[2]) : null);

        if (!complete)
            System.out.println(EngineSetting.ARPG_TOOL_USAGE);

        System.exit(passed ? EngineSetting.ARPG_TOOL_EXIT_SUCCESS : EngineSetting.ARPG_TOOL_EXIT_FAILURE);
    }

    // Dispatch \\

    private static boolean run(String command, File source, File target) {

        return switch (command) {
            case EngineSetting.ARPG_TOOL_COMMAND_ENCODE -> Lwjgl3ArpgUtility.encode(
                    source,
                    target != null ? target : Lwjgl3ArpgUtility.toSibling(source, EngineSetting.ARPG_FILE_EXTENSION));
            case EngineSetting.ARPG_TOOL_COMMAND_DECODE -> Lwjgl3ArpgUtility.decode(source, target);
            case EngineSetting.ARPG_TOOL_COMMAND_CONVERT -> Lwjgl3ArpgUtility.convert(source);
            case EngineSetting.ARPG_TOOL_COMMAND_EXPORT -> Lwjgl3ArpgUtility.export(source);
            case EngineSetting.ARPG_TOOL_COMMAND_VERIFY -> Lwjgl3ArpgUtility.verify(source);
            default -> false;
        };
    }

    private static boolean isKnownCommand(String command) {
        return command.equals(EngineSetting.ARPG_TOOL_COMMAND_ENCODE)
                || command.equals(EngineSetting.ARPG_TOOL_COMMAND_DECODE)
                || command.equals(EngineSetting.ARPG_TOOL_COMMAND_CONVERT)
                || command.equals(EngineSetting.ARPG_TOOL_COMMAND_EXPORT)
                || command.equals(EngineSetting.ARPG_TOOL_COMMAND_VERIFY);
    }
}
