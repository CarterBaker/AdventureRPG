package application.bootstrap.worldpipeline.coveringmanager;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CoveringBufferSystem extends SystemPackage {

    /*
     * Seeds the CoveringData UBO with every covering's tiles, indexed by
     * covering ID, so a terrain vertex carries only its packed coverage and
     * the surface shader looks the rest up. Each tile row holds the top
     * tile's atlas origin and the side tile's, or COVERING_TILE_NONE where
     * the covering has no sides; each style row holds its tint strength.
     * The reserved ID and any unused slot stay bare.
     */

    // Internal
    private UBOManager uboManager;
    private TextureManager textureManager;

    // Table
    private Vector4[] coveringTiles;
    private Vector4[] coveringStyle;

    // Base \\

    @Override
    protected void create() {

        // Table
        this.coveringTiles = new Vector4[EngineSetting.COVERING_ID_COUNT];
        this.coveringStyle = new Vector4[EngineSetting.COVERING_ID_COUNT];

        for (int i = 0; i < EngineSetting.COVERING_ID_COUNT; i++) {
            coveringTiles[i] = new Vector4(
                    EngineSetting.COVERING_TILE_NONE, EngineSetting.COVERING_TILE_NONE,
                    EngineSetting.COVERING_TILE_NONE, EngineSetting.COVERING_TILE_NONE);
            coveringStyle[i] = new Vector4();
        }
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Covering Table \\

    void pushCoveringTable(ObjectArrayList<CoveringHandle> coveringID2CoveringHandle) {

        for (int coveringID = 0; coveringID < coveringID2CoveringHandle.size(); coveringID++) {

            CoveringHandle coveringHandle = coveringID2CoveringHandle.get(coveringID);

            if (coveringHandle != null)
                writeCovering(coveringID, coveringHandle);
        }

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.COVERING_DATA_UBO);

        ubo.updateUniform(EngineSetting.UNIFORM_COVERING_TILES, coveringTiles);
        ubo.updateUniform(EngineSetting.UNIFORM_COVERING_STYLE, coveringStyle);
        uboManager.push(ubo);
    }

    private void writeCovering(int coveringID, CoveringHandle coveringHandle) {

        TextureHandle topTexture = textureManager.getTextureHandleFromTileID(coveringHandle.getTopTileID());
        Vector4 tiles = coveringTiles[coveringID];

        tiles.x = topTexture.getU0();
        tiles.y = topTexture.getV0();

        if (coveringHandle.hasSide()) {
            TextureHandle sideTexture = textureManager.getTextureHandleFromTileID(coveringHandle.getSideTileID());
            tiles.z = sideTexture.getU0();
            tiles.w = sideTexture.getV0();
        }

        coveringStyle[coveringID].x = coveringHandle.getTintStrength();
    }
}
