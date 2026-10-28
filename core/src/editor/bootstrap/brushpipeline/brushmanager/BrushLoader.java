package editor.bootstrap.brushpipeline.brushmanager;

import java.io.File;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BrushLoader extends LoaderPackage {

    /*
     * Scans the brush directory and builds every preset into BrushManager,
     * one preset to a file, named by its path. The manager requests them all
     * at once before any tool strokes, and hands every live edit of a preset
     * back here to be built the same way.
     */

    // Internal
    private File root;
    private BrushManager brushManager;
    private BrushBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EditorSetting.BRUSH_PATH);

        FileUtility.verifyDirectory(root, "Brush directory not found: " + root.getAbsolutePath());

        ObjectArrayList<File> brushFiles = FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS);

        for (int i = 0; i < brushFiles.size(); i++)
            queueFile(brushFiles.get(i));
    }

    @Override
    protected void create() {
        this.internalBuilder = create(BrushBuilder.class);
    }

    @Override
    protected void get() {
        this.brushManager = get(BrushManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        build(FileUtility.getPathWithFileNameWithoutExtension(root, file), ArpgUtility.loadObject(file));
    }

    void build(String brushName, ArpgObjectStruct brushArpg) {
        brushManager.addBrushData(internalBuilder.build(brushName, brushArpg));
    }
}
