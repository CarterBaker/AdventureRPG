package application.bootstrap.worldpipeline.tree;

public enum TreeStandIn {

    /*
     * How finely macro terrain shapes the trees it stands in one by one, in
     * the flat colors their bark and leaves show from afar, coarser the
     * farther its tile lies. Every shape is faceted round, never a box, and
     * stands on the crown its form grows, so a tree keeps its bulk from one
     * tier to the next.
     */

    FACETED, // Each crown lump a many-sided faceted dome or cone over a tapered four-sided trunk
    PLAIN, // Each crown lump a four-sided diamond or spire over a tapered three-sided trunk
    SPIRE // The whole crown one four-sided diamond or spire whose foot reaches down to the root
}
