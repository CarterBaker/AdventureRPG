package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import engine.root.EngineSetting;

public enum ShipPiece {

    /*
     * What one octant of a ship drawn at the drafting table is built as. Every
     * piece is laid in the octant grid and merged into as few boxes as its
     * octants allow, then becomes one vehicle part of its own role and
     * texture, named for it, in this order. The planking a hull is shelled in,
     * its bottom, the weather deck that caps it and the bulwarks round every
     * open deck all keep the sea out, so every layer of the hull stays sealed
     * however its decks flare or narrow; floors, walls, stairs, rails and
     * railings are what is walked on and built against. A lip and a gun
     * platform fill only the bottom of their octant, a sill stepped over and a
     * bed a cannon stands on; rails are built as posts under a handrail rather
     * than merged whole. Spars, canvas, rope and fittings rise above the
     * hull's grid and are built as boxes of their own.
     */

    HULL(EngineSetting.SHIP_PART_HULL, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_HULL, false,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    BOTTOM(EngineSetting.SHIP_PART_BOTTOM, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_DECK, true,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    WEATHER_DECK(EngineSetting.SHIP_PART_WEATHER_DECK, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_DECK, true,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    BULWARK(EngineSetting.SHIP_PART_BULWARK, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_HULL, false,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    DECKS(EngineSetting.SHIP_PART_DECKS, VehiclePartRole.DECK, EngineSetting.SHIP_TEXTURE_DECK, true,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    WALLS(EngineSetting.SHIP_PART_WALLS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_WALL, false,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    STAIRS(EngineSetting.SHIP_PART_STAIRS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK, false,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, false),
    RAILS(EngineSetting.SHIP_PART_RAILS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_HULL, false,
            EngineSetting.SHIP_OCTANT_SUB_VOXELS, true),
    LIPS(EngineSetting.SHIP_PART_LIPS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_HULL, false,
            EngineSetting.SHIP_LIP_SUB_VOXELS, false),
    PLATFORMS(EngineSetting.SHIP_PART_GUN_PLATFORMS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_WALL,
            false, EngineSetting.SHIP_GUN_PLATFORM_SUB_VOXELS, false);

    // Values
    public static final ShipPiece[] VALUES = values();

    // Identity
    private final String partName;
    private final VehiclePartRole role;
    private final String textureName;
    private final boolean floor;

    // Shape — the sub-voxels of its top octant a piece fills from the bottom up, and whether it is built as a railing
    private final int height;
    private final boolean railed;

    // Constructor \\

    ShipPiece(String partName, VehiclePartRole role, String textureName, boolean floor, int height, boolean railed) {

        // Identity
        this.partName = partName;
        this.role = role;
        this.textureName = textureName;
        this.floor = floor;

        // Shape
        this.height = height;
        this.railed = railed;
    }

    // The value this piece is stored as in an octant grid, zero being empty
    public byte toCell() {
        return (byte) (ordinal() + 1);
    }

    public static ShipPiece fromCell(byte cell) {
        return VALUES[cell - 1];
    }

    // Accessible \\

    public String getPartName() {
        return partName;
    }

    public VehiclePartRole getRole() {
        return role;
    }

    public String getTextureName() {
        return textureName;
    }

    // True for a piece a deck's floor is laid of, that things stand on
    public boolean isFloor() {
        return floor;
    }

    // The sub-voxels of the top octant of a merged box the piece keeps, from its bottom up
    public int getHeight() {
        return height;
    }

    // True for a piece built as posts under a handrail instead of merged whole
    public boolean isRailed() {
        return railed;
    }

    // True for a piece a door or a window may be cut through
    public boolean isCuttable() {
        return this == HULL || this == BULWARK || this == WALLS;
    }
}
