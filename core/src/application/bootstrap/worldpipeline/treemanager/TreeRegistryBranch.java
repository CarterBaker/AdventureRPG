package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.tree.TreeInstance;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeRegistryBranch extends BranchPackage {

    /*
     * Every tree standing in the loaded world, one instance per species and
     * root column however many chunks it reaches. Each chunk holding a tree
     * counts once; a wild tree leaves the registry when the last of them lets
     * it go, so a wild tree that comes back into reach regrows from its seed.
     * A tree a hand planted stays for the whole session, held or not, so
     * every chunk that later reaches it finds it again. Chunks generate on the
     * streaming threads and are released on the main thread, so every change
     * is made under the registry's lock.
     */

    // Registry — both guarded by key2Tree
    private Long2ObjectOpenHashMap<TreeInstance> key2Tree;
    private ObjectArrayList<TreeInstance> plantedTrees;

    // Base \\

    @Override
    protected void create() {
        this.key2Tree = new Long2ObjectOpenHashMap<>();
        this.plantedTrees = new ObjectArrayList<>();
    }

    // Management \\

    // The tree already standing at a key, held once more — null when no chunk holds it yet
    TreeInstance acquire(long key) {

        synchronized (key2Tree) {

            TreeInstance tree = key2Tree.get(key);

            if (tree != null)
                tree.setReferences(tree.getReferences() + 1);

            return tree;
        }
    }

    // A freshly grown tree held by its first chunk — or, when another chunk registered one at the same key
    // meanwhile, that tree held once more instead
    TreeInstance register(TreeInstance tree) {

        synchronized (key2Tree) {

            TreeInstance existing = key2Tree.get(tree.getRegistryKey());

            if (existing != null) {
                existing.setReferences(existing.getReferences() + 1);
                return existing;
            }

            tree.setReferences(1);
            key2Tree.put(tree.getRegistryKey(), tree);

            return tree;
        }
    }

    // A planted tree held once more by one more chunk
    void hold(TreeInstance tree) {

        synchronized (key2Tree) {
            tree.setReferences(tree.getReferences() + 1);
        }
    }

    // One chunk lets a tree go, a wild tree leaving the registry with its last holder
    void release(TreeInstance tree) {

        synchronized (key2Tree) {

            int references = tree.getReferences() - 1;

            tree.setReferences(references);

            if (references <= 0 && !tree.isPlanted() && key2Tree.get(tree.getRegistryKey()) == tree)
                key2Tree.remove(tree.getRegistryKey());
        }
    }

    // A tree a hand planted, kept for the session — false when a tree of its species already stands at its root
    boolean plant(TreeInstance tree) {

        synchronized (key2Tree) {

            if (key2Tree.containsKey(tree.getRegistryKey()))
                return false;

            tree.setReferences(0);
            key2Tree.put(tree.getRegistryKey(), tree);
            plantedTrees.add(tree);

            return true;
        }
    }

    // Accessible \\

    // Every registered tree copied out, so the caller walks them without holding the lock
    void collectTrees(ObjectArrayList<TreeInstance> out) {

        out.clear();

        synchronized (key2Tree) {
            out.addAll(key2Tree.values());
        }
    }

    // Every planted tree copied out
    void collectPlanted(ObjectArrayList<TreeInstance> out) {

        out.clear();

        synchronized (key2Tree) {
            out.addAll(plantedTrees);
        }
    }
}
