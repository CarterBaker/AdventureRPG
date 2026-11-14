package application.bootstrap.shaderpipeline.texture;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class TextureTileSetStruct extends StructPackage {

    /*
     * The distinct tile IDs a piece of geometry draws from, gathered as it is
     * built and handed to texture streaming when it reaches the GPU, so the
     * tiles it needs are resident while it is drawn. Geometry draws from few
     * tiles, so a plain list searched in place keeps them distinct; clear()
     * keeps its storage for the next build and release() gives it back.
     */

    // Tiles
    private final IntArrayList tileIDs;

    // Constructor \\

    public TextureTileSetStruct() {
        this.tileIDs = new IntArrayList();
    }

    // Management \\

    public void add(int tileID) {
        if (tileID > 0 && !tileIDs.contains(tileID))
            tileIDs.add(tileID);
    }

    public void addAll(int[] tileIDs) {
        for (int i = 0; i < tileIDs.length; i++)
            add(tileIDs[i]);
    }

    public void addAll(TextureTileSetStruct other) {
        for (int i = 0; i < other.tileIDs.size(); i++)
            add(other.tileIDs.getInt(i));
    }

    public void clear() {
        tileIDs.clear();
    }

    public void release() {
        tileIDs.clear();
        tileIDs.trim();
    }

    // Accessible \\

    public boolean isEmpty() {
        return tileIDs.isEmpty();
    }

    public IntArrayList getTileIDs() {
        return tileIDs;
    }
}
