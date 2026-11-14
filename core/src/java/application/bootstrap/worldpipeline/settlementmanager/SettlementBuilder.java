package application.bootstrap.worldpipeline.settlementmanager;

import java.io.File;

import application.bootstrap.worldpipeline.settlement.SettlementData;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import engine.root.BuilderPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class SettlementBuilder extends BuilderPackage {

    /*
     * Parses one settlement ARPG file into a SettlementData through
     * SettlementArpgUtility, the one definition of the format, under the ID
     * SettlementManager assigns its name, and wraps it in a SettlementHandle.
     * Every value is validated at load, so a malformed settlement fails at
     * boot.
     */

    // Internal
    private SettlementManager settlementManager;

    // Base \\

    @Override
    protected void get() {
        this.settlementManager = get(SettlementManager.class);
    }

    // Build \\

    SettlementHandle build(File file, String settlementName) {

        short settlementID = settlementManager.registerSettlementName(settlementName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        SettlementData settlementData;

        try {
            settlementData = SettlementArpgUtility.parse(settlementName, settlementID, arpg);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e.getCause());
        }

        SettlementHandle settlementHandle = create(SettlementHandle.class);
        settlementHandle.constructor(settlementData);

        return settlementHandle;
    }
}
