package application.bootstrap.contentpipeline.contentmanager;

import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ContentBufferSystem extends SystemPackage {

    /*
     * Seeds the ContentData UBO with every content's overlay tile, indexed by
     * content ID, so a terrain quad carries only the ID in its meta word and
     * the surface shader looks the rest up. Each tile row holds the overlay
     * tile's ID and the most a cell holds; each reveal row the average color
     * the tile shows, and each shares row the share of the tile it shows at
     * every coverage level, packed as coverings pack theirs, which distant
     * faces approximate the content with at the level its amount reaches.
     * The reserved ID and any unused slot stay empty.
     */

    // Internal
    private UBOManager uboManager;
    private TextureManager textureManager;

    // Table
    private Vector4[] contentTiles;
    private Vector4[] contentReveal;
    private Vector4Int[] contentRevealShares;

    // Base \\

    @Override
    protected void create() {

        // Table
        this.contentTiles = new Vector4[EngineSetting.CONTENT_ID_COUNT];
        this.contentReveal = new Vector4[EngineSetting.CONTENT_ID_COUNT];
        this.contentRevealShares = new Vector4Int[EngineSetting.CONTENT_ID_COUNT];

        for (int i = 0; i < EngineSetting.CONTENT_ID_COUNT; i++) {
            contentTiles[i] = new Vector4();
            contentReveal[i] = new Vector4();
            contentRevealShares[i] = new Vector4Int();
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
        ubo.updateUniform(EngineSetting.UNIFORM_CONTENT_REVEAL_SHARES, contentRevealShares);
        uboManager.push(ubo);
    }

    private void writeContent(int contentID, ContentHandle contentHandle) {

        TextureHandle texture = textureManager.getTextureHandleFromTileID(contentHandle.getTileID());
        Vector4 tiles = contentTiles[contentID];
        Vector4 reveal = contentReveal[contentID];
        TextureRevealStruct revealStruct = contentHandle.getReveal();
        int revealColor = revealStruct.getRevealColor();

        tiles.x = texture.getTileID();
        tiles.y = contentHandle.getMaxAmount();

        reveal.x = PackedColorUtility.red(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        reveal.y = PackedColorUtility.green(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        reveal.z = PackedColorUtility.blue(revealColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;

        contentRevealShares[contentID].set(
                CoverageUtility.packRevealShares(revealStruct, 0),
                CoverageUtility.packRevealShares(revealStruct, 1),
                CoverageUtility.packRevealShares(revealStruct, 2),
                CoverageUtility.packRevealShares(revealStruct, 3));
    }
}
