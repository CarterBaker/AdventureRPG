package application.bootstrap.dialoguepipeline.replymanager;

import application.bootstrap.dialoguepipeline.reply.ReplyHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ReplyManager extends ManagerPackage {

    /*
     * Owns the reply palette: everything a being can answer with. IDs are assigned
     * in registration order, and a name declared twice is rejected. Every reply is
     * resolved in awake(), so the response model holds one weight per reply ID.
     */

    // Palette
    private Object2IntOpenHashMap<String> replyName2ReplyID;
    private ObjectArrayList<ReplyHandle> replyID2ReplyHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.replyName2ReplyID = RegistryUtility.createNameIndex();
        this.replyID2ReplyHandle = RegistryUtility.createPalette();
        create(ReplyLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    short registerReplyName(String replyName) {
        return (short) RegistryUtility.registerID(
                replyName2ReplyID, replyID2ReplyHandle, replyName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addReplyHandle(ReplyHandle replyHandle) {

        short replyID = replyHandle.getReplyID();

        if (replyID2ReplyHandle.get(replyID) != null)
            throwException("Duplicate reply name: '" + replyHandle.getReplyName()
                    + "' is declared more than once — every reply name must be unique");

        replyID2ReplyHandle.set(replyID, replyHandle);
    }

    // Accessible \\

    public boolean hasReply(String replyName) {
        return RegistryUtility.getHandle(replyName2ReplyID, replyID2ReplyHandle, replyName) != null;
    }

    public short getReplyIDFromReplyName(String replyName) {

        if (!hasReply(replyName) && internalLoader != null)
            ((ReplyLoader) internalLoader).request(replyName);

        if (!hasReply(replyName))
            throwException("Reply not found after load: \"" + replyName + "\"");

        return (short) replyName2ReplyID.getInt(replyName);
    }

    public ReplyHandle getReplyHandleFromReplyID(short replyID) {

        ReplyHandle handle = RegistryUtility.getHandle(replyID2ReplyHandle, replyID);

        if (handle == null)
            throwException("Reply ID not found: " + replyID);

        return handle;
    }

    public ReplyHandle getReplyHandleFromReplyName(String replyName) {
        return getReplyHandleFromReplyID(getReplyIDFromReplyName(replyName));
    }

    // Every reply ID lies below this — the length of an array indexed by reply ID
    public int getReplyIDCount() {
        return replyID2ReplyHandle.size();
    }
}
