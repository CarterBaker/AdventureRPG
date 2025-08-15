package com.AdventureRPG.WorldSystem.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// TODO: AI largely made most of this. It needs intense scrutiny when I have timeS
public class BlockAtlas {

    // Debug
    private final boolean debug = false; // TODO: Remove debug line

    // Atlas
    private Texture texture;

    // Retrieval
    private Map<Integer, TextureRegion> idToRegion = new HashMap<>();
    private Map<String, Integer> nameToId = new HashMap<>();

    // Base

    public BlockAtlas(String folderPath, int tileSize, int padding) {

        FileHandle folder = Gdx.files.internal(folderPath);
        FileHandle[] files = folder.list((dir, name) -> name.endsWith(".png"));

        int estimatedSize = (int) Math.ceil(Math.sqrt(files.length)) * (tileSize + padding);

        PixmapPacker packer = new PixmapPacker(estimatedSize, estimatedSize, Pixmap.Format.RGBA8888, padding, false);

        AtomicInteger nextId = new AtomicInteger(0);

        for (FileHandle file : files) {

            try {
                String name = file.nameWithoutExtension();

                // Assign a unique ID
                int id = nextId.getAndIncrement();
                nameToId.put(name, id);

                Pixmap pixmap = new Pixmap(file);
                packer.pack(name, pixmap);
                pixmap.dispose();
            }

            catch (Exception e) {
                Gdx.app.error("BlockAtlas", "Failed to load: " + file.name(), e);
            }
        }

        if (debug) // TODO: Remove debug line
            debugSaveAtlasPages(packer);

        TextureAtlas atlas = packer.generateTextureAtlas(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest,
                false);
        this.texture = atlas.getTextures().first();

        // Map ID to region using nameToId
        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            Integer id = nameToId.get(region.name);
            if (id != null) {
                idToRegion.put(id, region);
            }
        }

        if (debug) // TODO: Remove debug line
            debugPrintNames();
    }

    // Get TextureRegion by ID
    public TextureRegion getRegion(int id) {
        return idToRegion.get(id);
    }

    // Get UV coords by ID
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

    // Get the ID for an image by name
    public Integer getIdByName(String name) {

        if (!nameToId.containsKey(name))
            return -1;

        return nameToId.get(name);
    }

    // Get the Texture
    public Texture getTexture() {
        return texture;
    }

    // Debug: Save the atlas to disk
    public void debugSaveAtlasPages(PixmapPacker packer) { // TODO: Remove debug line
        FileHandle outputDir = Gdx.files.local("packedAtlasDebug");
        if (!outputDir.exists())
            outputDir.mkdirs();

        int pageIndex = 0;
        for (PixmapPacker.Page page : packer.getPages()) {
            Pixmap pixmap = page.getPixmap();
            FileHandle outputFile = outputDir.child("atlasPage_" + pageIndex + ".png");
            PixmapIO.writePNG(outputFile, pixmap);
            pageIndex++;
        }
        Gdx.app.log("BlockAtlas", "Saved " + pageIndex + " atlas pages to " + outputDir.path());
    }

    public void debugPrintNames() { // TODO: Remove debug line
        Gdx.app.log("BlockAtlas", "=== nameToId contents ===");
        for (Map.Entry<String, Integer> entry : nameToId.entrySet()) {
            Gdx.app.log("BlockAtlas", "Name: " + entry.getKey() + ", ID: " + entry.getValue());
        }
        Gdx.app.log("BlockAtlas", "========================");
    }
}
