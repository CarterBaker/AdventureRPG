package application.bootstrap.worldpipeline.tree;

public enum TreeDetail {

    /*
     * How finely a tree is drawn in bark and leaf cards, from the grown tree
     * near the player to the plainest one a far mega keeps. Each tier is a
     * coarser look at the same tree, so it keeps its place, size and crown
     * as it passes from one to the next, and every tier draws with the same
     * bark and leaf materials.
     */

    FULL, // The grown sub-voxel tree and every leaf cluster, while its root lies within the tree detail distance
    MODEL, // Its own trunk and thick limbs as boxes of bark under its own crown merged into fewer clusters
    SKETCH, // Its own trunk under a coarser crown of its clusters, for the megas nearest the player
    OUTLINE // A trunk under its form's crown, sized from its mature height and age alone, for the farthest megas
}
