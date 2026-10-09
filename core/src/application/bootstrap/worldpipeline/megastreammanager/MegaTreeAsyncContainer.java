package application.bootstrap.worldpipeline.megastreammanager;

import engine.root.AsyncContainerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class MegaTreeAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one mega's tree stand-ins: the trunk and crown
     * vertices every tree rooted in the mega adds to. Kept warm between
     * builds, so a build allocates nothing once its lists have grown.
     */

    // Vertices
    FloatArrayList bark;
    FloatArrayList leaves;

    @Override
    protected void create() {
        this.bark = new FloatArrayList();
        this.leaves = new FloatArrayList();
    }

    @Override
    public void reset() {
        bark.clear();
        leaves.clear();
    }
}
