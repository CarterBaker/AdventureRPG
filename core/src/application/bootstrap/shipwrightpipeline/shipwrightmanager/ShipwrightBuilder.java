package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.io.File;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.util.ShipBlueprintArpgUtility;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgUtility;

class ShipwrightBuilder extends BuilderPackage {

    /*
     * Parses one ship ARPG file from the assets into a ShipBlueprintStruct
     * through ShipBlueprintArpgUtility, the one reader of ship files; a ship
     * in the assets that cannot be read fails at boot.
     */

    // Build \\

    ShipBlueprintStruct build(File file, String shipName) {
        return ShipBlueprintArpgUtility.parseContent(ArpgUtility.loadObject(file), shipName);
    }
}
