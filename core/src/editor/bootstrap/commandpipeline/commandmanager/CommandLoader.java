package editor.bootstrap.commandpipeline.commandmanager;

import java.io.File;

import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CommandLoader extends LoaderPackage {

    /*
     * Scans the command directory and builds every command file into
     * CommandManager. Each file is one group of the command console's tree,
     * named by its path. The manager requests them all at once before any
     * command console opens, so every one lists every command.
     */

    // Internal
    private File root;
    private CommandManager commandManager;
    private CommandBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.COMMAND_JSON_PATH);

        FileUtility.verifyDirectory(root, "Command directory not found: " + root.getAbsolutePath());

        ObjectArrayList<File> commandFiles = FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < commandFiles.size(); i++)
            queueFile(commandFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(CommandBuilder.class);
    }

    @Override
    protected void get() {
        this.commandManager = get(CommandManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String groupName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ObjectArrayList<CommandHandle> commandHandles = internalBuilder.build(file, groupName);

        for (int i = 0; i < commandHandles.size(); i++)
            commandManager.addCommandHandle(commandHandles.get(i));
    }

    // On-Demand \\
}
