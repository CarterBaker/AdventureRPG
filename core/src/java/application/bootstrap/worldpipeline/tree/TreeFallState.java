package application.bootstrap.worldpipeline.tree;

public enum TreeFallState {

    /*
     * Where a felled piece of tree is in its fall. Its geometry is built away
     * from the main thread first, while the tree still stands whole on
     * screen, and the stump it leaves is drawn after it; then it falls, lies still for a moment where it landed, and
     * breaks into the logs and seeds it drops.
     */

    BUILDING, // The piece's geometry is being built; the standing tree is still drawn whole
    WAITING, // The piece is built and the stump it leaves is being drawn; the standing tree is still drawn whole
    FALLING, // Toppling about its cut, or dropping straight down
    RESTING, // Lying where it landed before it breaks apart
    DONE // Broken into its drops and gone
}
