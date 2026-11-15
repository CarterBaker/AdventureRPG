package application.bootstrap.entitypipeline.bonecontrolmanager;

import java.io.File;

import application.bootstrap.entitypipeline.bonecontrol.BoneControlHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class BoneControlLoader extends LoaderPackage {

    /*
     * Scans the bone control ARPG directory and loads the controls of every
     * rig into BoneControlManager at bootstrap, each file named after the rig
     * it serves. A race that loads before the batch reaches its rig's file,
     * as one an NPC asks for while the managers awake, requests that file on
     * demand, so every race finds its rig's controls while it loads.
     */

    // Internal
    private File root;
    private BoneControlManager boneControlManager;
    private BoneControlBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> rigName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BONE_CONTROL_PATH);
        this.rigName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Bone control ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            rigName2File.put(FileUtility.getPathWithFileNameWithoutExtension(root, file), file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(BoneControlBuilder.class);
    }

    @Override
    protected void get() {
        this.boneControlManager = get(BoneControlManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String rigName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        BoneControlHandle handle = internalBuilder.build(ArpgUtility.loadObject(file), rigName);

        boneControlManager.addBoneControlHandle(handle);
    }

    // On-Demand \\

    // A rig's controls file loaded now, ahead of the batch; a rig without a controls file has none to load
    void request(String rigName) {

        File file = rigName2File.get(rigName);

        if (file != null)
            request(file);
    }

    // Update \\

    // A rig's controls as an editor changed them: parsed into its handle in place, or registered when it had none
    void update(BoneControlHandle handle, ArpgObjectStruct boneControlArpg, String rigName) {

        if (handle == null) {
            boneControlManager.addBoneControlHandle(internalBuilder.build(boneControlArpg, rigName));
            return;
        }

        handle.constructor(internalBuilder.parse(boneControlArpg, rigName, handle.getBoneControlID()));
    }
}
