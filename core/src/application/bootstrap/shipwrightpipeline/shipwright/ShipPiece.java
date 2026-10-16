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
     * however its decks flare or narrow; floors, walls, stairs, the porch and
     * its rails are what is walked on and built against. Spars, canvas, rope
     * and fittings rise above the hull's grid and are built as boxes of their
     * own.
     */

    HULL(EngineSetting.SHIP_PART_HULL, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_HULL, false),
    BOTTOM(EngineSetting.SHIP_PART_BOTTOM, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_DECK, true),
    WEATHER_DECK(EngineSetting.SHIP_PART_WEATHER_DECK, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_DECK, true),
    BULWARK(EngineSetting.SHIP_PART_BULWARK, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_HULL, false),
    DECKS(EngineSetting.SHIP_PART_DECKS, VehiclePartRole.DECK, EngineSetting.SHIP_TEXTURE_DECK, true),
    WALLS(EngineSetting.SHIP_PART_WALLS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_WALL, false),
    STAIRS(EngineSetting.SHIP_PART_STAIRS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK, false),
    PORCH(EngineSetting.SHIP_PART_PORCH, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK, true),
    RAILS(EngineSetting.SHIP_PART_RAILS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_HULL, false);

    // Values
    public static final ShipPiece[] VALUES = values();

    // Identity
    private final String partName;
    private final VehiclePartRole role;
    private final String textureName;
    private final boolean floor;

    // Constructor \\

    ShipPiece(String partName, VehiclePartRole role, String textureName, boolean floor) {

        // Identity
        this.partName = partName;
        this.role = role;
        this.textureName = textureName;
        this.floor = floor;
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

    // True for a piece a door or a window may be cut through
    public boolean isCuttable() {
        return this == HULL || this == BULWARK || this == WALLS;
    }
}
