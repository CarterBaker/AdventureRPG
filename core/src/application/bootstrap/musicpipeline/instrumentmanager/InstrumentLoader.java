package application.bootstrap.musicpipeline.instrumentmanager;

import java.io.File;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InstrumentLoader extends LoaderPackage {

    /*
     * Scans the instrument ARPG directory and remembers every file by name,
     * queueing none: an instrument loads only when something names it — an
     * item while the items load, a song, a tool — so the loader persists for
     * the engine lifetime.
     */

    // Internal
    private File root;
    private InstrumentManager instrumentManager;
    private InstrumentBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> instrumentName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.INSTRUMENT_PATH);
        this.instrumentName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Instrument ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            instrumentName2File.put(FileUtility.getPathWithFileNameWithoutExtension(root, file), file);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InstrumentBuilder.class);
    }

    @Override
    protected void get() {
        this.instrumentManager = get(InstrumentManager.class);
    }

    // Kept for the engine lifetime, so an instrument loads whenever it is first named
    @Override
    protected boolean persists() {
        return true;
    }

    // Load \\

    @Override
    protected void load(File file) {

        String instrumentName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        InstrumentHandle handle = internalBuilder.build(file, instrumentName);

        if (handle == null)
            throwException("Failed to build instrument from: " + file.getAbsolutePath());

        instrumentManager.addInstrument(handle);
    }

    // On-Demand \\

    void request(String instrumentName) {

        File file = instrumentName2File.get(instrumentName);

        if (file == null)
            throwException("On-demand instrument load failed — not found in scan registry: \""
                    + instrumentName + "\"");

        request(file);
    }
}
