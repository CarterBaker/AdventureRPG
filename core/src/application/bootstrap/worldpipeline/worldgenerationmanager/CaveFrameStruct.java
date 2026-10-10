package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

class CaveFrameStruct extends StructPackage {

    /*
     * Where cave density is read from: a filled cave lattice, its size, the
     * world height of its first row and where its first point sits within the
     * frame's own horizontal coordinates, plus the underground lakes reaching
     * the frame in those same coordinates. A chunk's frame is the chunk
     * itself; a single column's frame starts at the lattice point at or below
     * it. Every density read also reports whether the point is lake water.
     */

    // Lattice
    float[] lattice;
    int side;
    int rows;
    int originY;
    float latticeOffsetX;
    float latticeOffsetZ;

    // Lakes
    final CaveLakeSiteStruct[] lakeSites;
    int lakeSiteCount;

    // Result — whether the last density read lay in lake water
    boolean lakeWater;

    CaveFrameStruct() {

        this.lakeSites = new CaveLakeSiteStruct[EngineSetting.CAVE_LAKE_SITE_MAX];

        for (int i = 0; i < lakeSites.length; i++)
            this.lakeSites[i] = new CaveLakeSiteStruct();
    }

    void setLattice(float[] lattice, int side, int rows, int originY, float latticeOffsetX, float latticeOffsetZ) {

        this.lattice = lattice;
        this.side = side;
        this.rows = rows;
        this.originY = originY;
        this.latticeOffsetX = latticeOffsetX;
        this.latticeOffsetZ = latticeOffsetZ;
    }

    // The next free lake site, null once the frame holds as many as it can
    CaveLakeSiteStruct addLakeSite() {

        if (lakeSiteCount >= lakeSites.length)
            return null;

        return lakeSites[lakeSiteCount++];
    }

    void clearLakeSites() {
        lakeSiteCount = 0;
    }
}
