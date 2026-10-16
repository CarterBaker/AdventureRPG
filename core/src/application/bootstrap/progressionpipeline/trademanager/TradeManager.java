package application.bootstrap.progressionpipeline.trademanager;

import application.bootstrap.progressionpipeline.trade.TradeHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TradeManager extends ManagerPackage {

    /*
     * Owns the trade palette: the broad crafts a being grows in, from blades
     * to farming to diplomacy. IDs are assigned in registration order, which
     * is also the order trades are listed in, and a name declared twice is
     * rejected. Every trade is resolved in awake(), before any skill, deed or
     * profession names one, so a being's trades can be held in one array
     * indexed by ID.
     */

    // Palette
    private Object2IntOpenHashMap<String> tradeName2TradeID;
    private ObjectArrayList<TradeHandle> tradeID2TradeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.tradeName2TradeID = RegistryUtility.createNameIndex();
        this.tradeID2TradeHandle = RegistryUtility.createPalette();
        create(TradeLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerTradeName(String tradeName) {
        return (short) RegistryUtility.registerID(
                tradeName2TradeID, tradeID2TradeHandle, tradeName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addTradeHandle(TradeHandle tradeHandle) {

        short tradeID = tradeHandle.getTradeID();

        if (tradeID2TradeHandle.get(tradeID) != null)
            throwException("Duplicate trade name: '" + tradeHandle.getTradeName()
                    + "' is declared more than once — every trade name must be unique");

        tradeID2TradeHandle.set(tradeID, tradeHandle);
    }

    // Accessible \\

    public boolean hasTrade(String tradeName) {
        return RegistryUtility.getHandle(tradeName2TradeID, tradeID2TradeHandle, tradeName) != null;
    }

    public short getTradeIDFromTradeName(String tradeName) {

        if (!hasTrade(tradeName) && internalLoader != null)
            ((TradeLoader) internalLoader).request(tradeName);

        if (!hasTrade(tradeName))
            throwException("Trade not found after load: \"" + tradeName + "\"");

        return (short) tradeName2TradeID.getInt(tradeName);
    }

    public TradeHandle getTradeHandleFromTradeID(short tradeID) {

        TradeHandle handle = RegistryUtility.getHandle(tradeID2TradeHandle, tradeID);

        if (handle == null)
            throwException("Trade ID not found: " + tradeID);

        return handle;
    }

    public TradeHandle getTradeHandleFromTradeName(String tradeName) {
        return getTradeHandleFromTradeID(getTradeIDFromTradeName(tradeName));
    }

    // Every trade ID lies below this — the length of an array indexed by trade ID
    public int getTradeIDCount() {
        return tradeID2TradeHandle.size();
    }
}
