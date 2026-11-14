#include <stdint.h>

#include "application/bootstrap/worldpipeline/worldgenerationmanager/CaveDensityUtility.h"

/*
 * Fills a cave density field in three passes over the points, each a plain
 * loop the auto-vectoriser takes: the tunnels, passages and caverns read from
 * the trilinear lattice, then every lake's chamber and guard in lake order,
 * then the band penalties and the band's solid rock outside it. Every value is
 * the float arithmetic of the Java routine it replaced, step for step, with
 * Java's Math.max and Math.min, so every variant is bit-identical to it.
 */

// Java Arithmetic

// Math.max: NaN wins, and +0 is above -0
static float maxFloat(float a, float b) {
    return a != a ? a : (((a == 0.0f) & (b == 0.0f) & (__builtin_signbitf(a) != 0)) ? b : (a >= b ? a : b));
}

// Math.min: NaN wins, and -0 is below +0
static float minFloat(float a, float b) {
    return a != a ? a : (((a == 0.0f) & (b == 0.0f) & (__builtin_signbitf(b) != 0)) ? b : (a <= b ? a : b));
}

static float clamp01(float value) {
    return maxFloat(0.0f, minFloat(1.0f, value));
}

static float smoothstep(float t) {
    return t <= 0.0f ? 0.0f : (t >= 1.0f ? 1.0f : t * t * (3.0f - 2.0f * t));
}

static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
}

static float length3(float x, float y, float z) {
    return __builtin_sqrtf(x * x + y * y + z * z);
}

// Lattice

static float sampleCorners(const float *lattice, int64_t corner, int64_t strideX, int64_t strideY, int64_t strideZ,
                           float weightX, float weightY, float weightZ) {

    float near = lerp(lerp(lattice[corner], lattice[corner + strideX], weightX),
                      lerp(lattice[corner + strideY], lattice[corner + strideX + strideY], weightX), weightY);
    float far = lerp(lerp(lattice[corner + strideZ], lattice[corner + strideX + strideZ], weightX),
                     lerp(lattice[corner + strideY + strideZ], lattice[corner + strideX + strideY + strideZ], weightX),
                     weightY);

    return lerp(near, far, weightZ);
}

// Caves

static float computeSpaghetti(const CaveDensityArgumentStruct *field, float spaghettiA, float spaghettiB,
                              float radiusField, float tunnels) {

    float minRadius = (float) field->spaghettiMinRadius;
    float maxRadius = (float) field->spaghettiMaxRadius;
    float amountFloor = (float) field->spaghettiAmountFloor;
    float distance = __builtin_sqrtf(spaghettiA * spaghettiA + spaghettiB * spaghettiB) * (float) field->spaghettiScale;
    float swell = clamp01(radiusField * 0.5f + 0.5f);
    float radius = minRadius + (maxRadius - minRadius) * swell;
    float amount = amountFloor + (1.0f - amountFloor) * minFloat(tunnels, 1.0f);
    float spaghetti = radius * amount * smoothstep(swell / (float) field->spaghettiCloseShare) - distance;

    return tunnels <= 0.0f ? (float) field->densitySolid : spaghetti;
}

static float computeNoodle(const CaveDensityArgumentStruct *field, float noodleA, float noodleB, float gate,
                           float radiusField, float noodles) {

    float minRadius = (float) field->noodleMinRadius;
    float maxRadius = (float) field->noodleMaxRadius;
    float threshold = (float) field->noodleGateThreshold - noodles * (float) field->noodleGateRange;
    float opening = smoothstep((gate - threshold) / (float) field->noodleGateEdge);
    float open = noodles <= 0.0f ? 0.0f : opening;
    float distance = __builtin_sqrtf(noodleA * noodleA + noodleB * noodleB) * (float) field->noodleScale;
    float swell = clamp01(radiusField * 0.5f + 0.5f);
    float radius = minRadius + (maxRadius - minRadius) * swell;
    float noodle = radius * open - distance;

    return open <= 0.0f ? (float) field->densitySolid : noodle;
}

static float computeCheese(const CaveDensityArgumentStruct *field, float cheese, float caverns,
                           float depthBelowGroundBlocks) {

    float shallowShare = (float) field->cheeseShallowShare;
    float thresholdMax = (float) field->cheeseThresholdMax;
    float thresholdMin = (float) field->cheeseThresholdMin;
    float deep = clamp01((depthBelowGroundBlocks - (float) field->cheeseMinDepth) / (float) field->cheeseDepthRamp);
    float share = shallowShare + (1.0f - shallowShare) * deep;
    float threshold = thresholdMax - (thresholdMax - thresholdMin) * minFloat(caverns * share, 1.0f);
    float cavern = (cheese - threshold) * (float) field->cheeseScale;

    return caverns <= 0.0f ? (float) field->densitySolid : cavern;
}

// Lakes

// One lake with every setting its chamber and guard read, resolved once for the whole field
static CaveLakeShapeStruct resolveLakeShape(const CaveDensityArgumentStruct *field, const CaveLakeStruct *lake) {

    CaveLakeShapeStruct shape;
    float guard = (float) field->lakeGuardBlocks;

    shape.centerX = lake->centerX;
    shape.centerZ = lake->centerZ;
    shape.surfaceY = lake->waterY + 1.0f;
    shape.radiusX = lake->radiusX;
    shape.radiusZ = lake->radiusZ;
    shape.depth = lake->depth;
    shape.headroom = lake->headroom;
    shape.shoreWobble = (float) field->lakeShoreWobble;
    shape.guard = guard;
    shape.reach = guard + (float) field->lakeTaperBlocks;
    shape.guardPenalty = -(float) field->densitySolid;
    shape.taperPenalty = (float) field->lakeTaperPenaltyBlocks;
    shape.divisionEpsilon = (float) field->divisionEpsilon;

    return shape;
}

static float computeChamber(CaveLakeShapeStruct shape, float pointX, float pointY, float pointZ, float wobble) {

    float above = pointY - shape.surfaceY;
    float axisY = above >= 0.0f ? shape.headroom : shape.depth;
    float extent = length3((pointX - shape.centerX) / shape.radiusX, above / axisY,
                           (pointZ - shape.centerZ) / shape.radiusZ)
                   + wobble * shape.shoreWobble;

    return (1.0f - extent) * minFloat(minFloat(shape.radiusX, shape.radiusZ), axisY);
}

static float computeGuardPenalty(CaveLakeShapeStruct shape, float pointX, float pointY, float pointZ) {

    float below = shape.surfaceY - pointY;
    float dx = pointX - shape.centerX;
    float dz = pointZ - shape.centerZ;
    float guardExtent = length3(dx / (shape.radiusX + shape.guard), below / (shape.depth + shape.guard),
                                dz / (shape.radiusZ + shape.guard));
    float taperExtent = length3(dx / (shape.radiusX + shape.reach), below / (shape.depth + shape.reach),
                                dz / (shape.radiusZ + shape.reach));
    float share = (1.0f - taperExtent) / maxFloat(1.0f - taperExtent / guardExtent, shape.divisionEpsilon);
    float taper = taperExtent >= 1.0f ? 0.0f : shape.taperPenalty * minFloat(share, 1.0f);
    float penalty = guardExtent < 1.0f ? shape.guardPenalty : taper;

    return below <= 0.0f ? 0.0f : penalty;
}

// Band

static int isInsideBand(const CaveDensityArgumentStruct *field, int32_t column, float pointY) {
    return !(pointY < (float) field->columnFloorY[column] || pointY >= (float) field->columnCeilingY[column] + 1.0f);
}

static float computeBandPenalty(const CaveDensityArgumentStruct *field, int32_t column, float pointY) {

    float floorPenalty = maxFloat(0.0f, (float) (field->columnFloorY[column] + field->floorFadeBlocks) - pointY)
                         * (float) field->floorFadeStrength;
    float roofPenalty = maxFloat(0.0f, pointY - (float) (field->columnCeilingY[column] - field->roofTaperBlocks))
                        * (float) field->roofTaperStrength;
    float barrierWeight = field->columnBarrier[column];
    float above = (pointY - (float) field->tideBandMaxY) / (float) field->seaBarrierRiseBlocks;
    float barrier = barrierWeight * (float) field->seaBarrierPenaltyBlocks * (1.0f - clamp01(above));

    return floorPenalty + roofPenalty + (barrierWeight <= 0.0f ? 0.0f : barrier);
}

// Passes

static void fillCaves(const CaveDensityArgumentStruct *restrict field) {

    const float *restrict lattice = field->lattice;
    const float *restrict pointX = field->pointX;
    const float *restrict pointY = field->pointY;
    const float *restrict pointZ = field->pointZ;
    const int32_t *restrict pointColumn = field->pointColumn;
    float *restrict densities = field->densities;
    float *restrict radiusScratch = field->radiusScratch;
    float *restrict lakeScratch = field->lakeScratch;
    uint8_t *restrict lakeWater = field->lakeWater;
    int64_t side = field->latticeSide;
    int64_t step = field->latticeStep;
    int64_t strideX = field->latticeChannels;
    int64_t strideZ = side * strideX;
    int64_t strideY = side * strideZ;
    float stepSize = (float) step;
    float originY = (float) field->latticeOriginY;
    float offsetX = (float) field->latticeOffsetX;
    float offsetZ = (float) field->latticeOffsetZ;
    float solid = (float) field->densitySolid;
    int64_t pointCount = field->pointCount;

    for (int64_t i = 0; i < pointCount; i++) {

        int32_t column = pointColumn[i];
        float localX = pointX[i] - offsetX;
        float localY = pointY[i] - originY;
        float localZ = pointZ[i] - offsetZ;
        int64_t cellX = (int32_t) (localX / stepSize);
        int64_t cellY = (int32_t) (localY / stepSize);
        int64_t cellZ = (int32_t) (localZ / stepSize);

        cellX = cellX < side - 2 ? cellX : side - 2;
        cellY = cellY < field->latticeRows - 2 ? cellY : field->latticeRows - 2;
        cellZ = cellZ < side - 2 ? cellZ : side - 2;

        float weightX = (localX - (float) (int32_t) (cellX * step)) / stepSize;
        float weightY = (localY - (float) (int32_t) (cellY * step)) / stepSize;
        float weightZ = (localZ - (float) (int32_t) (cellZ * step)) / stepSize;
        int64_t corner = ((cellY * side + cellZ) * side + cellX) * strideX;

        float radiusField = sampleCorners(lattice, corner + field->channelSpaghettiRadius, strideX, strideY, strideZ,
                                          weightX, weightY, weightZ);
        float spaghetti = computeSpaghetti(
                field,
                sampleCorners(lattice, corner + field->channelSpaghettiA, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                sampleCorners(lattice, corner + field->channelSpaghettiB, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                radiusField, field->columnTunnels[column]);
        float noodle = computeNoodle(
                field,
                sampleCorners(lattice, corner + field->channelNoodleA, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                sampleCorners(lattice, corner + field->channelNoodleB, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                sampleCorners(lattice, corner + field->channelNoodleGate, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                radiusField, field->columnNoodles[column]);
        float cheese = computeCheese(
                field,
                sampleCorners(lattice, corner + field->channelCheese, strideX, strideY, strideZ, weightX, weightY,
                              weightZ),
                field->columnCaverns[column], (float) field->columnGroundY[column] - pointY[i]);

        densities[i] = maxFloat(maxFloat(spaghetti, noodle), cheese);
        radiusScratch[i] = radiusField;
        lakeScratch[i] = solid;
        lakeWater[i] = 0;
    }
}

static void fillLakes(const CaveDensityArgumentStruct *restrict field) {

    const float *restrict pointX = field->pointX;
    const float *restrict pointY = field->pointY;
    const float *restrict pointZ = field->pointZ;
    const float *restrict radiusScratch = field->radiusScratch;
    float *restrict densities = field->densities;
    float *restrict lakeScratch = field->lakeScratch;
    uint8_t *restrict lakeWater = field->lakeWater;
    int64_t pointCount = field->pointCount;
    int64_t lakeCount = field->lakeCount;

    for (int64_t lakeIndex = 0; lakeIndex < lakeCount; lakeIndex++) {

        CaveLakeShapeStruct shape = resolveLakeShape(field, &field->lakes[lakeIndex]);

        for (int64_t i = 0; i < pointCount; i++) {

            float chamber = computeChamber(shape, pointX[i], pointY[i], pointZ[i], radiusScratch[i]);
            int deeper = chamber > lakeScratch[i];

            densities[i] -= computeGuardPenalty(shape, pointX[i], pointY[i], pointZ[i]);
            lakeWater[i] = deeper ? (uint8_t) (pointY[i] < shape.surfaceY) : lakeWater[i];
            lakeScratch[i] = deeper ? chamber : lakeScratch[i];
        }
    }
}

static void fillResults(const CaveDensityArgumentStruct *restrict field) {

    const float *restrict pointY = field->pointY;
    const int32_t *restrict pointColumn = field->pointColumn;
    const float *restrict lakeScratch = field->lakeScratch;
    float *restrict densities = field->densities;
    uint8_t *restrict lakeWater = field->lakeWater;
    float solid = (float) field->densitySolid;
    int64_t pointCount = field->pointCount;

    for (int64_t i = 0; i < pointCount; i++) {

        int32_t column = pointColumn[i];
        int inside = isInsideBand(field, column, pointY[i]);
        float penalty = computeBandPenalty(field, column, pointY[i]);
        float lake = lakeScratch[i];
        int water = lakeWater[i] && lake - penalty > 0.0f;

        densities[i] = inside ? maxFloat(densities[i], lake) - penalty : solid;
        lakeWater[i] = (uint8_t) (inside && water);
    }
}

// Kernel

KERNEL(CaveDensity) {

    const CaveDensityArgumentStruct *field = (const CaveDensityArgumentStruct *) arguments;

    fillCaves(field);
    fillLakes(field);
    fillResults(field);

    return KERNEL_STATUS_SUCCESS;
}
