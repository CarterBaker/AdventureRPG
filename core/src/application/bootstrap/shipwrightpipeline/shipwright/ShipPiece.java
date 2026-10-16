package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import engine.root.EngineSetting;

public enum ShipPiece {

    /*
     * What one octant of a ship drawn at the drafting table is built as. Every
     * piece is laid in the octant grid and merged into as few boxes as its
     * octants allow, then becomes one vehicle part of its own role and
     * texture, named for it, in this order: the planking a hull is shelled in
     * and the weather deck that caps it both keep the sea out, so every layer
     * of a hull drawn narrowing as it rises stays sealed; floors, walls,
     * rails, stairs and the porch are what is walked on and built against.
     * Spars, canvas, rope and fittings rise above the hull's grid and are
     * built as boxes of their own.
     */

    HULL(EngineSetting.SHIP_PART_HULL, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_HULL),
    WEATHER_DECK(EngineSetting.SHIP_PART_WEATHER_DECK, VehiclePartRole.HULL, EngineSetting.SHIP_TEXTURE_DECK),
    DECKS(EngineSetting.SHIP_PART_DECKS, VehiclePartRole.DECK, EngineSetting.SHIP_TEXTURE_DECK),
    WALLS(EngineSetting.SHIP_PART_WALLS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_WALL),
    RAILS(EngineSetting.SHIP_PART_RAILS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_HULL),
    STAIRS(EngineSetting.SHIP_PART_STAIRS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK),
    PORCH(EngineSetting.SHIP_PART_PORCH, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK);

    // Values
    public static final ShipPiece[] VALUES = values();

    // Identity
    private final String partName;
    private final VehiclePartRole role;
    private final String textureName;

    // Constructor \\

    ShipPiece(String partName, VehiclePartRole role, String textureName) {

        // Identity
        this.partName = partName;
        this.role = role;
        this.textureName = textureName;
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

    // True for a piece a level's floor is laid of
    public boolean isFloor() {
        return this == WEATHER_DECK || this == DECKS || this == PORCH;
    }
}
