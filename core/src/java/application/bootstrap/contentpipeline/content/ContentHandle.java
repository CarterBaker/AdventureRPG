package application.bootstrap.contentpipeline.content;

import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import engine.root.HandlePackage;

public class ContentHandle extends HandlePackage {

    /*
     * Persistent content definition record. Wraps ContentData and delegates
     * all access through it. Registered in ContentManager from bootstrap to
     * shutdown.
     */

    // Internal
    private ContentData contentData;

    // Constructor \\

    public void constructor(ContentData contentData) {
        this.contentData = contentData;
    }

    // Accessible \\

    public ContentData getContentData() {
        return contentData;
    }

    public String getContentName() {
        return contentData.getContentName();
    }

    public short getContentID() {
        return contentData.getContentID();
    }

    public String getDisplayName() {
        return contentData.getDisplayName();
    }

    public boolean canHost(short blockID) {
        return contentData.canHost(blockID);
    }

    public int getTileID() {
        return contentData.getTileID();
    }

    public TextureRevealStruct getReveal() {
        return contentData.getReveal();
    }

    public float getRevealShare() {
        return contentData.getRevealShare();
    }

    public int getBreakTier() {
        return contentData.getBreakTier();
    }

    public int getDurability() {
        return contentData.getDurability();
    }

    public String getItemTextureName() {
        return contentData.getItemTextureName();
    }

    public boolean hasPiece() {
        return contentData.hasPiece();
    }

    public int getYield() {
        return contentData.getYield();
    }
}
