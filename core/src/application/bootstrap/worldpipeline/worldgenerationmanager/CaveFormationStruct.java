package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

class CaveFormationStruct extends StructPackage {

    /*
     * One giant speleothem built of blocks in a tall chamber: a stalagmite
     * cone rising from the floor, a stalactite cone hanging from the
     * ceiling, or both met in a column with a waist. Each cone narrows from
     * its base radius to a point over its reach, and flares straight down
     * or up into the rock for a few blocks so an uneven floor or ceiling
     * never leaves its base hanging. The centre is in chunk coordinates.
     */

    float centerX;
    float centerZ;
    int floorY;
    int ceilingY;
    float stalagmiteReach;
    float stalactiteReach;
    float radius;
    float waist;
    short blockID;

    void set(
            float centerX,
            float centerZ,
            int floorY,
            int ceilingY,
            float stalagmiteReach,
            float stalactiteReach,
            float radius,
            float waist,
            short blockID) {

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.floorY = floorY;
        this.ceilingY = ceilingY;
        this.stalagmiteReach = stalagmiteReach;
        this.stalactiteReach = stalactiteReach;
        this.radius = radius;
        this.waist = waist;
        this.blockID = blockID;
    }

    // Shape \\

    // The radius the formation spreads to at a height, 0 where it does not reach
    float computeRadius(float pointY) {

        float spread = 0f;

        if (stalagmiteReach > 0f)
            spread = computeCone(pointY - (floorY + 1f), stalagmiteReach);

        if (stalactiteReach > 0f)
            spread = Math.max(spread, computeCone(ceilingY - pointY, stalactiteReach));

        if (waist > 0f && pointY > floorY + 1f && pointY < ceilingY)
            spread = Math.max(spread, waist);

        return spread;
    }

    private float computeCone(float fromBase, float reach) {

        if (fromBase < 0f)
            return fromBase >= -EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS ? radius : 0f;

        if (fromBase >= reach)
            return 0f;

        return radius * (float) Math.pow(1f - fromBase / reach, EngineSetting.SPELEOTHEM_GIANT_TAPER_EXPONENT);
    }

    // Bounds \\

    int getLowestY() {
        return floorY + 1 - EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS;
    }

    int getHighestY() {
        return ceilingY - 1 + EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS;
    }
}
