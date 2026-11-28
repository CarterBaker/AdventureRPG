package application.bootstrap.worldpipeline.world;

public enum WorldShape {

    /*
     * How a world's ground joins up. A torus world is one flat block grid that
     * wraps around both edges of its image. A globe world is a ball: its image
     * holds the six faces of a cube map, the globe as painted, and its block
     * grid is a cube of six flat faces turned inside that globe by the world's
     * cube rotation, each place on it built from the globe at its true
     * direction (see GlobeUtility).
     */

    TORUS, // One grid wrapping east to west and north to south
    GLOBE // Six cube faces bent onto a ball, the images a cube map of the globe
}
