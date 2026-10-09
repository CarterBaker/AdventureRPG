package application.bootstrap.worldpipeline.tree;

import engine.root.HandlePackage;

public class TreePaletteHandle extends HandlePackage {

    /*
     * The trees that can reach one chunk, held for as long as the chunk is
     * loaded. The streaming thread publishes the whole set as the chunk
     * generates, and the main thread adds a tree planted into its reach, each
     * time as a fresh array, so chunk builds and space queries on any thread
     * read it without a lock.
     */

    private static final TreeInstance[] NONE = new TreeInstance[0];

    // Trees
    private volatile TreeInstance[] trees;

    // Constructor \\

    public void constructor() {
        this.trees = NONE;
    }

    // Management \\

    public void set(TreeInstance[] trees) {
        this.trees = trees;
    }

    // Main thread — one more tree, unless the chunk already holds it
    public boolean add(TreeInstance tree) {

        if (contains(tree))
            return false;

        TreeInstance[] current = trees;
        TreeInstance[] grown = new TreeInstance[current.length + 1];

        System.arraycopy(current, 0, grown, 0, current.length);
        grown[current.length] = tree;

        this.trees = grown;

        return true;
    }

    // Empties the palette, handing back what it held so the caller can release it
    public TreeInstance[] clear() {

        TreeInstance[] released = trees;

        this.trees = NONE;

        return released;
    }

    // Accessible \\

    public boolean contains(TreeInstance tree) {

        TreeInstance[] current = trees;

        for (int i = 0; i < current.length; i++)
            if (current[i] == tree)
                return true;

        return false;
    }

    public TreeInstance[] getTrees() {
        return trees;
    }

    public boolean isEmpty() {
        return trees.length == 0;
    }
}
