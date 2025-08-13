package com.AdventureRPG.WorldSystem.Blocks;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BlockAtlas {

    // Debug
    private final boolean debug = false;

    private Texture texture;
    private Map<Integer, TextureRegion> regionMap = new HashMap<>();

    public BlockAtlas(String folderPath, int tileSize, int padding) {
        FileHandle folder = Gdx.files.internal(folderPath);
        FileHandle[] files = folder.list((dir, name) -> name.endsWith(".png"));

        int estimatedSize = (int) Math.ceil(Math.sqrt(files.length)) * (tileSize + padding);

        PixmapPacker packer = new PixmapPacker(estimatedSize, estimatedSize, Pixmap.Format.RGBA8888, padding, false);

        for (FileHandle file : files) {

            try {

                String name = file.nameWithoutExtension();
                Pixmap pixmap = new Pixmap(file);
                packer.pack(name, pixmap);
                pixmap.dispose();
            }

            catch (Exception e) {
                Gdx.app.error("BlockAtlas", "Failed to load: " + file.name(), e);
            }
        }

        if (debug)
            debugSaveAtlasPages(packer);

        TextureAtlas atlas = packer.generateTextureAtlas(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest,
                false);
        this.texture = atlas.getTextures().first();

        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            int regionId = Integer.parseInt(region.name);
            regionMap.put(regionId, region);
        }
    }

    public TextureRegion getRegion(int id) {
        return regionMap.get(id);
    }

    public float[] getUV(int id) {
        TextureRegion region = getRegion(id);
        if (region == null)
            return new float[8];
        return new float[] {
                region.getU(), region.getV2(),
                region.getU2(), region.getV2(),
                region.getU2(), region.getV(),
                region.getU(), region.getV()
        };
    }

    public Texture getTexture() {
        return texture;
    }

    public void debugSaveAtlasPages(PixmapPacker packer) {
        FileHandle outputDir = Gdx.files.local("packedAtlasDebug");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int pageIndex = 0;
        for (PixmapPacker.Page page : packer.getPages()) {
            Pixmap pixmap = page.getPixmap();
            FileHandle outputFile = outputDir.child("atlasPage_" + pageIndex + ".png");
            PixmapIO.writePNG(outputFile, pixmap);
            pageIndex++;
        }

        Gdx.app.log("BlockAtlas", "Saved " + pageIndex + " atlas pages to " + outputDir.path());
    }
}
