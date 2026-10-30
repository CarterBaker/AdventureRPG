package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.io.File;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.util.ShipBlueprintArpgUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightLibraryBranch extends BranchPackage {

    /*
     * Reads and writes ship files where a drafting table keeps them: in the
     * game's assets for the editor, every player's to sail, and in the
     * player's saves for a table in the game, shared by every character on
     * the machine. A ship's resource name is its display name run together
     * into one PascalCase word, its file that name. A ship in the assets that
     * cannot be read stops the game, as any broken content does; one in the
     * saves is logged and passed over, and writing one never stops the game.
     * A ship for the assets goes to the asset writer when one is set, which
     * may refuse it with the reason why.
     */

    // Directory
    private File assetRoot;
    private File saveRoot;

    // Writer
    private ShipwrightManager.ShipwrightAssetWriter assetWriter;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.assetRoot = new File(EngineSetting.VEHICLE_PATH);
        this.saveRoot = new File(internal.path, EngineSetting.SAVE_DIRECTORY + EngineSetting.PATH_SEPARATOR
                + EngineSetting.VEHICLE_SAVE_DIRECTORY);
    }

    // Names \\

    // A display name's words run together into one PascalCase word, empty when it holds no letter or digit
    String toShipName(String displayName) {

        StringBuilder shipName = new StringBuilder(displayName.length());
        boolean wordStart = true;

        for (int i = 0; i < displayName.length(); i++) {

            char character = displayName.charAt(i);

            if (!Character.isLetterOrDigit(character)) {
                wordStart = character == ' ' || character == '-' || wordStart;
                continue;
            }

            shipName.append(wordStart ? Character.toUpperCase(character) : character);
            wordStart = false;
        }

        return shipName.toString();
    }

    // Every ship kept in a storage, by resource name, sorted
    ObjectArrayList<String> listShips(ShipwrightStorage storage) {

        File root = getRoot(storage);
        ObjectArrayList<String> shipNames = new ObjectArrayList<>();

        if (!root.isDirectory())
            return shipNames;

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            shipNames.add(FileUtility.getPathWithFileNameWithoutExtension(root, file));

        shipNames.sort(null);

        return shipNames;
    }

    boolean hasShip(ShipwrightStorage storage, String shipName) {
        return resolveFile(storage, shipName).isFile();
    }

    // Load \\

    // A ship read from its storage, null when a ship in the saves cannot be read
    ShipBlueprintStruct load(ShipwrightStorage storage, String shipName) {

        File file = resolveFile(storage, shipName);

        if (storage == ShipwrightStorage.ASSETS)
            return ShipBlueprintArpgUtility.parseContent(ArpgUtility.loadObject(file), shipName);

        ArpgObjectStruct arpg = ArpgUtility.tryLoadObject(file);

        if (arpg == null)
            return null;

        String problem = ShipBlueprintArpgUtility.findProblem(arpg);

        if (!problem.isEmpty()) {
            errorLog("Saved ship '" + shipName + "' was skipped: " + problem + ": " + file.getAbsolutePath());
            return null;
        }

        return ShipBlueprintArpgUtility.parse(arpg, shipName);
    }

    // Save \\

    void setAssetWriter(ShipwrightManager.ShipwrightAssetWriter assetWriter) {
        this.assetWriter = assetWriter;
    }

    // Why the ship could not be written to its storage under its resource name, empty once it is
    String save(ShipwrightStorage storage, ShipBlueprintStruct blueprint) {

        File file = resolveFile(storage, blueprint.getShipName());
        ArpgObjectStruct arpg = ShipBlueprintArpgUtility.write(blueprint);

        if (storage == ShipwrightStorage.ASSETS && assetWriter != null)
            return assetWriter.write(blueprint.getShipName(), arpg);

        if (storage == ShipwrightStorage.ASSETS) {
            ArpgUtility.writeObject(file, arpg);
            return EngineSetting.SHIPWRIGHT_STATUS_NONE;
        }

        return ArpgUtility.tryWriteObject(file, arpg)
                ? EngineSetting.SHIPWRIGHT_STATUS_NONE
                : EngineSetting.SHIPWRIGHT_STATUS_SAVE_FAILED;
    }

    // Utility \\

    private File resolveFile(ShipwrightStorage storage, String shipName) {
        return ArpgUtility.resolveFile(getRoot(storage), shipName);
    }

    private File getRoot(ShipwrightStorage storage) {
        return storage == ShipwrightStorage.ASSETS ? assetRoot : saveRoot;
    }
}
