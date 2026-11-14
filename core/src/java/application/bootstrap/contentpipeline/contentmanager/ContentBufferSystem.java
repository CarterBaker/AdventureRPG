package application.bootstrap.contentpipeline.contentmanager;

import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ContentBufferSystem extends SystemPackage {

    /*
     * Seeds the ContentData UBO with every content's overlay tile, indexed by
     * content ID, so a terrain quad carries only the ID in its meta word and
     * the surface shader looks the rest up. Each tile row holds the overlay
     * tile's atlas origin; each reveal row the average color the tile shows
     * and, in its last component, the share of the tile it shows, which
     * distant faces approximate the content with. The reserved ID and any
     * unused slot stay empty.
     */

    // Internal
    private UBOManager uboManager;
    private TextureManager textureManager;

    // Table
    private Vector4[] contentTiles;
    private Vector4[] contentReveal;

    // Base \\

    @Override
    protected void create() {

        // Table
        this.contentTiles = new Vector4[EngineSetting.CONTENT_ID_COUNT];
        this.contentReveal = new Vector4[EngineSetting.CONTENT_ID_COUNT];

        for (int i = 0; i < EngineSetting.CONTENT_ID_COUNT; i++) {
            contentTiles[i] = new Vector4();
            contentReveal[i] = new Vector4();
        }
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Content Table \\

    void pushContentTable(ObjectArrayList<ContentHandle> contentID2ContentHandle) {

        for (int contentID = 0; contentID < contentID2ContentHandle.size(); contentID++) {

            ContentHandle contentHandle = contentID2ContentHandle.get(contentID);

            if (contentHandle != null)
                writeContent(contentID, contentHandle);
        }

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.CONTENT_DATA_UBO);

        ubo.updateUniform(EngineSetting.UNIFORM_CONTENT_TILES, contentTiles);
        ubo.updateUniform(EngineSetting.UNIFORM_CONTENT_REVEAL, contentReveal);
        uboManager.push(ubo);
    }

    private void writeContent(int contentID, ContentHandle contentHandle) {

        TextureHandle texture = textureManager.getTextureHandleFromTileID(contentHandle.getTileID());
        Vector4 tiles = contentTiles[contentID];
        Vector4 reveal = contentReveal[contentID];
        int revealColor = contentHandle.getReveal().getRevealColor();

        tiles.x = texture.getU0();
        tiles.y = texture.getV0();

        reveal.x = PackedColorUtility.red(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        reveal.y = PackedColorUtility.green(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        reveal.z = PackedColorUtility.blue(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        reveal.w = contentHandle.getRevealShare();
    }
}
