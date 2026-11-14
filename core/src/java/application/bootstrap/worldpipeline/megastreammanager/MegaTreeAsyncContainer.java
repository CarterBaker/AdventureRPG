package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.tree.TreeModelStruct;
import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class MegaTreeAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one mega's tree stand-ins: the bark and leaf
     * vertices every tree rooted in the mega adds to, and the model scratch
     * each tree is drawn through. Kept warm between builds, so a build
     * allocates nothing once its lists have grown.
     */

    // Vertices
    FloatArrayList bark;
    FloatArrayList leaves;

    // Model
    TreeModelStruct model;

    @Override
    protected void create() {
        this.bark = new FloatArrayList();
        this.leaves = new FloatArrayList();
        this.model = new TreeModelStruct();
    }

    @Override
    public void reset() {
        bark.clear();
        leaves.clear();
    }
}
