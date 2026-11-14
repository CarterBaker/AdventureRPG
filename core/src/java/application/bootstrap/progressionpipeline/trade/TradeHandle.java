package application.bootstrap.progressionpipeline.trade;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.HandlePackage;

public class TradeHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded trade definition. Registered and owned
     * by TradeManager. Delegates all accessors through TradeData.
     */

    // Internal
    private TradeData tradeData;

    // Constructor \\

    public void constructor(TradeData tradeData) {

        // Internal
        this.tradeData = tradeData;
    }

    // Accessible \\

    public TradeData getTradeData() {
        return tradeData;
    }

    public String getTradeName() {
        return tradeData.getTradeName();
    }

    public short getTradeID() {
        return tradeData.getTradeID();
    }

    public String getDisplayName() {
        return tradeData.getDisplayName();
    }

    public String getDescription() {
        return tradeData.getDescription();
    }

    public String getGroup() {
        return tradeData.getGroup();
    }

    public float getMin() {
        return tradeData.getMin();
    }

    public float getMax() {
        return tradeData.getMax();
    }

    public float getLearnRate() {
        return tradeData.getLearnRate();
    }

    public boolean isDecaying() {
        return tradeData.isDecaying();
    }

    public ValueMapStruct getStatBonuses() {
        return tradeData.getStatBonuses();
    }
}
