#ifndef ARPG_CAVE_DENSITY_UTILITY_H
#define ARPG_CAVE_DENSITY_UTILITY_H

#include <stdint.h>

#include "application/kernel/kernelpipeline/kernelmanager/KernelUtility.h"

/*
 * The cave density field: for every point it is given, the signed distance in
 * blocks from the nearest cave wall, positive inside the hollow, and whether
 * the point lies in a lake's water. CaveCarveBranch writes the argument slots
 * in exactly the order CaveDensityArgumentStruct lists them; every buffer is
 * the caller's and sized on the Java side before the call.
 */

// One lake of the frame, seven floats in this order
typedef struct {
    float centerX;
    float centerZ;
    float waterY;
    float radiusX;
    float radiusZ;
    float depth;
    float headroom;
} CaveLakeStruct;

// One lake with every setting its chamber and guard read, resolved once per field
typedef struct {
    float centerX;
    float centerZ;
    float surfaceY;
    float radiusX;
    float radiusZ;
    float depth;
    float headroom;
    float shoreWobble;
    float guard;
    float reach;
    float guardPenalty;
    float taperPenalty;
    float divisionEpsilon;
} CaveLakeShapeStruct;

typedef struct {

    // Lattice
    const float *lattice;
    int64_t latticeSide;
    int64_t latticeRows;
    int64_t latticeChannels;
    int64_t latticeStep;
    int64_t latticeOriginY;
    double latticeOffsetX;
    double latticeOffsetZ;
    int64_t channelSpaghettiA;
    int64_t channelSpaghettiB;
    int64_t channelSpaghettiRadius;
    int64_t channelCheese;
    int64_t channelNoodleA;
    int64_t channelNoodleB;
    int64_t channelNoodleGate;

    // Lakes
    const CaveLakeStruct *lakes;
    int64_t lakeCount;

    // Columns
    const int32_t *columnFloorY;
    const int32_t *columnCeilingY;
    const int32_t *columnGroundY;
    const float *columnTunnels;
    const float *columnNoodles;
    const float *columnCaverns;
    const float *columnBarrier;

    // Points
    const float *pointX;
    const float *pointY;
    const float *pointZ;
    const int32_t *pointColumn;
    int64_t pointCount;

    // Results
    float *densities;
    uint8_t *lakeWater;

    // Scratch
    float *radiusScratch;
    float *lakeScratch;

    // Settings
    double densitySolid;
    double spaghettiScale;
    double spaghettiMinRadius;
    double spaghettiMaxRadius;
    double spaghettiCloseShare;
    double spaghettiAmountFloor;
    double noodleScale;
    double noodleMinRadius;
    double noodleMaxRadius;
    double noodleGateThreshold;
    double noodleGateRange;
    double noodleGateEdge;
    double cheeseScale;
    int64_t cheeseMinDepth;
    int64_t cheeseDepthRamp;
    double cheeseShallowShare;
    double cheeseThresholdMax;
    double cheeseThresholdMin;
    int64_t floorFadeBlocks;
    double floorFadeStrength;
    int64_t roofTaperBlocks;
    double roofTaperStrength;
    int64_t tideBandMaxY;
    int64_t seaBarrierRiseBlocks;
    double seaBarrierPenaltyBlocks;
    double lakeShoreWobble;
    double lakeGuardBlocks;
    double lakeTaperBlocks;
    double lakeTaperPenaltyBlocks;
    double divisionEpsilon;
} CaveDensityArgumentStruct;

KERNEL(CaveDensity);

#endif
