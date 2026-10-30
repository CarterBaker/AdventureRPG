package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.io.File;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.util.ShipBlueprintArpgUtility;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgUtility;

class ShipwrightBuilder extends BuilderPackage {

    /*
     * Parses one vehicle ARPG file from the assets into a ShipBlueprintStruct
     * through ShipBlueprintArpgUtility, the one reader of vehicle files; a
     * vehicle in the assets that cannot be read fails at boot.
     */

    // Build \\

    ShipBlueprintStruct build(File file, String shipName) {
        return ShipBlueprintArpgUtility.parseContent(ArpgUtility.loadObject(file), shipName);
    }
}
