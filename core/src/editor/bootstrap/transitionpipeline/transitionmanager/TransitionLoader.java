package editor.bootstrap.transitionpipeline.transitionmanager;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TransitionLoader extends LoaderPackage {

    /*
     * Scans the transition directory and builds every transition into
     * TransitionManager, one transition to a file, named by its path. The
     * manager requests them all at once before any clip is played, and hands
     * every live edit of a transition back here to be built the same way.
     */

    // Internal
    private File root;
    private TransitionManager transitionManager;
    private TransitionBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.TRANSITION_PATH);

        FileUtility.verifyDirectory(root, "Transition directory not found: " + root.getAbsolutePath());

        ObjectArrayList<File> transitionFiles = FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS);

        for (int i = 0; i < transitionFiles.size(); i++)
            queueFile(transitionFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TransitionBuilder.class);
    }

    @Override
    protected void get() {
        this.transitionManager = get(TransitionManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        build(FileUtility.getPathWithFileNameWithoutExtension(root, file), ArpgUtility.loadObject(file));
    }

    void build(String transitionName, ArpgObjectStruct transitionArpg) {
        transitionManager.addTransitionData(internalBuilder.build(transitionName, transitionArpg));
    }
}
