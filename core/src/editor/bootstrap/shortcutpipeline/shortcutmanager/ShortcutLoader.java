package editor.bootstrap.shortcutpipeline.shortcutmanager;

import java.io.File;

import editor.bootstrap.shortcutpipeline.shortcut.ShortcutHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShortcutLoader extends LoaderPackage {

    /*
     * Scans the shortcut directory and builds every shortcut file into
     * ShortcutManager. Each file holds one tool's shortcuts and is named by
     * its path, which qualifies the name of every shortcut it declares. The
     * manager requests them all at once before any tool opens, so every tool
     * has its keys from its first frame.
     */

    // Internal
    private File root;
    private ShortcutManager shortcutManager;
    private ShortcutBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.SHORTCUT_PATH);

        FileUtility.verifyDirectory(root, "Shortcut directory not found: " + root.getAbsolutePath());

        ObjectArrayList<File> shortcutFiles = FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS);

        for (int i = 0; i < shortcutFiles.size(); i++)
            queueFile(shortcutFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ShortcutBuilder.class);
    }

    @Override
    protected void get() {
        this.shortcutManager = get(ShortcutManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String setName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ObjectArrayList<ShortcutHandle> shortcutHandles = internalBuilder.build(file, setName);

        for (int i = 0; i < shortcutHandles.size(); i++)
            shortcutManager.addShortcutHandle(shortcutHandles.get(i));
    }
}
