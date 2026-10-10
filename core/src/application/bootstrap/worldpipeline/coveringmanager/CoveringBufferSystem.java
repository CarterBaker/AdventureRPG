package application.bootstrap.worldpipeline.coveringmanager;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import engine.util.mathematics.vectors.Vector4Int;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CoveringBufferSystem extends SystemPackage {

    /*
     * Seeds the CoveringData UBO with every covering's tiles, indexed by
     * covering ID, so a terrain vertex carries only its packed coverage and
     * the surface shader looks the rest up. Each tile row holds the top
     * tile's atlas origin and the side tile's, or COVERING_TILE_NONE where
     * the covering has no sides; each style row holds its tint strength.
     * Each covering face, top then side, also gets its reveal: the average
     * color it shows at its full level, whole and tintable, and the share of
     * its tile shown at every level packed a byte per level, which distant
     * faces approximate the covering with. The reserved ID, a sideless
     * covering's side face and any unused slot stay bare.
     */

    // Internal
    private UBOManager uboManager;
    private TextureManager textureManager;

    // Table
    private Vector4[] coveringTiles;
    private Vector4[] coveringStyle;

    // Reveal
    private Vector4[] coveringRevealColor;
    private Vector4[] coveringRevealTintable;
    private Vector4Int[] coveringRevealShares;

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

        // Reveal
        this.coveringRevealColor = new Vector4[EngineSetting.COVERING_FACE_COUNT];
        this.coveringRevealTintable = new Vector4[EngineSetting.COVERING_FACE_COUNT];
        this.coveringRevealShares = new Vector4Int[EngineSetting.COVERING_FACE_COUNT];

        for (int i = 0; i < EngineSetting.COVERING_FACE_COUNT; i++) {
            coveringRevealColor[i] = new Vector4();
            coveringRevealTintable[i] = new Vector4();
            coveringRevealShares[i] = new Vector4Int();
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
        ubo.updateUniform(EngineSetting.UNIFORM_COVERING_REVEAL_COLOR, coveringRevealColor);
        ubo.updateUniform(EngineSetting.UNIFORM_COVERING_REVEAL_TINTABLE, coveringRevealTintable);
        ubo.updateUniform(EngineSetting.UNIFORM_COVERING_REVEAL_SHARES, coveringRevealShares);
        uboManager.push(ubo);
    }

    private void writeCovering(int coveringID, CoveringHandle coveringHandle) {

        TextureHandle topTexture = textureManager.getTextureHandleFromTileID(coveringHandle.getTopTileID());
        Vector4 tiles = coveringTiles[coveringID];
        int faceID = coveringID * EngineSetting.COVERING_FACES_PER_COVERING;

        tiles.x = topTexture.getU0();
        tiles.y = topTexture.getV0();
        writeReveal(faceID, coveringHandle.getTopReveal());

        if (coveringHandle.hasSide()) {
            TextureHandle sideTexture = textureManager.getTextureHandleFromTileID(coveringHandle.getSideTileID());
            tiles.z = sideTexture.getU0();
            tiles.w = sideTexture.getV0();
            writeReveal(faceID + 1, coveringHandle.getSideReveal());
        }

        coveringStyle[coveringID].x = coveringHandle.getTintStrength();
    }

    // Reveal \\

    private void writeReveal(int faceID, TextureRevealStruct reveal) {

        writeColor(coveringRevealColor[faceID], reveal.getRevealColor());
        writeColor(coveringRevealTintable[faceID], reveal.getTintableColor());

        Vector4Int shares = coveringRevealShares[faceID];
        shares.set(packShares(reveal, 0), packShares(reveal, 1), packShares(reveal, 2), packShares(reveal, 3));
    }

    private void writeColor(Vector4 target, int packedColor) {
        target.x = PackedColorUtility.red(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        target.y = PackedColorUtility.green(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        target.z = PackedColorUtility.blue(packedColor) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
    }

    // One component of the shares row — four consecutive levels, a byte each, the lowest level in the low byte
    private int packShares(TextureRevealStruct reveal, int component) {

        int word = 0;
        int firstLevel = component * EngineSetting.COVERAGE_SHARES_PER_WORD;

        for (int i = 0; i < EngineSetting.COVERAGE_SHARES_PER_WORD; i++) {

            int level = firstLevel + i;

            if (level > EngineSetting.COVERAGE_LEVEL_MAX)
                break;

            int share = Math.round(reveal.getLevelShare(level) * EngineSetting.COVERAGE_SHARE_MAX);
            word |= share << (i * EngineSetting.COVERAGE_SHARE_BITS);
        }

        return word;
    }
}
