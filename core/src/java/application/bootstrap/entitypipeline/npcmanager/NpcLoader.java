package application.bootstrap.entitypipeline.npcmanager;

import java.io.File;

import application.bootstrap.entitypipeline.npc.NpcHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class NpcLoader extends LoaderPackage {

    /*
     * Scans the NPC ARPG directory and loads every NPC definition into
     * NpcManager, one definition per file, named by its path below the
     * directory. Supports on-demand loading by NPC name, and requestAll() so
     * NpcManager completes its palette before any structure is lived in.
     */

    // Internal
    private File root;
    private NpcManager npcManager;
    private NpcBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> npcName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(NpcBuilder.class);
    }

    @Override
    protected void get() {
        this.npcManager = get(NpcManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.NPC_PATH);
        this.npcName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "NPC root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String npcName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            npcName2File.put(npcName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String npcName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        NpcHandle npcHandle = internalBuilder.build(file, npcName);

        npcManager.addNpcHandle(npcHandle);
    }

    // On-Demand \\

    void request(String npcName) {

        if (npcManager.hasNpc(npcName))
            return;

        File file = npcName2File.get(npcName);

        if (file == null)
            throwException("On-demand NPC load failed — no file found for: \"" + npcName + "\"");

        request(file);
    }
}
