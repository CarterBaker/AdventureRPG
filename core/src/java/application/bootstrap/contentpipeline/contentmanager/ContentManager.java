package application.bootstrap.contentpipeline.contentmanager;

import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.contentpipeline.util.ContentUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ContentManager extends ManagerPackage {

    /*
     * Owns the content palette by name and ID. Contents are the ores, rocks
     * and roots whole blocks hold; a cell's content lives in its subchunk's
     * content palette, never in the block palette, so one block holds any
     * content its hosts allow and no two contents ever share a texture.
     * Every content is loaded in awake(), before any chunk generates, and
     * the table of their overlay tiles is pushed to the surface shader
     * through ContentBufferSystem in the same breath, so the streaming
     * threads only ever read the palette. Content IDs are assigned in
     * registration order and capped at the table's size.
     */

    // Internal
    private ContentBufferSystem contentBufferSystem;

    // Palette
    private Object2IntOpenHashMap<String> contentName2ContentID;
    private ObjectArrayList<ContentHandle> contentID2ContentHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.contentName2ContentID = RegistryUtility.createNameIndex();
        this.contentID2ContentHandle = RegistryUtility.createPalette();

        // Internal
        this.contentBufferSystem = create(ContentBufferSystem.class);

        create(ContentLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
        contentBufferSystem.pushContentTable(contentID2ContentHandle);
    }

    // Management \\

    short registerContentName(String contentName) {
        return (short) RegistryUtility.registerID(
                contentName2ContentID, contentID2ContentHandle, contentName, EngineSetting.CONTENT_ID_COUNT);
    }

    void addContentHandle(ContentHandle contentHandle) {

        short contentID = contentHandle.getContentID();

        if (contentID2ContentHandle.get(contentID) != null)
            throwException("Duplicate content name: '" + contentHandle.getContentName()
                    + "' was registered more than once");

        contentID2ContentHandle.set(contentID, contentHandle);
    }

    // On-Demand \\

    public void request(String contentName) {
        ((ContentLoader) internalLoader).request(contentName);
    }

    // Content \\

    // A named content's ID for a host block, refused naming its owner when the block cannot hold it
    public short resolveContent(String contentName, short hostBlockID, String ownerName) {

        ContentHandle contentHandle = getContentHandleFromContentName(contentName);

        if (!contentHandle.canHost(hostBlockID))
            throwException("\"" + ownerName + "\" sets content \"" + contentName
                    + "\" into a block it does not name among its \"hosts\".");

        return contentHandle.getContentID();
    }

    // A face's color with the content its block holds laid over it by the share of its tile it shows — the same
    // approximation the surface shader draws distant faces with
    public int resolveContentColor(int faceColor, short contentID) {

        ContentHandle contentHandle = getContentHandleFromContent(contentID);

        if (contentHandle == null)
            return faceColor;

        return PackedColorUtility.mix(
                faceColor, contentHandle.getReveal().getRevealColor(), contentHandle.getRevealShare());
    }

    // The content a cell holds, null when it holds none
    public ContentHandle getContentHandleFromContent(short contentID) {

        if (!ContentUtility.isContained(contentID))
            return null;

        return getContentHandleFromContentID(contentID);
    }

    // Accessible \\

    public boolean hasContent(String contentName) {
        return findContentHandle(contentName) != null;
    }

    // A loaded content by name, null when none carries it — never loads, so any thread may ask
    public ContentHandle findContentHandle(String contentName) {
        return RegistryUtility.getHandle(contentName2ContentID, contentID2ContentHandle, contentName);
    }

    public ContentHandle getContentHandleFromContentName(String contentName) {

        ContentHandle handle = findContentHandle(contentName);

        if (handle == null) {
            request(contentName);
            handle = findContentHandle(contentName);
        }

        if (handle == null)
            throwException("Content \"" + contentName
                    + "\" was not registered after its on-demand load completed — "
                    + "check for a resource-name/path mismatch in the content directory.");

        return handle;
    }

    public ContentHandle getContentHandleFromContentID(int contentID) {

        ContentHandle handle = RegistryUtility.getHandle(contentID2ContentHandle, contentID);

        if (handle == null)
            throwException("No handle registered for content ID: " + contentID);

        return handle;
    }

    // Every content ID lies below this — the length of an array indexed by content ID
    public int getContentIDCount() {
        return contentID2ContentHandle.size();
    }
}
