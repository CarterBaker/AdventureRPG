package application.bootstrap.shipwrightpipeline.util;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ShipBlueprintArpgUtility extends EngineUtility {

    /*
     * The one reader and writer of vehicle files, shared by the loader at boot
     * and the drafting table's library while the game runs. A vehicle file
     * keeps its display name, the "category" it is built as and its "decks"
     * from the bottom up. Each deck keeps its
     * "outline": the octants it reaches out from the centre line at every
     * octant along the sheet from its "start", aft to fore; the "waists" cut
     * through its floor; and everything
     * standing on it, each at [x, z] across the full breadth, or [x, z, spin]
     * where it faces a way: "walls", "half_walls", "doors", "windows",
     * "hatches", "stairs", "ladders" and every piece of "furniture" naming its
     * furnishing table and
     * its "cell". The "helm" and "capstan" are one [deck, x, z] each, empty
     * for none, the "porch" the deck it hangs at, SHIP_DECK_NONE for none, and
     * every mast keeps only its "x", everything else about it following from
     * the hull. findProblem() says what keeps a file from
     * being read, so a ship in the assets fails loudly and a ship in a
     * player's saves is skipped.
     */

    // Problem \\

    // What keeps a ship file from being read, SHIP_PROBLEM_NONE when it is sound
    public static String findProblem(ArpgObjectStruct arpg) {

        if (!ArpgUtility.hasArray(arpg, "decks"))
            return "it lists no decks";

        ArpgArrayStruct decksArpg = arpg.getAsArray("decks");

        if (decksArpg.size() < 1 || decksArpg.size() > EngineSetting.SHIP_MAX_DECKS)
            return "it lists " + decksArpg.size() + " decks, outside the 1 to " + EngineSetting.SHIP_MAX_DECKS
                    + " a ship may have";

        for (ArpgElementStruct deckArpg : decksArpg) {

            String deckProblem = deckArpg.isObject()
                    ? findDeckProblem(deckArpg.getAsObject())
                    : "a deck is not an object";

            if (!deckProblem.isEmpty())
                return deckProblem;
        }

        int deckCount = decksArpg.size();

        if (hasCell(arpg, "helm") && !isCellSound(arpg.getAsArray("helm"), deckCount)
                || hasCell(arpg, "capstan") && !isCellSound(arpg.getAsArray("capstan"), deckCount))
            return "its helm or capstan stands off its sheet";

        if (ArpgUtility.hasNumber(arpg, "porch") && arpg.get("porch").getAsInt() != EngineSetting.SHIP_DECK_NONE
                && !isWithin(arpg.get("porch").getAsInt(), 0, deckCount - 1))
            return "its porch hangs at a deck it does not have";

        if (ArpgUtility.hasArray(arpg, "masts")) {

            ArpgArrayStruct mastsArpg = arpg.getAsArray("masts");

            if (mastsArpg.size() > EngineSetting.SHIP_MAX_MASTS)
                return "it steps more than " + EngineSetting.SHIP_MAX_MASTS + " masts";

            for (ArpgElementStruct mastArpg : mastsArpg)
                if (!mastArpg.isObject()
                        || !ArpgUtility.hasNumber(mastArpg.getAsObject(), "x")
                        || !isWithin(mastArpg.getAsObject().get("x").getAsInt(), 0,
                                EngineSetting.SHIP_SHEET_LENGTH - EngineSetting.SHIP_MAST_OCTANTS))
                    return "a mast stands off its sheet";
        }

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    private static String findDeckProblem(ArpgObjectStruct deckArpg) {

        if (!ArpgUtility.hasNumber(deckArpg, "start") || !ArpgUtility.hasArray(deckArpg, "outline"))
            return "a deck does not give its start and outline";

        int start = deckArpg.get("start").getAsInt();
        ArpgArrayStruct outlineArpg = deckArpg.getAsArray("outline");

        if (start < 0 || start + outlineArpg.size() > EngineSetting.SHIP_SHEET_LENGTH)
            return "a deck's outline runs off its sheet";

        for (ArpgElementStruct halfBeam : outlineArpg)
            if (!halfBeam.isValue() || !halfBeam.getAsValue().isNumber()
                    || !isWithin(halfBeam.getAsInt(), 0, EngineSetting.SHIP_SHEET_HALF_BREADTH))
                return "a deck's outline reaches wider than its sheet";

        if (!areCellsSound(deckArpg, "waists", EngineSetting.SHIP_PLACE_FIELDS)
                || !areCellsSound(deckArpg, "walls", EngineSetting.SHIP_PLACE_FIELDS)
                || !areCellsSound(deckArpg, "half_walls", EngineSetting.SHIP_PLACE_FIELDS)
                || !areCellsSound(deckArpg, "doors", EngineSetting.SHIP_PLACE_FIELDS + 1)
                || !areCellsSound(deckArpg, "windows", EngineSetting.SHIP_PLACE_FIELDS + 1)
                || !areCellsSound(deckArpg, "hatches", EngineSetting.SHIP_PLACE_FIELDS + 1)
                || !areCellsSound(deckArpg, "stairs", EngineSetting.SHIP_PLACE_FIELDS + 1)
                || !areCellsSound(deckArpg, "ladders", EngineSetting.SHIP_PLACE_FIELDS + 1))
            return "its waist, a wall, door, window, hatch, stair or ladder stands off its sheet";

        if (ArpgUtility.hasArray(deckArpg, "furniture"))
            for (ArpgElementStruct pieceArpg : deckArpg.getAsArray("furniture"))
                if (!pieceArpg.isObject()
                        || !ArpgUtility.hasString(pieceArpg.getAsObject(), "furnishing")
                        || !ArpgUtility.hasArray(pieceArpg.getAsObject(), "cell")
                        || !isPlaceSound(pieceArpg.getAsObject().getAsArray("cell"),
                                EngineSetting.SHIP_PLACE_FIELDS + 1))
                    return "a piece of its furniture names no furnishing or stands off its sheet";

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    private static boolean areCellsSound(ArpgObjectStruct deckArpg, String key, int fields) {

        if (!ArpgUtility.hasArray(deckArpg, key))
            return true;

        for (ArpgElementStruct cellArpg : deckArpg.getAsArray(key))
            if (!cellArpg.isArray() || !isPlaceSound(cellArpg.getAsArray(), fields))
                return false;

        return true;
    }

    // An [x, z] or [x, z, spin] place on a deck's sheet
    private static boolean isPlaceSound(ArpgArrayStruct placeArpg, int fields) {

        if (placeArpg.size() != fields || !areNumbers(placeArpg))
            return false;

        return isWithin(placeArpg.get(0).getAsInt(), 0, EngineSetting.SHIP_SHEET_LENGTH - 1)
                && isWithin(placeArpg.get(1).getAsInt(), 0, EngineSetting.SHIP_SHEET_BREADTH - 1)
                && (fields == EngineSetting.SHIP_PLACE_FIELDS
                        || isWithin(placeArpg.get(2).getAsInt(), 0, EngineSetting.ENCODED_FACE_SPIN_COUNT - 1));
    }

    // A [deck, x, z] cell on one of the ship's decks
    private static boolean isCellSound(ArpgArrayStruct cellArpg, int deckCount) {
        return cellArpg.size() == EngineSetting.SHIP_PLACE_FIELDS + 1 && areNumbers(cellArpg)
                && isWithin(cellArpg.get(0).getAsInt(), 0, deckCount - 1)
                && isWithin(cellArpg.get(1).getAsInt(), 0, EngineSetting.SHIP_SHEET_LENGTH - 1)
                && isWithin(cellArpg.get(2).getAsInt(), 0, EngineSetting.SHIP_SHEET_BREADTH - 1);
    }

    private static boolean areNumbers(ArpgArrayStruct valuesArpg) {

        for (int i = 0; i < valuesArpg.size(); i++)
            if (!valuesArpg.get(i).isValue() || !valuesArpg.get(i).getAsValue().isNumber())
                return false;

        return true;
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
            throwException("Vehicle '" + shipName + "' cannot be read: " + problem + ".");

        return parse(arpg, shipName);
    }

    // A sound ship file as a blueprint; findProblem() must have found nothing wrong with it
    public static ShipBlueprintStruct parse(ArpgObjectStruct arpg, String shipName) {

        ShipBlueprintStruct blueprint = new ShipBlueprintStruct();
        ArpgArrayStruct decksArpg = arpg.getAsArray("decks");

        blueprint.setShipName(shipName);
        blueprint.setDisplayName(ArpgUtility.getString(arpg, "display_name", shipName));
        blueprint.setCategory(ArpgUtility.getEnum(arpg, "category", VehicleCategory.class, VehicleCategory.SHIP));
        blueprint.setDeckCount(decksArpg.size());

        for (int deck = 0; deck < decksArpg.size(); deck++)
            parseDeck(decksArpg.get(deck).getAsObject(), deck, blueprint);

        if (hasCell(arpg, "helm"))
            blueprint.setHelm(parseCell(arpg.getAsArray("helm")));

        if (hasCell(arpg, "capstan"))
            blueprint.setCapstan(parseCell(arpg.getAsArray("capstan")));

        blueprint.setPorchDeck(ArpgUtility.getInt(arpg, "porch", EngineSetting.SHIP_DECK_NONE));

        if (ArpgUtility.hasArray(arpg, "masts"))
            for (ArpgElementStruct mastArpg : arpg.getAsArray("masts"))
                blueprint.addMast(new ShipMastStruct(mastArpg.getAsObject().get("x").getAsInt()));

        return blueprint;
    }

    private static void parseDeck(ArpgObjectStruct deckArpg, int deck, ShipBlueprintStruct blueprint) {

        int start = deckArpg.get("start").getAsInt();
        ArpgArrayStruct outlineArpg = deckArpg.getAsArray("outline");

        for (int i = 0; i < outlineArpg.size(); i++)
            blueprint.setOutline(deck, start + i, outlineArpg.get(i).getAsInt());

        parseWaists(deckArpg, deck, blueprint);
        parseWalls(deckArpg, "walls", deck, false, blueprint);
        parseWalls(deckArpg, "half_walls", deck, true, blueprint);
        parsePlaces(deckArpg, "doors", deck, blueprint.getDoors());
        parsePlaces(deckArpg, "windows", deck, blueprint.getWindows());
        parsePlaces(deckArpg, "hatches", deck, blueprint.getHatches());
        parsePlaces(deckArpg, "stairs", deck, blueprint.getStairs());
        parsePlaces(deckArpg, "ladders", deck, blueprint.getLadders());

        if (ArpgUtility.hasArray(deckArpg, "furniture"))
            for (ArpgElementStruct pieceArpg : deckArpg.getAsArray("furniture"))
                blueprint.getFurniture().add(new ShipFurnitureStruct(
                        pieceArpg.getAsObject().get("furnishing").getAsString(),
                        parsePlace(pieceArpg.getAsObject().getAsArray("cell"), deck)));
    }

    private static void parseWaists(ArpgObjectStruct deckArpg, int deck, ShipBlueprintStruct blueprint) {

        if (!ArpgUtility.hasArray(deckArpg, "waists"))
            return;

        for (ArpgElementStruct placeArpg : deckArpg.getAsArray("waists"))
            blueprint.setWaist(deck, placeArpg.getAsArray().get(0).getAsInt(),
                    placeArpg.getAsArray().get(1).getAsInt(), true);
    }

    private static void parseWalls(
            ArpgObjectStruct deckArpg,
            String key,
            int deck,
            boolean half,
            ShipBlueprintStruct blueprint) {

        if (!ArpgUtility.hasArray(deckArpg, key))
            return;

        for (ArpgElementStruct placeArpg : deckArpg.getAsArray(key))
            blueprint.setWall(deck, placeArpg.getAsArray().get(0).getAsInt(),
                    placeArpg.getAsArray().get(1).getAsInt(), true, half);
    }

    private static void parsePlaces(ArpgObjectStruct deckArpg, String key, int deck, IntArrayList cells) {

        if (!ArpgUtility.hasArray(deckArpg, key))
            return;

        for (ArpgElementStruct placeArpg : deckArpg.getAsArray(key)) {

            int cell = parsePlace(placeArpg.getAsArray(), deck);

            if (ShipBlueprintStruct.findPlacement(cells, cell) == EngineSetting.INDEX_NOT_FOUND)
                ShipBlueprintStruct.togglePlacement(cells, cell);
        }
    }

    private static int parsePlace(ArpgArrayStruct placeArpg, int deck) {
        return ShipCellUtility.pack(deck, placeArpg.get(0).getAsInt(), placeArpg.get(1).getAsInt(),
                placeArpg.size() > EngineSetting.SHIP_PLACE_FIELDS ? placeArpg.get(2).getAsInt() : 0);
    }

    private static int parseCell(ArpgArrayStruct cellArpg) {
        return ShipCellUtility.pack(cellArpg.get(0).getAsInt(), cellArpg.get(1).getAsInt(),
                cellArpg.get(2).getAsInt(), 0);
    }

    // Write \\

    public static ArpgObjectStruct write(ShipBlueprintStruct blueprint) {

        ArpgObjectStruct arpg = new ArpgObjectStruct();
        ArpgArrayStruct decksArpg = new ArpgArrayStruct();

        for (int deck = 0; deck < blueprint.getDeckCount(); deck++)
            decksArpg.add(writeDeck(blueprint, deck));

        arpg.addProperty("display_name", blueprint.getDisplayName());
        arpg.addProperty("category", ArpgUtility.toEnumName(blueprint.getCategory()));
        arpg.add("decks", decksArpg);
        arpg.add("helm", writeCell(blueprint.getHelm()));
        arpg.add("capstan", writeCell(blueprint.getCapstan()));
        arpg.addProperty("porch", blueprint.getPorchDeck());
        arpg.add("masts", writeMasts(blueprint));

        return arpg;
    }

    private static ArpgObjectStruct writeDeck(ShipBlueprintStruct blueprint, int deck) {

        ArpgObjectStruct deckArpg = new ArpgObjectStruct();
        ArpgArrayStruct outlineArpg = new ArpgArrayStruct();
        int start = 0;
        int end = 0;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            if (blueprint.getOutline(deck, x) > 0) {
                start = end == 0 ? x : start;
                end = x + 1;
            }

        for (int x = start; x < end; x++)
            outlineArpg.add(blueprint.getOutline(deck, x));

        deckArpg.addProperty("start", start);
        deckArpg.add("outline", outlineArpg);
        deckArpg.add("waists", writePlaces(blueprint.getWaists(), deck, false));
        deckArpg.add("walls", writePlaces(blueprint.getWalls(), deck, false));
        deckArpg.add("half_walls", writePlaces(blueprint.getHalfWalls(), deck, false));
        deckArpg.add("doors", writePlaces(blueprint.getDoors(), deck, true));
        deckArpg.add("windows", writePlaces(blueprint.getWindows(), deck, true));
        deckArpg.add("hatches", writePlaces(blueprint.getHatches(), deck, true));
        deckArpg.add("stairs", writePlaces(blueprint.getStairs(), deck, true));
        deckArpg.add("ladders", writePlaces(blueprint.getLadders(), deck, true));
        deckArpg.add("furniture", writeFurniture(blueprint, deck));

        return deckArpg;
    }

    private static ArpgArrayStruct writePlaces(IntArrayList cells, int deck, boolean facing) {

        ArpgArrayStruct placesArpg = new ArpgArrayStruct();

        for (int i = 0; i < cells.size(); i++)
            if (ShipCellUtility.unpackLevel(cells.getInt(i)) == deck)
                placesArpg.add(writePlace(cells.getInt(i), facing));

        return placesArpg;
    }

    private static ArpgArrayStruct writePlace(int cell, boolean facing) {

        ArpgArrayStruct placeArpg = new ArpgArrayStruct();

        placeArpg.add(ShipCellUtility.unpackX(cell));
        placeArpg.add(ShipCellUtility.unpackZ(cell));

        if (facing)
            placeArpg.add(ShipCellUtility.unpackSpin(cell));

        return placeArpg;
    }

    // A [deck, x, z] cell, empty for none
    private static ArpgArrayStruct writeCell(int cell) {

        ArpgArrayStruct cellArpg = new ArpgArrayStruct();

        if (cell == EngineSetting.SHIP_CELL_NONE)
            return cellArpg;

        cellArpg.add(ShipCellUtility.unpackLevel(cell));
        cellArpg.add(ShipCellUtility.unpackX(cell));
        cellArpg.add(ShipCellUtility.unpackZ(cell));

        return cellArpg;
    }

    private static ArpgArrayStruct writeFurniture(ShipBlueprintStruct blueprint, int deck) {

        ArpgArrayStruct furnitureArpg = new ArpgArrayStruct();

        for (int i = 0; i < blueprint.getFurniture().size(); i++) {

            ShipFurnitureStruct piece = blueprint.getFurniture().get(i);

            if (ShipCellUtility.unpackLevel(piece.getCell()) != deck)
                continue;

            ArpgObjectStruct pieceArpg = new ArpgObjectStruct();

            pieceArpg.addProperty("furnishing", piece.getFurnishingName());
            pieceArpg.add("cell", writePlace(piece.getCell(), true));
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
            mastsArpg.add(mastArpg);
        }

        return mastsArpg;
    }
}
