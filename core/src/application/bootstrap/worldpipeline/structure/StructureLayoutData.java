package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureLayoutData extends DataPackage {

    /*
     * The generator parameters shared by SETTLEMENT and DUNGEON. A layout is
     * a main street (or corridor) through the placement point, with branches
     * spawned perpendicular off it and, up to branchDepth levels deep, off
     * each other. Every street side is then walked and lined with child
     * structures facing the street — landmarks first, in declared order and
     * each at most once, so they land nearest the centre; then weighted
     * buildings until the side is full. Afterwards fillAttempts random spots
     * are tried for the weighted fill structures in the space left between
     * streets. Everything stays inside radiusBlocks of the centre. An
     * underground layout with entrance set also cuts a sloped entrance
     * passage from the start of its main street up to the surface, at
     * most entranceMaxLengthBlocks long.
     */

    private final String streetRoadName;
    private final int radiusBlocks;

    private final int mainStreetMinLength;
    private final int mainStreetMaxLength;
    private final int branchMinCount;
    private final int branchMaxCount;
    private final int branchMinLength;
    private final int branchMaxLength;
    private final int branchDepth;

    private final int lotSpacingBlocks;
    private final int lotSetbackBlocks;

    private final ObjectArrayList<String> landmarkNames;
    private final ObjectArrayList<String> buildingNames;
    private final FloatArrayList buildingWeights;
    private final ObjectArrayList<String> fillNames;
    private final FloatArrayList fillWeights;
    private final int fillAttempts;

    private final boolean entrance;
    private final String entranceRoadName;
    private final int entranceMaxLengthBlocks;

    public StructureLayoutData(
            String streetRoadName,
            int radiusBlocks,
            int mainStreetMinLength,
            int mainStreetMaxLength,
            int branchMinCount,
            int branchMaxCount,
            int branchMinLength,
            int branchMaxLength,
            int branchDepth,
            int lotSpacingBlocks,
            int lotSetbackBlocks,
            ObjectArrayList<String> landmarkNames,
            ObjectArrayList<String> buildingNames,
            FloatArrayList buildingWeights,
            ObjectArrayList<String> fillNames,
            FloatArrayList fillWeights,
            int fillAttempts,
            boolean entrance,
            String entranceRoadName,
            int entranceMaxLengthBlocks) {

        this.streetRoadName = streetRoadName;
        this.radiusBlocks = radiusBlocks;
        this.mainStreetMinLength = mainStreetMinLength;
        this.mainStreetMaxLength = mainStreetMaxLength;
        this.branchMinCount = branchMinCount;
        this.branchMaxCount = branchMaxCount;
        this.branchMinLength = branchMinLength;
        this.branchMaxLength = branchMaxLength;
        this.branchDepth = branchDepth;
        this.lotSpacingBlocks = lotSpacingBlocks;
        this.lotSetbackBlocks = lotSetbackBlocks;
        this.landmarkNames = landmarkNames;
        this.buildingNames = buildingNames;
        this.buildingWeights = buildingWeights;
        this.fillNames = fillNames;
        this.fillWeights = fillWeights;
        this.fillAttempts = fillAttempts;
        this.entrance = entrance;
        this.entranceRoadName = entranceRoadName;
        this.entranceMaxLengthBlocks = entranceMaxLengthBlocks;
    }

    public String getStreetRoadName() {
        return streetRoadName;
    }

    public int getRadiusBlocks() {
        return radiusBlocks;
    }

    public int getMainStreetMinLength() {
        return mainStreetMinLength;
    }

    public int getMainStreetMaxLength() {
        return mainStreetMaxLength;
    }

    public int getBranchMinCount() {
        return branchMinCount;
    }

    public int getBranchMaxCount() {
        return branchMaxCount;
    }

    public int getBranchMinLength() {
        return branchMinLength;
    }

    public int getBranchMaxLength() {
        return branchMaxLength;
    }

    public int getBranchDepth() {
        return branchDepth;
    }

    public int getLotSpacingBlocks() {
        return lotSpacingBlocks;
    }

    public int getLotSetbackBlocks() {
        return lotSetbackBlocks;
    }

    public ObjectArrayList<String> getLandmarkNames() {
        return landmarkNames;
    }

    public ObjectArrayList<String> getBuildingNames() {
        return buildingNames;
    }

    public FloatArrayList getBuildingWeights() {
        return buildingWeights;
    }

    public ObjectArrayList<String> getFillNames() {
        return fillNames;
    }

    public FloatArrayList getFillWeights() {
        return fillWeights;
    }

    public int getFillAttempts() {
        return fillAttempts;
    }

    public boolean hasEntrance() {
        return entrance;
    }

    public String getEntranceRoadName() {
        return entranceRoadName;
    }

    public int getEntranceMaxLengthBlocks() {
        return entranceMaxLengthBlocks;
    }
}
