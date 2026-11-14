package application.bootstrap.worldpipeline.tree;

public enum TreeDistribution {

    /*
     * How a biome spreads one kind of tree across its ground, named in lower
     * case by biome data. Every pattern is a pure function of the world seed,
     * the tree and the place, so neighbouring chunks agree on every tree.
     */

    SCATTERED, // One chance per cell of the spacing, so trees stand alone and apart
    CLUSTERED, // One chance per cell of the spacing to raise a grove, its trees spread inside the cluster radius
    FIELD // A tree in nearly every cell, thinned into woods and clearings by a slow patch field
}
