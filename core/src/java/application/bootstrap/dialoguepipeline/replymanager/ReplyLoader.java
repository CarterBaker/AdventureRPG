package application.bootstrap.dialoguepipeline.replymanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.reply.ReplyHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ReplyLoader extends LoaderPackage {

    /*
     * Scans the reply ARPG directory and loads every reply into ReplyManager. Each
     * reply's name is mapped to its file during scan, so a name asked for before
     * the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private ReplyManager replyManager;
    private ReplyBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> replyName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.REPLY_PATH);
        this.replyName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Reply directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ReplyBuilder.class);
    }

    @Override
    protected void get() {
        this.replyManager = get(ReplyManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct replyArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "replies");

        for (int i = 0; i < replyArray.size(); i++) {
            ArpgObjectStruct replyArpg = replyArray.get(i).getAsObject();
            replyName2File.put(ArpgUtility.validateString(replyArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<ReplyHandle> replies = internalBuilder.build(file);

        for (int i = 0; i < replies.size(); i++)
            replyManager.addReplyHandle(replies.get(i));
    }

    // On-Demand \\

    void request(String replyName) {

        File file = replyName2File.get(replyName);

        if (file == null)
            throwException("On-demand reply load failed — no file found for: \"" + replyName + "\"");

        request(file);
    }
}
