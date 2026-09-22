package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructurePlanStruct extends StructPackage {

    /*
     * Everything one placement writes into the world, resolved once and
     * then only read: the streets or corridors of a layout (plus its
     * entrance passage, if it has one) and every template stamped by it —
     * the lone template of a plain STRUCTURE, or each building, landmark and
     * fill piece of a SETTLEMENT or DUNGEON. Streets are stamped before
     * templates so a building's doorstep always wins over the street edge.
     * The connection point is where the world road network meets this
     * placement — the outer end of a layout's main street or entrance
     * passage, or just outside a template's front face — stored as an
     * offset from the placement point so it never has to be re-wrapped.
     */

    private final StructurePlacementStruct placement;
    private final ObjectArrayList<RoadPathStruct> streets = new ObjectArrayList<>();
    private final ObjectArrayList<StructureTemplatePieceStruct> pieces = new ObjectArrayList<>();

    private double connectOffsetX;
    private double connectOffsetZ;
    private int connectY;

    public StructurePlanStruct(StructurePlacementStruct placement) {
        this.placement = placement;
    }

    public void addStreet(RoadPathStruct street) {
        streets.add(street);
    }

    public void addPiece(StructureTemplatePieceStruct piece) {
        pieces.add(piece);
    }

    public void setConnection(double offsetX, double offsetZ, int y) {
        this.connectOffsetX = offsetX;
        this.connectOffsetZ = offsetZ;
        this.connectY = y;
    }

    public double getConnectOffsetX() {
        return connectOffsetX;
    }

    public double getConnectOffsetZ() {
        return connectOffsetZ;
    }

    public int getConnectY() {
        return connectY;
    }

    public StructurePlacementStruct getPlacement() {
        return placement;
    }

    public ObjectArrayList<RoadPathStruct> getStreets() {
        return streets;
    }

    public ObjectArrayList<StructureTemplatePieceStruct> getPieces() {
        return pieces;
    }
}
