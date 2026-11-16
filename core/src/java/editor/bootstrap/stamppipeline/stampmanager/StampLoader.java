package editor.bootstrap.stamppipeline.stampmanager;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StampLoader extends LoaderPackage {

    /*
     * Scans the stamp directory and builds every stamp into StampManager, one
     * stamp to a file, named by its path. The manager requests them all at
     * once before any part is placed, and hands every live edit of a stamp
     * back here to be built the same way.
     */

    // Internal
    private File root;
    private StampManager stampManager;
    private StampBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.STAMP_PATH);

        FileUtility.verifyDirectory(root, "Stamp directory not found: " + root.getAbsolutePath());

        ObjectArrayList<File> stampFiles = FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS);

        for (int i = 0; i < stampFiles.size(); i++)
            queueFile(stampFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(StampBuilder.class);
    }

    @Override
    protected void get() {
        this.stampManager = get(StampManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        build(FileUtility.getPathWithFileNameWithoutExtension(root, file), ArpgUtility.loadObject(file));
    }

    void build(String stampName, ArpgObjectStruct stampArpg) {
        stampManager.addStampData(internalBuilder.build(stampName, stampArpg));
    }
}
