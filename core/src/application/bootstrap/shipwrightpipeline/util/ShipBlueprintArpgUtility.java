package application.bootstrap.shipwrightpipeline.util;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ShipBlueprintArpgUtility extends EngineUtility {

    /*
     * The one reader and writer of ship files, shared by the loader at boot
     * and the drafting table's library while the game runs. A ship file keeps
     * its display name, the octants its grid spans along the "length", out
     * from the centre line across the "half_breadth" and up the "height", the
     * design "waterline" layer and the "rails" height. Its "hull" lists every
     * layer from the keel up as a row per octant out from the centre line,
     * each a string of the length's octants, filled or open; "decks" lists the
     * interior floor layers. "walls" list [level, x, z] cells and "doors" and
     * "stairs" [level, x, z, spin] cells across the full breadth; "helm" and
     * "capstan" are one cell each and the stern "porch" a level, all three
     * optional, an empty cell or a porch at SHIP_LEVEL_NONE standing for
     * none. Every piece of "furniture" names its furnishing table and its
     * cell, and every mast its "x", "height", and whether it carries a
     * "crows_nest" and "rigging". findProblem() says what keeps a file from
     * being read, so a ship in the assets fails loudly and a ship in a
     * player's saves is skipped.
     */

    // Problem \\

    // What keeps a ship file from being read, SHIP_PROBLEM_NONE when it is sound
    public static String findProblem(ArpgObjectStruct arpg) {

        if (!ArpgUtility.hasNumber(arpg, "length") || !ArpgUtility.hasNumber(arpg, "half_breadth")
                || !ArpgUtility.hasNumber(arpg, "height") || !ArpgUtility.hasNumber(arpg, "waterline"))
            return "it does not give its length, half-breadth, height and waterline";

        int length = arpg.get("length").getAsInt();
        int halfBreadth = arpg.get("half_breadth").getAsInt();
        int height = arpg.get("height").getAsInt();

        if (!isWithin(length, EngineSetting.SHIP_MIN_LENGTH, EngineSetting.SHIP_MAX_LENGTH)
                || !isWithin(halfBreadth, EngineSetting.SHIP_MIN_HALF_BREADTH, EngineSetting.SHIP_MAX_HALF_BREADTH)
                || !isWithin(height, EngineSetting.SHIP_MIN_HEIGHT, EngineSetting.SHIP_MAX_HEIGHT))
            return "its grid of " + length + " by " + halfBreadth + " by " + height + " octants is outside the "
                    + "sizes a ship may be drawn at";

        if (!isWithin(arpg.get("waterline").getAsInt(), 0, height - 1))
            return "its waterline lies outside its hull's layers";

        if (ArpgUtility.hasNumber(arpg, "rails")
                && !isWithin(arpg.get("rails").getAsInt(), 0, EngineSetting.SHIP_MAX_RAIL_HEIGHT))
            return "its rails are higher than a rail may be";

        if (ArpgUtility.hasNumber(arpg, "porch") && arpg.get("porch").getAsInt() != EngineSetting.SHIP_LEVEL_NONE
                && !isWithin(arpg.get("porch").getAsInt(), 0, height - 1))
            return "its porch hangs at a level outside its hull";

        String hullProblem = findHullProblem(arpg, length, halfBreadth, height);

        if (!hullProblem.isEmpty())
            return hullProblem;

        if (ArpgUtility.hasArray(arpg, "decks"))
            for (ArpgElementStruct deck : arpg.getAsArray("decks"))
                if (!deck.isValue() || !isWithin(deck.getAsInt(), 0, height - 1))
                    return "it lays a deck outside its hull's layers";

        if (!areCellsSound(arpg, "walls", EngineSetting.SHIP_CELL_FIELDS - 1, length, halfBreadth, height)
                || !areCellsSound(arpg, "doors", EngineSetting.SHIP_CELL_FIELDS, length, halfBreadth, height)
                || !areCellsSound(arpg, "stairs", EngineSetting.SHIP_CELL_FIELDS, length, halfBreadth, height))
            return "a wall, door or stair stands off its grid";

        if (hasCell(arpg, "helm")
                && !isCellSound(arpg.getAsArray("helm"), EngineSetting.SHIP_CELL_FIELDS - 1, length, halfBreadth,
                        height)
                || hasCell(arpg, "capstan")
                        && !isCellSound(arpg.getAsArray("capstan"), EngineSetting.SHIP_CELL_FIELDS - 1, length,
                                halfBreadth, height))
            return "its helm or capstan stands off its grid";

        if (ArpgUtility.hasArray(arpg, "furniture"))
            for (ArpgElementStruct pieceArpg : arpg.getAsArray("furniture"))
                if (!pieceArpg.isObject()
                        || !ArpgUtility.hasString(pieceArpg.getAsObject(), "furnishing")
                        || !ArpgUtility.hasArray(pieceArpg.getAsObject(), "cell")
                        || !isCellSound(pieceArpg.getAsObject().getAsArray("cell"), EngineSetting.SHIP_CELL_FIELDS,
                                length, halfBreadth, height))
                    return "a piece of its furniture names no furnishing or stands off its grid";

        if (ArpgUtility.hasArray(arpg, "masts")) {

            ArpgArrayStruct mastsArpg = arpg.getAsArray("masts");

            if (mastsArpg.size() > EngineSetting.SHIP_MAX_MASTS)
                return "it steps more than " + EngineSetting.SHIP_MAX_MASTS + " masts";

            for (ArpgElementStruct mastArpg : mastsArpg)
                if (!mastArpg.isObject()
                        || !ArpgUtility.hasNumber(mastArpg.getAsObject(), "x")
                        || !ArpgUtility.hasNumber(mastArpg.getAsObject(), "height")
                        || !isWithin(mastArpg.getAsObject().get("x").getAsInt(), 0,
                                length - EngineSetting.SHIP_MAST_OCTANTS)
                        || !isWithin(mastArpg.getAsObject().get("height").getAsInt(),
                                EngineSetting.SHIP_MAST_MIN_HEIGHT, EngineSetting.SHIP_MAST_MAX_HEIGHT))
                    return "a mast stands off its grid or rises outside the heights a mast may";
        }

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    private static String findHullProblem(ArpgObjectStruct arpg, int length, int halfBreadth, int height) {

        if (!ArpgUtility.hasArray(arpg, "hull") || arpg.getAsArray("hull").size() != height)
            return "its hull does not list one layer for each of its " + height + " layers";

        for (ArpgElementStruct layerArpg : arpg.getAsArray("hull")) {

            if (!layerArpg.isArray() || layerArpg.getAsArray().size() != halfBreadth)
                return "a layer of its hull does not list one row for each of its " + halfBreadth + " octants out";

            for (ArpgElementStruct rowArpg : layerArpg.getAsArray()) {

                if (!rowArpg.isValue() || !rowArpg.getAsValue().isString())
                    return "a row of its hull is not a string";

                String row = rowArpg.getAsString();

                if (row.length() != length)
                    return "a row of its hull is not " + length + " octants long";

                for (int x = 0; x < row.length(); x++) {

                    char mark = row.charAt(x);

                    if (mark != EngineSetting.SHIP_HULL_FILLED && mark != EngineSetting.SHIP_HULL_OPEN)
                        return "a row of its hull holds '" + mark + "', which is neither '"
                                + EngineSetting.SHIP_HULL_FILLED + "' nor '" + EngineSetting.SHIP_HULL_OPEN + "'";
                }
            }
        }

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    private static boolean areCellsSound(
            ArpgObjectStruct arpg,
            String key,
            int fields,
            int length,
            int halfBreadth,
            int height) {

        if (!ArpgUtility.hasArray(arpg, key))
            return true;

        for (ArpgElementStruct cellArpg : arpg.getAsArray(key))
            if (!cellArpg.isArray() || !isCellSound(cellArpg.getAsArray(), fields, length, halfBreadth, height))
                return false;

        return true;
    }

    private static boolean isCellSound(ArpgArrayStruct cellArpg, int fields, int length, int halfBreadth, int height) {

        if (cellArpg.size() != fields)
            return false;

        for (int i = 0; i < fields; i++)
            if (!cellArpg.get(i).isValue() || !cellArpg.get(i).getAsValue().isNumber())
                return false;

        return isWithin(cellArpg.get(0).getAsInt(), 0, height - 1)
                && isWithin(cellArpg.get(1).getAsInt(), 0, length - 1)
                && isWithin(cellArpg.get(2).getAsInt(), 0, halfBreadth * 2 - 1)
                && (fields < EngineSetting.SHIP_CELL_FIELDS
                        || isWithin(cellArpg.get(3).getAsInt(), 0, EngineSetting.ENCODED_FACE_SPIN_COUNT - 1));
    }

    // True when a single cell is given; an empty one stands for none
    private static boolean hasCell(ArpgObjectStruct arpg, String key) {
        return ArpgUtility.hasArray(arpg, key) && arpg.getAsArray(key).size() > 0;
    }

    private static boolean isWithin(int value, int min, int max) {
        return value >= min && value <= max;
    }

    // Read \\

    // A ship in the game's own content, which stops the game when anything about it is wrong
    public static ShipBlueprintStruct parseContent(ArpgObjectStruct arpg, String shipName) {

        String problem = findProblem(arpg);

        if (!problem.isEmpty())
            throwException("Ship '" + shipName + "' cannot be read: " + problem + ".");

        return parse(arpg, shipName);
    }

    // A sound ship file as a blueprint; findProblem() must have found nothing wrong with it
    public static ShipBlueprintStruct parse(ArpgObjectStruct arpg, String shipName) {

        ShipBlueprintStruct blueprint = new ShipBlueprintStruct(
                ArpgUtility.validateInt(arpg, "length"),
                ArpgUtility.validateInt(arpg, "half_breadth"),
                ArpgUtility.validateInt(arpg, "height"));

        blueprint.setShipName(shipName);
        blueprint.setDisplayName(ArpgUtility.getString(arpg, "display_name", shipName));
        blueprint.setWaterline(ArpgUtility.validateInt(arpg, "waterline"));
        blueprint.setRailHeight(ArpgUtility.getInt(arpg, "rails", 0));
        blueprint.setPorchLevel(ArpgUtility.getInt(arpg, "porch", EngineSetting.SHIP_LEVEL_NONE));

        parseHull(arpg.getAsArray("hull"), blueprint);

        if (ArpgUtility.hasArray(arpg, "decks"))
            for (ArpgElementStruct deck : arpg.getAsArray("decks"))
                if (!blueprint.hasDeck(deck.getAsInt()))
                    blueprint.toggleDeck(deck.getAsInt());

        if (ArpgUtility.hasArray(arpg, "walls"))
            for (ArpgElementStruct cellArpg : arpg.getAsArray("walls"))
                blueprint.setWall(
                        cellArpg.getAsArray().get(0).getAsInt(),
                        cellArpg.getAsArray().get(1).getAsInt(),
                        cellArpg.getAsArray().get(2).getAsInt(),
                        true);

        parseCells(arpg, "doors", blueprint.getDoors());
        parseCells(arpg, "stairs", blueprint.getStairs());

        if (hasCell(arpg, "helm"))
            blueprint.setHelm(parseCell(arpg.getAsArray("helm")));

        if (hasCell(arpg, "capstan"))
            blueprint.setCapstan(parseCell(arpg.getAsArray("capstan")));

        if (ArpgUtility.hasArray(arpg, "furniture"))
            for (ArpgElementStruct pieceArpg : arpg.getAsArray("furniture"))
                blueprint.getFurniture().add(new ShipFurnitureStruct(
                        pieceArpg.getAsObject().get("furnishing").getAsString(),
                        parseCell(pieceArpg.getAsObject().getAsArray("cell"))));

        if (ArpgUtility.hasArray(arpg, "masts"))
            for (ArpgElementStruct mastArpg : arpg.getAsArray("masts"))
                blueprint.addMast(new ShipMastStruct(
                        mastArpg.getAsObject().get("x").getAsInt(),
                        mastArpg.getAsObject().get("height").getAsInt(),
                        ArpgUtility.getBoolean(mastArpg.getAsObject(), "crows_nest", false),
                        ArpgUtility.getBoolean(mastArpg.getAsObject(), "rigging", false)));

        return blueprint;
    }

    private static void parseHull(ArpgArrayStruct hullArpg, ShipBlueprintStruct blueprint) {

        for (int y = 0; y < hullArpg.size(); y++) {

            ArpgArrayStruct layerArpg = hullArpg.get(y).getAsArray();

            for (int halfZ = 0; halfZ < layerArpg.size(); halfZ++) {

                String row = layerArpg.get(halfZ).getAsString();

                for (int x = 0; x < row.length(); x++)
                    blueprint.setHull(x, y, halfZ, row.charAt(x) == EngineSetting.SHIP_HULL_FILLED);
            }
        }
    }

    private static void parseCells(ArpgObjectStruct arpg, String key, IntArrayList cells) {

        if (!ArpgUtility.hasArray(arpg, key))
            return;

        for (ArpgElementStruct cellArpg : arpg.getAsArray(key)) {

            int cell = parseCell(cellArpg.getAsArray());

            if (ShipBlueprintStruct.findPlacement(cells, cell) == EngineSetting.INDEX_NOT_FOUND)
                ShipBlueprintStruct.togglePlacement(cells, cell);
        }
    }

    private static int parseCell(ArpgArrayStruct cellArpg) {
        return ShipCellUtility.pack(
                cellArpg.get(0).getAsInt(),
                cellArpg.get(1).getAsInt(),
                cellArpg.get(2).getAsInt(),
                cellArpg.size() == EngineSetting.SHIP_CELL_FIELDS ? cellArpg.get(3).getAsInt() : 0);
    }

    // Write \\

    public static ArpgObjectStruct write(ShipBlueprintStruct blueprint) {

        ArpgObjectStruct arpg = new ArpgObjectStruct();

        arpg.addProperty("display_name", blueprint.getDisplayName());
        arpg.addProperty("length", blueprint.getLength());
        arpg.addProperty("half_breadth", blueprint.getHalfBreadth());
        arpg.addProperty("height", blueprint.getHeight());
        arpg.addProperty("waterline", blueprint.getWaterline());
        arpg.addProperty("rails", blueprint.getRailHeight());

        if (blueprint.hasPorch())
            arpg.addProperty("porch", blueprint.getPorchLevel());

        arpg.add("hull", writeHull(blueprint));
        arpg.add("decks", writeInts(blueprint.getDecks()));
        arpg.add("walls", writeCells(blueprint.getWalls(), EngineSetting.SHIP_CELL_FIELDS - 1));
        arpg.add("doors", writeCells(blueprint.getDoors(), EngineSetting.SHIP_CELL_FIELDS));
        arpg.add("stairs", writeCells(blueprint.getStairs(), EngineSetting.SHIP_CELL_FIELDS));

        if (blueprint.hasHelm())
            arpg.add("helm", writeCell(blueprint.getHelm(), EngineSetting.SHIP_CELL_FIELDS - 1));

        if (blueprint.hasCapstan())
            arpg.add("capstan", writeCell(blueprint.getCapstan(), EngineSetting.SHIP_CELL_FIELDS - 1));

        arpg.add("furniture", writeFurniture(blueprint));
        arpg.add("masts", writeMasts(blueprint));

        return arpg;
    }

    private static ArpgArrayStruct writeHull(ShipBlueprintStruct blueprint) {

        ArpgArrayStruct hullArpg = new ArpgArrayStruct();
        StringBuilder row = new StringBuilder(blueprint.getLength());

        for (int y = 0; y < blueprint.getHeight(); y++) {

            ArpgArrayStruct layerArpg = new ArpgArrayStruct();

            for (int halfZ = 0; halfZ < blueprint.getHalfBreadth(); halfZ++) {

                row.setLength(0);

                for (int x = 0; x < blueprint.getLength(); x++)
                    row.append(blueprint.isHull(x, y, halfZ)
                            ? EngineSetting.SHIP_HULL_FILLED
                            : EngineSetting.SHIP_HULL_OPEN);

                layerArpg.add(row.toString());
            }

            hullArpg.add(layerArpg);
        }

        return hullArpg;
    }

    private static ArpgArrayStruct writeInts(IntArrayList values) {

        ArpgArrayStruct valuesArpg = new ArpgArrayStruct();

        for (int i = 0; i < values.size(); i++)
            valuesArpg.add(values.getInt(i));

        return valuesArpg;
    }

    private static ArpgArrayStruct writeCells(IntArrayList cells, int fields) {

        ArpgArrayStruct cellsArpg = new ArpgArrayStruct();

        for (int i = 0; i < cells.size(); i++)
            cellsArpg.add(writeCell(cells.getInt(i), fields));

        return cellsArpg;
    }

    private static ArpgArrayStruct writeCell(int cell, int fields) {

        ArpgArrayStruct cellArpg = new ArpgArrayStruct();

        cellArpg.add(ShipCellUtility.unpackLevel(cell));
        cellArpg.add(ShipCellUtility.unpackX(cell));
        cellArpg.add(ShipCellUtility.unpackZ(cell));

        if (fields == EngineSetting.SHIP_CELL_FIELDS)
            cellArpg.add(ShipCellUtility.unpackSpin(cell));

        return cellArpg;
    }

    private static ArpgArrayStruct writeFurniture(ShipBlueprintStruct blueprint) {

        ArpgArrayStruct furnitureArpg = new ArpgArrayStruct();

        for (int i = 0; i < blueprint.getFurniture().size(); i++) {

            ShipFurnitureStruct piece = blueprint.getFurniture().get(i);
            ArpgObjectStruct pieceArpg = new ArpgObjectStruct();

            pieceArpg.addProperty("furnishing", piece.getFurnishingName());
            pieceArpg.add("cell", writeCell(piece.getCell(), EngineSetting.SHIP_CELL_FIELDS));
            furnitureArpg.add(pieceArpg);
        }

        return furnitureArpg;
    }

    private static ArpgArrayStruct writeMasts(ShipBlueprintStruct blueprint) {

        ArpgArrayStruct mastsArpg = new ArpgArrayStruct();

        for (int i = 0; i < blueprint.getMasts().size(); i++) {

            ShipMastStruct mast = blueprint.getMasts().get(i);
            ArpgObjectStruct mastArpg = new ArpgObjectStruct();

            mastArpg.addProperty("x", mast.getX());
            mastArpg.addProperty("height", mast.getHeight());
            mastArpg.addProperty("crows_nest", mast.hasCrowsNest());
            mastArpg.addProperty("rigging", mast.isRigged());
            mastsArpg.add(mastArpg);
        }

        return mastsArpg;
    }
}
