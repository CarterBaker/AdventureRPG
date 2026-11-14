package application.bootstrap.worldpipeline.block;

import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.util.mathematics.extras.Direction3Vector;

public enum SubBlockShape {

    /*
     * Named sub-block masks content can build partial blocks from instead of
     * spelling out octant bits: slabs, halves, stairs that rise toward a
     * side, quarter posts standing in a corner and ledges along one side.
     * Every mask is composed through SubBlockUtility, so the shapes share its
     * one definition of which bit is which octant.
     */

    FULL(SubBlockUtility.MASK_FULL),
    SLAB_BOTTOM(SubBlockUtility.layerMask(0)),
    SLAB_TOP(SubBlockUtility.layerMask(1)),
    HALF_NORTH(SubBlockUtility.sideMask(Direction3Vector.NORTH)),
    HALF_EAST(SubBlockUtility.sideMask(Direction3Vector.EAST)),
    HALF_SOUTH(SubBlockUtility.sideMask(Direction3Vector.SOUTH)),
    HALF_WEST(SubBlockUtility.sideMask(Direction3Vector.WEST)),
    STAIR_NORTH(SubBlockUtility.layerMask(0) | SubBlockUtility.sideMask(Direction3Vector.NORTH)),
    STAIR_EAST(SubBlockUtility.layerMask(0) | SubBlockUtility.sideMask(Direction3Vector.EAST)),
    STAIR_SOUTH(SubBlockUtility.layerMask(0) | SubBlockUtility.sideMask(Direction3Vector.SOUTH)),
    STAIR_WEST(SubBlockUtility.layerMask(0) | SubBlockUtility.sideMask(Direction3Vector.WEST)),
    POST_NORTH_EAST(SubBlockUtility.sideMask(Direction3Vector.NORTH) & SubBlockUtility.sideMask(Direction3Vector.EAST)),
    POST_NORTH_WEST(SubBlockUtility.sideMask(Direction3Vector.NORTH) & SubBlockUtility.sideMask(Direction3Vector.WEST)),
    POST_SOUTH_EAST(SubBlockUtility.sideMask(Direction3Vector.SOUTH) & SubBlockUtility.sideMask(Direction3Vector.EAST)),
    POST_SOUTH_WEST(SubBlockUtility.sideMask(Direction3Vector.SOUTH) & SubBlockUtility.sideMask(Direction3Vector.WEST)),
    LEDGE_NORTH(SubBlockUtility.layerMask(0) & SubBlockUtility.sideMask(Direction3Vector.NORTH)),
    LEDGE_EAST(SubBlockUtility.layerMask(0) & SubBlockUtility.sideMask(Direction3Vector.EAST)),
    LEDGE_SOUTH(SubBlockUtility.layerMask(0) & SubBlockUtility.sideMask(Direction3Vector.SOUTH)),
    LEDGE_WEST(SubBlockUtility.layerMask(0) & SubBlockUtility.sideMask(Direction3Vector.WEST));

    // Internal
    private final int mask;

    // Constructor \\

    SubBlockShape(int mask) {
        this.mask = mask;
    }

    // Accessible \\

    public int getMask() {
        return mask;
    }
}
