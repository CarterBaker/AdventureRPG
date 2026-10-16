package application.bootstrap.progressionpipeline.trademanager;

import java.io.File;

import application.bootstrap.progressionpipeline.trade.TradeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TradeLoader extends LoaderPackage {

    /*
     * Scans the trade ARPG directory and loads every trade into TradeManager.
     * Each trade's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private TradeManager tradeManager;
    private TradeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> tradeName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TRADE_PATH);
        this.tradeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Trade directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TradeBuilder.class);
    }

    @Override
    protected void get() {
        this.tradeManager = get(TradeManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct tradeArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "trades");

        for (int i = 0; i < tradeArray.size(); i++) {
            ArpgObjectStruct tradeArpg = tradeArray.get(i).getAsObject();
            tradeName2File.put(ArpgUtility.validateString(tradeArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<TradeHandle> trades = internalBuilder.build(file);

        for (int i = 0; i < trades.size(); i++)
            tradeManager.addTradeHandle(trades.get(i));
    }

    // On-Demand \\

    void request(String tradeName) {

        File file = tradeName2File.get(tradeName);

        if (file == null)
            throwException("On-demand trade load failed — no file found for: \"" + tradeName + "\"");

        request(file);
    }
}
